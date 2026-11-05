import { createEnv } from "@t3-oss/env-nextjs";
import { z } from "zod";

const bool = z
  .enum(["0", "1", "true", "false"])
  .optional()
  .transform((v) => v === "1" || v === "true");

const csv = z
  .string()
  .optional()
  .transform((v) =>
    (v ?? "")
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean),
  );

export const env = createEnv({
  server: {
    NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
    DATABASE_URL: z.string().url(),
    AUTH_SECRET: z.string().min(16).optional(),
    AUTH_DISCORD_ID: z.string().optional(),
    AUTH_DISCORD_SECRET: z.string().optional(),
    DISCORD_BOT_TOKEN: z.string().optional(),
    DISCORD_API_MOCK: bool,
    AUTH_DEV_LOGIN: bool,
    BOT_API_MODE: z.enum(["database", "http"]).default("database"),
    BOT_API_URL: z.string().url().optional(),
    BOT_API_SECRET: z.string().min(16).optional(),
    BOT_WEBHOOK_SECRET: z.string().min(16).optional(),
    ADMIN_DISCORD_IDS: csv,
    TOKEN_ENCRYPTION_KEY: z.string().optional(),
    IP_HASH_SALT: z.string().default("withcode-dev-salt"),
    TURNSTILE_SECRET_KEY: z.string().optional(),
    SENTRY_DSN: z.string().url().optional(),
  },
  client: {
    NEXT_PUBLIC_SITE_URL: z.string().url().default("http://localhost:3000"),
    NEXT_PUBLIC_DISCORD_CLIENT_ID: z.string().default("000000000000000000"),
    NEXT_PUBLIC_SUPPORT_SERVER_URL: z.string().url().default("https://discord.gg/withcode"),
    NEXT_PUBLIC_TURNSTILE_SITE_KEY: z.string().optional(),
    NEXT_PUBLIC_SENTRY_DSN: z.string().url().optional(),
  },
  experimental__runtimeEnv: {
    NEXT_PUBLIC_SITE_URL: process.env.NEXT_PUBLIC_SITE_URL,
    NEXT_PUBLIC_DISCORD_CLIENT_ID: process.env.NEXT_PUBLIC_DISCORD_CLIENT_ID,
    NEXT_PUBLIC_SUPPORT_SERVER_URL: process.env.NEXT_PUBLIC_SUPPORT_SERVER_URL,
    NEXT_PUBLIC_TURNSTILE_SITE_KEY: process.env.NEXT_PUBLIC_TURNSTILE_SITE_KEY,
    NEXT_PUBLIC_SENTRY_DSN: process.env.NEXT_PUBLIC_SENTRY_DSN,
  },
  emptyStringAsUndefined: true,
  skipValidation: process.env.SKIP_ENV_VALIDATION === "1",
});
