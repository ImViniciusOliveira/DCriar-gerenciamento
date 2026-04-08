# Guia de Comandos

Este arquivo reúne comandos diretos para rodar, inspecionar, publicar e limpar a aplicação.

## Desenvolvimento

### 1. Subir só a infraestrutura

Use este modo quando o backend vai rodar pela IDE e o frontend vai rodar fora do Docker.

```bash
docker compose --project-name seu-projeto-dev --env-file /caminho/do/seu-projeto/.env.dev.local -f /caminho/do/seu-projeto/docker-compose.dev.yml up -d
```

Depois rode o backend:

```bash
cd backend
mvn spring-boot:run
```

E o frontend:

```bash
cd frontend
npm start
```

### 2. Subir infraestrutura + backend no Docker

```bash
docker compose --project-name seu-projeto-dev --env-file /caminho/do/seu-projeto/.env.dev.local -f /caminho/do/seu-projeto/docker-compose.dev.yml -f /caminho/do/seu-projeto/docker-compose.override.yml up -d
```

O frontend continua rodando fora do Docker:

```bash
cd frontend
npm start
```

### 3. Ver status da stack de desenvolvimento

```bash
docker compose --project-name seu-projeto-dev --env-file /caminho/do/seu-projeto/.env.dev.local -f /caminho/do/seu-projeto/docker-compose.dev.yml ps
```

### 4. Ver logs da infraestrutura em desenvolvimento

```bash
docker compose --project-name seu-projeto-dev --env-file /caminho/do/seu-projeto/.env.dev.local -f /caminho/do/seu-projeto/docker-compose.dev.yml logs -f
```

### 5. Parar a stack de desenvolvimento

```bash
docker compose --project-name seu-projeto-dev --env-file /caminho/do/seu-projeto/.env.dev.local -f /caminho/do/seu-projeto/docker-compose.dev.yml down
```

---

## Build de Imagens

### Backend

Exemplo com tag `1.0.0`:

```bash
docker build -t SEU_USUARIO_DOCKER_HUB/NOME_DA_IMAGEM_BACKEND:1.0.0 backend/
```

Exemplo com tag `latest`:

```bash
docker build -t SEU_USUARIO_DOCKER_HUB/NOME_DA_IMAGEM_BACKEND:latest backend/
```

### Frontend

Exemplo com tag `1.0.0`:

```bash
docker build -t SEU_USUARIO_DOCKER_HUB/NOME_DA_IMAGEM_FRONTEND:1.0.0 frontend/
```

Exemplo com tag `latest`:

```bash
docker build -t SEU_USUARIO_DOCKER_HUB/NOME_DA_IMAGEM_FRONTEND:latest frontend/
```

---

## Publicar no Docker Hub

### Login

```bash
docker login -u SEU_USUARIO_DOCKER_HUB
```

### Enviar backend

```bash
docker push SEU_USUARIO_DOCKER_HUB/NOME_DA_IMAGEM_BACKEND:1.0.0
docker push SEU_USUARIO_DOCKER_HUB/NOME_DA_IMAGEM_BACKEND:latest
```

### Enviar frontend

```bash
docker push SEU_USUARIO_DOCKER_HUB/NOME_DA_IMAGEM_FRONTEND:1.0.0
docker push SEU_USUARIO_DOCKER_HUB/NOME_DA_IMAGEM_FRONTEND:latest
```

---

## Produção

### Antes de subir

Os arquivos abaixo precisam existir:

- `/etc/seu-projeto/.env.prod`
- `/opt/seu-projeto/docker-compose.prod.yml`

No `/etc/seu-projeto/.env.prod`, a versão precisa bater com a tag publicada:

```dotenv
APP_VERSION=1.0.0
DOCKER_REGISTRY_USER=SEU_USUARIO_DOCKER_HUB
BACKEND_IMAGE_NAME=NOME_DA_IMAGEM_BACKEND
FRONTEND_IMAGE_NAME=NOME_DA_IMAGEM_FRONTEND
```

### Baixar as imagens

```bash
docker compose --project-name seu-projeto-prod --env-file /etc/seu-projeto/.env.prod -f /opt/seu-projeto/docker-compose.prod.yml pull
```

### Subir a produção

```bash
docker compose --project-name seu-projeto-prod --env-file /etc/seu-projeto/.env.prod -f /opt/seu-projeto/docker-compose.prod.yml up -d
```

### Ver status

```bash
docker compose --project-name seu-projeto-prod --env-file /etc/seu-projeto/.env.prod -f /opt/seu-projeto/docker-compose.prod.yml ps
```

### Ver logs

Todos os serviços:

```bash
docker compose --project-name seu-projeto-prod --env-file /etc/seu-projeto/.env.prod -f /opt/seu-projeto/docker-compose.prod.yml logs -f
```

Só backend:

```bash
docker compose --project-name seu-projeto-prod --env-file /etc/seu-projeto/.env.prod -f /opt/seu-projeto/docker-compose.prod.yml logs -f backend
```

Só frontend:

```bash
docker compose --project-name seu-projeto-prod --env-file /etc/seu-projeto/.env.prod -f /opt/seu-projeto/docker-compose.prod.yml logs -f frontend
```

### Parar a produção

```bash
docker compose --project-name seu-projeto-prod --env-file /etc/seu-projeto/.env.prod -f /opt/seu-projeto/docker-compose.prod.yml down
```

---

## Comandos Úteis

### Ver containers em execução

```bash
docker ps
```

### Ver imagens locais

```bash
docker images
```

### Remover imagens locais

```bash
docker rmi SEU_USUARIO_DOCKER_HUB/NOME_DA_IMAGEM_BACKEND:1.0.0
docker rmi SEU_USUARIO_DOCKER_HUB/NOME_DA_IMAGEM_FRONTEND:1.0.0
```

### Testar a aplicação em produção

```bash
curl -k -I https://IP_DO_SERVIDOR/health
curl -I http://IP_DO_SERVIDOR/
curl -k -I https://IP_DO_SERVIDOR/
```
