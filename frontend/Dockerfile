# frontend/Dockerfile (multi-stage: build -> nginx)

# --- ESTÁGIO 1: build com Node ---
FROM node:20-alpine AS build
WORKDIR /app

# Copia apenas os arquivos de dependências primeiro para aproveitar o cache do Docker
COPY package*.json ./
# Usa npm ci se existir package-lock; caso contrário, usa npm install
RUN npm ci --legacy-peer-deps || npm install --legacy-peer-deps

# Copia o restante do código e executa o build
# (A saída padrão do Angular ficará em dist/<nome-do-projeto>)
COPY . .
RUN npm run build -- --configuration production

# Verifica se o build gerou o diretório esperado (falha cedo se não existir)
RUN if [ ! -d "dist/dcriar" ] || [ -z "$(ls -A dist/dcriar 2>/dev/null)" ]; then echo "ERRO: build não gerou 'dist/dcriar'" >&2; exit 1; fi

# --- ESTÁGIO 2: servir com nginx ---
FROM nginx:stable-alpine AS prod

# Remove o site padrão do nginx (iremos gerar uma configuração personalizada)
RUN rm -f /etc/nginx/conf.d/default.conf || true

# Copia a aplicação compilada.
# Mantemos a estrutura padrão do Angular (ex: dist/dcriar/browser)
COPY --from=build /app/dist/dcriar /usr/share/nginx/html

# Templates e script de bootstrap do nginx.
# O script escolhe entre HTTP puro e HTTPS com redirect, mantendo o certificado fora da imagem.
COPY nginx/app.common.conf.template /opt/dcriar/nginx/app.common.conf.template
COPY nginx/default.http.conf.template /opt/dcriar/nginx/default.http.conf.template
COPY nginx/default.https.conf.template /opt/dcriar/nginx/default.https.conf.template
COPY nginx/30-generate-config.sh /docker-entrypoint.d/30-generate-config.sh
RUN chmod +x /docker-entrypoint.d/30-generate-config.sh

# Verifica se o index.html existe (para evitar servir a página padrão do nginx)
RUN if [ ! -f /usr/share/nginx/html/index.html ] && [ ! -f /usr/share/nginx/html/browser/index.html ]; then echo "ERRO: index.html não encontrado na imagem" >&2; exit 1; fi

# Garante permissões para que o nginx (usuário nginx) consiga ler os arquivos
RUN chown -R nginx:nginx /usr/share/nginx/html || true
RUN chmod -R a+r /usr/share/nginx/html || true

# Expõe HTTP e HTTPS no container.
EXPOSE 80 443

# Comando principal que mantém o nginx em execução
CMD ["nginx", "-g", "daemon off;"]
