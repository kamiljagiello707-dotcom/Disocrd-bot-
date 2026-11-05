import "server-only";
import { db } from "@/lib/db";

type Result = { ok: boolean; remaining: number; resetAt: number };

const memory = new Map<string, { count: number; reset: number }>();

/**
 * Rate limit typu fixed-window. Domyślnie w pamięci procesu (szybko, wystarcza na jednym
 * serwerze/VPS); `RATE_LIMIT_STORE=database` używa tabeli RateLimitHit (wiele instancji).
 */
export async function rateLimit(key: string, limit: number, windowSec: number): Promise<Result> {
  const now = Date.now();
  const windowMs = windowSec * 1000;
  const windowStart = Math.floor(now / windowMs) * windowMs;
  const reset = windowStart + windowMs;

  if (process.env.RATE_LIMIT_STORE === "database") {
    try {
      const row = await db.rateLimitHit.upsert({
        where: { key_windowStart: { key, windowStart: new Date(windowStart) } },
        create: { key, windowStart: new Date(windowStart), count: 1 },
        update: { count: { increment: 1 } },
      });
      return { ok: row.count <= limit, remaining: Math.max(0, limit - row.count), resetAt: reset };
    } catch {
      /* fallback do pamięci */
    }
  }

  const entry = memory.get(key);
  if (!entry || entry.reset <= now) {
    memory.set(key, { count: 1, reset });
    if (memory.size > 10_000) for (const [k, v] of memory) if (v.reset <= now) memory.delete(k);
    return { ok: true, remaining: limit - 1, resetAt: reset };
  }
  entry.count += 1;
  return {
    ok: entry.count <= limit,
    remaining: Math.max(0, limit - entry.count),
    resetAt: entry.reset,
  };
}

export function resetRateLimits(): void {
  memory.clear();
}
