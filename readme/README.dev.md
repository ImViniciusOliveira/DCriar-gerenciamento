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
- Acesse o frontend (http://localhost:4200) e verifique chamadas à API local via `/api` (o proxy do Angular encaminha para `http://localhost:8080`).
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

O projeto inclui scripts em `./scripts/develop` para facilitar subir e derrubar o ambiente de desenvolvimento.

Principais scripts:

- `./scripts/develop/up-dev.sh` — sobe os serviços de infraestrutura (Postgres, MinIO) para desenvolvimento. Usa `./.env.dev.local` por padrão.
- `./scripts/develop/down-dev.sh` — derruba a stack de desenvolvimento e realiza limpeza local: para/remover containers que exponham as portas conhecidas e mata processos locais que estejam usando essas portas.

Exemplos rápidos:

```bash
# subir serviços de dev
./scripts/develop/up-dev.sh

# derrubar e limpar
./scripts/develop/down-dev.sh
```

Observação:
- O `down-dev.sh` faz uma limpeza proativa nas portas conhecidas (8080, 4200, 9000, 9001, 5432). Tenha cuidado ao rodar em máquinas com outros serviços que usem essas portas.
