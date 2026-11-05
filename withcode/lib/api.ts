import "server-only";
import { NextResponse } from "next/server";
import { AccessError, getGuildAccess, type GuildContext } from "@/lib/permissions";

/** Guard dla Route Handlerów panelu — te same reguły co w Server Actions. */
export async function withGuild(
  guildId: string,
  fn: (ctx: GuildContext) => Promise<Response>,
): Promise<Response> {
  try {
    const ctx = await getGuildAccess(guildId);
    return await fn(ctx);
  } catch (e) {
    if (e instanceof AccessError) {
      const status = e.code === "unauthorized" ? 401 : e.code === "not_found" ? 404 : 403;
      return NextResponse.json({ error: e.code }, { status });
    }
    console.error("[api]", e);
    return NextResponse.json({ error: "server" }, { status: 500 });
  }
}
