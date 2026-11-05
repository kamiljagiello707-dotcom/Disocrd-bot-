/**
 * Jedyne źródło listy komend WithBota.
 * Dodanie komendy = dopisanie obiektu do tablicy `commands`. Strona /commands, endpoint
 * /api/commands i dokumentacja czytają dane stąd.
 */
import type { Localized } from "@/lib/utils";

export const commandCategories = [
  { id: "moderation", label: { pl: "Moderacja", en: "Moderation" } },
  { id: "automod", label: { pl: "AutoMod", en: "AutoMod" } },
  { id: "antiraid", label: { pl: "Anty-raid", en: "Anti-raid" } },
  { id: "logs", label: { pl: "Logi", en: "Logs" } },
  { id: "verification", label: { pl: "Weryfikacja", en: "Verification" } },
  { id: "roles", label: { pl: "Role", en: "Roles" } },
  { id: "welcome", label: { pl: "Powitania", en: "Greetings" } },
  { id: "tickets", label: { pl: "Tickety", en: "Tickets" } },
  { id: "utility", label: { pl: "Narzędzia", en: "Utility" } },
] as const satisfies readonly { id: string; label: Localized }[];

export type CommandCategory = (typeof commandCategories)[number]["id"];

export type Command = {
  name: string;
  category: CommandCategory;
  description: Localized;
  syntax: string;
  permissions: string[];
  example: string;
  aliases?: string[];
  premium?: boolean;
};

export const commands: Command[] = [
  // Moderacja
  {
    name: "warn",
    category: "moderation",
    description: {
      pl: "Nadaje ostrzeżenie i zapisuje je jako sprawę. Liczy się do automatycznych kar.",
      en: "Issues a warning and records it as a case. Counts towards auto punishments.",
    },
    syntax: "/warn <użytkownik> [powód]",
    permissions: ["Moderate Members"],
    example: "/warn @Kuba Spam na #ogólny",
  },
  {
    name: "timeout",
    category: "moderation",
    description: {
      pl: "Wycisza użytkownika na określony czas (do 28 dni).",
      en: "Times a user out for a set duration (up to 28 days).",
    },
    syntax: "/timeout <użytkownik> <czas> [powód]",
    permissions: ["Moderate Members"],
    example: "/timeout @Kuba 1h Flood",
    aliases: ["mute"],
  },
  {
    name: "untimeout",
    category: "moderation",
    description: { pl: "Zdejmuje timeout przed czasem.", en: "Removes a timeout early." },
    syntax: "/untimeout <użytkownik> [powód]",
    permissions: ["Moderate Members"],
    example: "/untimeout @Kuba Odwołanie przyjęte",
    aliases: ["unmute"],
  },
  {
    name: "kick",
    category: "moderation",
    description: { pl: "Wyrzuca użytkownika z serwera.", en: "Kicks a user from the server." },
    syntax: "/kick <użytkownik> [powód]",
    permissions: ["Kick Members"],
    example: "/kick @Kuba Reklama w DM",
  },
  {
    name: "ban",
    category: "moderation",
    description: {
      pl: "Banuje użytkownika, opcjonalnie czasowo i z usunięciem wiadomości z ostatnich dni.",
      en: "Bans a user, optionally temporarily and with message deletion from recent days.",
    },
    syntax: "/ban <użytkownik> [czas] [usuń-dni] [powód]",
    permissions: ["Ban Members"],
    example: "/ban @Kuba 7d 1 Raid",
  },
  {
    name: "unban",
    category: "moderation",
    description: { pl: "Odbanowuje użytkownika po ID.", en: "Unbans a user by ID." },
    syntax: "/unban <id> [powód]",
    permissions: ["Ban Members"],
    example: "/unban 123456789012345678 Druga szansa",
  },
  {
    name: "softban",
    category: "moderation",
    description: {
      pl: "Banuje i od razu odbanowuje, usuwając wiadomości użytkownika.",
      en: "Bans and immediately unbans, deleting the user's messages.",
    },
    syntax: "/softban <użytkownik> [usuń-dni] [powód]",
    permissions: ["Ban Members"],
    example: "/softban @Kuba 1 Spam",
  },
  {
    name: "purge",
    category: "moderation",
    description: {
      pl: "Usuwa do 1000 wiadomości z filtrami: autor, boty, linki, załączniki.",
      en: "Deletes up to 1000 messages with filters: author, bots, links, attachments.",
    },
    syntax: "/purge <ilość> [użytkownik] [filtr]",
    permissions: ["Manage Messages"],
    example: "/purge 50 filtr:linki",
    aliases: ["clear"],
  },
  {
    name: "case",
    category: "moderation",
    description: {
      pl: "Pokazuje szczegóły sprawy moderacyjnej.",
      en: "Shows the details of a moderation case.",
    },
    syntax: "/case <numer>",
    permissions: ["Moderate Members"],
    example: "/case 128",
  },
  {
    name: "history",
    category: "moderation",
    description: { pl: "Lista wszystkich spraw użytkownika.", en: "Lists all cases of a user." },
    syntax: "/history <użytkownik>",
    permissions: ["Moderate Members"],
    example: "/history @Kuba",
    aliases: ["modlogs"],
  },
  {
    name: "reason",
    category: "moderation",
    description: {
      pl: "Zmienia powód istniejącej sprawy.",
      en: "Updates the reason of an existing case.",
    },
    syntax: "/reason <numer> <powód>",
    permissions: ["Moderate Members"],
    example: "/reason 128 Spam linkami",
  },
  {
    name: "slowmode",
    category: "moderation",
    description: { pl: "Ustawia tryb powolny na kanale.", en: "Sets slowmode on a channel." },
    syntax: "/slowmode <czas> [kanał]",
    permissions: ["Manage Channels"],
    example: "/slowmode 10s",
  },
  // AutoMod
  {
    name: "automod status",
    category: "automod",
    description: {
      pl: "Pokazuje, które moduły AutoModu są włączone i z jaką czułością.",
      en: "Shows which AutoMod modules are on and their sensitivity.",
    },
    syntax: "/automod status",
    permissions: ["Manage Server"],
    example: "/automod status",
  },
  {
    name: "automod toggle",
    category: "automod",
    description: {
      pl: "Włącza lub wyłącza moduł AutoModu.",
      en: "Turns an AutoMod module on or off.",
    },
    syntax: "/automod toggle <moduł> <wł/wył>",
    permissions: ["Manage Server"],
    example: "/automod toggle anty-link wł",
  },
  {
    name: "automod words",
    category: "automod",
    description: {
      pl: "Dodaje lub usuwa zakazane słowa. Obsługuje dzikie karty (*).",
      en: "Adds or removes banned words. Supports wildcards (*).",
    },
    syntax: "/automod words <dodaj/usuń/lista> [słowo]",
    permissions: ["Manage Server"],
    example: "/automod words dodaj kup*",
  },
  {
    name: "automod exempt",
    category: "automod",
    description: {
      pl: "Dodaje kanał lub rolę do wyjątków AutoModu.",
      en: "Adds a channel or role to AutoMod exemptions.",
    },
    syntax: "/automod exempt <kanał|rola>",
    permissions: ["Manage Server"],
    example: "/automod exempt #memy",
  },
  // Anty-raid
  {
    name: "lockdown",
    category: "antiraid",
    description: {
      pl: "Natychmiast blokuje pisanie dla @everyone na wszystkich kanałach.",
      en: "Instantly blocks @everyone from sending messages in every channel.",
    },
    syntax: "/lockdown [powód]",
    permissions: ["Manage Server"],
    example: "/lockdown Trwa raid",
  },
  {
    name: "unlock",
    category: "antiraid",
    description: {
      pl: "Kończy lockdown i przywraca poprzednie uprawnienia kanałów.",
      en: "Ends the lockdown and restores previous channel permissions.",
    },
    syntax: "/unlock",
    permissions: ["Manage Server"],
    example: "/unlock",
  },
  {
    name: "raid cleanup",
    category: "antiraid",
    description: {
      pl: "Banuje wszystkie konta, które dołączyły w podanym oknie czasu.",
      en: "Bans every account that joined within the given time window.",
    },
    syntax: "/raid cleanup <ostatnie-minuty>",
    permissions: ["Ban Members"],
    example: "/raid cleanup 5",
    premium: true,
  },
  // Logi
  {
    name: "logs set",
    category: "logs",
    description: {
      pl: "Ustawia kanał logów dla typu zdarzenia.",
      en: "Sets the log channel for an event type.",
    },
    syntax: "/logs set <zdarzenie> <kanał>",
    permissions: ["Manage Server"],
    example: "/logs set usunięte-wiadomości #logi",
  },
  {
    name: "logs disable",
    category: "logs",
    description: {
      pl: "Wyłącza logowanie wybranego zdarzenia.",
      en: "Disables logging for an event.",
    },
    syntax: "/logs disable <zdarzenie>",
    permissions: ["Manage Server"],
    example: "/logs disable głosowe",
  },
  // Weryfikacja
  {
    name: "verify setup",
    category: "verification",
    description: {
      pl: "Wysyła wiadomość weryfikacyjną z przyciskiem lub captchą.",
      en: "Posts the verification message with a button or captcha.",
    },
    syntax: "/verify setup <kanał> <rola> [metoda]",
    permissions: ["Manage Server"],
    example: "/verify setup #weryfikacja @Zweryfikowany captcha",
  },
  {
    name: "verify user",
    category: "verification",
    description: { pl: "Ręcznie weryfikuje użytkownika.", en: "Manually verifies a user." },
    syntax: "/verify user <użytkownik>",
    permissions: ["Manage Roles"],
    example: "/verify user @Ola",
  },
  // Role
  {
    name: "reactionroles create",
    category: "roles",
    description: {
      pl: "Tworzy wiadomość z rolami do samodzielnego wyboru.",
      en: "Creates a message with self-assignable roles.",
    },
    syntax: "/reactionroles create <kanał> <tytuł>",
    permissions: ["Manage Roles"],
    example: "/reactionroles create #role Wybierz gry",
  },
  {
    name: "autorole add",
    category: "roles",
    description: {
      pl: "Dodaje rolę nadawaną automatycznie przy dołączeniu.",
      en: "Adds a role granted automatically on join.",
    },
    syntax: "/autorole add <rola> [opóźnienie]",
    permissions: ["Manage Roles"],
    example: "/autorole add @Członek 10m",
  },
  {
    name: "role give",
    category: "roles",
    description: {
      pl: "Nadaje lub zdejmuje rolę użytkownikowi.",
      en: "Gives or removes a role from a user.",
    },
    syntax: "/role <dodaj/usuń> <użytkownik> <rola>",
    permissions: ["Manage Roles"],
    example: "/role dodaj @Ola @VIP",
  },
  // Powitania
  {
    name: "welcome test",
    category: "welcome",
    description: {
      pl: "Wysyła testowe powitanie z Twoimi danymi.",
      en: "Sends a test welcome message with your data.",
    },
    syntax: "/welcome test",
    permissions: ["Manage Server"],
    example: "/welcome test",
  },
  {
    name: "welcome set",
    category: "welcome",
    description: {
      pl: "Ustawia kanał i treść powitania. Zmienne: {user}, {server}, {memberCount}.",
      en: "Sets the welcome channel and text. Variables: {user}, {server}, {memberCount}.",
    },
    syntax: "/welcome set <kanał> <treść>",
    permissions: ["Manage Server"],
    example: "/welcome set #start Cześć {user}, jesteś {memberCount}. osobą!",
  },
  // Tickety
  {
    name: "ticket panel",
    category: "tickets",
    description: {
      pl: "Wysyła panel z przyciskiem otwierania ticketów.",
      en: "Posts a panel with a button for opening tickets.",
    },
    syntax: "/ticket panel <kanał> [kategoria]",
    permissions: ["Manage Server"],
    example: "/ticket panel #pomoc",
  },
  {
    name: "ticket close",
    category: "tickets",
    description: {
      pl: "Zamyka ticket i zapisuje transkrypt.",
      en: "Closes the ticket and saves a transcript.",
    },
    syntax: "/ticket close [powód]",
    permissions: ["Manage Channels"],
    example: "/ticket close Rozwiązane",
  },
  {
    name: "ticket add",
    category: "tickets",
    description: { pl: "Dodaje osobę do ticketa.", en: "Adds a person to the ticket." },
    syntax: "/ticket add <użytkownik>",
    permissions: ["Manage Channels"],
    example: "/ticket add @Ola",
  },
  // Narzędzia
  {
    name: "userinfo",
    category: "utility",
    description: {
      pl: "Informacje o użytkowniku: wiek konta, role, liczba spraw.",
      en: "User info: account age, roles, case count.",
    },
    syntax: "/userinfo [użytkownik]",
    permissions: [],
    example: "/userinfo @Kuba",
    aliases: ["whois"],
  },
  {
    name: "serverinfo",
    category: "utility",
    description: {
      pl: "Statystyki serwera i aktywnych modułów WithBota.",
      en: "Server statistics and active WithBot modules.",
    },
    syntax: "/serverinfo",
    permissions: [],
    example: "/serverinfo",
  },
  {
    name: "ping",
    category: "utility",
    description: { pl: "Opóźnienie bota i numer sharda.", en: "Bot latency and shard number." },
    syntax: "/ping",
    permissions: [],
    example: "/ping",
  },
  {
    name: "dashboard",
    category: "utility",
    description: { pl: "Link do panelu tego serwera.", en: "Link to this server's dashboard." },
    syntax: "/dashboard",
    permissions: [],
    example: "/dashboard",
  },
];
