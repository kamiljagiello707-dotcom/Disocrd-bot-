import "server-only";
import { DatabaseBotApi } from "./database";
import { HttpBotApi } from "./http";
import type { BotApi } from "./types";

let instance: BotApi | null = null;

/** Zwraca implementację wybraną zmienną BOT_API_MODE (database | http). */
export function getBotApi(): BotApi {
  if (instance) return instance;
  const mode = process.env.BOT_API_MODE ?? "database";
  if (mode === "http") {
    const url = process.env.BOT_API_URL;
    const secret = process.env.BOT_API_SECRET;
    if (!url || !secret)
      throw new Error("BOT_API_MODE=http requires BOT_API_URL and BOT_API_SECRET");
    instance = new HttpBotApi(url, secret);
  } else {
    instance = new DatabaseBotApi();
  }
  return instance;
}

export type { BotApi, BotStatus, ConfigSection } from "./types";
