# Guia de Deploy (Resumo) — DCriar

Este documento descreve os passos mínimos para instalar e executar o sistema usando Docker Compose em dois ambientes: Debian (Linux) e Windows.

Importante: NUNCA comite o arquivo `.env.prod` com senhas reais no repositório.

1) Preparar arquivo de variáveis de ambiente (produção)
- Linux/Debian (recomendado): crie o arquivo `/etc/dcriar/.env.prod` com o conteúdo:

  ```dotenv
  POSTGRES_DB=seu_db
  POSTGRES_USER=seu_user
  POSTGRES_PASSWORD=sua_senha_segura
  MINIO_ROOT_USER=seu_minio_user
  MINIO_ROOT_PASSWORD=sua_senha_segura_minio
  MINIO_BUCKET_NAME=nome_do_bucket
  JWT_SECRET=seu_jwt_secret
  ```

  Ajuste permissões para que somente root leia:

  ```bash
  sudo mkdir -p /etc/dcriar
  sudo chown root:root /etc/dcriar
  sudo chmod 700 /etc/dcriar
  sudo tee /etc/dcriar/.env.prod > /dev/null <<'EOF'
  # cole aqui as variáveis
  EOF
  sudo chmod 600 /etc/dcriar/.env.prod
  ```

- Windows (quando não há /etc): crie `C:\dcriar\.env.prod` (ou outra pasta segura) e garanta permissões adequadas no Windows (somente Administradores/Usuário específico podem ler).

1) Executar o Docker Compose (produção)
- No Debian (exemplo):

  ```bash
  cd /caminho/para/dcriar-sistema-inventario
  docker compose -f docker-compose.prod.yml --env-file /etc/dcriar/.env.prod up -d
  ```

- No Windows (exemplo usando PowerShell com arquivo em C:):

  ```powershell
  cd C:\caminho\para\dcriar-sistema-inventario
  docker compose -f docker-compose.prod.yml --env-file C:\dcriar\.env.prod up -d
  ```

1) Observações
- O arquivo `docker-compose.prod.yml` foi ajustado para usar `env_file: /etc/dcriar/.env.prod` nos serviços que exigem variáveis sensíveis.
- Se usar outra localização no servidor do cliente, atualize o caminho em `env_file` ou passe `--env-file` ao executar `docker compose`.

1) Segurança
- Sempre use senhas fortes e únicas. Se um segredo foi exposto em um repo, rotacione-o.
- Considere usar um gerenciador de segredos (Vault, AWS Secrets Manager) em ambientes críticos.

## Variáveis de caminho para arquivos .env (DEV_ENV_FILE / PROD_ENV_FILE)

Para facilitar, os arquivos `docker-compose` aceitam variáveis que apontam para o arquivo `.env` a ser usado:

- `DEV_ENV_FILE` — caminho para o `.env` usado no `docker-compose.dev.yml`.
  - Padrão: `./.env.dev` (arquivo local no diretório do projeto)
  - Exemplo de uso (usar seu arquivo de secrets):

```bash
# opção 1: exportar a variável no shell
export DEV_ENV_FILE=/home/viniciusdev/dcriar/.secrets/.env.dev
docker compose -f docker-compose.dev.yml up --build

# opção 2: prefixar o comando (one-liner)
DEV_ENV_FILE=/home/viniciusdev/dcriar/.secrets/.env.dev docker compose -f docker-compose.dev.yml up --build
```

- `PROD_ENV_FILE` — caminho para o `.env` usado no `docker-compose.prod.yml`.
  - Padrão: `/etc/dcriar/.env.prod` (recomendado para servidores)
  - Exemplo de uso local com seus secrets:

```bash
PROD_ENV_FILE=/home/viniciusdev/dcriar/.secrets/.env.prod docker compose -f docker-compose.prod.yml up --build
```

### Observações importantes
- Nunca comite arquivos `.env` com segredos reais.
- Em produção, prefira o caminho `/etc/dcriar/.env.prod` com permissões restritas (chmod 600).

## Scripts auxiliares para desenvolvimento

Para facilitar, adicionei dois scripts em `scripts/` que executam o `docker compose` com o `--env-file` correto automaticamente:

- `scripts/up-dev.sh` — sobe o ambiente de desenvolvimento (equivalente a usar `--env-file /caminho/.env.dev`)
- `scripts/down-dev.sh` — derruba o ambiente e remove containers órfãos

Estes scripts escolhem o arquivo de env na seguinte ordem:
1. `DEV_ENV_FILE` (se estiver setada no ambiente)
2. `/home/viniciusdev/dcriar/.secrets/.env.dev` (se existir)
3. `./.env.dev` (arquivo local no projeto)

Exemplos:

```bash
# Subir com o arquivo de secrets do seu diretório
./scripts/up-dev.sh

# Subir e passar serviços/flags extras
./scripts/up-dev.sh --scale backend=0

# Derrubar o ambiente
./scripts/down-dev.sh
```
