export type DiscordUserGuild = {
  id: string;
  name: string;
  icon: string | null;
  owner: boolean;
  permissions: string;
  approximate_member_count?: number;
};

export type DiscordChannel = {
  id: string;
  name: string;
  type: number;
  position: number;
  parent_id: string | null;
};
export type DiscordRole = {
  id: string;
  name: string;
  color: number;
  position: number;
  managed: boolean;
};
export type DiscordMember = { user?: { id: string; username: string }; roles: string[] };

export const ChannelType = {
  GUILD_TEXT: 0,
  GUILD_VOICE: 2,
  GUILD_CATEGORY: 4,
  GUILD_ANNOUNCEMENT: 5,
} as const;
