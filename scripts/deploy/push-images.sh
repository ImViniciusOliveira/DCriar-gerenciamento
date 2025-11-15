#!/usr/bin/env bash
set -euo pipefail

# push-images.sh
# Constrói e faz push das imagens do backend e frontend para o registry configurado em .env.prod

# Define o caminho padrão para o arquivo de ambiente de produção.
ENV_FILE="/etc/dcriar/.env.prod"

if [ ! -f "$ENV_FILE" ]; then
  echo "Erro: Arquivo de ambiente '$ENV_FILE' não encontrado." >&2
  echo "Dica: Rode 'sudo ./scripts/deploy/install-prod-env.sh ./.env.prod' para criá-lo." >&2
  exit 2
fi

# carrega apenas as variáveis necessárias
set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

# Pede a tag ao usuário
read -rp "Digite a tag para as imagens (ex: 1.0.1, latest): " IMAGE_TAG
if [ -z "$IMAGE_TAG" ]; then
  echo "Tag não pode ser vazia. Saindo." >&2
  exit 1
fi

# Monta o nome completo das imagens usando as variáveis do .env.prod
DOCKER_REGISTRY_USER=${DOCKER_REGISTRY_USER:-}
BACKEND_IMAGE_NAME=${BACKEND_IMAGE_NAME:-}
FRONTEND_IMAGE_NAME=${FRONTEND_IMAGE_NAME:-}
BACKEND_IMAGE="${DOCKER_REGISTRY_USER}/${BACKEND_IMAGE_NAME}:${IMAGE_TAG}"
FRONTEND_IMAGE="${DOCKER_REGISTRY_USER}/${FRONTEND_IMAGE_NAME}:${IMAGE_TAG}"

if [ -z "$DOCKER_REGISTRY_USER" ] || [ -z "$BACKEND_IMAGE_NAME" ] || [ -z "$FRONTEND_IMAGE_NAME" ]; then
  echo "As variáveis DOCKER_REGISTRY_USER, BACKEND_IMAGE_NAME e FRONTEND_IMAGE_NAME devem estar definidas em $ENV_FILE" >&2
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

