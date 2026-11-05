import { expect, test } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { acceptCookies } from "./helpers";

test.beforeEach(async ({ context, baseURL }) => acceptCookies(context, baseURL!));

test("strona główna się ładuje @mobile", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Moderacja");
  await expect(page.getByRole("link", { name: /Dodaj do Discorda/ }).first()).toHaveAttribute(
    "href",
    /discord\.com\/oauth2\/authorize/,
  );
  await expect(page.locator('script[type="application/ld+json"]')).toHaveCount(2);
  await expect(page.locator("#story-title")).toBeAttached();
  expect(errors).toEqual([]);
});

test("fallback bez WebGL", async ({ page }) => {
  await page.addInitScript(() => {
    const orig = HTMLCanvasElement.prototype.getContext;
    HTMLCanvasElement.prototype.getContext = function (
      this: HTMLCanvasElement,
      type: string,
      ...rest: unknown[]
    ) {
      if (type === "webgl" || type === "webgl2") return null;
      return (orig as (...a: unknown[]) => RenderingContext | null).call(this, type, ...rest);
    } as typeof HTMLCanvasElement.prototype.getContext;
  });
  await page.goto("/");
  await expect(page.locator("html")).toHaveAttribute("data-webgl", "off");
  await expect(page.locator("canvas")).toHaveCount(0);
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
});

test("przełączanie języka", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("link", { name: /Język/ }).first().click();
  await expect(page).toHaveURL(/\/en$/);
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Moderation");
  await expect(page.locator("html")).toHaveAttribute("lang", "en");
});

for (const path of ["/", "/commands", "/premium", "/status", "/docs/automod", "/contact"]) {
  test(`dostępność (axe, WCAG AA): ${path}`, async ({ page }) => {
    await page.goto(path);
    await page.waitForTimeout(1200);
    const results = await new AxeBuilder({ page })
      .exclude("canvas")
      .withTags(["wcag2a", "wcag2aa"])
      .analyze();
    const serious = results.violations.filter(
      (v) => v.impact === "serious" || v.impact === "critical",
    );
    expect(
      serious.map(
        (v) =>
          `${v.id}: ${v.nodes
            .map((n) => n.target.join(" "))
            .slice(0, 3)
            .join(" | ")}`,
      ),
    ).toEqual([]);
  });
}

test("strona 404 jest zlokalizowana", async ({ page }) => {
  const res = await page.goto("/to-nie-istnieje");
  expect(res?.status()).toBe(404);
  await expect(page.getByRole("heading", { level: 1 })).toContainText("zbanowana");
});
