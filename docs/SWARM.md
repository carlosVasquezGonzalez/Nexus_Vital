# Clúster Docker Swarm de NexusVital

## 1. Topología del clúster

La infraestructura está compuesta por tres instancias Amazon EC2 de tipo t3.small, con Amazon Linux 2023 y Docker Engine 25.x.

| Nodo | Rol | Servicios |
|---|---|---|
| nexus-manager | Manager (Leader) | MySQL y administración del clúster |
| nexus-worker-1 | Worker | Réplicas de los microservicios Java |
| nexus-worker-2 | Worker | Réplicas de los microservicios Java |

El stack de Docker Swarm se denomina `nexus`.

## 2. Distribución de servicios

| Servicio | Réplicas | Puerto |
|---|---|---|
| ms-usuarios | 2 | 8081 |
| ms-historial | 2 | 8082 |
| ms-autenticacion | 2 | 8083 |
| ms-documentos | 2 | 8084 |
| ms-busqueda | 2 | 8085 |
| MySQL | 1 | 3306 interno |

La infraestructura cuenta con seis servicios y once réplicas en total.

## 3. Decisiones técnicas

- **Escalabilidad:** cada microservicio dispone de dos réplicas y permite ajustar su cantidad mediante Docker Swarm.
- **Disponibilidad:** las réplicas se distribuyen entre los dos workers y se configura una política de reinicio ante fallos.
- **Actualizaciones progresivas:** se utiliza `update_config` con `order: stop-first` y `parallelism: 1`, reduciendo el consumo simultáneo de recursos durante las actualizaciones.
- **Comunicación interna:** se utiliza la red overlay `nexus-net`.
- **Optimización de recursos:** cada tarea Java dispone de un límite de memoria de 450 MB.
- **Persistencia:** MySQL utiliza un volumen externo denominado `nexus_mysql-recovery-data`.
- **Mantenibilidad:** el despliegue se administra mediante Docker Compose, scripts y GitHub Actions.
- **Trazabilidad:** las imágenes desplegadas utilizan etiquetas asociadas al commit de GitHub.

## 4. Requisitos de red

Los siguientes puertos deben estar habilitados entre los nodos del clúster mediante el Security Group:

| Puerto | Protocolo | Finalidad |
|---|---|---|
| 2377 | TCP | Administración del clúster |
| 7946 | TCP y UDP | Descubrimiento y comunicación entre nodos |
| 4789 | UDP | Comunicación de la red overlay |
| 22 | TCP | Administración mediante SSH |

El acceso SSH debe restringirse a orígenes autorizados.

Los puertos 8081 a 8085 se encuentran habilitados para las pruebas académicas. Como mejora de seguridad, se debe restringir el acceso directo al backend para evitar que se omita API Gateway.

## 5. Inicializar el nodo Manager

Desde el nodo Manager, con el repositorio clonado:

```bash
chmod +x scripts/*.sh
./scripts/init-manager.sh
```

El script inicializa Docker Swarm utilizando la dirección IP privada del Manager y proporciona los tokens de incorporación de nodos.

Para verificar:

```bash
docker node ls
```

## 6. Incorporar nodos Worker

Desde el Manager, obtener el token correspondiente:

```bash
docker swarm join-token worker
```

Desde cada Worker, con los scripts disponibles:

```bash
./scripts/join-node.sh worker <TOKEN> <IP_PRIVADA_MANAGER>
```

Reemplazar los valores indicados por el token generado y la dirección IP privada del Manager.

Verificar desde el Manager:

```bash
docker node ls
```

Los tres nodos deben encontrarse en estado `Ready` y disponibilidad `Active`.

## 7. Incorporar nodos Manager adicionales

Para obtener el token:

```bash
docker swarm join-token manager
```

Desde el nuevo nodo:

```bash
./scripts/join-node.sh manager <TOKEN> <IP_PRIVADA_MANAGER>
```

En un entorno productivo se recomienda utilizar un número impar de managers para mantener el quórum de Raft.

La implementación académica utiliza un único Manager y dos Workers.

## 8. Configuración del almacenamiento persistente

MySQL utiliza la base de datos `nexusvital_db` y un volumen externo definido en `docker-compose.yml`:

```yaml
volumes:
  mysql-data:
    external: true
    name: nexus_mysql-recovery-data
```

Verificar la existencia del volumen desde el Manager:

```bash
docker volume inspect nexus_mysql-recovery-data
```

En una instalación completamente nueva, se debe crear el volumen antes del despliegue:

```bash
docker volume create nexus_mysql-recovery-data
```

Si existe una instalación anterior, se deben conservar o restaurar sus datos y utilizar las credenciales correspondientes.

No eliminar los volúmenes existentes sin realizar previamente un respaldo.

## 9. Desplegar los servicios

Desde el Manager, con el repositorio clonado, configurar las variables de entorno:

```bash
read -rsp "Contraseña MySQL: " MYSQL_ROOT_PASSWORD
echo
export MYSQL_ROOT_PASSWORD

read -rp "URL de la cola SQS: " SQS_QUEUE_URL
export SQS_QUEUE_URL

read -rp "Etiqueta de imágenes publicada: " TAG
export TAG
```

La etiqueta debe corresponder a una versión disponible de los cinco microservicios en Docker Hub.

Ejecutar:

```bash
./scripts/deploy-stack.sh
```

Docker Stack no carga automáticamente las variables de un archivo `.env`, por lo que deben proporcionarse antes del despliegue.

El despliegue también puede realizarse mediante el pipeline de GitHub Actions.

## 10. Verificar los servicios

```bash
docker node ls
docker stack services nexus
docker stack ps nexus
docker service ps nexus_ms-usuarios
```

Resultado esperado:

| Servicio | Estado esperado |
|---|---|
| nexus_ms-usuarios | 2/2 |
| nexus_ms-historial | 2/2 |
| nexus_ms-autenticacion | 2/2 |
| nexus_ms-documentos | 2/2 |
| nexus_ms-busqueda | 2/2 |
| nexus_mysql | 1/1 |

Verificar el estado de salud de los microservicios:

```bash
for puerto in 8081 8082 8083 8084 8085; do
  echo "Puerto $puerto:"
  curl --connect-timeout 5 --max-time 10 -sS \
    -w '\nHTTP %{http_code}\n' \
    "http://127.0.0.1:$puerto/actuator/health"
done
```

El resultado esperado es un estado `UP` y código HTTP 200.

## 11. Escalabilidad de servicios

Durante las pruebas se modificó dinámicamente el número de réplicas del microservicio Usuarios.

Reducir a una réplica:

```bash
docker service scale nexus_ms-usuarios=1
docker service ps nexus_ms-usuarios
```

Verificar disponibilidad:

```bash
curl --max-time 10 http://127.0.0.1:8081/api/usuarios
```

Aumentar nuevamente a dos réplicas:

```bash
docker service scale nexus_ms-usuarios=2
docker service ps nexus_ms-usuarios
docker stack services nexus
```

Verificar nuevamente el funcionamiento:

```bash
curl --max-time 10 http://127.0.0.1:8081/api/usuarios
```

Esta prueba permite demostrar el ajuste dinámico de réplicas y la continuidad del servicio durante el escalado.

El número máximo de réplicas debe ajustarse a los recursos disponibles en las instancias EC2.

## 12. Retirar un nodo Worker

Desde el Manager:

```bash
docker node update --availability drain <NODO>
```

Desde el Worker que se desea retirar:

```bash
docker swarm leave
```

Desde el Manager:

```bash
docker node rm <NODO>
```

Antes de retirar un nodo, verificar que los nodos restantes dispongan de recursos suficientes para ejecutar los servicios.

## 13. Consideraciones de AWS Academy

Al reiniciar el laboratorio, se debe verificar:

- Estado de las instancias EC2.
- Direcciones IP públicas y privadas.
- Vigencia de las credenciales temporales de AWS.
- Dirección del Manager configurada en GitHub Secrets.
- Integraciones de API Gateway.
- Variable HISTORIAL_URL de Lambda.
- Estado del disparador SQS asociado a Lambda.
- Estado de los nodos y servicios Docker Swarm.

Si cambia la dirección pública del Manager, se deben actualizar las configuraciones que dependan de ella.

## 14. Limitaciones y mejoras futuras

- Implementar alta disponibilidad del plano de control mediante managers adicionales.
- Migrar MySQL a un servicio administrado como Amazon RDS.
- Restringir el acceso directo a los microservicios.
- Implementar un mecanismo seguro de acceso SSH para futuros despliegues automatizados.
- Incorporar pruebas de carga y escalabilidad automática basada en métricas.

La configuración actual permite demostrar el funcionamiento de una arquitectura distribuida en un entorno académico.