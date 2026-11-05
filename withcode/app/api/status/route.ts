import { NextResponse } from "next/server";
import { getBotApi } from "@/lib/bot-api";

export const revalidate = 30;

export async function GET() {
  const status = await getBotApi().getStatus();
  return NextResponse.json(status, {
    headers: { "Cache-Control": "public, s-maxage=30, stale-while-revalidate=60" },
  });
}
