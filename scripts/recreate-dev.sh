#!/usr/bin/env bash
# scripts/recriar-dev.sh (versão PT-BR atualizada)
# "Botão de Pânico": Força a parada de portas e recria o ambiente de dev.

set -euo pipefail

# --- Configuração ---
NO_START=false
# Este array vai guardar todos os argumentos que não são do 'recriar-dev'
# para repassar ao 'up-dev.sh' (ex: --no-build)
UP_ARGS=()

# --- Funções ---

usage() {
  cat <<EOF
Uso: $0 [opções] [opções_para_up_dev]

Este script é um "reset total". Ele força a parada de processos nas portas de
desenvolvimento (8080, 4200, 5432, 9000, 9001), executa o 'down-dev.sh'
para limpar os containers, e então executa o 'up-dev.sh' para recriar tudo.

Opções do Script:
  --no-start        Não sobe o ambiente no final (apenas limpa/paralisa).
  -h, --help        Mostra esta ajuda.

Opções para 'up-dev.sh':
  Qualquer argumento desconhecido (ex: --no-build) será repassado
  diretamente para o script './scripts/up-dev.sh'.

Exemplos:
  ./scripts/recriar-dev.sh            # Mata portas, 'down', 'up --build' (padrão do up-dev)
  ./scripts/recriar-dev.sh --no-build # Mata portas, 'down', 'up --no-build'
  ./scripts/recriar-dev.sh --no-start # Mata portas, 'down' e para.
EOF
}

# Função: mata processos ouvindo em uma porta
kill_port() {
  local port=$1
  echo "[recriar-dev] Verificando porta $port..."

  # Tenta com lsof (mais comum em macOS e alguns Linux)
  if command -v lsof >/dev/null 2>&1; then
    pids=$(lsof -ti tcp:"$port" || true)
  else
    # Fallback para ss (moderno no Linux)
    pids=$(ss -ltnp 2>/dev/null | awk -v p=":$port" '$0~p {match($0, /pid=([0-9]+)/, a); if(a[1]) print a[1]}' || true)
  fi

  if [ -n "$pids" ]; then
    echo "[recriar-dev] Matando processos na porta $port: $pids"
    # Tenta matar (graceful)
    for pid in $pids; do kill "$pid" 2>/dev/null || true; done
    sleep 1
    # Tenta forçar (kill -9) se ainda estiverem vivos
    for pid in $pids; do
      if kill -0 "$pid" 2>/dev/null; then
        echo "[recriar-dev] Forçando parada (kill -9) do PID $pid"
        kill -9 "$pid" 2>/dev/null || true
      fi
    done
  else
    echo "[recriar-dev] Porta $port está livre."
  fi
}

# --- Processamento de Argumentos ---

# Loop para processar os argumentos
while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-start)
      NO_START=true
      shift # consome o argumento --no-start
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      # Se não for um argumento deste script, guarda para o up-dev.sh
      UP_ARGS+=("$1")
      shift # consome o argumento
      ;;
  esac
done


# --- Execução Principal ---

echo "[recriar-dev] Iniciando reset total do ambiente de desenvolvimento..."

# 1. Matar processos nas portas
# Portas conhecidas do ambiente dev
DEV_PORTS=(8080 4200 9000 9001 5432)
for p in "${DEV_PORTS[@]}"; do
  kill_port "$p"
done

# 2. Chamar o script 'down-dev.sh' (reutilização!)
echo "[recriar-dev] Executando ./scripts/down-dev.sh para limpar containers..."
if [ -x ./scripts/down-dev.sh ]; then
  ./scripts/down-dev.sh
else
  echo "[recriar-dev] AVISO: ./scripts/down-dev.sh não encontrado. Tentando limpeza manual..."
  # Fallback (caso o down-dev.sh não exista, tenta o comando direto)
  docker compose -f docker-compose.dev.yml -f docker-compose.override.yml down --remove-orphans || true
fi

# 3. Chamar o script 'up-dev.sh' (reutilização!)
if [ "$NO_START" = false ]; then
  echo "[recriar-dev] Executando ./scripts/up-dev.sh ${UP_ARGS[*]}..."
  if [ -x ./scripts/up-dev.sh ]; then
    # Passa os argumentos extras (como --no-build)
    ./scripts/up-dev.sh "${UP_ARGS[@]}"
  else
    echo "[recriar-dev] ERRO: ./scripts/up-dev.sh não encontrado. Não é possível subir o ambiente." >&2
    exit 1
  fi
else
  echo "[recriar-dev] Flag --no-start detectada. Ambiente limpo. Não iniciando."
fi

echo "[recriar-dev] Processo de recriação concluído."