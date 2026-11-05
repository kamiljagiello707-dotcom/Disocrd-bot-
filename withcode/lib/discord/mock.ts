import type { DiscordChannel, DiscordRole, DiscordUserGuild } from "./types";

/** Dane deweloperskie/E2E używane, gdy DISCORD_API_MOCK=1 (brak prawdziwej aplikacji Discord). */
export const MOCK_GUILDS: DiscordUserGuild[] = [
  {
    id: "200000000000000001",
    name: "WithCode Community",
    icon: null,
    owner: true,
    permissions: "2147483647",
    approximate_member_count: 12840,
  },
  {
    id: "200000000000000002",
    name: "Polski Minecraft",
    icon: null,
    owner: false,
    permissions: String(1n << 5n),
    approximate_member_count: 48210,
  },
  {
    id: "200000000000000003",
    name: "Dev Corner PL",
    icon: null,
    owner: false,
    permissions: String(1n << 3n),
    approximate_member_count: 15340,
  },
  {
    id: "200000000000000004",
    name: "Serwer znajomych",
    icon: null,
    owner: false,
    permissions: String(1n << 5n),
    approximate_member_count: 34,
  },
  {
    id: "200000000000000005",
    name: "Gdzie jestem zwykłym członkiem",
    icon: null,
    owner: false,
    permissions: "0",
    approximate_member_count: 900,
  },
];

export function mockChannels(guildId: string): DiscordChannel[] {
  const base = guildId.slice(-3);
  const names = [
    "ogólny",
    "ogłoszenia",
    "logi",
    "logi-automod",
    "weryfikacja",
    "witamy",
    "role",
    "memy",
    "pomoc",
    "offtopic",
  ];
  return [
    { id: `3${base}00000000000000`, name: "TEKSTOWE", type: 4, position: 0, parent_id: null },
    ...names.map((name, i) => ({
      id: `3${base}0000000000000${i + 1}`.padEnd(18, "0"),
      name,
      type: 0,
      position: i + 1,
      parent_id: `3${base}00000000000000`,
    })),
  ];
}

export function mockRoles(guildId: string): DiscordRole[] {
  const base = guildId.slice(-3);
  const names = [
    "@everyone",
    "Administrator",
    "Moderator",
    "Zweryfikowany",
    "Członek",
    "VIP",
    "Minecraft",
    "Valorant",
    "Ogłoszenia",
  ];
  return names.map((name, i) => ({
    id: `4${base}0000000000000${i}`.padEnd(18, "0"),
    name,
    color: 0,
    position: names.length - i,
    managed: false,
  }));
}
