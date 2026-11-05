import "server-only";
import { db } from "@/lib/db";
import { decryptToken, encryptToken } from "@/lib/security/crypto";

/** Zwraca ważny access token Discorda użytkownika, odświeżając go w razie potrzeby. */
export async function getDiscordAccessToken(userId: string): Promise<string | null> {
  const account = await db.account.findFirst({ where: { userId, provider: "discord" } });
  if (!account) return null;
  const access = decryptToken(account.access_token);
  const expiresAt = (account.expires_at ?? 0) * 1000;
  if (access && expiresAt - Date.now() > 60_000) return access;

  const refresh = decryptToken(account.refresh_token);
  if (!refresh || !process.env.AUTH_DISCORD_ID || !process.env.AUTH_DISCORD_SECRET) return access;
  try {
    const res = await fetch("https://discord.com/api/v10/oauth2/token", {
      method: "POST",
      headers: { "content-type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "refresh_token",
        refresh_token: refresh,
        client_id: process.env.AUTH_DISCORD_ID,
        client_secret: process.env.AUTH_DISCORD_SECRET,
      }),
      signal: AbortSignal.timeout(8000),
    });
    if (!res.ok) return null;
    const data = (await res.json()) as {
      access_token: string;
      refresh_token: string;
      expires_in: number;
    };
    await db.account.update({
      where: {
        provider_providerAccountId: {
          provider: "discord",
          providerAccountId: account.providerAccountId,
        },
      },
      data: {
        access_token: encryptToken(data.access_token) ?? null,
        refresh_token: encryptToken(data.refresh_token) ?? null,
        expires_at: Math.floor(Date.now() / 1000) + data.expires_in,
      },
    });
    return data.access_token;
  } catch {
    return null;
  }
}
