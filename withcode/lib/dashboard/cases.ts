import "server-only";
import { db, type Prisma } from "@/lib/db";
import type { CasesQuery } from "@/lib/validators/guild";

export const CASES_PAGE_SIZE = 20;

export function casesWhere(guildId: string, q: CasesQuery): Prisma.ModCaseWhereInput {
  const where: Prisma.ModCaseWhereInput = { guildId };
  if (q.type) where.type = q.type;
  if (q.status === "active") where.revokedAt = null;
  if (q.status === "revoked") where.revokedAt = { not: null };
  if (q.q) {
    const num = Number(q.q.replace(/^#/, ""));
    where.OR = [
      { targetId: { contains: q.q } },
      { targetTag: { contains: q.q, mode: "insensitive" } },
      { reason: { contains: q.q, mode: "insensitive" } },
      ...(Number.isInteger(num) && num > 0 ? [{ caseNumber: num }] : []),
    ];
  }
  return where;
}

export async function listCases(guildId: string, q: CasesQuery) {
  const where = casesWhere(guildId, q);
  const [total, rows] = await Promise.all([
    db.modCase.count({ where }),
    db.modCase.findMany({
      where,
      orderBy: { createdAt: "desc" },
      skip: (q.page - 1) * CASES_PAGE_SIZE,
      take: CASES_PAGE_SIZE,
    }),
  ]);
  return { total, rows, pages: Math.max(1, Math.ceil(total / CASES_PAGE_SIZE)) };
}

/** Escapowanie CSV (RFC 4180) + ochrona przed formula injection w arkuszach. */
export function csvCell(value: unknown): string {
  let s =
    value === null || value === undefined
      ? ""
      : value instanceof Date
        ? value.toISOString()
        : String(value);
  if (/^[=+\-@\t\r]/.test(s)) s = `'${s}`;
  return /[",\n\r;]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}
