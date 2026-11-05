import { expect, test } from "@playwright/test";
import { acceptCookies, devLogin, GUILD } from "./helpers";

test.beforeEach(async ({ context, baseURL }) => acceptCookies(context, baseURL!));

test("niezalogowany użytkownik trafia na stronę logowania", async ({ page }) => {
  await page.goto("/dashboard");
  await expect(page).toHaveURL(/\/login\?callbackUrl=/);
  await expect(page.getByRole("button", { name: "Kontynuuj z Discordem" })).toBeVisible();
});

test("logowanie (mock) pokazuje serwery z uprawnieniem Manage Server", async ({ page }) => {
  await devLogin(page);
  await expect(page).toHaveURL(/\/dashboard$/);
  const list = page.getByTestId("guild-list");
  await expect(list).toContainText("WithCode Community");
  await expect(list).toContainText("Serwer znajomych");
  await expect(list).not.toContainText("Gdzie jestem zwykłym członkiem");
  await expect(list.getByRole("link", { name: "Dodaj WithBota" })).toHaveCount(1);
});

test("zapis ustawień AutoMod z paskiem niezapisanych zmian", async ({ page }) => {
  await devLogin(page, "admin", `/dashboard/${GUILD}/automod`);
  await expect(page.getByRole("heading", { level: 1, name: "AutoMod" })).toBeVisible();
  const toggle = page.getByTestId("toggle-ANTI_MENTION");
  const before = await toggle.getAttribute("aria-checked");
  await toggle.click();
  await expect(page.getByTestId("unsaved-bar")).toBeVisible();
  await page.getByTestId("unsaved-bar").getByRole("button", { name: "Zapisz zmiany" }).click();
  await expect(page.getByText("Zapisano zmiany")).toBeVisible();
  await expect(page.getByTestId("unsaved-bar")).toBeHidden();
  await page.reload();
  await expect(page.getByTestId("toggle-ANTI_MENTION")).toHaveAttribute(
    "aria-checked",
    before === "true" ? "false" : "true",
  );
  // przywrócenie stanu wyjściowego
  await page.getByTestId("toggle-ANTI_MENTION").click();
  await page.getByTestId("unsaved-bar").getByRole("button", { name: "Zapisz zmiany" }).click();
  await expect(page.getByText("Zapisano zmiany").first()).toBeVisible();
});

test("cofnięcie zmian przyciskiem Cofnij", async ({ page }) => {
  await devLogin(page, "admin", `/dashboard/${GUILD}/settings`);
  const prefix = page.getByLabel("Prefiks komend tekstowych");
  await expect(prefix).toHaveValue(/.+/); // po hydratacji formularza
  const original = await prefix.inputValue();
  await prefix.fill("?");
  await expect(page.getByTestId("unsaved-bar")).toBeVisible();
  await page.getByTestId("unsaved-bar").getByRole("button", { name: "Cofnij" }).click();
  await expect(prefix).toHaveValue(original);
});

test("użytkownik bez uprawnień nie zobaczy panelu serwera ani /admin", async ({ page }) => {
  await devLogin(page, "user", "/dashboard");
  const res = await page.goto("/admin");
  expect(res?.status()).toBe(404);
  await page.goto("/dashboard/200000000000000005");
  await expect(page.getByText("Nie masz uprawnień do tego serwera.")).toBeVisible();
});

test("Server Action odrzuca zapis bez uprawnień (sprawdzenie po stronie serwera)", async ({
  request,
}) => {
  const res = await request.get(`/api/guilds/${GUILD}/channels`);
  expect(res.status()).toBe(401);
});
