import { randomBytes } from "node:crypto";
import { NextResponse, type NextRequest } from "next/server";
import { db } from "@/lib/db";
import { devLoginEnabled } from "@/lib/auth/dev";

/**
 * Logowanie testowe (mock OAuth) dla developmentu i E2E. Tworzy użytkownika i sesję w bazie
 * — dalej działa zwykły mechanizm sesji Auth.js. Wyłączone w produkcji.
 */
export async function GET(request: NextRequest) {
  if (!devLoginEnabled()) return new NextResponse("Not found", { status: 404 });
  const discordId =
    request.nextUrl.searchParams.get("as") === "user" ? "100000000000000099" : "100000000000000001";
  const callback = request.nextUrl.searchParams.get("callbackUrl") ?? "/dashboard";
  const user = await db.user.upsert({
    where: { discordId },
    create: {
      discordId,
      name: discordId.endsWith("1") ? "Admin (dev)" : "Moderator (dev)",
      image: null,
    },
    update: {},
  });
  const sessionToken = randomBytes(32).toString("hex");
  await db.session.create({
    data: { sessionToken, userId: user.id, expires: new Date(Date.now() + 30 * 864e5) },
  });
  const secure = request.nextUrl.protocol === "https:";
  const safeCallback =
    callback.startsWith("/") && !callback.startsWith("//") ? callback : "/dashboard";
  const res = NextResponse.redirect(new URL(safeCallback, request.url));
  res.cookies.set(secure ? "__Secure-authjs.session-token" : "authjs.session-token", sessionToken, {
    httpOnly: true,
    sameSite: "lax",
    secure,
    path: "/",
    expires: new Date(Date.now() + 30 * 864e5),
  });
  return res;
}
