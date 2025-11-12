#!/usr/bin/env bash
# scripts/lib/compose-run.sh
# Utilitário compartilhado para executar docker compose com parsing de opções
# Suporta: --mode (dev|prod), --env-file <arquivo>, --compose-file <arquivo> (repetível)
# Ações: up, down, pull, ps, logs

set -euo pipefail

imprimir_uso() {
  cat <<EOF
Uso: $0 [opções] <acao> [args...]

Opções:
  --mode <dev|prod>         Modo (dev ou prod). Default: dev
  --env-file <arquivo>      Caminho para o arquivo .env a ser usado (padrão depende do modo)
  --compose-file <arquivo>  Caminho para um docker-compose YML (pode repetir). Se não informado, usa o padrão do modo.
  --no-sudo                 Não usar sudo mesmo que o env esteja em /etc ou /opt
  -h, --help                Mostrar esta ajuda

Ações:
  up [args...]              docker compose up [args...]
  down [args...]            docker compose down [args...]
  pull [args...]            docker compose pull [args...]
  ps                       docker compose ps
  logs [serviço]           docker compose logs -f [serviço]

Exemplos:
  $0 --mode dev up --build
  $0 --mode prod --env-file /etc/dcriar/.env.prod --compose-file /opt/dcriar/docker-compose.prod.yml up -d
EOF
}

# Padrões
MODO="dev"
ARQ_ENV=""
COMPOSE_FILES=()
USAR_SUDO_SE_ETC=true

# Parse de argumentos
POSITIONAL=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode)
      MODO="$2"; shift 2;;
    --mode=*)
      MODO="${1#*=}"; shift 1;;
    --env-file)
      ARQ_ENV="$2"; shift 2;;
    --env-file=*)
      ARQ_ENV="${1#*=}"; shift 1;;
    --compose-file)
      COMPOSE_FILES+=("$2"); shift 2;;
    --compose-file=*)
      COMPOSE_FILES+=("${1#*=}"); shift 1;;
    --no-sudo)
      USAR_SUDO_SE_ETC=false; shift;;
    -h|--help)
      imprimir_uso; exit 0;;
    up|down|pull|ps|logs)
      POSITIONAL+=("$1"); shift;
      while [[ $# -gt 0 ]]; do POSITIONAL+=("$1"); shift; done
      break
      ;;
    *)
      POSITIONAL+=("$1"); shift;;
  esac
done

if [ ${#POSITIONAL[@]} -eq 0 ]; then
  echo "Erro: ação não informada." >&2
  imprimir_uso
  exit 2
fi

ACAO="${POSITIONAL[0]}"
ACAO_ARGS=("${POSITIONAL[@]:1}")

# Valores padrão se não informados
if [ -z "$ARQ_ENV" ]; then
  if [ "$MODO" = "prod" ]; then
    ARQ_ENV="/etc/dcriar/.env.prod"
  else
    ARQ_ENV="./.env.dev.local"
  fi
fi

if [ ${#COMPOSE_FILES[@]} -eq 0 ]; then
  if [ "$MODO" = "prod" ]; then
    COMPOSE_FILES=("/opt/dcriar/docker-compose.prod.yml")
  else
    COMPOSE_FILES=("docker-compose.dev.yml" "docker-compose.override.yml")
  fi
fi

# Validações leves
for f in "${COMPOSE_FILES[@]}"; do
  if [ ! -f "$f" ]; then
    echo "Aviso: arquivo compose '$f' não encontrado." >&2
  fi
done

if [ ! -f "$ARQ_ENV" ]; then
  echo "Aviso: arquivo env '$ARQ_ENV' não encontrado ou sem permissão de leitura." >&2
fi

# Monta argumentos do docker compose
COMPOSE_ARGS=()
for f in "${COMPOSE_FILES[@]}"; do
  if [ -f "$f" ]; then
    COMPOSE_ARGS+=("-f" "$f")
  fi
done

# Nome da variável de ambiente que será exportada
if [ "$MODO" = "prod" ]; then
  ENV_VAR_NAME=PROD_ENV_FILE
else
  ENV_VAR_NAME=DEV_ENV_FILE
fi

# Decide se deve prefixar com sudo
USAR_SUDO=false
if $USAR_SUDO_SE_ETC; then
  case "$ARQ_ENV" in
    /etc/*|/opt/*)
      if [ ! -r "$ARQ_ENV" ]; then
        USAR_SUDO=true
      fi
      ;;
  esac
fi

# Executa o docker compose com a variável de ambiente adequada
executar_compose() {
  local env_assignment="$ENV_VAR_NAME=$ARQ_ENV"

  if [ "$USAR_SUDO" = false ]; then
    export "$ENV_VAR_NAME"="$ARQ_ENV"
    case "$ACAO" in
      up)
        echo "Executando: $env_assignment docker compose --env-file $ARQ_ENV ${COMPOSE_ARGS[*]} up ${ACAO_ARGS[*]}"
        docker compose --env-file "$ARQ_ENV" "${COMPOSE_ARGS[@]}" up "${ACAO_ARGS[@]}"
        ;;
      down)
        echo "Executando: $env_assignment docker compose --env-file $ARQ_ENV ${COMPOSE_ARGS[*]} down ${ACAO_ARGS[*]}"
        docker compose --env-file "$ARQ_ENV" "${COMPOSE_ARGS[@]}" down "${ACAO_ARGS[@]}"
        ;;
      pull)
        echo "Executando: $env_assignment docker compose --env-file $ARQ_ENV ${COMPOSE_ARGS[*]} pull ${ACAO_ARGS[*]}"
        docker compose --env-file "$ARQ_ENV" "${COMPOSE_ARGS[@]}" pull "${ACAO_ARGS[@]}"
        ;;
      ps)
        echo "Executando: $env_assignment docker compose --env-file $ARQ_ENV ${COMPOSE_ARGS[*]} ps"
        docker compose --env-file "$ARQ_ENV" "${COMPOSE_ARGS[@]}" ps
        ;;
      logs)
        echo "Executando: $env_assignment docker compose --env-file $ARQ_ENV ${COMPOSE_ARGS[*]} logs -f ${ACAO_ARGS[*]}"
        docker compose --env-file "$ARQ_ENV" "${COMPOSE_ARGS[@]}" logs -f "${ACAO_ARGS[@]}"
        ;;
      *)
        echo "Ação desconhecida: $ACAO" >&2; exit 3
        ;;
    esac
  else
    # Quando precisa usar sudo, prefixamos com 'sudo env VAR=valor' para garantir
    # que a variável de ambiente é passada ao processo root sem depender de sh -c ou similar.
    case "$ACAO" in
      up)
        echo "Executando: sudo $env_assignment docker compose --env-file $ARQ_ENV ${COMPOSE_ARGS[*]} up ${ACAO_ARGS[*]}"
        sudo env "$ENV_VAR_NAME"="$ARQ_ENV" docker compose --env-file "$ARQ_ENV" "${COMPOSE_ARGS[@]}" up "${ACAO_ARGS[@]}"
        ;;
      down)
        echo "Executando: sudo $env_assignment docker compose --env-file $ARQ_ENV ${COMPOSE_ARGS[*]} down ${ACAO_ARGS[*]}"
        sudo env "$ENV_VAR_NAME"="$ARQ_ENV" docker compose --env-file "$ARQ_ENV" "${COMPOSE_ARGS[@]}" down "${ACAO_ARGS[@]}"
        ;;
      pull)
        echo "Executando: sudo $env_assignment docker compose --env-file $ARQ_ENV ${COMPOSE_ARGS[*]} pull ${ACAO_ARGS[*]}"
        sudo env "$ENV_VAR_NAME"="$ARQ_ENV" docker compose --env-file "$ARQ_ENV" "${COMPOSE_ARGS[@]}" pull "${ACAO_ARGS[@]}"
        ;;
      ps)
        echo "Executando: sudo $env_assignment docker compose --env-file $ARQ_ENV ${COMPOSE_ARGS[*]} ps"
        sudo env "$ENV_VAR_NAME"="$ARQ_ENV" docker compose --env-file "$ARQ_ENV" "${COMPOSE_ARGS[@]}" ps
        ;;
      logs)
        echo "Executando: sudo $env_assignment docker compose --env-file $ARQ_ENV ${COMPOSE_ARGS[*]} logs -f ${ACAO_ARGS[*]}"
        sudo env "$ENV_VAR_NAME"="$ARQ_ENV" docker compose --env-file "$ARQ_ENV" "${COMPOSE_ARGS[@]}" logs -f "${ACAO_ARGS[@]}"
        ;;
      *)
        echo "Ação desconhecida: $ACAO" >&2; exit 3
        ;;
    esac
  fi
}

executar_compose
