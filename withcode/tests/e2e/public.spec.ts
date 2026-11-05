import { expect, test } from "@playwright/test";
import { acceptCookies } from "./helpers";

test.beforeEach(async ({ context, baseURL }) => acceptCookies(context, baseURL!));

test("wyszukiwarka komend toleruje literówki i filtruje kategorie", async ({ page }) => {
  await page.goto("/commands");
  await page.getByRole("searchbox").fill("lockdwon");
  await expect(page.getByRole("heading", { name: "/lockdown" })).toBeVisible();
  await page.getByRole("searchbox").fill("");
  await page.getByRole("button", { name: "Tickety" }).click();
  await expect(page).toHaveURL(/category=tickets/);
  await expect(page.getByRole("heading", { name: "/ticket close" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "/ban" })).toHaveCount(0);
});

test("formularz kontaktowy waliduje pola", async ({ page }) => {
  await page.goto("/contact");
  await page.getByRole("button", { name: "Wyślij wiadomość" }).click();
  await expect(page.getByText("Minimum 2 znaków")).toBeVisible();
  await expect(page.getByText("Podaj poprawny adres e-mail")).toBeVisible();
});

test("demo: spam zostaje usunięty", async ({ page }) => {
  await page.goto("/demo");
  await page.getByRole("button", { name: "Zaproszenie na inny serwer" }).click();
  await expect(page.getByRole("log").getByText(/Wiadomość usunięta przez WithBota/)).toBeVisible();
});

test("API komend zwraca dane z jednego źródła", async ({ request }) => {
  const res = await request.get("/api/commands");
  expect(res.ok()).toBe(true);
  const json = (await res.json()) as { count: number; commands: unknown[] };
  expect(json.count).toBe(json.commands.length);
  expect(json.count).toBeGreaterThan(20);
});

test("webhook bota odrzuca niepodpisane żądania", async ({ request }) => {
  const res = await request.post("/api/webhooks/bot", { data: { type: "heartbeat", pingMs: 1 } });
  expect([401, 503]).toContain(res.status());
});

test("nagłówki bezpieczeństwa", async ({ request }) => {
  const res = await request.get("/");
  const h = res.headers();
  expect(h["content-security-policy"]).toContain("frame-ancestors 'none'");
  expect(h["x-frame-options"]).toBe("DENY");
  expect(h["strict-transport-security"]).toContain("max-age=");
  expect(h["referrer-policy"]).toBe("strict-origin-when-cross-origin");
});
