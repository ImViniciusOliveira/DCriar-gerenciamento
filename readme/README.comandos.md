# Guia de Comandos Docker

## DEVELOP

### 1. infra e backend pela IDE

- sobe so a infra no docker
- backend roda pela IDE

comando:

```bash
docker compose --project-name dcriar-dev --env-file .env.dev.local -f docker-compose.dev.yml up -d
```

depois rode o backend pela IDE ou:

```bash
cd backend
mvn spring-boot:run
```

------------------------------------------------------------------------

### 2. infra e backend

- sobe infra + backend no docker

comando:

```bash
docker compose --project-name dcriar-dev --env-file .env.dev.local -f docker-compose.dev.yml -f docker-compose.override.yml up -d
```

------------------------------------------------------------------------

## FRONTEND

- frontend em desenvolvimento roda fora do docker

comando:

```bash
cd frontend/
npm start
```

//////////////////////////////////////////////////////////////////////////////

## PASSOS DEPLOY ENTRE 2 PCS

PC1 = maquina onde voce gera e envia as imagens

PC2 = maquina onde voce baixa as imagens e sobe a producao

SEU USUARIO DOCKER HUB:

`imviniciusoliveira`

SUAS IMAGENS:

- `imviniciusoliveira/dcriar-frontend`
- `imviniciusoliveira/dcriar-api`

------------------------------------------------------------------------

## PC1

### O QUE ELE FAZ

- builda a imagem do backend
- builda a imagem do frontend
- envia as duas imagens para o Docker Hub

### LOGIN NO DOCKER HUB

```bash
docker login -u imviniciusoliveira
```

-----------------------------------------------------------------------

### ENVIAR IMAGENS COM TAG 1.0.0

backend:

```bash
docker build -t imviniciusoliveira/dcriar-api:1.0.0 backend/
docker push imviniciusoliveira/dcriar-api:1.0.0
```

frontend:

```bash
docker build -t imviniciusoliveira/dcriar-frontend:1.0.0 frontend/
docker push imviniciusoliveira/dcriar-frontend:1.0.0
```

-----------------------------------------------------------------------

### ENVIAR IMAGENS COM TAG latest

backend:

```bash
docker build -t imviniciusoliveira/dcriar-api:latest backend/
docker push imviniciusoliveira/dcriar-api:latest
```

frontend:

```bash
docker build -t imviniciusoliveira/dcriar-frontend:latest frontend/
docker push imviniciusoliveira/dcriar-frontend:latest
```

-----------------------------------------------------------------------
-----------------------------------------------------------------------

## PC2

### O QUE ELE FAZ

- baixa as imagens do Docker Hub
- sobe a producao com docker compose

### ANTES DE SUBIR

- o arquivo `/etc/dcriar/.env.prod` precisa existir
- o arquivo `/opt/dcriar/docker-compose.prod.yml` precisa existir
- a versao no `.env.prod` precisa bater com a tag enviada no PC1

exemplo no `.env.prod`:

```dotenv
APP_VERSION=1.0.0
DOCKER_REGISTRY_USER=imviniciusoliveira
BACKEND_IMAGE_NAME=dcriar-api
FRONTEND_IMAGE_NAME=dcriar-frontend
```

-----------------------------------------------------------------------

### BAIXAR AS IMAGENS DO DOCKER HUB

```bash
docker compose --project-name dcriar-prod --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml pull
```

-----------------------------------------------------------------------

### SUBIR A PRODUCAO

```bash
docker compose --project-name dcriar-prod --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml up -d
```

-----------------------------------------------------------------------

### VER SE FUNCIONOU

```bash
docker compose --project-name dcriar-prod --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml ps
```

-----------------------------------------------------------------------
