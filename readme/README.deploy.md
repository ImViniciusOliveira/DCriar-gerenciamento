# Guia de Deploy (Do Dev para a Produção)

Este guia cobre o fluxo completo de deploy, dividido em duas partes:

Parte 1: O Workflow do Desenvolvedor (O que você faz no seu PC para construir e enviar as imagens para o Docker Hub).  
Parte 2: O Workflow do Administrador (O que é feito no servidor Linux para baixar e rodar o sistema).

Parte 1: O Workflow do Desenvolvedor (Build e Push)

O que: Esta parte é feita na sua máquina de desenvolvimento local. O objetivo é transformar seu código-fonte em imagens Docker prontas para produção e enviá-las para o Docker Hub.

Pré-requisitos:
- Docker Desktop instalado e rodando.
- Código-fonte do projeto.
- Uma conta no Docker Hub (ou outro registry de containers).

Passo 1.1: Login no Docker Hub

Antes de enviar qualquer imagem, você precisa se autenticar.  
Abra seu terminal.  
Execute o comando de login. Ele pedirá seu nome de usuário e senha:

```bash
docker login
```

Passo 1.2: Build e Tag das Imagens

Agora, vamos construir as imagens a partir do seu código, usando os Dockerfiles. O comando docker build usa o Dockerfile da pasta e o -t aplica uma "tag" (etiqueta), que é o nome da imagem no Docker Hub.

Importante: Substitua your-registry pelo seu nome de usuário real do Docker Hub (ex: imviniciusoliveira).

Construir o Backend:

Navegue até a pasta do seu backend:

```bash
cd /caminho/para/o/projeto/backend
```

Construa e "etiquete" a imagem (o . significa "use esta pasta"):

```bash
docker build -t your-registry/dcriar-api:1.0.0 .
```

Construir o Frontend:

Navegue até a pasta do seu frontend:

```bash
cd /caminho/para/o/projeto/frontend
```

Construa e "etiquete" a imagem:

```bash
docker build -t your-registry/dcriar-frontend:1.0.0 .
```

Passo 1.3: Push (Upload) das Imagens

Com as imagens construídas e "etiquetadas" (taggeadas) localmente, o último passo é enviá-las para o Docker Hub.

```bash
docker push your-registry/dcriar-api:1.0.0
docker push your-registry/dcriar-frontend:1.0.0
```

Pronto! A "Parte 1" terminou. Agora suas imagens estão prontas na nuvem, e o administrador do sistema pode executar a "Parte 2".

Parte 2: O Workflow do Administrador (Deploy no Servidor Linux)

O que: Este é o manual de operações simplificado para um administrador de sistema Linux. O deploy consiste em 2 arquivos e 3 comandos principais.

Pré-requisitos:
- Servidor Linux (ex: Ubuntu, Debian, CentOS) com Docker e Docker-Compose instalados.
- Acesso à internet para puxar as imagens do Docker Hub.
- Acesso de administrador (root/sudo) no servidor.

Os 2 Arquivos Essenciais

O sistema inteiro é definido por apenas dois arquivos no servidor, em locais padronizados do Linux:

- `/etc/dcriar/.env.prod`: O arquivo de segredos (Senhas, chaves de API, etc.).
- `/opt/dcriar/docker-compose.prod.yml`: O arquivo de orquestração (quais containers rodar).

Passo 2.1: Criar o Arquivo de Segredos (no Servidor)

Este passo é feito uma única vez. Os segredos devem ficar fora da pasta da aplicação por segurança.

Crie o diretório seguro:

```bash
sudo mkdir -p /etc/dcriar
```

Crie e abra o arquivo de segredos com um editor (ex: nano):

```bash
sudo nano /etc/dcriar/.env.prod
```

Cole o template de produção (o esqueleto do seu arquivo .env.prod do projeto) e preencha com as senhas reais:

```dotenv
# /etc/dcriar/.env.prod
# =================================================
# ARQUIVO DE PRODUÇÃO (esqueleto) - NÃO comite segredos reais
# Copie este arquivo para /etc/dcriar/.env.prod e preencha com valores reais
# =================================================

# Portas expostas no HOST de Produção (padrões)
APP_PORT=8080
FRONTEND_PORT=80

# Imagens de Produção (substitua pelo seu registry/imagem)
BACKEND_IMAGE=your-registry/dcriar-api:1.0.0
FRONTEND_IMAGE=your-registry/dcriar-frontend:1.0.0

# Variáveis para o serviço do PostgreSQL (preencha com valores reais)
POSTGRES_DB=your_postgres_db
POSTGRES_USER=your_postgres_user
POSTGRES_PASSWORD=your_postgres_password

# Variáveis para o serviço do MinIO (preencha com valores reais)
MINIO_ROOT_USER=your_minio_user
MINIO_ROOT_PASSWORD=your_minio_password
MINIO_BUCKET_NAME=your_minio_bucket

# Observação: este arquivo é somente um esqueleto. Proteja o arquivo real em produção (ex: /etc/dcriar/.env.prod com chmod 600).
```

Defina as permissões corretas (só o root pode ler):

```bash
sudo chmod 600 /etc/dcriar/.env.prod
sudo chown root:root /etc/dcriar/.env.prod
```

Passo 2.2: Criar o Arquivo docker-compose.prod.yml (no Servidor)

Crie a pasta de operação:

```bash
sudo mkdir -p /opt/dcriar
cd /opt/dcriar
```

Crie o arquivo docker-compose.prod.yml:

```bash
sudo nano docker-compose.prod.yml
```

Cole o seguinte conteúdo dentro deste arquivo:

```yaml
services:
  postgres-prod:
    image: postgres:14-alpine
    container_name: postgres-prod
    env_file: ${PROD_ENV_FILE}
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data_prod:/var/lib/postgresql/data
    networks:
      - dcriar-net
    restart: always

  minio-prod:
    image: minio/minio:latest
    container_name: minio-prod
    env_file: ${PROD_ENV_FILE}
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD}
    command: server /data --console-address ":9001"
    volumes:
      - minio_data_prod:/data
    networks:
      - dcriar-net
    restart: always

  minio-setup:
    image: minio/mc
    container_name: minio-setup
    depends_on:
      - minio-prod
    env_file: ${PROD_ENV_FILE}
    networks:
      - dcriar-net
    entrypoint: >
      /bin/sh -c "
      echo 'Esperando o MinIO ficar online...';
      until (/usr/bin/mc alias set dcriar-minio http://minio-prod:9000 ${MINIO_ROOT_USER} ${MINIO_ROOT_PASSWORD}) do echo '...tentando novamente' && sleep 1; done;
      echo 'MinIO está online. Configurando bucket...';
      /usr/bin/mc mb dcriar-minio/${MINIO_BUCKET_NAME} --ignore-existing;
      echo 'Bucket criado ou já existente.';
      /usr/bin/mc policy set public dcriar-minio/${MINIO_BUCKET_NAME};
      echo 'Política do bucket definida como public (leitura pública).';
      exit 0;
      "

  backend:
    image: ${BACKEND_IMAGE}
    container_name: backend-api
    depends_on:
      - postgres-prod
      - minio-setup
    env_file: ${PROD_ENV_FILE}
    ports:
      - "${APP_PORT}:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-prod:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      MINIO_URL: http://minio-prod:9000
      MINIO_ACCESS_KEY: ${MINIO_ROOT_USER}
      MINIO_SECRET_KEY: ${MINIO_ROOT_PASSWORD}
      MINIO_BUCKET_NAME: ${MINIO_BUCKET_NAME}
      SPRING_PROFILES_ACTIVE: prod
    networks:
      - dcriar-net
    restart: always

  frontend:
    image: ${FRONTEND_IMAGE}
    container_name: frontend-app
    ports:
      - "${FRONTEND_PORT}:80"
    networks:
      - dcriar-net
    restart: always

networks:
  dcriar-net:
    driver: bridge

volumes:
  postgres_data_prod:
  minio_data_prod:
```

Passo 2.3: Puxar as Imagens do Docker Hub (recomendado)

Agora, vamos baixar as imagens mais recentes do Docker Hub para o servidor. Isso garante que você tenha a versão mais atualizada antes de subir os containers.

Na pasta onde está o arquivo docker-compose.prod.yml (`/opt/dcriar`):

```bash
# opcional: carregar as variáveis e puxar manualmente
set -a; source /etc/dcriar/.env.prod; set +a
docker pull "${BACKEND_IMAGE}"
docker pull "${FRONTEND_IMAGE}"

# ou usar o compose para puxar as imagens (lê PROD_ENV_FILE)
PROD_ENV_FILE=/etc/dcriar/.env.prod docker compose -f /opt/dcriar/docker-compose.prod.yml pull
```

Passo 2.4: Validar a Configuração do Compose

Antes de subir os containers, é bom validar se a configuração do Docker Compose está correta. Isso ajuda a evitar erros comuns de sintaxe ou configuração.

```bash
PROD_ENV_FILE=/etc/dcriar/.env.prod docker compose -f /opt/dcriar/docker-compose.prod.yml config
```

Se houver erro, conserte `/etc/dcriar/.env.prod` ou o `docker-compose.prod.yml` antes de seguir.

Passo 2.5: Subir o Sistema (modo detached)

Com tudo configurado e validado, é hora de subir os containers da aplicação. O parâmetro -d faz o Docker Compose rodar em segundo plano (detached mode).

```bash
PROD_ENV_FILE=/etc/dcriar/.env.prod docker compose -f /opt/dcriar/docker-compose.prod.yml up -d
```

Passo 2.6: Verificações Pós-Deploy

Após o deploy, é importante verificar se tudo está funcionando como esperado.

```bash
# listar containers do projeto
docker ps --filter "name=dcriar" --format "table {{.Names}}\t{{.Image}}\t{{.Status}}"

# logs em tempo real do backend
PROD_ENV_FILE=/etc/dcriar/.env.prod docker compose -f /opt/dcriar/docker-compose.prod.yml logs -f backend

# status do compose
PROD_ENV_FILE=/etc/dcriar/.env.prod docker compose -f /opt/dcriar/docker-compose.prod.yml ps
```

Testes rápidos:
- Abra no navegador: `http://<IP_DO_SERVIDOR>:${FRONTEND_PORT}`
- Cheque health endpoint (se disponível):

```bash
curl -f http://localhost:${FRONTEND_PORT}/health || echo 'frontend health failed'
```

Passo 2.7: Parar e Atualizar (Redeploy)

Se precisar atualizar a aplicação (por exemplo, uma nova versão do código), siga estes passos:

1. Pare e remova os containers atuais:

```bash
PROD_ENV_FILE=/etc/dcriar/.env.prod docker compose -f /opt/dcriar/docker-compose.prod.yml down
```

1. Atualize para uma nova tag (ex.: 1.0.1):
- Atualize `/etc/dcriar/.env.prod` trocando a tag em `BACKEND_IMAGE` / `FRONTEND_IMAGE`.
- No servidor:

```bash
set -a; source /etc/dcriar/.env.prod; set +a
docker pull "${BACKEND_IMAGE}"
docker pull "${FRONTEND_IMAGE}"
PROD_ENV_FILE=/etc/dcriar/.env.prod docker compose -f /opt/dcriar/docker-compose.prod.yml up -d --no-deps --build backend frontend
```

Observações importantes
- `docker-compose.prod.yml` depende de variáveis obrigatórias: se alguma faltar, o Compose irá falhar — corrija `/etc/dcriar/.env.prod`.
- Não remova volumes do Postgres/MinIO sem backup (dados persistem em volumes).
- Se uma imagem não existir no Docker Hub, verifique nome e tag.
- Proteja `/etc/dcriar/.env.prod` com permissão `600` e propriedade `root`.

# Adição: scripts helper do repositório

## Scripts helper (instalação rápida em servidor)

Para simplificar a instalação do ambiente de produção no servidor, este repositório inclui dois scripts utilitários em `./scripts/deploy`:

- `install-prod-env.sh [caminho_para_.env.prod]` — copia o arquivo de ambiente para `/etc/dcriar/.env.prod`, configura `root:root` e `chmod 600`.
- `install-prod-compose.sh [caminho_para_docker-compose.prod.yml]` — copia `docker-compose.prod.yml` para `/opt/dcriar/docker-compose.prod.yml` e ajusta permissões (owner root, perm 644).

Uso recomendado no servidor (exemplo mínimo):

```bash
# executar a partir da raiz do repositório (ou informe caminhos absolutos)
sudo ./scripts/deploy/install-prod-env.sh ./.env.prod
sudo ./scripts/deploy/install-prod-compose.sh ./docker-compose.prod.yml
# subir a stack
export PROD_ENV_FILE=/etc/dcriar/.env.prod
PROD_ENV_FILE=$PROD_ENV_FILE docker compose -f /opt/dcriar/docker-compose.prod.yml up -d
```

## Flags / opções rápidas (scripts de deploy)

Os scripts sob `./scripts/deploy` e `./scripts/deploys` suportam algumas opções com `--`; aqui estão as mais úteis:

- `./scripts/deploy/deploy-prod-local.sh`
  - `--no-push` — não fará build/push das imagens; usará imagens locais se disponíveis e só instalará `/.env` e `docker-compose.prod.yml` e subirá o compose (útil para testes locais quando você já tem imagens locais).
  - `--env=/caminho/.env.prod` — especifica um arquivo de ambiente diferente do padrão `./.env.prod`.
  - Exemplo: `sudo ./scripts/deploy/deploy-prod-local.sh --no-push --env=./.env.prod`

- `./scripts/deploy/push-images.sh`
  - aceita um argumento posicional (arquivo `.env`) que contém `BACKEND_IMAGE` e `FRONTEND_IMAGE`. Não há flags `--` adicionais; apenas passe o caminho para o arquivo de ambiente. Use um arquivo local preenchido com valores reais (não o esqueleto do repositório).
  - Exemplo: `./scripts/deploy/push-images.sh ./.env.prod`

- `./scripts/deploy/install-prod-env.sh` e `./scripts/deploy/install-prod-compose.sh`
  - não possuem flags `--`; ambos aceitam um argumento posicional (caminho do arquivo de origem) e precisam de `sudo` para copiar para `/etc/dcriar` e `/opt/dcriar`.
  - Exemplo: `sudo ./scripts/deploy/install-prod-env.sh ./.env.prod`

- `./scripts/deploys/deploy-prod.sh` (antigo)
  - aceita um argumento posicional: caminho para `.env.prod` (que será copiado para `/etc/dcriar/.env.prod`) e faz `docker compose up -d --build` (usa `--env-file` internamente).
  - Exemplo: `./scripts/deploys/deploy-prod.sh ./.env.prod`

# Atualizado: o script legado foi movido para `./scripts/deploy/deploy-prod.sh`.
# Use:
#   ./scripts/deploy/deploy-prod.sh ./.env.prod


# Nota importante
O arquivo `./.env.prod` no repositório é comumente um esqueleto com placeholders — antes de usar os scripts acima, crie e edite um arquivo local com valores reais (senhas, nomes de imagem) e NÃO o comite. Por exemplo:

```bash
cp .env.prod .env.prod.local  # cria uma cópia local
# editar .env.prod.local (preencher BACKEND_IMAGE, FRONTEND_IMAGE, senhas, etc.)
# então usar .env.prod.local como argumento nos scripts, ex:
./scripts/deploy/push-images.sh .env.prod.local
sudo ./scripts/deploy/install-prod-env.sh .env.prod.local
```
