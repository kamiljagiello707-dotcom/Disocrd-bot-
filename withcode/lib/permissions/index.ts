import "server-only";
import { cache } from "react";
import { auth } from "@/lib/auth";
import { getDiscordAccessToken } from "@/lib/auth/tokens";
import { db } from "@/lib/db";
import { getGuildMember, getUserGuilds } from "@/lib/discord/rest";
import { canManageGuild } from "@/lib/discord/permissions";
import { getBotApi } from "@/lib/bot-api";
import type { DiscordUserGuild } from "@/lib/discord/types";
import { resolveAccess, type AccessLevel } from "./resolve";

export class AccessError extends Error {
  constructor(readonly code: "unauthorized" | "forbidden" | "not_found") {
    super(code);
  }
}

export type SessionUser = {
  id: string;
  discordId: string;
  name: string | null;
  image: string | null;
  isAdmin: boolean;
};

export const requireUser = cache(async (): Promise<SessionUser> => {
  const session = await auth();
  if (!session?.user?.id || !session.user.discordId) throw new AccessError("unauthorized");
  return {
    id: session.user.id,
    discordId: session.user.discordId,
    name: session.user.name ?? null,
    image: session.user.image ?? null,
    isAdmin: session.user.isAdmin,
  };
});

export const getOptionalUser = cache(async (): Promise<SessionUser | null> => {
  try {
    return await requireUser();
  } catch {
    return null;
  }
});

async function loadUserGuilds(user: SessionUser): Promise<DiscordUserGuild[]> {
  const token = await getDiscordAccessToken(user.id);
  return getUserGuilds(user.discordId, token);
}

export type ManageableGuild = DiscordUserGuild & { botPresent: boolean };

/** Serwery z panelem: Manage Server/owner + serwery, gdzie dostęp nadano przez DashboardAccess. */
export const getManageableGuilds = cache(async (): Promise<ManageableGuild[]> => {
  const user = await requireUser();
  const guilds = await loadUserGuilds(user);
  const managed = guilds.filter((g) => canManageGuild(g));
  const extra = await db.dashboardAccess
    .findMany({
      where: { subjectType: "USER", subjectId: user.discordId },
      select: { guildId: true },
    })
    .catch(() => []);
  const extraIds = new Set(extra.map((e) => e.guildId));
  const list = guilds.filter((g) => canManageGuild(g) || extraIds.has(g.id));
  const present = await getBotApi().getGuildPresence(list.map((g) => g.id));
  void managed;
  return list
    .map((g) => ({ ...g, botPresent: present.has(g.id) }))
    .sort((a, b) => Number(b.botPresent) - Number(a.botPresent) || a.name.localeCompare(b.name));
});

export type GuildContext = { user: SessionUser; guild: DiscordUserGuild; level: AccessLevel };

/**
 * Sprawdzenie uprawnień do serwera — wywoływane w KAŻDYM layoucie panelu, Server Action i
 * Route Handlerze. Nie ufa niczemu, co przyszło z przeglądarki.
 */
export const getGuildAccess = cache(async (guildId: string): Promise<GuildContext> => {
  const user = await requireUser();
  if (!/^\d{17,20}$/.test(guildId)) throw new AccessError("not_found");
  const guilds = await loadUserGuilds(user);
  const userGuild = guilds.find((g) => g.id === guildId) ?? null;
  const [entries, blacklist, present] = await Promise.all([
    db.dashboardAccess.findMany({ where: { guildId } }),
    db.blacklistEntry.findMany({
      where: {
        OR: [
          { type: "GUILD", targetId: guildId },
          { type: "USER", targetId: user.discordId },
        ],
      },
    }),
    getBotApi().getGuildPresence([guildId]),
  ]);
  const active = blacklist.filter((b) => !b.expiresAt || b.expiresAt > new Date());
  const needsRoles = !userGuild || (!userGuild.owner && !canManageGuild(userGuild));
  const member =
    needsRoles && entries.some((e) => e.subjectType === "ROLE")
      ? await getGuildMember(guildId, user.discordId)
      : null;
  const level = resolveAccess({
    userDiscordId: user.discordId,
    userGuild,
    accessEntries: entries,
    memberRoleIds: member?.roles ?? [],
    botPresent: present.has(guildId),
    blacklisted: {
      guild: active.some((b) => b.type === "GUILD"),
      user: active.some((b) => b.type === "USER"),
    },
    isGlobalAdmin: user.isAdmin,
  });
  if (!level) throw new AccessError("forbidden");
  const guild: DiscordUserGuild = userGuild ?? {
    id: guildId,
    name: guildId,
    icon: null,
    owner: false,
    permissions: "0",
  };
  return { user, guild, level };
});

export async function requireAdmin(): Promise<SessionUser> {
  const user = await requireUser();
  if (!user.isAdmin) throw new AccessError("not_found");
  return user;
}
