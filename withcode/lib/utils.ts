import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}

export type Localized = { pl: string; en: string };
export type AppLocale = "pl" | "en";

export function l(value: Localized, locale: string): string {
  return locale === "en" ? value.en : value.pl;
}

export function pick<T extends { [K in `${P}Pl` | `${P}En`]: string }, P extends string>(
  row: T,
  prefix: P,
  locale: string,
): string {
  const key = (locale === "en" ? `${prefix}En` : `${prefix}Pl`) as `${P}Pl` | `${P}En`;
  return row[key];
}

export function formatNumber(n: number, locale: string, compact = false): string {
  return new Intl.NumberFormat(locale === "en" ? "en-US" : "pl-PL", {
    notation: compact ? "compact" : "standard",
    maximumFractionDigits: compact ? 1 : 0,
  }).format(n);
}

export function clamp(v: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, v));
}

export function siteUrl(path = ""): string {
  const base = (process.env.NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000").replace(/\/$/, "");
  return `${base}${path}`;
}

export function inviteUrl(guildId?: string): string {
  const id = process.env.NEXT_PUBLIC_DISCORD_CLIENT_ID ?? "000000000000000000";
  const params = new URLSearchParams({
    client_id: id,
    permissions: "1099780156438",
    scope: "bot applications.commands",
  });
  if (guildId) {
    params.set("guild_id", guildId);
    params.set("disable_guild_select", "true");
  }
  return `https://discord.com/oauth2/authorize?${params.toString()}`;
}
