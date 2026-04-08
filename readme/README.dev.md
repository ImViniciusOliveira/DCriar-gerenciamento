# Guia De Desenvolvimento

Este guia é para o fluxo rápido de desenvolvimento: frontend local, backend local e só a infraestrutura no Docker.

## Resumo

- frontend: `http://localhost:4200`
- backend: `http://localhost:8080`
- banco: PostgreSQL via Docker
- storage: MinIO via Docker

## Pré-Requisitos

- Java 21
- Node.js + npm
- Docker + Docker Compose

## 1. Criar O Arquivo Local

O projeto usa `.env.dev.local` como arquivo real de desenvolvimento.  
`.env.dev` é só esqueleto.

```bash
cp .env.dev .env.dev.local
```

Preencha no `.env.dev.local`:
- banco
- MinIO
- `CORS_ALLOWED_ORIGIN`
- `DATA_ENCRYPTION_KEY`
- `WAIT_TIMEOUT`
- `WAIT_INTERVAL`

## 2. Subir A Infraestrutura

```bash
./scripts/develop/up-dev.sh
```

Isso sobe:
- `postgres-dev`
- `minio-dev`
- `minio-setup-dev`

Para derrubar:

```bash
./scripts/develop/down-dev.sh
```

## 3. Rodar O Backend

O backend deve rodar localmente, normalmente pela IDE.

Opção recomendada:
- profile `local`
- variáveis vindas do `.env.dev.local`

Ou por Maven:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

API:
- `http://localhost:8080`
- actuator: `http://localhost:8080/actuator/health`

## 4. Rodar O Frontend

```bash
cd frontend
npm install
npm start
```

Aplicação:
- `http://localhost:4200`

## 5. O Que Validar

- frontend abre em `localhost:4200`
- backend responde em `localhost:8080`
- frontend consegue chamar `/api`
- MinIO console abre em `http://localhost:9001`
- a API sobe só se `DATA_ENCRYPTION_KEY` estiver configurada

## 6. Estrutura Do Fluxo Dev

Em desenvolvimento:
- Docker sobe só infraestrutura
- backend roda localmente
- frontend roda localmente

Isso dá:
- hot reload no Angular
- depuração melhor no Spring Boot
- menos atrito no ciclo de alteração

## 7. Comandos Úteis

Subir infra:

```bash
./scripts/develop/up-dev.sh
```

Derrubar infra:

```bash
./scripts/develop/down-dev.sh
```

Compilar backend:

```bash
cd backend
./mvnw -DskipTests compile
```

Build do frontend:

```bash
cd frontend
npm run build
```

## 8. Se Precisar Entender Melhor

Pontos importantes deste projeto em desenvolvimento:
- `.env.dev.local` é o arquivo real
- `.env.dev` não deve ser usado em runtime
- dados sensíveis de venda dependem de `DATA_ENCRYPTION_KEY`
- `docker-compose.dev.yml` não sobe backend nem frontend
