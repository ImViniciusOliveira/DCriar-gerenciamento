#!/usr/bin/env bash
# Wrapper "Burro" para Docker Compose
# - Não conhece ambientes (dev/prod)
# - Apenas parseia flags genéricas e repassa para `docker compose`
#   Flags suportadas do wrapper:
#     --project-name <nome>
#     --env-file <arquivo>        (pode repetir)
#     --compose-file <arquivo>    (pode repetir)
#     --no-sudo                   (não usa sudo)
#     -h | --help
# - Qualquer outro argumento é passado como COMANDO do docker compose (ex: up -d, down, pull, ps, logs -f backend)

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
cd "$PROJECT_ROOT" || exit 1

SUDO_CMD="sudo"
PROJECT_NAME_ARG=()
ENV_FILES_ARGS=()
COMPOSE_FILES_ARGS=()
COMMANDS=()

show_help() {
  echo "Uso: $0 [OPÇÕES_WRAPPER] [COMANDOS_DOCKER_COMPOSE]"
  echo ""
  echo "Wrapper 'burro' para o Docker Compose. Repassa argumentos para 'docker compose'."
  echo ""
  echo "Opções do Wrapper:"
  echo "  --project-name <nome>    Passa --project-name para o docker compose"
  echo "  --env-file <arquivo>     Passa --env-file (pode repetir)"
  echo "  --compose-file <arquivo> Passa -f/--file (pode repetir)"
  echo "  --no-sudo                Executa sem 'sudo'"
  echo "  -h, --help               Mostra esta ajuda"
  echo ""
  echo "Exemplo:"
  echo "  $0 --project-name dcriar-dev --env-file ./.env.dev.local --compose-file docker-compose.dev.yml --compose-file docker-compose.override.yml up -d"
}

# Parse dos argumentos do wrapper
while [[ $# -gt 0 ]]; do
  case "$1" in
    --project-name)
      if [[ $# -ge 2 ]]; then PROJECT_NAME_ARG=("--project-name" "$2"); shift 2; else echo "Erro: --project-name requer valor" >&2; exit 1; fi ;;
    --project-name=*)
      PROJECT_NAME_ARG=("--project-name" "${1#*=}"); shift ;;
    --env-file)
      if [[ $# -ge 2 ]]; then ENV_FILES_ARGS+=("--env-file" "$2"); shift 2; else echo "Erro: --env-file requer valor" >&2; exit 1; fi ;;
    --env-file=*)
      ENV_FILES_ARGS+=("--env-file" "${1#*=}"); shift ;;
    --compose-file)
      if [[ $# -ge 2 ]]; then COMPOSE_FILES_ARGS+=("-f" "$2"); shift 2; else echo "Erro: --compose-file requer valor" >&2; exit 1; fi ;;
    --compose-file=*)
      COMPOSE_FILES_ARGS+=("-f" "${1#*=}"); shift ;;
    --no-sudo)
      SUDO_CMD=""; shift ;;
    -h|--help)
      show_help; exit 0 ;;
    *)
      # Qualquer coisa diferente cai como comando para o docker compose
      COMMANDS+=("$1"); shift ;;
  esac
done

if [ ${#COMMANDS[@]} -eq 0 ]; then
  echo "Erro: nenhum comando do docker compose informado (ex: up -d, down, pull, ps, logs)" >&2
  show_help
  exit 2
fi

# Monta linha final
FINAL_CMD=(docker compose)
if [ ${#PROJECT_NAME_ARG[@]} -gt 0 ]; then FINAL_CMD+=("${PROJECT_NAME_ARG[@]}"); fi
if [ ${#ENV_FILES_ARGS[@]} -gt 0 ]; then FINAL_CMD+=("${ENV_FILES_ARGS[@]}"); fi
if [ ${#COMPOSE_FILES_ARGS[@]} -gt 0 ]; then FINAL_CMD+=("${COMPOSE_FILES_ARGS[@]}"); fi
FINAL_CMD+=("${COMMANDS[@]}")

printf 'INFO: Executando comando: %s\n' "${SUDO_CMD:+$SUDO_CMD }${FINAL_CMD[*]}"

if [ -n "$SUDO_CMD" ]; then
  exec sudo "${FINAL_CMD[@]}"
else
  exec "${FINAL_CMD[@]}"
fi

