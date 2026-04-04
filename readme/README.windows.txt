Windows + WSL2 + Debian

Guia curto para subir o sistema no Windows usando WSL2 + Debian + Docker Desktop.

1. Instalar base no Windows

No PowerShell como administrador:

wsl --install -d Debian

Depois:
- reinicie o Windows se ele pedir
- abra o terminal Debian

2. Instalar Docker

No Windows:
- instale o Docker Desktop
- ative a integração com WSL2
- ative a distro Debian em:
  Settings > Resources > WSL Integration

3. Preparar o Debian

No terminal Debian:

sudo apt update
sudo apt install -y git curl ca-certificates

Confirme:

docker --version
docker compose version

4. Clonar o projeto

mkdir -p ~/dcriar
cd ~/dcriar
git clone <URL_DO_REPOSITORIO> dcriar-sistema-inventario
cd dcriar-sistema-inventario

5. Criar arquivos reais de produção

sudo mkdir -p /etc/dcriar
sudo mkdir -p /opt/dcriar
sudo cp docker-compose.prod.yml /opt/dcriar/docker-compose.prod.yml
sudo nano /etc/dcriar/.env.prod

No .env.prod, preencher:
- DOCKER_REGISTRY_USER
- BACKEND_IMAGE_NAME
- FRONTEND_IMAGE_NAME
- APP_VERSION
- FRONTEND_PORT
- dados do Postgres
- dados do MinIO
- CORS_ALLOWED_ORIGIN

Proteger o arquivo:

sudo chmod 600 /etc/dcriar/.env.prod
sudo chown root:root /etc/dcriar/.env.prod

6. Subir a stack

docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml pull
docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml up -d

Ver status:

docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml ps
docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml logs -f

7. Como acessar

No próprio Windows:
- http://localhost

No celular ou outro PC da mesma rede:
- use o IP do Windows, nao o IP do WSL
- exemplo: http://192.168.0.150

Para descobrir o IP no Windows:

ipconfig

8. Regra importante sobre IP

Nao use IP fixo do WSL no código.

O sistema foi preparado para funcionar assim:
- frontend exposto no host
- backend interno no Docker
- frontend chama /api
- nginx encaminha para o backend

Entao:
- mesma maquina: localhost
- outra maquina da rede: IP do Windows

9. Atualizar versão

Quando mudar a versão da imagem:
- edite /etc/dcriar/.env.prod
- troque APP_VERSION
- rode:

docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml pull
docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml up -d

10. Se quiser banco limpo

Somente para teste inicial:

docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml down
docker volume rm dcriar_postgres_data_prod
docker compose --env-file /etc/dcriar/.env.prod -f /opt/dcriar/docker-compose.prod.yml up -d

Isso apaga os dados do Postgres local de teste.
