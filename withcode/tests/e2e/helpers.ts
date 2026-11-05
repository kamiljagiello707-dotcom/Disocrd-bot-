import type { BrowserContext, Page } from "@playwright/test";

/** Pomija baner cookies (zgoda zapisana z góry). */
export async function acceptCookies(context: BrowserContext, baseURL: string) {
  await context.addCookies([
    {
      name: "wc_consent",
      value: encodeURIComponent(
        JSON.stringify({
          necessary: true,
          analytics: false,
          marketing: false,
          v: 1,
          at: new Date().toISOString(),
        }),
      ),
      url: baseURL,
    },
  ]);
}

/** Logowanie testowe (mock OAuth Discorda) — dostępne tylko z AUTH_DEV_LOGIN=1 i E2E=1. */
export async function devLogin(
  page: Page,
  as: "admin" | "user" = "admin",
  callbackUrl = "/dashboard",
) {
  await page.goto(`/api/dev/login?as=${as}&callbackUrl=${encodeURIComponent(callbackUrl)}`);
}

export const GUILD = "200000000000000001";
