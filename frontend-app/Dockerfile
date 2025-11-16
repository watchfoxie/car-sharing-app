# Multi-stage build pentru frontend-app (Angular 20)
# Faza 1: Build cu Node.js
FROM node:22-alpine AS builder

WORKDIR /build

# Copiaz\u0103 package files pentru dependency caching
COPY frontend-app/package*.json ./

# Install dependencies
RUN npm ci --legacy-peer-deps

# Copiaz\u0103 surse \u0219i build pentru produc\u021bie
COPY frontend-app/ ./
RUN npm run build -- --configuration production

# Faza 2: Runtime cu nginx minimal
FROM nginx:1.27-alpine

# Metadata
LABEL maintainer="car-sharing-dev@example.com"
LABEL service="frontend-app"
LABEL version="1.0.0"

# Copiaz\u0103 build artifacts
COPY --from=builder /build/dist/frontend-app/browser /usr/share/nginx/html

# Copiaz\u0103 configura\u021bie nginx custom pentru SPA routing
COPY frontend-app/nginx.conf /etc/nginx/conf.d/default.conf

# Port nginx
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost/health || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
