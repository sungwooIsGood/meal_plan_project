# syntax=docker/dockerfile:1

# ---- Build stage: pnpm 으로 정적 파일(dist) 생성 ----
FROM node:22-alpine AS build
WORKDIR /app

# pnpm 활성화 (corepack)
RUN corepack enable

# 1) 의존성 매니페스트 먼저 복사 → 캐시 활용
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

# 2) 소스 복사 후 빌드 (VITE_API_BASE_URL 은 비워둠 → nginx 가 /api 프록시)
COPY . .
RUN pnpm build

# ---- Runtime stage: nginx 가 정적 파일 서빙 + /api 프록시 ----
FROM nginx:1.27-alpine AS runtime

# SPA 라우팅 + /api → backend 프록시 설정
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
