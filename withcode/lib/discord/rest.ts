import "server-only";
import { unstable_cache } from "next/cache";
import { MOCK_GUILDS, mockChannels, mockRoles } from "./mock";
import {
  ChannelType,
  type DiscordChannel,
  type DiscordMember,
  type DiscordRole,
  type DiscordUserGuild,
} from "./types";

const API = "https://discord.com/api/v10";
const isMock = () =>
  process.env.DISCORD_API_MOCK === "1" || process.env.DISCORD_API_MOCK === "true";

export class DiscordApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

/** Kolejka per „bucket” (ścieżka) + obsługa 429 retry_after i nagłówków X-RateLimit-*. */
const buckets = new Map<string, Promise<unknown>>();
const resetAt = new Map<string, number>();

async function discordFetch<T>(
  path: string,
  auth: { bot: true } | { bearer: string },
  attempt = 0,
): Promise<T> {
  const bucketKey = path.replace(/\d{17,20}/g, ":id");
  const prev = buckets.get(bucketKey) ?? Promise.resolve();
  const run = prev
    .catch(() => undefined)
    .then(async () => {
      const wait = (resetAt.get(bucketKey) ?? 0) - Date.now();
      if (wait > 0) await new Promise((r) => setTimeout(r, wait));
      const token =
        "bot" in auth ? `Bot ${process.env.DISCORD_BOT_TOKEN ?? ""}` : `Bearer ${auth.bearer}`;
      const res = await fetch(`${API}${path}`, {
        headers: {
          Authorization: token,
          "User-Agent": "WithCodeDashboard (https://withcode.pl, 1.0)",
        },
        cache: "no-store",
        signal: AbortSignal.timeout(8000),
      });
      const remaining = Number(res.headers.get("x-ratelimit-remaining") ?? "1");
      const resetAfter = Number(res.headers.get("x-ratelimit-reset-after") ?? "0");
      if (remaining === 0 && resetAfter > 0) resetAt.set(bucketKey, Date.now() + resetAfter * 1000);
      if (res.status === 429 && attempt < 3) {
        const body = (await res.json().catch(() => ({}))) as { retry_after?: number };
        await new Promise((r) => setTimeout(r, Math.ceil((body.retry_after ?? 1) * 1000)));
        return discordFetch<T>(path, auth, attempt + 1);
      }
      if (!res.ok) throw new DiscordApiError(res.status, `Discord ${path} → ${res.status}`);
      return (await res.json()) as T;
    });
  buckets.set(bucketKey, run);
  return run as Promise<T>;
}

/** Serwery użytkownika (token OAuth użytkownika) — cache 60 s per użytkownik. */
export async function getUserGuilds(
  userId: string,
  accessToken: string | null,
): Promise<DiscordUserGuild[]> {
  if (isMock()) return MOCK_GUILDS;
  if (!accessToken) throw new DiscordApiError(401, "Missing access token");
  return unstable_cache(
    () =>
      discordFetch<DiscordUserGuild[]>("/users/@me/guilds?with_counts=true", {
        bearer: accessToken,
      }),
    ["discord-user-guilds", userId],
    { revalidate: 60, tags: [`user:${userId}:guilds`] },
  )();
}

export const getGuildChannels = (guildId: string): Promise<DiscordChannel[]> =>
  isMock()
    ? Promise.resolve(mockChannels(guildId))
    : unstable_cache(
        () => discordFetch<DiscordChannel[]>(`/guilds/${guildId}/channels`, { bot: true }),
        ["discord-channels", guildId],
        {
          revalidate: 60,
          tags: [`guild:${guildId}:channels`],
        },
      )();

export const getGuildRoles = (guildId: string): Promise<DiscordRole[]> =>
  isMock()
    ? Promise.resolve(mockRoles(guildId))
    : unstable_cache(
        () => discordFetch<DiscordRole[]>(`/guilds/${guildId}/roles`, { bot: true }),
        ["discord-roles", guildId],
        {
          revalidate: 60,
          tags: [`guild:${guildId}:roles`],
        },
      )();

export async function getGuildMember(
  guildId: string,
  userId: string,
): Promise<DiscordMember | null> {
  if (isMock()) return { roles: [] };
  try {
    return await unstable_cache(
      () => discordFetch<DiscordMember>(`/guilds/${guildId}/members/${userId}`, { bot: true }),
      ["discord-member", guildId, userId],
      {
        revalidate: 60,
      },
    )();
  } catch (e) {
    if (e instanceof DiscordApiError && e.status === 404) return null;
    throw e;
  }
}

export function textChannels(channels: DiscordChannel[]): DiscordChannel[] {
  return channels
    .filter((c) => c.type === ChannelType.GUILD_TEXT || c.type === ChannelType.GUILD_ANNOUNCEMENT)
    .sort((a, b) => a.position - b.position);
}

export function assignableRoles(roles: DiscordRole[]): DiscordRole[] {
  return roles
    .filter((r) => !r.managed && r.name !== "@everyone")
    .sort((a, b) => b.position - a.position);
}

export function guildIconUrl(guild: { id: string; icon: string | null }, size = 96): string | null {
  return guild.icon
    ? `https://cdn.discordapp.com/icons/${guild.id}/${guild.icon}.webp?size=${size}`
    : null;
}
