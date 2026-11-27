#!/usr/bin/env bash
# Inicializa el nodo MANAGER del clúster Docker Swarm.
# Uso (en el nodo manager): ./scripts/init-manager.sh
# Opcional: MANAGER_IP=172.31.0.107 ./scripts/init-manager.sh
set -euo pipefail

# Si no se indica, usa la IP privada de la instancia (metadata de EC2, IMDSv2)
if [ -z "${MANAGER_IP:-}" ]; then
  TOKEN=$(curl -s -X PUT "http://169.254.169.254/latest/api/token" \
    -H "X-aws-ec2-metadata-token-ttl-seconds: 60")
  MANAGER_IP=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/meta-data/local-ipv4)
fi

if docker info --format '{{.Swarm.LocalNodeState}}' | grep -q active; then
  echo "Swarm ya está activo en este nodo."
else
  docker swarm init --advertise-addr "$MANAGER_IP"
fi

echo
echo "== Token para unir WORKERS =="
docker swarm join-token worker
echo
echo "== Token para unir otros MANAGERS =="
docker swarm join-token manager
echo
docker node ls
