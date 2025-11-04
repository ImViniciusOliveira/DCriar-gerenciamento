# frontend/Dockerfile (multi-stage: build -> nginx)

# --- STAGE 1: build with Node ---
FROM node:20-alpine AS build
WORKDIR /app

# copy package files first to leverage cache
COPY package*.json ./
# Use npm ci if package-lock exists; fallback to npm install otherwise
RUN npm ci --legacy-peer-deps || npm install --legacy-peer-deps

# copy rest of the sources and build
COPY . .
# Build for production (Angular default output will be under dist/<project-name>)
RUN npm run build -- --configuration production

# --- STAGE 2: serve with nginx ---
FROM nginx:stable-alpine AS prod

# Remove default nginx site (we will overwrite the conf)
RUN rm -f /etc/nginx/conf.d/default.conf || true

# Copy compiled app. Angular default output folder: dist/<project-name>
# Project name is 'dcriar' (ver /frontend/package.json)
COPY --from=build /app/dist/dcriar /usr/share/nginx/html

# Copy a custom nginx config if present in the build context
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

# Expose default HTTP port
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

