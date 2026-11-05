import sanitizeHtml from "sanitize-html";

/** Usuwa całe HTML, normalizuje białe znaki i blokuje masowe wzmianki w treściach użytkownika. */
export function sanitizeUserText(
  input: string,
  { allowMassMentions = false }: { allowMassMentions?: boolean } = {},
): string {
  let out = sanitizeHtml(input, {
    allowedTags: [],
    allowedAttributes: {},
    disallowedTagsMode: "discard",
  });
  // sanitize-html zamienia & < > na encje — przywracamy, bo Discord renderuje czysty tekst
  out = out
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'");
  out = out
    .replace(/\u0000/g, "")
    .replace(/\r\n?/g, "\n")
    .replace(/\n{4,}/g, "\n\n\n");
  if (!allowMassMentions) out = out.replace(/@(everyone|here)/g, "@​$1");
  return out.trim();
}

export const greetingVariables = ["{user}", "{username}", "{server}", "{memberCount}"] as const;

/** Podstawia zmienne w podglądzie powitania. Nieznane zmienne zostają bez zmian. */
export function renderGreeting(
  template: string,
  vars: { user: string; username: string; server: string; memberCount: number },
  locale = "pl",
): string {
  return template
    .replace(/\{user\}/g, `@${vars.username}`)
    .replace(/\{username\}/g, vars.username)
    .replace(/\{server\}/g, vars.server)
    .replace(
      /\{memberCount\}/g,
      new Intl.NumberFormat(locale === "en" ? "en-US" : "pl-PL").format(vars.memberCount),
    );
}
