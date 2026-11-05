import { type NextRequest } from "next/server";
import { withGuild } from "@/lib/api";
import { db } from "@/lib/db";
import { casesQuerySchema } from "@/lib/validators/guild";
import { casesWhere, csvCell } from "@/lib/dashboard/cases";

/** Eksport spraw do CSV z bieżącymi filtrami (strumieniowo, partiami po 500). */
export async function GET(req: NextRequest, { params }: { params: Promise<{ guildId: string }> }) {
  const { guildId } = await params;
  const parsed = casesQuerySchema.safeParse(Object.fromEntries(req.nextUrl.searchParams));
  const q = parsed.success ? parsed.data : casesQuerySchema.parse({});
  return withGuild(guildId, async () => {
    const where = casesWhere(guildId, q);
    const header = [
      "case",
      "type",
      "target_id",
      "target",
      "moderator_id",
      "moderator",
      "reason",
      "source",
      "duration_s",
      "created_at",
      "revoked_at",
      "revoked_by",
    ];
    const encoder = new TextEncoder();
    const stream = new ReadableStream({
      async start(controller) {
        controller.enqueue(encoder.encode(`﻿${header.join(",")}\n`));
        let cursor: string | undefined;
        for (;;) {
          const batch = await db.modCase.findMany({
            where,
            orderBy: { id: "asc" },
            take: 500,
            ...(cursor ? { skip: 1, cursor: { id: cursor } } : {}),
          });
          for (const c of batch) {
            const row = [
              c.caseNumber,
              c.type,
              c.targetId,
              c.targetTag,
              c.moderatorId,
              c.moderatorTag,
              c.reason,
              c.source,
              c.duration,
              c.createdAt,
              c.revokedAt,
              c.revokedBy,
            ];
            controller.enqueue(encoder.encode(`${row.map(csvCell).join(",")}\n`));
          }
          if (batch.length < 500) break;
          cursor = batch[batch.length - 1]?.id;
        }
        controller.close();
      },
    });
    return new Response(stream, {
      headers: {
        "Content-Type": "text/csv; charset=utf-8",
        "Content-Disposition": `attachment; filename="withbot-cases-${guildId}.csv"`,
        "Cache-Control": "private, no-store",
      },
    });
  });
}
