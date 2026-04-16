# Guia De Produção

Este guia cobre o fluxo direto de produção:
- arquivo de ambiente real em `/etc/...`
- compose real em `/opt/...`
- imagens vindas do Docker Hub
- frontend com HTTPS no nginx e redirecionamento de HTTP para HTTPS

## Estrutura Esperada No Servidor

Arquivos reais:
- `/etc/nome-do-projeto/.env.prod`
- `/opt/nome-do-projeto/docker-compose.prod.yml`
- `/etc/nome-do-projeto/tls/fullchain.crt`
- `/etc/nome-do-projeto/tls/private.key`

Permissões recomendadas:
- `/etc/nome-do-projeto/.env.prod` com `600`
- certificados com `600`
- arquivos de `/opt/nome-do-projeto` com owner `root`

## Arquivo De Ambiente

Use o esqueleto do projeto como base:

- [Arquivo de ambiente de produção](/home/viniciusdev/dcriar/dcriar-sistema-inventario/.env.prod)

O arquivo real deve ficar em:

```bash
/etc/nome-do-projeto/.env.prod
```

Pontos importantes:
- `APP_VERSION` deve bater com a tag publicada no Docker Hub
- `FRONTEND_SERVER_NAME` deve bater com o certificado
- `CORS_ALLOWED_ORIGIN` deve bater com a origem HTTPS real
- `DATA_ENCRYPTION_KEY` é obrigatória

## Compose De Produção

Use o compose do projeto como base:

- [docker-compose.prod.yml](/home/viniciusdev/dcriar/dcriar-sistema-inventario/docker-compose.prod.yml)

O arquivo real deve ficar em:

```bash
/opt/nome-do-projeto/docker-compose.prod.yml
```

## Certificados

Arquivos esperados:

```text
/etc/nome-do-projeto/tls/fullchain.crt
/etc/nome-do-projeto/tls/private.key
```

Se `FRONTEND_TLS_ENABLED=true` e esses arquivos não existirem:
- o frontend não sobe

Se precisar gerar os arquivos do zero:
- [Guia de Certificado TLS](README.certificado.md)

## Subir A Produção

Baixar as imagens:

```bash
docker compose --project-name nome-do-projeto-prod --env-file /etc/nome-do-projeto/.env.prod -f /opt/nome-do-projeto/docker-compose.prod.yml pull
```

Subir a stack:

```bash
docker compose --project-name nome-do-projeto-prod --env-file /etc/nome-do-projeto/.env.prod -f /opt/nome-do-projeto/docker-compose.prod.yml up -d
```

Ver status:

```bash
docker compose --project-name nome-do-projeto-prod --env-file /etc/nome-do-projeto/.env.prod -f /opt/nome-do-projeto/docker-compose.prod.yml ps
```

Ver logs:

```bash
docker compose --project-name nome-do-projeto-prod --env-file /etc/nome-do-projeto/.env.prod -f /opt/nome-do-projeto/docker-compose.prod.yml logs -f
```

Parar a produção:

```bash
docker compose --project-name nome-do-projeto-prod --env-file /etc/nome-do-projeto/.env.prod -f /opt/nome-do-projeto/docker-compose.prod.yml down
```

## O Que Validar Depois

HTTPS:

```bash
curl -k -I https://seu-ip-do-servidor/health
curl -I http://seu-ip-do-servidor/
curl -k -I https://seu-ip-do-servidor/
```

Esperado:
- `/health` em HTTPS retorna `200`
- `/` em HTTP retorna `301`
- `/` em HTTPS retorna `200`

## Segurança E Operação

Em produção:
- exponha só o frontend
- mantenha o backend interno no compose
- proteja `/etc/nome-do-projeto/.env.prod`
- proteja `private.key`
- não comite segredos reais
