import { NextResponse } from "next/server";
import { withGuild } from "@/lib/api";
import { getGuildChannels, textChannels } from "@/lib/discord/rest";

export async function GET(_req: Request, { params }: { params: Promise<{ guildId: string }> }) {
  const { guildId } = await params;
  return withGuild(guildId, async () => {
    const list = textChannels(await getGuildChannels(guildId)).map((c) => ({
      id: c.id,
      name: c.name,
    }));
    return NextResponse.json(list, { headers: { "Cache-Control": "private, max-age=60" } });
  });
}
