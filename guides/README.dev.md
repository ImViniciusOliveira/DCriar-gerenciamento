# Guia De Desenvolvimento

Este guia cobre o fluxo mais comum de desenvolvimento:
- infraestrutura no Docker
- backend local pela IDE ou terminal
- frontend local com Angular

## Endereços Locais

- frontend: `http://localhost:4200`
- backend: `http://localhost:8080`
- health check: `http://localhost:8080/actuator/health`
- console do MinIO: `http://localhost:9001`

Esses endereços são seguros para desenvolvimento local e são o padrão do projeto.

## Pré-Requisitos

- Java 21
  - recomendado: Eclipse Temurin 21
- Node.js `^20.19.0` ou `^22.12.0`
- npm instalado junto com o Node.js compatível
- Docker Engine 29.2.1
- Docker Compose Plugin v2.40.3

Referencia do Node.js para Angular 21: [Version compatibility](https://angular.dev/reference/versions)

## Arquivo De Ambiente

Copie o esqueleto e preencha os dados reais do seu ambiente:

```bash
cp /caminho/do/esqueleto/.env.dev /caminho/onde/o/arquivo/de/ambiente/.env.dev
```

Use o conteúdo do arquivo de esqueleto como base:

- [Arquivo de ambiente de desenvolvimento](.env.dev.example)

## Subir Só A Infraestrutura

```bash
docker compose --project-name seu-projeto-dev --env-file /caminho/onde/o/arquivo/de/ambiente/.env.dev -f /caminho/do/seu-projeto/docker-compose.dev.yml up -d
```

Isso sobe:
- PostgreSQL
- MinIO
- setup inicial do bucket

Para parar:

```bash
docker compose --project-name seu-projeto-dev --env-file /caminho/onde/o/arquivo/de/ambiente/.env.dev -f /caminho/do/seu-projeto/docker-compose.dev.yml down
```

## Rodar O Backend Pela IDE

Se for usar a IDE, a configuração de execução do backend precisa subir com o profile `local`.

Sem isso, a aplicação pode tentar subir com outro profile e carregar a configuração errada.

Formas comuns de configurar isso:
- em `Program arguments`: `--spring.profiles.active=local`
- em variável de ambiente da execução: `SPRING_PROFILES_ACTIVE=local`

O importante é a execução final do backend usar:

```text
spring.profiles.active=local
```

## Rodar O Backend Pelo Terminal

```bash
cd /caminho/do/seu-projeto/backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## Rodar O Frontend

```bash
cd /caminho/do/seu-projeto/frontend
npm install
npm start
```

## O Que Validar

- frontend abrindo em `http://localhost:4200`
- backend respondendo em `http://localhost:8080`
- `http://localhost:8080/actuator/health` retornando `UP`
- frontend conseguindo chamar a API
- MinIO abrindo em `http://localhost:9001`

## Comandos Úteis

Logs da infraestrutura:

```bash
docker compose --project-name seu-projeto-dev --env-file /caminho/onde/o/arquivo/de/ambiente/.env.dev -f /caminho/do/seu-projeto/docker-compose.dev.yml logs -f
```

Status dos containers:

```bash
docker compose --project-name seu-projeto-dev --env-file /caminho/onde/o/arquivo/de/ambiente/.env.dev -f /caminho/do/seu-projeto/docker-compose.dev.yml ps
```

Se precisar só verificar se o backend subiu:

```bash
curl http://localhost:8080/actuator/health
```
