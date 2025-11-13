# DCriar - Frontend

Este projeto front-end foi gerado com o Angular CLI (versão 20) e serve como interface web para a API do DCriar.

## Servidor de desenvolvimento

Para iniciar o servidor de desenvolvimento (hot-reload), execute:

```bash
npm install
ng serve
```

Abra o navegador em `http://localhost:4200/`. O app recarrega automaticamente quando você altera os arquivos fonte.

> Observação: em produção usamos a build Angular empacotada em uma imagem Nginx. Localmente o `ng serve` é útil para desenvolvimento rápido.

## Estrutura básica

- `src/` — código-fonte Angular
- `angular.json`, `package.json` — configurações do projeto
- `Dockerfile` — instruções para build multi-stage (Node -> Nginx)

## Build para produção

Para gerar os arquivos estáticos prontos para produção, rode:

```bash
npm ci --legacy-peer-deps
npm run build -- --configuration production
```

Os arquivos de saída serão colocados em `dist/<nome-do-projeto>` e no Dockerfile eles são copiados para `/usr/share/nginx/html`.

## Rodando com Docker (imagem local)

O `Dockerfile` no diretório `frontend` realiza um build multi-stage (Node -> Nginx). Para gerar a imagem localmente, execute:

```bash
# dentro da pasta frontend
sudo docker build -t imviniciusoliveira/dcriar-frontend:latest -f Dockerfile .
```

Para testar localmente executando o container, use:

```bash
sudo docker run --rm -p 80:80 imviniciusoliveira/dcriar-frontend:latest
```

> Se você publicar a imagem no Docker Hub, basta `docker pull` no servidor e subir via Compose.

## Testes

Para executar testes unitários (Karma) e e2e (dependendo da configuração), use os comandos padrão do Angular CLI:

```bash
npm test
# e2e (se configurado)
# ng e2e
```

## Documentação adicional

Consulte o README na raiz do projeto para instruções sobre como executar o backend e o stack via `scripts/lib/compose-run.sh`.
