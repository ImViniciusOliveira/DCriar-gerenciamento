# Guia de Deploy (Produção)

Este guia descreve três fluxos complementares e responsabilidades separadas:

- Fluxo 1 (DEV): Build & Push — como o Desenvolvedor (Debian 12) constrói as imagens e envia para o Docker Hub.
- Fluxo 2 (DEV): Teste Local — como o Desenvolvedor simula produção na própria máquina usando os caminhos reais (/etc e /opt).
- Fluxo 3 (ADMIN): Deploy Manual — como o Admin do servidor (Windows/WSL) faz o deploy no servidor real, sem usar scripts do projeto.

Observação de segurança: Segredos de produção (.env.prod) devem ficar em /etc/dcriar/.env.prod (perm 600, owner root). O arquivo docker-compose.prod.yml fica em /opt/dcriar/docker-compose.prod.yml (perm 644, owner root). Certificados TLS do frontend devem ficar fora da imagem, por exemplo em /etc/dcriar/tls.

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
FRONTEND_HTTP_PORT=80
FRONTEND_HTTPS_PORT=443
FRONTEND_SERVER_NAME=seu-frontend.exemplo.com
FRONTEND_TLS_ENABLED=true
TLS_CERT_FILE=/etc/dcriar/tls/fullchain.crt
TLS_KEY_FILE=/etc/dcriar/tls/private.key

# Imagens (ajuste para a tag desejada)
DOCKER_REGISTRY_USER=imviniciusoliveira
BACKEND_IMAGE_NAME=dcriar-api
FRONTEND_IMAGE_NAME=dcriar-frontend
APP_VERSION=1.0.0

# PostgreSQL
POSTGRES_DB=dcriar_prod_db
POSTGRES_USER=dcriar_prod_user
POSTGRES_PASSWORD=SENHA_REAL_DO_BANCO_DE_PRODUCAO

# MinIO
MINIO_ROOT_USER=minio_prod_user
MINIO_ROOT_PASSWORD=SENHA_REAL_DO_MINIO_DE_PRODUCAO
MINIO_BUCKET_NAME=dcriar-prod-bucket

# Backend interno no compose
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-prod:5432/dcriar_prod_db
SPRING_DATASOURCE_USERNAME=dcriar_prod_user
SPRING_DATASOURCE_PASSWORD=SENHA_REAL_DO_BANCO_DE_PRODUCAO
MINIO_URL=http://minio-prod:9000
MINIO_ACCESS_KEY=minio_prod_user
MINIO_SECRET_KEY=SENHA_REAL_DO_MINIO_DE_PRODUCAO
DATA_ENCRYPTION_KEY=CHAVE_AES_256_EM_BASE64

# Use o domínio/host público do frontend.
# Em testes locais na rede, pode ser localhost e/ou o IP do host.
CORS_ALLOWED_ORIGIN=https://seu-frontend.exemplo.com
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

4) Certificados TLS do frontend/proxy

O nginx do frontend termina o HTTPS e encaminha `/api` para o backend internamente no compose. Por isso, o certificado fica apenas no serviço `frontend`.

Exemplo de diretório:

```bash
sudo mkdir -p /etc/dcriar/tls
sudo chmod 700 /etc/dcriar/tls
```

Arquivos esperados:

```text
/etc/dcriar/tls/fullchain.crt
/etc/dcriar/tls/private.key
```

Observações:
- Em produção com domínio público, prefira certificado emitido por uma CA confiável.
- Em rede local, use uma CA interna ou `mkcert` e instale a CA nas máquinas clientes.
- O `FRONTEND_SERVER_NAME` deve bater com o nome presente no certificado.
- O backend também precisa de `DATA_ENCRYPTION_KEY` no ambiente para criptografar os dados sensíveis das vendas.

B) Deploy / Atualização

1) Puxar imagens (opcional, recomendado)

```bash
docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml pull
```

2) Subir

```bash
docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml up -d
```

Observação:
- Apenas o frontend deve ficar exposto no host.
- O backend atende internamente e recebe chamadas via `/api` através do nginx do frontend.
- Quando `FRONTEND_TLS_ENABLED=true`, a porta 80 responde só para healthcheck/redirect e a navegação real acontece em HTTPS na porta 443.

3) Logs e status

```bash
docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml ps
docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml logs -f
```

4) Parar

```bash
docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml down
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
