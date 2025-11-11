#!/usr/bin/env bash
set -euo pipefail

# push-images.sh
# Constrói e faz push das imagens do backend e frontend para o registry configurado em .env.prod
# Uso: ./scripts/deploy/push-images.sh [caminho_para_.env.prod]

ENV_FILE="${1:-./.env.prod}"

if [ ! -f "$ENV_FILE" ]; then
  echo "Arquivo de ambiente '$ENV_FILE' não encontrado." >&2
  exit 2
fi

# carrega apenas as variáveis necessárias
set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

BACKEND_IMAGE=${BACKEND_IMAGE:-}
FRONTEND_IMAGE=${FRONTEND_IMAGE:-}

if [ -z "$BACKEND_IMAGE" ] || [ -z "$FRONTEND_IMAGE" ]; then
  echo "As variáveis BACKEND_IMAGE e FRONTEND_IMAGE devem estar definidas em $ENV_FILE" >&2
  exit 3
fi

echo "Preparando para build+push images:"
echo "  BACKEND_IMAGE = $BACKEND_IMAGE"
echo "  FRONTEND_IMAGE = $FRONTEND_IMAGE"

if ! docker info >/dev/null 2>&1; then
  echo "Docker não parece estar rodando ou você não tem permissão. Saindo." >&2
  exit 4
fi


# Build backend
if [ -d backend ]; then
  echo "Construindo imagem do backend: $BACKEND_IMAGE"
  docker build -t "$BACKEND_IMAGE" backend/
else
  echo "Diretório backend/ não encontrado, pulando build do backend." >&2
fi

# Build frontend
if [ -d frontend ]; then
  echo "Construindo imagem do frontend: $FRONTEND_IMAGE"
  docker build -t "$FRONTEND_IMAGE" frontend/
else
  echo "Diretório frontend/ não encontrado, pulando build do frontend." >&2
fi

# Envia as imagens para o registry
echo "Enviando imagens para o registry..."
docker push "$BACKEND_IMAGE"
docker push "$FRONTEND_IMAGE"

echo "Build e push concluídos. Certifique-se de que as imagens existam no registry antes de rodar o compose no servidor."
