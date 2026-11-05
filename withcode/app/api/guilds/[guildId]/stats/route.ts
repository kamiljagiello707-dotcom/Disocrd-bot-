import { NextResponse, type NextRequest } from "next/server";
import { withGuild } from "@/lib/api";
import { getGuildStats, ranges, type Range } from "@/lib/dashboard/stats";

export async function GET(req: NextRequest, { params }: { params: Promise<{ guildId: string }> }) {
  const { guildId } = await params;
  const r = req.nextUrl.searchParams.get("range") ?? "7d";
  const range: Range = r in ranges ? (r as Range) : "7d";
  return withGuild(guildId, async () =>
    NextResponse.json(await getGuildStats(guildId, range), {
      headers: { "Cache-Control": "private, max-age=30" },
    }),
  );
}
