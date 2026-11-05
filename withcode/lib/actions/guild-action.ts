import "server-only";
import type { z } from "zod";
import { revalidatePath } from "next/cache";
import { db, type Prisma } from "@/lib/db";
import { AccessError, getGuildAccess, type GuildContext } from "@/lib/permissions";
import { canEdit, canManageAccess } from "@/lib/permissions/resolve";
import { rateLimit } from "@/lib/security/rate-limit";
import { computeDiff } from "@/lib/audit";
import { getBotApi, type ConfigSection } from "@/lib/bot-api";
import type { ActionResult } from "./result";

export class ConflictError extends Error {}

type Options = { section: ConfigSection; auditSection?: string; requireAccessManage?: boolean };

/**
 * Wspólny wrapper dla zapisów konfiguracji:
 * auth → walidacja zod → uprawnienia (przy każdym zapisie) → rate limit → transakcja + audit log
 * → powiadomienie bota → revalidate. Zwraca wynik zamiast rzucać wyjątki do UI.
 */
export async function guildAction<S extends z.ZodType, R>(
  guildId: string,
  schema: S,
  input: unknown,
  opts: Options,
  handler: (ctx: {
    tx: Prisma.TransactionClient;
    access: GuildContext;
    data: z.infer<S>;
  }) => Promise<{ result: R; before: unknown; after: unknown }>,
): Promise<ActionResult<R>> {
  let access: GuildContext;
  try {
    access = await getGuildAccess(guildId);
  } catch (e) {
    if (e instanceof AccessError)
      return { ok: false, error: e.code === "unauthorized" ? "unauthorized" : "forbidden" };
    return { ok: false, error: "server" };
  }
  if (!canEdit(access.level) || (opts.requireAccessManage && !canManageAccess(access.level))) {
    return { ok: false, error: "forbidden" };
  }
  const parsed = schema.safeParse(input);
  if (!parsed.success) {
    return {
      ok: false,
      error: "validation",
      issues: parsed.error.issues.map((i) => ({ path: i.path.join("."), message: i.message })),
    };
  }
  const limit = await rateLimit(`action:${access.user.discordId}:${guildId}`, 30, 60);
  if (!limit.ok) return { ok: false, error: "rate_limited" };

  try {
    const result = await db.$transaction(async (tx) => {
      const out = await handler({ tx, access, data: parsed.data });
      const diff = computeDiff(out.before, out.after);
      if (diff.length > 0) {
        await tx.configAuditLog.create({
          data: {
            guildId,
            actorId: access.user.id,
            section: opts.auditSection ?? opts.section,
            diff: diff as unknown as Prisma.InputJsonValue,
          },
        });
      }
      return out.result;
    });
    await getBotApi().notifyConfigChanged(guildId, opts.section);
    revalidatePath(`/[locale]/dashboard/${guildId}`, "layout");
    return { ok: true, data: result };
  } catch (e) {
    if (e instanceof ConflictError) return { ok: false, error: "conflict" };
    console.error("[guildAction]", opts.section, e);
    return { ok: false, error: "server" };
  }
}

/** Optimistic concurrency: rzuca ConflictError, gdy wersja w bazie jest inna niż oczekiwana. */
export function assertVersion(current: number | undefined, expected: number): void {
  if (current !== undefined && current !== expected) throw new ConflictError();
}
