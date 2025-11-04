#!/usr/bin/env bash
set -euo pipefail

# deploy-prod-local.sh
# Helper para testes locais de deploy: faz build das imagens, push (opcional), instala os arquivos em /etc/dcriar e /opt/dcriar e sobe o compose.
# USO (safest flow): sudo ./scripts/deploy/deploy-prod-local.sh --no-push

ENV_FILE="${1:-./.env.prod}"
PUSH=true

# parse args
for a in "$@"; do
  case "$a" in
    --no-push) PUSH=false; shift ;;
    --env=*) ENV_FILE="${a#*=}"; shift ;;
    *) shift ;;
  esac
done

if [ ! -f "$ENV_FILE" ]; then
  echo "Arquivo de ambiente '$ENV_FILE' não encontrado." >&2
  exit 2
fi

# load variables for sanity checks (do not export globally)
# shellcheck disable=SC1090
set -a; source "$ENV_FILE"; set +a

# sanity check for image names
if [[ -z "${BACKEND_IMAGE:-}" || -z "${FRONTEND_IMAGE:-}" ]]; then
  echo "AVISO: BACKEND_IMAGE e/ou FRONTEND_IMAGE não estão definidas em $ENV_FILE." >&2
  echo "Verifique se você está usando o arquivo correto (o esqueleto no repositório normalmente contém placeholders)." >&2
fi

# 1) build imagens localmente (e push se PUSH=true)
if [ "$PUSH" = true ]; then
  ./scripts/deploy/push-images.sh "$ENV_FILE"
else
  echo "Skipping build+push step (--no-push).";
fi

# 2) instalar .env e compose em /etc e /opt (requer sudo)
sudo ./scripts/deploy/install-prod-env.sh "$ENV_FILE"
sudo ./scripts/deploy/install-prod-compose.sh ./docker-compose.prod.yml

# 3) puxar imagens no host (opcional)
if [ "$PUSH" = true ]; then
  echo "Pulling images declared in $ENV_FILE on host..."
  sudo PROD_ENV_FILE=/etc/dcriar/.env.prod docker compose -f /opt/dcriar/docker-compose.prod.yml pull
else
  echo "Skipping pull (no-push path); compose will use local images if available.";
fi

# 4) subir serviços
sudo PROD_ENV_FILE=/etc/dcriar/.env.prod docker compose -f /opt/dcriar/docker-compose.prod.yml up -d --pull always

echo "Deploy local (server-like) concluído. Verifique logs com: sudo docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml logs -f backend"
