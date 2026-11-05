import "server-only";
import { db } from "@/lib/db";
import type { BotApi, BotStatus, ConfigSection } from "./types";

/**
 * Wariant „wspólna baza”: bot i strona używają tej samej bazy PostgreSQL.
 * Zmiany sygnalizujemy przez NOTIFY — bot nasłuchuje `LISTEN withbot_config`.
 */
export class DatabaseBotApi implements BotApi {
  readonly mode = "database" as const;

  async getStatus(): Promise<BotStatus> {
    try {
      const [guilds, last] = await Promise.all([
        db.guild.count({ where: { botPresent: true } }),
        db.serviceCheck.findFirst({ where: { service: "BOT" }, orderBy: { checkedAt: "desc" } }),
      ]);
      if (!last) throw new Error("no heartbeat");
      return {
        online: last.ok && Date.now() - last.checkedAt.getTime() < 5 * 60_000,
        pingMs: last.latencyMs ?? 0,
        guilds,
        shards: [{ id: 0, online: last.ok, pingMs: last.latencyMs ?? 0, guilds }],
        version: "2.4.0",
        checkedAt: last.checkedAt.toISOString(),
        sample: false,
      };
    } catch {
      return sampleStatus();
    }
  }

  async getGuildPresence(guildIds: string[]): Promise<Set<string>> {
    if (guildIds.length === 0) return new Set();
    try {
      const rows = await db.guild.findMany({
        where: { id: { in: guildIds }, botPresent: true },
        select: { id: true },
      });
      return new Set(rows.map((r) => r.id));
    } catch {
      return new Set();
    }
  }

  async notifyConfigChanged(guildId: string, section: ConfigSection): Promise<void> {
    const payload = JSON.stringify({ guildId, section, at: Date.now() });
    await db.$executeRaw`SELECT pg_notify('withbot_config', ${payload})`.catch(() => undefined);
  }

  async setLockdown(guildId: string, active: boolean, actorId: string): Promise<void> {
    await db.antiRaidConfig.upsert({
      where: { guildId },
      create: {
        guildId,
        lockdownActive: active,
        lockdownAt: active ? new Date() : null,
        lockdownBy: active ? actorId : null,
      },
      update: {
        lockdownActive: active,
        lockdownAt: active ? new Date() : null,
        lockdownBy: active ? actorId : null,
        version: { increment: 1 },
      },
    });
    await this.notifyConfigChanged(guildId, "antiraid");
  }

  async revokeCase(guildId: string, caseId: string, actorId: string): Promise<void> {
    void actorId;
    await this.notifyConfigChanged(guildId, "punishments");
    void caseId;
  }
}

export function sampleStatus(): BotStatus {
  return {
    online: true,
    pingMs: 42,
    guilds: 18_420,
    shards: Array.from({ length: 8 }, (_, id) => ({
      id,
      online: true,
      pingMs: 38 + ((id * 7) % 12),
      guilds: 2300,
    })),
    version: "2.4.0",
    checkedAt: new Date().toISOString(),
    sample: true,
  };
}
