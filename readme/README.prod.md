```markdown
// filepath: readme/README.prod.md

# README - Deploy em produção (servidor)

Objetivo: usar este servidor como ambiente de produção. O processo assume que você tem Docker e Docker Compose instalados no servidor.

1) Preparar o arquivo de variáveis de ambiente
- Crie o diretório /etc/dcriar (se ainda não existir):
  sudo mkdir -p /etc/dcriar && sudo chown $USER:$USER /etc/dcriar
- Copie o template e edite com valores reais (não commitar):
  cp .env.prod.example /etc/dcriar/.env.prod
  nano /etc/dcriar/.env.prod
- As variáveis importantes criam os nomes e as credenciais do Postgres e MinIO.

2) Fazer o deploy
- Do diretório do projeto execute:
  sudo ./scripts/deploy-prod.sh /etc/dcriar/.env.prod
- Isso fará copy para /etc/dcriar/.env.prod (caso já não esteja lá), puxará imagens (se aplicável) e subirá os containers.

3) Logs e diagnóstico
- Verificar status:
  docker compose -f docker-compose.prod.yml --env-file /etc/dcriar/.env.prod ps
- Visualizar logs:
  docker compose -f docker-compose.prod.yml --env-file /etc/dcriar/.env.prod logs -f backend

4) Parar/remover
- docker compose -f docker-compose.prod.yml --env-file /etc/dcriar/.env.prod down --remove-orphans
- ou usar o helper script:
  sudo ./scripts/down-prod.sh

Observações:
- Certifique-se de configurar BACKEND_IMAGE e FRONTEND_IMAGE no arquivo /etc/dcriar/.env.prod para apontar para imagens construídas/pushadas no registry.
- Em servidores de produção é recomendado usar políticas de permissão e backups para volumes (Postgres/MinIO).
```

