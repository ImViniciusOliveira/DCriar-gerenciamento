#!/usr/bin/env python3
"""minio-init: espera o MinIO ficar acessível, cria bucket e aplica política."""
import os
import sys
import time
import subprocess

MINIO_HOST = os.environ.get("MINIO_HOST", "minio-dev")
MINIO_PORT = os.environ.get("MINIO_PORT", "9000")
MINIO_ROOT_USER = os.environ.get("MINIO_ROOT_USER")
MINIO_ROOT_PASSWORD = os.environ.get("MINIO_ROOT_PASSWORD")
MINIO_BUCKET_NAME = os.environ.get("MINIO_BUCKET_NAME")
WAIT_TIMEOUT = int(os.environ.get("WAIT_TIMEOUT", "60"))
SLEEP_INTERVAL = 1

if not MINIO_ROOT_USER or not MINIO_ROOT_PASSWORD or not MINIO_BUCKET_NAME:
    print("[minio-init] ERRO: as variáveis MINIO_ROOT_USER, MINIO_ROOT_PASSWORD e MINIO_BUCKET_NAME são obrigatórias", file=sys.stderr)
    sys.exit(2)

mc = "/usr/bin/mc"

print(f"[minio-init] aguardando {MINIO_HOST}:{MINIO_PORT} (timeout {WAIT_TIMEOUT}s)")
start = time.time()

# tenta configurar alias até funcionar ou timeout
while True:
    try:
        subprocess.check_call([mc, "alias", "set", "minio", f"http://{MINIO_HOST}:{MINIO_PORT}", MINIO_ROOT_USER, MINIO_ROOT_PASSWORD], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        break
    except subprocess.CalledProcessError:
        if time.time() - start > WAIT_TIMEOUT:
            print(f"[minio-init] timeout ao conectar em {MINIO_HOST}:{MINIO_PORT}", file=sys.stderr)
            sys.exit(1)
        time.sleep(SLEEP_INTERVAL)
    except FileNotFoundError:
        print("[minio-init] ERRO: 'mc' (MinIO client) não encontrado em /usr/bin/mc", file=sys.stderr)
        sys.exit(3)

print("[minio-init] MinIO acessível. Criando bucket (se necessário):", MINIO_BUCKET_NAME)
try:
    subprocess.check_call([mc, "mb", "--ignore-existing", f"minio/{MINIO_BUCKET_NAME}"])
except subprocess.CalledProcessError:
    print("[minio-init] aviso: falha ao criar bucket (continuando)")

try:
    subprocess.check_call([mc, "policy", "set", "public", f"minio/{MINIO_BUCKET_NAME}"])
except subprocess.CalledProcessError:
    print("[minio-init] aviso: falha ao aplicar policy (continuando)")

print("[minio-init] finalizado com sucesso")
sys.exit(0)
