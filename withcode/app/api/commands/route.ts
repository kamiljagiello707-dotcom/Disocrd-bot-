import { NextResponse } from "next/server";
import { commandCategories, commands } from "@/content/commands";

export const dynamic = "force-static";

/** Publiczny JSON z listą komend — to samo źródło co strona /commands. Bot może go synchronizować. */
export function GET() {
  return NextResponse.json({ categories: commandCategories, commands, count: commands.length });
}
