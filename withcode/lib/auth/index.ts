import "server-only";
import NextAuth, { type DefaultSession } from "next-auth";
import Discord from "next-auth/providers/discord";
import { PrismaAdapter } from "@auth/prisma-adapter";
import type { Adapter, AdapterAccount } from "next-auth/adapters";
import { db } from "@/lib/db";
import { encryptToken } from "@/lib/security/crypto";

declare module "next-auth" {
  interface Session {
    user: { id: string; discordId: string | null; isAdmin: boolean } & DefaultSession["user"];
  }
  interface User {
    discordId?: string | null;
  }
}

function adapter(): Adapter {
  const base = PrismaAdapter(db as unknown as Parameters<typeof PrismaAdapter>[0]);
  return {
    ...base,
    // tokeny OAuth zapisujemy zaszyfrowane (AES-256-GCM)
    linkAccount: (account: AdapterAccount) =>
      base.linkAccount!({
        ...account,
        access_token: encryptToken(account.access_token) ?? undefined,
        refresh_token: encryptToken(account.refresh_token) ?? undefined,
      }),
  };
}

export function isAdminDiscordId(discordId: string | null | undefined): boolean {
  if (!discordId) return false;
  const ids = (process.env.ADMIN_DISCORD_IDS ?? "")
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
  return ids.includes(discordId);
}

export const { handlers, auth, signIn, signOut } = NextAuth({
  adapter: adapter(),
  session: { strategy: "database", maxAge: 60 * 60 * 24 * 30, updateAge: 60 * 60 * 24 },
  trustHost: true,
  pages: { signIn: "/login", error: "/login" },
  providers: [
    Discord({
      clientId: process.env.AUTH_DISCORD_ID,
      clientSecret: process.env.AUTH_DISCORD_SECRET,
      authorization: { params: { scope: "identify guilds", prompt: "none" } },
      profile(profile) {
        const avatar = profile.avatar
          ? `https://cdn.discordapp.com/avatars/${profile.id}/${profile.avatar}.${profile.avatar.startsWith("a_") ? "gif" : "png"}`
          : `https://cdn.discordapp.com/embed/avatars/${Number(BigInt(profile.id) >> 22n) % 6}.png`;
        return {
          id: profile.id,
          discordId: profile.id,
          name: profile.global_name ?? profile.username,
          email: null,
          image: avatar,
        };
      },
    }),
  ],
  callbacks: {
    session({ session, user }) {
      const discordId = (user as { discordId?: string | null }).discordId ?? null;
      session.user.id = user.id;
      session.user.discordId = discordId;
      session.user.isAdmin = isAdminDiscordId(discordId);
      return session;
    },
  },
  events: {
    async signIn({ user, account }) {
      // aktualizacja tokenów przy kolejnym logowaniu (adapter zapisuje je tylko przy pierwszym)
      if (account?.provider === "discord" && user.id) {
        await db.account
          .update({
            where: {
              provider_providerAccountId: {
                provider: "discord",
                providerAccountId: account.providerAccountId,
              },
            },
            data: {
              access_token: encryptToken(account.access_token) ?? null,
              refresh_token: encryptToken(account.refresh_token) ?? null,
              expires_at: account.expires_at ?? null,
              scope: account.scope ?? null,
            },
          })
          .catch(() => undefined);
        await db.user
          .update({ where: { id: user.id }, data: { discordId: account.providerAccountId } })
          .catch(() => undefined);
      }
    },
  },
});
