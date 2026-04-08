# Guia de Certificado TLS

Este guia mostra como gerar os dois arquivos usados pelo frontend/nginx para subir o HTTPS em rede local.

## Arquivos Esperados

```text
/etc/nome-do-projeto/tls/fullchain.crt
/etc/nome-do-projeto/tls/private.key
```

## 1. Criar A Pasta

```bash
sudo mkdir -p /etc/nome-do-projeto/tls
sudo chmod 700 /etc/nome-do-projeto/tls
sudo chown root:root /etc/nome-do-projeto/tls
```

## 2. Gerar O Certificado E A Chave

Exemplo para um servidor acessado por IP local:

```bash
sudo openssl req -x509 -nodes -newkey rsa:2048 \
  -keyout /etc/nome-do-projeto/tls/private.key \
  -out /etc/nome-do-projeto/tls/fullchain.crt \
  -days 365 \
  -subj "/CN=<ip-ou-host-do-servidor>" \
  -addext "subjectAltName=IP:<ip-do-servidor>,IP:127.0.0.1,DNS:localhost"
```

Se o valor do servidor for outro, troque:
- `<ip-ou-host-do-servidor>`
- `<ip-do-servidor>`

## 3. Proteger Os Arquivos

```bash
sudo chmod 600 /etc/nome-do-projeto/tls/fullchain.crt
sudo chmod 600 /etc/nome-do-projeto/tls/private.key
sudo chown root:root /etc/nome-do-projeto/tls/fullchain.crt
sudo chown root:root /etc/nome-do-projeto/tls/private.key
```

## 4. Conferir

```bash
ls -l /etc/nome-do-projeto/tls
```

Esperado:
- `fullchain.crt`
- `private.key`

## 5. Como O Compose Usa Isso

No `.env.prod`:

```dotenv
TLS_CERT_FILE=/etc/nome-do-projeto/tls/fullchain.crt
TLS_KEY_FILE=/etc/nome-do-projeto/tls/private.key
```

No `docker-compose.prod.yml`, o frontend monta esses arquivos no container para abrir a porta `443`.

## 6. Se Os Arquivos Não Existirem

Se `FRONTEND_TLS_ENABLED=true` e os arquivos não existirem:
- o frontend não sobe
- o HTTPS não fica disponível

Para verificar:

```bash
docker compose --env-file /etc/nome-do-projeto/.env.prod -f /opt/nome-do-projeto/docker-compose.prod.yml logs -f frontend
```
