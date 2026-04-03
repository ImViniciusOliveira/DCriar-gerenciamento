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

# Em vez de mover arquivos, geramos dinamicamente uma configuração nginx no momento do build.
# Ela define o `root` como /usr/share/nginx/html/browser (padrão do Angular 20)
# ou /usr/share/nginx/html caso a pasta browser não exista.
RUN set -eux; \
    if [ -d /usr/share/nginx/html/browser ]; then ROOT_DIR="/usr/share/nginx/html/browser"; else ROOT_DIR="/usr/share/nginx/html"; fi; \
    cat > /etc/nginx/conf.d/default.conf <<EOF
server {
    listen 80;
    server_name _;

    root ${ROOT_DIR};
    index index.html;

    # Cabeçalhos de segurança
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Desabilita listagem de diretórios
    autoindex off;

    # Endpoint simples de healthcheck
    location = /health {
        default_type text/plain;
        return 200 'ok';
    }

    # O frontend fala com /api e o nginx encaminha para o backend na rede interna do compose.
    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    # Cache longo para arquivos estáticos versionados (hash no nome)
    location ~* \.(?:css|js|svg|png|jpg|jpeg|gif|ico|woff2?|ttf|eot)\$ {
        try_files \$uri =404;
        expires max;
        add_header Cache-Control "public, max-age=31536000, immutable" always;
    }

    # Não aplicar cache pesado ao index.html
    location = /index.html {
        add_header Cache-Control "no-cache, must-revalidate, private" always;
        try_files /index.html =404;
    }

    # Fallback para rotas SPA (Single Page Application)
    location / {
        try_files \$uri \$uri/ /index.html;
    }

    # Habilita compressão gzip
    gzip on;
    gzip_types text/plain application/javascript application/json text/css application/xml;
    gzip_min_length 1000;
    gzip_comp_level 5;
}
EOF

# Verifica se o index.html existe (para evitar servir a página padrão do nginx)
RUN if [ ! -f /usr/share/nginx/html/index.html ] && [ ! -f /usr/share/nginx/html/browser/index.html ]; then echo "ERRO: index.html não encontrado na imagem" >&2; exit 1; fi

# Garante permissões para que o nginx (usuário nginx) consiga ler os arquivos
RUN chown -R nginx:nginx /usr/share/nginx/html || true
RUN chmod -R a+r /usr/share/nginx/html || true

# Expõe a porta HTTP padrão
EXPOSE 80

# Comando principal que mantém o nginx em execução
CMD ["nginx", "-g", "daemon off;"]
