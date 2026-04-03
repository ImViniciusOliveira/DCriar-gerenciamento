# DCriar - Sistema de Gestão de Estoque e Produção

## Visão Geral

O DCriar é um sistema completo para gestão de estoque e produção, projetado para pequenas e médias empresas que trabalham com a transformação de matéria-prima em produtos acabados. O sistema permite o controle total do ciclo de vida do produto, desde a entrada da matéria-prima até a venda final.

## Estrutura do Projeto

O projeto é dividido em duas partes principais:

* `backend/` — Aplicação Spring Boot que implementa a API REST e a lógica de negócio.
* `frontend/` — Aplicação Angular 20 que consome a API e fornece a interface do usuário.

Existem também scripts de deploy e um wrapper para `docker compose` em `scripts/lib/compose-run.sh`.

## Como Começar (Desenvolvimento)

1. Clone o repositório:

```bash
git clone https://github.com/seu-usuario/dcriar-sistema-inventario.git
cd dcriar-sistema-inventario
```

1. Backend (rodando localmente):

```bash
cd backend
mvn clean package
mvn spring-boot:run
```

A API ficará disponível em `http://localhost:8080`.

1. Frontend (desenvolvimento):

```bash
cd frontend
npm install
ng serve
```

Abra `http://localhost:4200` no navegador.

## Deploy com Docker Compose (dev / prod)

O projeto inclui um script `scripts/lib/compose-run.sh` que é um wrapper em torno do `docker compose` para facilitar builds, pushes e deploys em modos `dev` e `prod`.

Principais opções:

- `--mode <dev|prod>` — define o modo. Em `prod` o script usa por padrão `/etc/dcriar/.env.prod` e `/opt/dcriar/docker-compose.prod.yml`.
- `--env-file <arquivo>` — arquivo .env a ser usado (substitui o padrão do modo).
- `--compose-file <arquivo>` — arquivo compose adicional.
- `--build-images` — constrói as imagens locais antes do deploy.
- `--push-images` — envia (`docker push`) as imagens construídas para o registry.
- `--image-tag <tag>` — permite selecionar a tag das imagens construídas.
- `--no-sudo` — executa sem `sudo`.

Exemplos:

- Subir em `prod` usando as imagens do Docker Hub (pull + up -d):

```bash
sudo bash scripts/lib/compose-run.sh --mode prod --env-file /etc/dcriar/.env.prod --compose-file /opt/dcriar/docker-compose.prod.yml pull
sudo bash scripts/lib/compose-run.sh --mode prod --env-file /etc/dcriar/.env.prod --compose-file /opt/dcriar/docker-compose.prod.yml up -d
```

- Build local + push + subir (tag `latest` ou outra):

```bash
sudo bash scripts/lib/compose-run.sh --mode prod --env-file /etc/dcriar/.env.prod --compose-file /opt/dcriar/docker-compose.prod.yml --build-images --push-images --image-tag latest up -d
```

## Acesso pela rede local

Se o host em que você subiu o stack estiver na mesma rede local (e a porta `80:80` estiver mapeada para o host), outros computadores na mesma rede poderão acessar o sistema via `http://<IP_DO_HOST>/`.

O frontend encaminha `/api` para o backend internamente pelo nginx, então o backend não precisa ficar exposto publicamente no host.

Exemplo para descobrir o IP do host (Linux):

```bash
hostname -I | awk '{print $1}'
```

Observações de segurança:
- Em produção, configure firewalls e regras de rede apropriadas.
- Exponha apenas o frontend; mantenha o backend acessível só pela rede interna do compose sempre que possível.

---

Consulte `backend/README.md` e `frontend/README.md` para instruções específicas de cada parte.
