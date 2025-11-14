# Guia de Deploy (Produção)

Este guia descreve três fluxos complementares e responsabilidades separadas:

- Fluxo 1 (DEV): Build & Push — como o Desenvolvedor (Debian 12) constrói as imagens e envia para o Docker Hub.
- Fluxo 2 (DEV): Teste Local — como o Desenvolvedor simula produção na própria máquina usando os caminhos reais (/etc e /opt).
- Fluxo 3 (ADMIN): Deploy Manual — como o Admin do servidor (Windows/WSL) faz o deploy no servidor real, sem usar scripts do projeto.

Observação de segurança: Segredos de produção (.env.prod) devem ficar em /etc/dcriar/.env.prod (perm 600, owner root). O arquivo docker-compose.prod.yml fica em /opt/dcriar/docker-compose.prod.yml (perm 644, owner root).

---

## Fluxo 1 (DEV): Build & Push

Pré-requisitos (PC do Dev): Docker instalado e login no Docker Hub.

1) Login no Docker Hub

```bash
docker login -u imviniciusoliveira
```

1) Build & Push via script

```bash
chmod +x ./scripts/deploy/push-images.sh
./scripts/deploy/push-images.sh
```

- O script perguntará a tag (ex.: 1.0.1) e fará build/push das imagens backend e frontend do projeto.

---

## Fluxo 2 (DEV): Teste Local de Produção

Objetivo: simular a produção no PC do Dev (Debian 12) utilizando os mesmos caminhos de produção.

A) Configuração inicial (uma vez)

```bash
sudo mkdir -p /etc/dcriar
sudo mkdir -p /opt/dcriar

# 1) Criar/instalar .env.prod em /etc (use seu arquivo local real)
sudo ./scripts/deploy/install-prod-env.sh ./.env.prod

# 2) Copiar docker-compose.prod.yml para /opt
sudo ./scripts/deploy/install-prod-compose.sh ./docker-compose.prod.yml
```

Edite o /etc/dcriar/.env.prod e garanta que há uma versão/tag definida se você usar imagens versionadas.

B) Subir e derrubar a stack (sempre que testar)

```bash
# subir
sudo ./scripts/deploy/deploy-prod.sh

# logs (opcional)
sudo ./scripts/lib/compose-run.sh --project-name dcriar-prod \
  --env-file /etc/dcriar/.env.prod \
  --compose-file /opt/dcriar/docker-compose.prod.yml \
  logs -f backend

# derrubar
sudo ./scripts/deploy/down-prod.sh
```
---

## Fluxo 3 (ADMIN): Deploy Manual no Servidor Real (Windows/WSL)

O Admin não usa scripts do repositório; apenas cria os arquivos e executa docker compose manualmente.

A) Configuração inicial (uma vez)

1) Diretórios

```bash
sudo mkdir -p /opt/dcriar
sudo mkdir -p /etc/dcriar
```

2) Arquivo de segredos

```bash
sudo nano /etc/dcriar/.env.prod
```

Conteúdo de exemplo (preencha valores reais):

```dotenv
# /etc/dcriar/.env.prod
APP_PORT=8080
FRONTEND_PORT=80

# Imagens (ajuste para a tag desejada)
BACKEND_IMAGE=imviniciusoliveira/dcriar-backend:1.0.0
FRONTEND_IMAGE=imviniciusoliveira/dcriar-frontend:1.0.0

# PostgreSQL
POSTGRES_DB=dcriar
POSTGRES_USER=dcriar_user
POSTGRES_PASSWORD=SENHA_REAL_DO_BANCO_DE_PRODUCAO

# MinIO
MINIO_ROOT_USER=minio_admin
MINIO_ROOT_PASSWORD=SENHA_REAL_DO_MINIO_DE_PRODUCAO
MINIO_BUCKET_NAME=dcriar-bucket
```

Proteja o arquivo:

```bash
sudo chmod 600 /etc/dcriar/.env.prod
sudo chown root:root /etc/dcriar/.env.prod
```

3) Arquivo docker-compose

```bash
sudo nano /opt/dcriar/docker-compose.prod.yml
```

Cole o conteúdo do docker-compose.prod.yml do projeto.

B) Deploy / Atualização

1) Puxar imagens (opcional, recomendado)

```bash
PROD_ENV_FILE=/etc/dcriar/.env.prod docker compose -f /opt/dcriar/docker-compose.prod.yml pull
```

2) Subir

```bash
PROD_ENV_FILE=/etc/dcriar/.env.prod docker compose -f /opt/dcriar/docker-compose.prod.yml up -d
```

3) Logs e status

```bash
PROD_ENV_FILE=/etc/dcriar/.env.prod docker compose -f /opt/dcriar/docker-compose.prod.yml ps
PROD_ENV_FILE=/etc/dcriar/.env.prod docker compose -f /opt/dcriar/docker-compose.prod.yml logs -f
```

4) Parar

```bash
PROD_ENV_FILE=/etc/dcriar/.env.prod docker compose -f /opt/dcriar/docker-compose.prod.yml down
```

---

## Arquitetura de Scripts

- scripts/lib/compose-run.sh:
  - Apenas repassa: --project-name, --env-file (repetível), --compose-file (repetível), --no-sudo, e os comandos docker compose.
  - Exemplo:

```bash
./scripts/lib/compose-run.sh \
  --project-name dcriar-dev \
  --env-file ./.env.dev.local \
  --compose-file docker-compose.dev.yml \
  --compose-file docker-compose.override.yml \
  up -d
```

- scripts/develop/* — Atalhos de DEV (usam env/compose locais do projeto e --no-sudo).
- scripts/deploy/* — Atalhos de PROD (usam /etc e /opt e rodam com sudo).

Dica: Nunca comite segredos.
