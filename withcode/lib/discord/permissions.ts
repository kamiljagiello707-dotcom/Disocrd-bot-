/** Bity uprawnień Discorda (https://discord.com/developers/docs/topics/permissions). */
export const Permission = {
  ADMINISTRATOR: 1n << 3n,
  MANAGE_GUILD: 1n << 5n,
  MANAGE_ROLES: 1n << 28n,
} as const;

export function hasPermission(permissions: string | bigint, bit: bigint): boolean {
  try {
    const value = typeof permissions === "bigint" ? permissions : BigInt(permissions);
    return (value & bit) === bit;
  } catch {
    return false;
  }
}

/** Manage Server albo Administrator albo właściciel. */
export function canManageGuild(guild: { owner?: boolean; permissions: string }): boolean {
  if (guild.owner) return true;
  return (
    hasPermission(guild.permissions, Permission.ADMINISTRATOR) ||
    hasPermission(guild.permissions, Permission.MANAGE_GUILD)
  );
}
