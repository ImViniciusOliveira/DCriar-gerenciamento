#!/usr/bin/env bash
# scripts/develop/down-dev.sh

set -euo pipefail

# Este script derruba a stack de desenvolvimento e faz uma limpeza local:
# - Para e remove containers Docker que exponham as portas conhecidas de dev
# - Mata PIDs locais que estejam escutando nessas portas (quando seguro)
# - Executa 'docker compose down --remove-orphans' usando o utilitário central

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)
cd "$PROJECT_ROOT" || exit 1

# Função: mata processos ouvindo em uma porta e remove containers Docker que a exponham
kill_port_and_containers() {
  local port=$1
  echo "[down-dev] Verificando porta $port..."

  # 1) Detecta containers Docker que publicam essa porta e remove-os
  if command -v docker >/dev/null 2>&1; then
    mapfile -t containers < <(docker ps --format '{{.ID}} {{.Names}} {{.Ports}}' 2>/dev/null | grep -E ":[0-9]+:${port}|:${port}->" || true)
    if [ ${#containers[@]} -gt 0 ]; then
      echo "[down-dev] Encontrado container(s) Docker expondo a porta $port:"
      for entry in "${containers[@]}"; do
        echo "  $entry"
        cid=$(awk '{print $1}' <<<"$entry") || cid=""
        if [ -n "$cid" ]; then
          echo "[down-dev] Parando container $cid..."
          docker stop "$cid" >/dev/null 2>&1 || sudo docker stop "$cid" >/dev/null 2>&1 || true
          echo "[down-dev] Removendo container $cid..."
          docker rm -f "$cid" >/dev/null 2>&1 || sudo docker rm -f "$cid" >/dev/null 2>&1 || true
        fi
      done
      sleep 1
    fi
  fi

  # 2) Tenta detectar e matar PIDs locais que estejam usando a porta
  local pids=""
  if command -v lsof >/dev/null 2>&1; then
    pids=$(lsof -ti tcp:"$port" 2>/dev/null || true)
  else
    pids=$(ss -ltnp 2>/dev/null | awk -v p=":$port" '$0~p {match($0, /pid=([0-9]+)/, a); if(a[1]) print a[1]}' || true)
  fi

  if [ -n "$pids" ]; then
    echo "[down-dev] Matando processos na porta $port: $pids"
    for pid in $pids; do
      if kill "$pid" 2>/dev/null; then
        echo "[down-dev] kill $pid ok"
      else
        echo "[down-dev] Tentando sudo kill $pid..."
        sudo kill "$pid" 2>/dev/null || true
      fi
    done
    sleep 1
    for pid in $pids; do
      if kill -0 "$pid" 2>/dev/null; then
        echo "[down-dev] Forçando parada PID $pid"
        kill -9 "$pid" 2>/dev/null || sudo kill -9 "$pid" 2>/dev/null || true
      fi
    done
  else
    echo "[down-dev] Porta $port está livre.";
  fi
}

# Portas conhecidas do ambiente de desenvolvimento
DEV_PORTS=(8080 4200 9000 9001 5432)
for p in "${DEV_PORTS[@]}"; do
  kill_port_and_containers "$p"
done

echo "[down-dev] Executando docker compose down (dev)..."
exec ./scripts/lib/compose-run.sh --mode dev down --remove-orphans
