# Guia de Certificado TLS

Este guia mostra como gerar os dois arquivos usados pelo frontend/nginx para subir o HTTPS em rede local.

## Arquivos Esperados

```text
/etc/seu-projeto/tls/fullchain.crt
/etc/seu-projeto/tls/private.key
```

## 1. Criar A Pasta

```bash
sudo mkdir -p /etc/seu-projeto/tls
sudo chmod 700 /etc/seu-projeto/tls
sudo chown root:root /etc/seu-projeto/tls
```

## 2. Gerar O Certificado E A Chave

Exemplo para um servidor acessado por IP local:

```bash
sudo openssl req -x509 -nodes -newkey rsa:2048 \
  -keyout /etc/seu-projeto/tls/private.key \
  -out /etc/seu-projeto/tls/fullchain.crt \
  -days 365 \
  -subj "/CN=192.168.0.245" \
  -addext "subjectAltName=IP:192.168.0.245,IP:127.0.0.1,DNS:localhost"
```

Se o IP do servidor for outro, troque:
- `CN=192.168.0.245`
- `IP:192.168.0.245`

## 3. Proteger Os Arquivos

```bash
sudo chmod 600 /etc/seu-projeto/tls/fullchain.crt
sudo chmod 600 /etc/seu-projeto/tls/private.key
sudo chown root:root /etc/seu-projeto/tls/fullchain.crt
sudo chown root:root /etc/seu-projeto/tls/private.key
```

## 4. Conferir

```bash
ls -l /etc/seu-projeto/tls
```

Esperado:
- `fullchain.crt`
- `private.key`

## 5. Como O Compose Usa Isso

No `.env.prod`:

```dotenv
TLS_CERT_FILE=/etc/seu-projeto/tls/fullchain.crt
TLS_KEY_FILE=/etc/seu-projeto/tls/private.key
```

No `docker-compose.prod.yml`, o frontend monta esses arquivos no container para abrir a porta `443`.

## 6. Se Os Arquivos Não Existirem

Se `FRONTEND_TLS_ENABLED=true` e os arquivos não existirem:
- o frontend não sobe
- o HTTPS não fica disponível

Para verificar:

```bash
docker compose --env-file /etc/seu-projeto/.env.prod -f /opt/seu-projeto/docker-compose.prod.yml logs -f frontend
```
