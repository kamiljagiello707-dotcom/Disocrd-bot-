import { canManageGuild } from "@/lib/discord/permissions";

export type AccessLevel = "OWNER" | "MANAGER" | "EDITOR" | "VIEWER";
export type AccessEntry = {
  subjectType: "USER" | "ROLE";
  subjectId: string;
  level: "VIEWER" | "EDITOR";
};

export type AccessInput = {
  userDiscordId: string;
  userGuild: { owner: boolean; permissions: string } | null;
  accessEntries: AccessEntry[];
  memberRoleIds: string[];
  botPresent: boolean;
  blacklisted: { guild: boolean; user: boolean };
  isGlobalAdmin?: boolean;
};

/**
 * Czysta funkcja decydująca o dostępie do panelu serwera (testowana jednostkowo).
 * Kolejność: blacklista → bot obecny → owner/Manage Server → wpisy DashboardAccess.
 */
export function resolveAccess(input: AccessInput): AccessLevel | null {
  if (input.blacklisted.user && !input.isGlobalAdmin) return null;
  if (input.blacklisted.guild && !input.isGlobalAdmin) return null;
  if (!input.botPresent) return null;
  if (input.userGuild?.owner) return "OWNER";
  if (input.userGuild && canManageGuild(input.userGuild)) return "MANAGER";
  let level: AccessLevel | null = null;
  for (const e of input.accessEntries) {
    const match =
      e.subjectType === "USER"
        ? e.subjectId === input.userDiscordId
        : input.memberRoleIds.includes(e.subjectId);
    if (!match) continue;
    if (e.level === "EDITOR") return "EDITOR";
    level = "VIEWER";
  }
  return level;
}

export const canEdit = (level: AccessLevel | null): boolean =>
  level === "OWNER" || level === "MANAGER" || level === "EDITOR";
/** Zarządzanie dostępem do panelu tylko dla właściciela/Manage Server. */
export const canManageAccess = (level: AccessLevel | null): boolean =>
  level === "OWNER" || level === "MANAGER";
