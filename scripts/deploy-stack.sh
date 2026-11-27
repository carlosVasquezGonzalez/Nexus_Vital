#!/usr/bin/env bash
# Despliega (o actualiza) el stack "nexus" en el Swarm. Ejecutar en el MANAGER.
# Variables obligatorias (docker stack deploy NO lee el .env automáticamente):
#   MYSQL_ROOT_PASSWORD, SQS_QUEUE_URL
# Opcional: TAG (por defecto "latest"), STACK (por defecto "nexus")
set -euo pipefail

: "${MYSQL_ROOT_PASSWORD:?Falta MYSQL_ROOT_PASSWORD}"
: "${SQS_QUEUE_URL:?Falta SQS_QUEUE_URL}"
STACK="${STACK:-nexus}"
export TAG="${TAG:-latest}"

docker stack deploy \
  --with-registry-auth \
  --resolve-image always \
  -c docker-compose.yml "$STACK"

echo
docker stack services "$STACK"
echo
echo "Escalar un servicio:  docker service scale ${STACK}_ms-usuarios=3"
echo "Ver distribución:     docker service ps ${STACK}_ms-usuarios"
