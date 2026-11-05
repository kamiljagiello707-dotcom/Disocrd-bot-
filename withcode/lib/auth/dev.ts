/** Logowanie testowe jest dostępne tylko poza produkcją albo w E2E (jawnie włączone). */
export function devLoginEnabled(): boolean {
  if (process.env.AUTH_DEV_LOGIN !== "1") return false;
  return process.env.NODE_ENV !== "production" || process.env.E2E === "1";
}
