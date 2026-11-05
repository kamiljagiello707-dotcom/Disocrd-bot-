export type ShardStatus = { id: number; online: boolean; pingMs: number; guilds: number };

export type BotStatus = {
  online: boolean;
  pingMs: number;
  guilds: number;
  shards: ShardStatus[];
  version: string;
  checkedAt: string;
  sample: boolean;
};

export type ConfigSection =
  | "automod"
  | "antiraid"
  | "punishments"
  | "logs"
  | "verification"
  | "welcome"
  | "roles"
  | "access"
  | "settings";

/**
 * Abstrakcja komunikacji z botem. Dwa warianty:
 * - `database`: bot czyta konfigurację z tej samej bazy; strona tylko powiadamia o zmianach,
 * - `http`: wewnętrzne API bota (podpis HMAC).
 */
export interface BotApi {
  readonly mode: "database" | "http";
  getStatus(): Promise<BotStatus>;
  /** ID serwerów (z podanej listy), na których jest bot */
  getGuildPresence(guildIds: string[]): Promise<Set<string>>;
  /** powiadom bota, że sekcja konfiguracji serwera się zmieniła */
  notifyConfigChanged(guildId: string, section: ConfigSection): Promise<void>;
  setLockdown(guildId: string, active: boolean, actorId: string): Promise<void>;
  revokeCase(guildId: string, caseId: string, actorId: string): Promise<void>;
}
