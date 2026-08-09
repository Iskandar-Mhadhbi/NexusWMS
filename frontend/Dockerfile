# ---- Build stage ----
FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# VITE_* vars are baked into the compiled JS at build time (Vite's own
# behavior, not a choice made here) — passed as build args so this image
# can be rebuilt with different values per environment without editing
# source. Docker Compose / CI supply these via `build.args`.
ARG VITE_API_BASE_URL
ARG VITE_WS_BASE_URL
ARG VITE_ANALYTICS_BASE_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL} \
    VITE_WS_BASE_URL=${VITE_WS_BASE_URL} \
    VITE_ANALYTICS_BASE_URL=${VITE_ANALYTICS_BASE_URL}

RUN npm run build

# ---- Runtime stage ----
FROM nginx:1.27-alpine

# Non-root user — required for K8s securityContext.runAsNonRoot
RUN addgroup -S webapp && adduser -S webapp -G webapp

COPY --from=build --chown=webapp:webapp /app/dist /usr/share/nginx/html
COPY --chown=webapp:webapp nginx.conf /etc/nginx/conf.d/default.conf

# nginx needs write access to these paths even when just serving static
# files (pid file, temp buffers for proxying/caching) — chown them to the
# non-root user rather than leaving nginx to fail trying to write as
# webapp with no permission on paths it doesn't own.
RUN chown -R webapp:webapp /var/cache/nginx /var/run \
    && touch /var/run/nginx.pid \
    && chown webapp:webapp /var/run/nginx.pid

USER webapp

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/ || exit 1

CMD ["nginx", "-g", "daemon off;"]