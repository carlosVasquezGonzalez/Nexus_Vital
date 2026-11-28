# Pipeline CI/CD - NexusVital

## 1. Objetivo

Automatizar las pruebas, construcción de imágenes Docker, publicación, configuración de servicios AWS, despliegue y verificación de los microservicios de NexusVital.

El pipeline está implementado mediante GitHub Actions.

Archivo de configuración:

`.github/workflows/ci-cd.yml`

## 2. Activación del pipeline

El workflow se ejecuta mediante:

- Push a la rama main.
- Ejecución manual mediante workflow_dispatch.

## 3. Etapas del pipeline

### Etapa 1: Pruebas automatizadas

Se ejecutan pruebas Maven de los cinco microservicios utilizando Java 21 y una instancia temporal de MySQL 8.4.

```bash
mvn -B clean test
```

Las pruebas actuales verifican la carga del contexto de Spring Boot y su configuración.

Los resultados se almacenan como artefactos de GitHub Actions.

### Etapa 2: Construcción y publicación de imágenes

Una vez superadas las pruebas, se construyen las imágenes Docker de los cinco microservicios.

Las imágenes se publican en Docker Hub bajo el usuario `carlos0801`.

Cada imagen recibe dos etiquetas:

- latest.
- Los primeros siete caracteres del identificador del commit.

Esto permite identificar la versión desplegada y facilita la trazabilidad.

### Etapa 3: Aprovisionamiento AWS

El pipeline ejecuta el script:

```bash
bash scripts/provision-aws.sh
```

Este script realiza las siguientes operaciones:

- Validación de credenciales AWS.
- Creación o configuración de la cola principal SQS.
- Creación o configuración de la Dead Letter Queue.
- Configuración de la política de redirección de mensajes.
- Creación o actualización de la función Lambda.
- Configuración del disparador SQS.
- Actualización de las variables de entorno de Lambda.
- Verificación de los requisitos de API Key en API Gateway.
- Despliegue de la etapa prod de API Gateway.

El script reutiliza los recursos existentes cuando corresponde.

Las instancias EC2, el clúster inicial y la creación original de API Gateway requieren configuración previa.

### Etapa 4: Despliegue en Docker Swarm

GitHub Actions establece una conexión SSH con el nodo Manager de AWS.

El archivo `docker-compose.yml` se transfiere al servidor y se ejecuta el despliegue del stack NexusVital.

```bash
docker stack deploy -c docker-compose.yml --prune nexus
```

Las imágenes se despliegan utilizando la etiqueta correspondiente al commit procesado.

El pipeline verifica que los seis servicios alcancen el número de réplicas configurado.

### Etapa 5: Verificación del despliegue

Se ejecutan pruebas de acceso a API Gateway.

| Prueba | Resultado esperado |
|---|---|
| Petición sin API Key | HTTP 403 |
| Petición con API Key válida | HTTP 200 |

Estas pruebas permiten comprobar la disponibilidad del endpoint Usuarios y la protección de la ruta mediante API Key.

Las pruebas completas de los demás microservicios y del flujo E2E se realizan adicionalmente.

## 4. Flujo del pipeline

Push a main → Pruebas Maven → Construcción y publicación de imágenes Docker → Despliegue en Swarm → Verificación.

El aprovisionamiento AWS se ejecuta después de las pruebas, en paralelo con la construcción de imágenes.

El despliegue comienza cuando ambas etapas finalizan correctamente.

## 5. GitHub Secrets

Las credenciales necesarias se administran mediante GitHub Secrets.

| Secret | Finalidad |
|---|---|
| AWS_ACCESS_KEY_ID | Identificador de acceso AWS |
| AWS_SECRET_ACCESS_KEY | Clave secreta AWS |
| AWS_SESSION_TOKEN | Token temporal de AWS Academy |
| DOCKERHUB_TOKEN | Autenticación en Docker Hub |
| MANAGER_HOST | Dirección pública del nodo Manager |
| MYSQL_ROOT_PASSWORD | Contraseña de MySQL |
| SSH_PRIVATE_KEY | Clave privada para conexión SSH |

Los valores de estas variables no deben incluirse en el repositorio ni en las evidencias de la evaluación.

## 6. Evidencia de ejecución exitosa

Se validó el funcionamiento del pipeline mediante GitHub Actions.

| Elemento | Resultado |
|---|---|
| Workflow | CI/CD NexusVital |
| Ejecución | Número 7, segundo intento |
| Commit | 12b6d54 |
| Estado | Success |
| Duración | 2 minutos y 59 segundos |
| Trabajos completados | 13 |
| Artefactos generados | 10 |

Posteriormente, se verificó directamente desde el nodo Manager que los servicios se encontraban operativos.

| Servicio | Réplicas |
|---|---|
| nexus_ms-usuarios | 2/2 |
| nexus_ms-historial | 2/2 |
| nexus_ms-autenticacion | 2/2 |
| nexus_ms-documentos | 2/2 |
| nexus_ms-busqueda | 2/2 |
| nexus_mysql | 1/1 |

Total: 11 réplicas operativas.

Los cinco microservicios respondieron correctamente a sus endpoints de salud y consultas funcionales, obteniendo códigos HTTP 200.

## 7. Validación de integración posterior al despliegue

Después de finalizar exitosamente el pipeline, se realizó una prueba de extremo a extremo.

Se registró el documento de prueba `prueba_final_cicd.pdf` mediante API Gateway.

El microservicio Documentos publicó el evento correspondiente en Amazon SQS, que fue procesado por AWS Lambda.

La función Lambda realizó una petición al microservicio Historial, generando automáticamente un nuevo registro.

La ejecución se verificó mediante:

- Respuesta HTTP 200 al registrar el documento.
- Consulta del historial generado.
- Registros de ejecución en Amazon CloudWatch.
- Confirmación del disparador SQS habilitado.

## 8. Manejo de errores

Durante las pruebas del pipeline se identificaron y solucionaron problemas relacionados con:

- Compilación del microservicio Documentos.
- Autenticación en Docker Hub.
- Vigencia de credenciales temporales de AWS.
- Formato de la clave privada SSH.
- Incompatibilidad entre las credenciales de MySQL y su volumen persistente.

La recuperación de MySQL se realizó mediante un respaldo y un volumen independiente, conservando los datos anteriores.

Después de solucionar estos problemas, el pipeline finalizó correctamente y se verificó nuevamente la integración completa.

## 9. Limitaciones y mejoras futuras

- Ampliar las pruebas automatizadas para cubrir la lógica de negocio.
- Incorporar pruebas E2E automatizadas dentro del pipeline.
- Automatizar la creación inicial de toda la infraestructura cloud.
- Incorporar verificaciones adicionales de salud durante el despliegue.
- Implementar un mecanismo seguro de conexión entre GitHub Actions y el Manager.
- Actualizar las credenciales temporales cuando AWS Academy lo requiera.

Durante las pruebas se habilitó temporalmente el acceso SSH necesario para GitHub Actions. Esta regla fue eliminada posteriormente por seguridad.

Para ejecutar nuevos despliegues será necesario disponer de un mecanismo de acceso SSH autorizado.

## 10. Conclusión

El pipeline implementado permite automatizar las principales etapas de integración y despliegue de NexusVital, desde la ejecución de pruebas hasta la publicación de imágenes y actualización de los servicios distribuidos en AWS.

Su funcionamiento fue comprobado mediante una ejecución exitosa, la verificación de los microservicios desplegados y una prueba de integración de extremo a extremo.