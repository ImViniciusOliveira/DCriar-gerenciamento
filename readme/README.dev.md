# Guia de Desenvolvimento — DCriar

Este documento descreve como rodar o projeto em ambiente de desenvolvimento local.
Objetivo: máxima velocidade de iteração para frontend e facilidade para backend.

Resumo rápido
- Frontend: rodar localmente com `npm start` / `ng serve` (hot reload).
- Backend: rodar via IDE (mvnw spring-boot run) ou em container com o `backend/Dockerfile` (multi-stage).
- Dependências locais (Postgres, MinIO) via Docker Compose (desenvolvimento).

1) Preparar arquivo de ambiente local

- Copie o esqueleto e crie seu arquivo de segredos local:

```bash
cp .env.dev .env.dev.local
# editar .env.dev.local com valores locais (DB, MinIO credentials)
```

- Este arquivo **NÃO** deve ser comitado (está em `.gitignore`).

1) Subir dependências (Postgres, MinIO) via Docker Compose

- Comando (scripts disponíveis):

```bash
# sobe postgres, minio e o job de setup que cria o bucket
./scripts/up-dev.sh

# derrubar
./scripts/down-dev.sh
```

- Ou manualmente:

```bash
# sem usar scripts (exporte variável que os yml usam)
export DEV_ENV_FILE=./.env.dev.local
docker compose -f docker-compose.dev.yml -f docker-compose.override.yml up --build
```

1) Rodar o frontend (modo dev — recomendado)

- Instale dependências se necessário:

```bash
cd frontend
npm ci
# ou npm install
```

- Rodar com hot-reload:

```bash
npm start
# abre por padrão em http://localhost:4200
```

1) Rodar o backend

Opção A — rodar pela IDE (recomendado para depuração):
- Importar o projeto Maven (`backend/pom.xml`) na sua IDE (IntelliJ/VSCode+Extension).
- Configurar a profile `dev` (ou passe `--spring.profiles.active=dev`) e a variável de ambiente `MINIO_URL=http://minio-dev:9000` no run configuration.
- Rodar usando o `mvnw` ou a Run Configuration da IDE.

Opção B — rodar em container (construção local rápida):

```bash
# build da imagem local do backend
docker build -t dcriar-api:local backend/
# rodar
docker run --rm -p 8080:8080 --env-file ./.env.dev.local dcriar-api:local
```

1) Testar a integração
- Acesse o frontend (http://localhost:4200) e verifique chamadas à API local (que deve apontar para `http://localhost:8080` se o backend estiver exposto).
- Verifique o MinIO console em `http://localhost:9001` (user/pass conforme `.env.dev.local`).

1) Dicas para IDE (IntelliJ)
- Importar como Maven project.
- Configure uma Run Configuration do tipo `Spring Boot` apontando para o main class do backend.
- Configure variáveis de ambiente na Run Configuration (ou use `application-dev.yml` para dev settings).

1) Limpeza
- Para remover containers e volumes do compose de dev:

```bash
./scripts/down-dev.sh
# ou
export DEV_ENV_FILE=./.env.dev.local
docker compose -f docker-compose.dev.yml -f docker-compose.override.yml down --remove-orphans
```

## Scripts úteis (desenvolvimento)

O projeto inclui alguns scripts prontos para facilitar levantar/derrubar e resetar o ambiente de desenvolvimento. Eles ficam em `./scripts/develop`.

Principais scripts:

- `./scripts/develop/up-dev.sh` — sobe os serviços de infraestrutura necessários (Postgres, MinIO) para desenvolvimento. Lê `DEV_ENV_FILE` (por padrão `./.env.dev.local`).
- `./scripts/develop/down-dev.sh` — derruba os serviços de desenvolvimento levantados pelo compose.
- `./scripts/develop/recreate-dev.sh` — reset total do ambiente de desenvolvimento: mata processos/containers que ocupam as portas conhecidas (8080, 4200, 9000, 9001, 5432), executa `down-dev.sh` e (por padrão) re-executa `up-dev.sh`.

Flags e comportamento importantes do `recreate-dev.sh`:

- `--no-start` — faz apenas a limpeza (mata containers/processos e roda `down-dev.sh`) e NÃO roda `up-dev.sh` no final.
- Qualquer argumento adicional é repassado para `up-dev.sh`. Ex.: `./scripts/develop/recreate-dev.sh --no-build` irá repassar `--no-build` para o `up-dev.sh`.

Exemplos rápidos:

```bash
# resetar e subir (padrão)
./scripts/develop/recreate-dev.sh

# apenas limpar (não subir)
./scripts/develop/recreate-dev.sh --no-start

# resetar e passar flag para up-dev.sh
./scripts/develop/recreate-dev.sh --no-build
```

Observações:
- O `recreate-dev.sh` agora detecta containers Docker que publicam as portas de dev e os para/remove automaticamente (útil quando um container de outra stack está ocupando a porta). Ele também tenta matar PIDs locais como fallback.
- Tenha cuidado antes de rodar em uma máquina com outros serviços importantes que possam usar as portas listadas.

## Scripts de deploy helper (no repositório)

Para facilitar a instalação em servidor, o repositório contém auxiliares em `./scripts/deploy`:

- `./scripts/deploy/install-prod-env.sh [caminho_para_.env.prod]` — copia o `.env.prod` do repositório (ou do caminho indicado) para `/etc/dcriar/.env.prod`, define owner root e `chmod 600`.
- `./scripts/deploy/install-prod-compose.sh [caminho_para_docker-compose.prod.yml]` — copia `docker-compose.prod.yml` para `/opt/dcriar/docker-compose.prod.yml` e ajusta permissões (owner root, perm 644).

Exemplo de uso no servidor:

```bash
# instalar .env.prod em /etc/dcriar
sudo ./scripts/deploy/install-prod-env.sh ./.env.prod
# instalar docker-compose em /opt/dcriar
sudo ./scripts/deploy/install-prod-compose.sh ./docker-compose.prod.yml
# subir stack
export PROD_ENV_FILE=/etc/dcriar/.env.prod
PROD_ENV_FILE=$PROD_ENV_FILE docker compose -f /opt/dcriar/docker-compose.prod.yml up -d
```

## Flags / opções rápidas (scripts de desenvolvimento)

Aqui estão as opções com `--` suportadas pelos scripts em `./scripts/develop` (apenas as flags relevantes):

- `./scripts/develop/up-dev.sh`
  - `--no-backend` — não inclui `docker-compose.override.yml` no `docker compose up` (útil se você quiser subir apenas infra como Postgres/MinIO sem o backend no container).
  - Exemplo: `./scripts/develop/up-dev.sh --no-backend`

- `./scripts/develop/down-dev.sh`
  - Não tem flags com `--` específicas; usa `./.env.dev.local` por padrão.

- `./scripts/develop/recreate-dev.sh`
  - `--no-start` — faz a limpeza e `down` (mata processos/containers e roda `down-dev.sh`), mas NÃO executa `up-dev.sh` no final.
  - Qualquer argumento desconhecido é repassado para `up-dev.sh`. Por exemplo `./scripts/develop/recreate-dev.sh --no-build` passará `--no-build` para o `up-dev.sh`.
  - Exemplo (apenas limpar): `./scripts/develop/recreate-dev.sh --no-start`
  - Exemplo (recriar e passar flag para up-dev): `./scripts/develop/recreate-dev.sh --no-build`
