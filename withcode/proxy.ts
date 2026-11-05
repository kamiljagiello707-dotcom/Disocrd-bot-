import { NextResponse, type NextRequest } from "next/server";
import createIntlMiddleware from "next-intl/middleware";
import { routing } from "./i18n/routing";
import { buildCsp } from "./lib/security/csp";

const intl = createIntlMiddleware(routing);
const SESSION_COOKIES = ["authjs.session-token", "__Secure-authjs.session-token"];

function isProtected(pathname: string): boolean {
  return /^\/(en\/)?(dashboard|admin)(\/|$)/.test(pathname);
}

/**
 * Proxy: routing i18n, nagłówki CSP (nonce dla panelu/admina — strony dynamiczne,
 * statyczny CSP dla marketingu, żeby zachować SSG/ISR) oraz szybkie przekierowanie
 * niezalogowanych z /dashboard i /admin. Właściwa autoryzacja jest po stronie serwera.
 */
export default function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const dev = process.env.NODE_ENV !== "production";
  const https =
    request.nextUrl.protocol === "https:" || request.headers.get("x-forwarded-proto") === "https";

  if (isProtected(pathname)) {
    const hasSession = SESSION_COOKIES.some((c) => request.cookies.has(c));
    if (!hasSession) {
      const locale = pathname.startsWith("/en/") || pathname === "/en" ? "en" : "pl";
      const url = request.nextUrl.clone();
      url.pathname = locale === "en" ? "/en/login" : "/login";
      url.search = `?callbackUrl=${encodeURIComponent(pathname + request.nextUrl.search)}`;
      return NextResponse.redirect(url);
    }
    const nonce = Buffer.from(crypto.randomUUID()).toString("base64");
    const csp = buildCsp({ nonce, dev, https });
    request.headers.set("x-nonce", nonce);
    request.headers.set("Content-Security-Policy", csp);
    const response = intl(request);
    response.headers.set("Content-Security-Policy", csp);
    response.headers.set("Cache-Control", "private, no-store");
    return response;
  }

  const response = intl(request);
  response.headers.set("Content-Security-Policy", buildCsp({ dev, https }));
  return response;
}

export const config = {
  matcher: ["/((?!api|_next|_vercel|.*\\..*).*)"],
};
