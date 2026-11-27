# Clúster Docker Swarm de NexusVital

## Topología

| Nodo | Rol | Qué corre |
|---|---|---|
| `nexus-manager` | manager (Leader) | MySQL (1 réplica, volumen `mysql-data`) |
| `nexus-worker-1` | worker | réplicas de los microservicios Java |
| `nexus-worker-2` | worker | réplicas de los microservicios Java |

Instancias EC2 `t3.small` con Amazon Linux 2023 y Docker 25.x. El stack se llama `nexus`.

Decisiones de diseño (en `docker-compose.yml`):

- **Escalabilidad:** cada microservicio tiene `replicas: 2` y se reescala con `docker service scale`.
- **Disponibilidad:** `restart_policy` en `on-failure` y réplicas repartidas en dos workers.
- **Actualizaciones sin corte:** `update_config` con `order: start-first` y `parallelism: 1`.
- **Aislamiento:** red overlay `nexus-net`; MySQL solo es accesible dentro de ella.
- **Recursos:** límite de 450 MB por tarea Java.
- **Limitación conocida:** MySQL usa volumen local en el manager; si ese nodo cae se pierde el servicio de datos. Mejora posible: Amazon RDS.

## Requisitos de red (Security Group)

Entre los nodos del clúster deben estar abiertos:

| Puerto | Protocolo | Uso |
|---|---|---|
| 2377 | TCP | comunicación del clúster (manager) |
| 7946 | TCP y UDP | descubrimiento entre nodos |
| 4789 | UDP | tráfico de la red overlay |

El puerto 22 solo desde tu IP. Los puertos 8081 a 8085 deben limitarse al origen que corresponda, para que el API Gateway no se pueda saltar.

## 1. Inicializar el manager

En el nodo manager, con el repo clonado:

```bash
chmod +x scripts/*.sh
./scripts/init-manager.sh
```

Imprime los tokens de unión para workers y managers. Usa la IP privada del manager.

## 2. Añadir un nodo worker

En el manager, obtén el token (si lo perdiste): `docker swarm join-token worker`.
En el nodo nuevo:

```bash
./scripts/join-node.sh worker <TOKEN> <IP_PRIVADA_MANAGER>
```

## 3. Añadir un nodo manager

En el manager: `docker swarm join-token manager`. En el nodo nuevo:

```bash
./scripts/join-node.sh manager <TOKEN> <IP_PRIVADA_MANAGER>
```

Usa un número impar de managers (3 o 5) para mantener el quórum de Raft. Con un solo manager no hay alta disponibilidad del plano de control.

## 4. Desplegar los servicios

En el manager:

```bash
export MYSQL_ROOT_PASSWORD='<password>'
export SQS_QUEUE_URL='<url-de-la-cola-sqs>'
./scripts/deploy-stack.sh
```

`docker stack deploy` no lee el archivo `.env`, por eso las variables se exportan antes.

## 5. Verificar y escalar

```bash
docker node ls
docker stack services nexus
docker service ps nexus_ms-usuarios

# escalar hacia arriba o abajo
docker service scale nexus_ms-usuarios=4
docker service scale nexus_ms-usuarios=2
```

## 6. Retirar un nodo

```bash
# en el manager
docker node update --availability drain <NODO>   # reprograma sus tareas
# en el nodo a retirar
docker swarm leave
# en el manager
docker node rm <NODO>
```

## Nota sobre AWS Academy (Learner Lab)

Las IP públicas cambian cuando el laboratorio se reinicia. Dentro del clúster se usan siempre las IP privadas. Tras cada reinicio, actualiza la IP del manager en la integración del API Gateway y en la variable de la Lambda.
