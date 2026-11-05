import { NextResponse } from "next/server";
import { withGuild } from "@/lib/api";
import { assignableRoles, getGuildRoles } from "@/lib/discord/rest";

export async function GET(_req: Request, { params }: { params: Promise<{ guildId: string }> }) {
  const { guildId } = await params;
  return withGuild(guildId, async () => {
    const list = assignableRoles(await getGuildRoles(guildId)).map((r) => ({
      id: r.id,
      name: r.name,
    }));
    return NextResponse.json(list, { headers: { "Cache-Control": "private, max-age=60" } });
  });
}
