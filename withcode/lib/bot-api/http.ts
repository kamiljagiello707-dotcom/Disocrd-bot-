import "server-only";
import { randomUUID } from "node:crypto";
import { sign, NONCE_HEADER, SIGNATURE_HEADER, TIMESTAMP_HEADER } from "@/lib/security/hmac";
import { sampleStatus } from "./database";
import type { BotApi, BotStatus, ConfigSection } from "./types";

/** Wariant „API bota”: żądania podpisywane HMAC wspólnym sekretem BOT_API_SECRET. */
export class HttpBotApi implements BotApi {
  readonly mode = "http" as const;
  constructor(
    private readonly baseUrl: string,
    private readonly secret: string,
  ) {}

  private async request<T>(
    path: string,
    init: { method?: string; body?: unknown; revalidate?: number } = {},
  ): Promise<T> {
    const body = init.body === undefined ? "" : JSON.stringify(init.body);
    const ts = Math.floor(Date.now() / 1000).toString();
    const res = await fetch(`${this.baseUrl.replace(/\/$/, "")}${path}`, {
      method: init.method ?? "GET",
      headers: {
        "content-type": "application/json",
        [SIGNATURE_HEADER]: sign(this.secret, ts, body),
        [TIMESTAMP_HEADER]: ts,
        [NONCE_HEADER]: randomUUID(),
      },
      ...(body ? { body } : {}),
      signal: AbortSignal.timeout(5000),
      ...(init.revalidate
        ? { next: { revalidate: init.revalidate } }
        : { cache: "no-store" as const }),
    });
    if (!res.ok) throw new Error(`Bot API ${path} → ${res.status}`);
    return (await res.json()) as T;
  }

  async getStatus(): Promise<BotStatus> {
    try {
      return {
        ...(await this.request<Omit<BotStatus, "sample">>("/status", { revalidate: 30 })),
        sample: false,
      };
    } catch {
      return { ...sampleStatus(), online: false };
    }
  }

  async getGuildPresence(guildIds: string[]): Promise<Set<string>> {
    if (guildIds.length === 0) return new Set();
    const res = await this.request<{ present: string[] }>("/guilds/presence", {
      method: "POST",
      body: { guildIds },
    });
    return new Set(res.present);
  }

  async notifyConfigChanged(guildId: string, section: ConfigSection): Promise<void> {
    await this.request(`/guilds/${guildId}/config-changed`, {
      method: "POST",
      body: { section },
    }).catch(() => undefined);
  }

  async setLockdown(guildId: string, active: boolean, actorId: string): Promise<void> {
    await this.request(`/guilds/${guildId}/lockdown`, {
      method: "POST",
      body: { active, actorId },
    });
  }

  async revokeCase(guildId: string, caseId: string, actorId: string): Promise<void> {
    await this.request(`/guilds/${guildId}/cases/${caseId}/revoke`, {
      method: "POST",
      body: { actorId },
    });
  }
}
