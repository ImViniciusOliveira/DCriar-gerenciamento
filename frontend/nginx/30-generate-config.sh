#!/bin/sh
set -eu

ROOT_DIR="/usr/share/nginx/html"
if [ -d "/usr/share/nginx/html/browser" ]; then
  ROOT_DIR="/usr/share/nginx/html/browser"
fi

SERVER_NAME="${FRONTEND_SERVER_NAME:-_}"
TLS_ENABLED="${FRONTEND_TLS_ENABLED:-false}"
TLS_CERT_PATH="${NGINX_TLS_CERT_PATH:-/etc/nginx/tls/server.crt}"
TLS_KEY_PATH="${NGINX_TLS_KEY_PATH:-/etc/nginx/tls/server.key}"

export NGINX_ROOT_DIR="$ROOT_DIR"
export FRONTEND_SERVER_NAME="$SERVER_NAME"
export NGINX_TLS_CERT_PATH="$TLS_CERT_PATH"
export NGINX_TLS_KEY_PATH="$TLS_KEY_PATH"

mkdir -p /etc/nginx/snippets

envsubst "\${NGINX_ROOT_DIR}" \
  < /opt/dcriar/nginx/app.common.conf.template \
  > /etc/nginx/snippets/app.common.conf

if [ "$TLS_ENABLED" = "true" ]; then
  if [ ! -f "$TLS_CERT_PATH" ] || [ ! -f "$TLS_KEY_PATH" ]; then
    echo "ERRO: TLS habilitado, mas certificado ou chave nao encontrados em '$TLS_CERT_PATH' e '$TLS_KEY_PATH'." >&2
    exit 1
  fi

  envsubst "\${FRONTEND_SERVER_NAME} \${NGINX_TLS_CERT_PATH} \${NGINX_TLS_KEY_PATH}" \
    < /opt/dcriar/nginx/default.https.conf.template \
    > /etc/nginx/conf.d/default.conf
else
  envsubst "\${FRONTEND_SERVER_NAME}" \
    < /opt/dcriar/nginx/default.http.conf.template \
    > /etc/nginx/conf.d/default.conf
fi
