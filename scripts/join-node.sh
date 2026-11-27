#!/usr/bin/env bash
# Une este nodo al clúster Swarm como worker o como manager.
# Uso: ./scripts/join-node.sh <worker|manager> <TOKEN> <IP_PRIVADA_MANAGER>
# El token se obtiene en el manager con:
#   docker swarm join-token worker    (o manager)
set -euo pipefail

if [ "$#" -ne 3 ]; then
  echo "Uso: $0 <worker|manager> <TOKEN> <IP_PRIVADA_MANAGER>"
  exit 1
fi

ROL="$1"
TOKEN="$2"
MANAGER_IP="$3"

if [ "$ROL" != "worker" ] && [ "$ROL" != "manager" ]; then
  echo "El rol debe ser 'worker' o 'manager'"
  exit 1
fi

if docker info --format '{{.Swarm.LocalNodeState}}' | grep -q active; then
  echo "Este nodo ya pertenece a un Swarm."
  exit 0
fi

docker swarm join --token "$TOKEN" "$MANAGER_IP:2377"
echo "Nodo unido como $ROL. Verifica en el manager con: docker node ls"
