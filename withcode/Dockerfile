# syntax=docker/dockerfile:1.7
# WithCode.pl — obraz produkcyjny (Next.js standalone)

FROM node:22-alpine AS base
RUN corepack enable && apk add --no-cache libc6-compat
WORKDIR /app

FROM base AS deps
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY prisma ./prisma
COPY prisma.config.ts ./
ENV DATABASE_URL="postgresql://build:build@localhost:5432/build"
RUN --mount=type=cache,id=pnpm,target=/root/.local/share/pnpm/store pnpm install --frozen-lockfile

FROM base AS builder
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1 \
    NEXT_OUTPUT=standalone \
    SKIP_ENV_VALIDATION=1 \
    DATABASE_URL="postgresql://build:build@localhost:5432/build"
ARG NEXT_PUBLIC_SITE_URL=https://withcode.pl
ARG NEXT_PUBLIC_DISCORD_CLIENT_ID=
ARG NEXT_PUBLIC_SUPPORT_SERVER_URL=https://discord.gg/withcode
ENV NEXT_PUBLIC_SITE_URL=$NEXT_PUBLIC_SITE_URL \
    NEXT_PUBLIC_DISCORD_CLIENT_ID=$NEXT_PUBLIC_DISCORD_CLIENT_ID \
    NEXT_PUBLIC_SUPPORT_SERVER_URL=$NEXT_PUBLIC_SUPPORT_SERVER_URL
RUN pnpm prisma generate && pnpm build

FROM base AS migrator
# Lekki etap do `prisma migrate deploy` (uruchamiany przez docker compose przed aplikacją)
COPY --from=deps /app/node_modules ./node_modules
COPY prisma ./prisma
COPY prisma.config.ts package.json ./
COPY content ./content
COPY lib ./lib
CMD ["sh", "-c", "pnpm prisma migrate deploy && if [ \"$SEED\" = \"1\" ]; then pnpm db:seed; fi"]

FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 PORT=3000 HOSTNAME=0.0.0.0
RUN addgroup -S nodejs -g 1001 && adduser -S nextjs -u 1001 -G nodejs
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static
COPY --from=builder --chown=nextjs:nodejs /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/content ./content
USER nextjs
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s CMD wget -qO- http://127.0.0.1:3000/api/status >/dev/null || exit 1
CMD ["node", "server.js"]
