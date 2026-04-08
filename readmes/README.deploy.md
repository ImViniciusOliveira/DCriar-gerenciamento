# Guia De Produção

Este guia é o caminho direto para subir a aplicação em produção ou em um ambiente de teste que simule produção.

## O Que Vai Subir

A stack de produção sobe:
- `postgres-prod`
- `minio-prod`
- `minio-setup`
- `backend-api`
- `frontend-app`

O frontend:
- atende em `80` e `443`
- faz redirect de HTTP para HTTPS
- encaminha `/api` para o backend

## Estrutura Real Esperada No Servidor

Arquivos reais:
- `/etc/dcriar/.env.prod`
- `/opt/dcriar/docker-compose.prod.yml`
- `/etc/dcriar/tls/fullchain.crt`
- `/etc/dcriar/tls/private.key`

Permissões recomendadas:
- `/etc/dcriar/.env.prod` com `600`
- certificados com `600`
- arquivos de `/opt/dcriar` com owner `root`

## 1. Preparar O Arquivo De Ambiente

Exemplo de `/etc/dcriar/.env.prod`:

```dotenv
DOCKER_REGISTRY_USER=imviniciusoliveira
BACKEND_IMAGE_NAME=dcriar-api
FRONTEND_IMAGE_NAME=dcriar-frontend
APP_VERSION=1.0.0

FRONTEND_HTTP_PORT=80
FRONTEND_HTTPS_PORT=443
FRONTEND_SERVER_NAME=SEU_IP_DO_SERVIDOR
FRONTEND_TLS_ENABLED=true
TLS_CERT_FILE=/etc/dcriar/tls/fullchain.crt
TLS_KEY_FILE=/etc/dcriar/tls/private.key

POSTGRES_DB=dcriar_prod_db
POSTGRES_USER=dcriar_prod_user
POSTGRES_PASSWORD=SENHA_REAL_DO_BANCO

MINIO_ROOT_USER=minio_prod_user
MINIO_ROOT_PASSWORD=SENHA_REAL_DO_MINIO
MINIO_BUCKET_NAME=dcriar-prod-bucket

MINIO_URL=http://minio-prod:9000
MINIO_ACCESS_KEY=minio_prod_user
MINIO_SECRET_KEY=SENHA_REAL_DO_MINIO

CORS_ALLOWED_ORIGIN=https://SEU_IP_DO_SERVIDOR
DATA_ENCRYPTION_KEY=CHAVE_AES_256_EM_BASE64
```

Pontos importantes:
- `DATA_ENCRYPTION_KEY` é obrigatória
- `FRONTEND_SERVER_NAME` deve bater com o certificado
- `CORS_ALLOWED_ORIGIN` deve bater com a origem HTTPS real
- `SPRING_DATASOURCE_*` não precisa entrar aqui

## 2. Preparar O Compose

O arquivo real do compose deve ficar em:

```bash
/opt/dcriar/docker-compose.prod.yml
```

Use o conteúdo do [docker-compose.prod.yml](/home/viniciusdev/dcriar/dcriar-sistema-inventario/docker-compose.prod.yml) do projeto.

## 3. Preparar Os Certificados

Arquivos esperados:

```text
/etc/dcriar/tls/fullchain.crt
/etc/dcriar/tls/private.key
```

Se `FRONTEND_TLS_ENABLED=true` e esses arquivos não existirem:
- o frontend não sobe

## 4. Subir A Stack

Puxar imagens:

```bash
docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml pull
```

Subir:

```bash
docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml up -d
```

Ver status:

```bash
docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml ps
```

Ver logs:

```bash
docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml logs -f
```

Parar:

```bash
docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml down
```

## 5. O Que Validar Depois

HTTPS:

```bash
curl -k -I https://IP_DO_SERVIDOR/health
curl -I http://IP_DO_SERVIDOR/
curl -k -I https://IP_DO_SERVIDOR/
```

Esperado:
- `/health` em HTTPS => `200`
- `/` em HTTP => `301`
- `/` em HTTPS => `200`

## 6. Segurança E Operação

Em produção:
- exponha só o frontend
- mantenha o backend interno no compose
- proteja `/etc/dcriar/.env.prod`
- proteja `private.key`
- não comite segredos

## 7. Se Quiser Ir Mais Fundo

Este projeto também tem:
- scripts para build e push em `scripts/deploy/`
- testes locais de produção usando os mesmos caminhos `/etc` e `/opt`

Mas o fluxo acima já é suficiente para subir e operar a aplicação.
