/** Budowa nagłówka Content-Security-Policy. */
export function buildCsp({
  nonce,
  dev,
  https = true,
}: {
  nonce?: string;
  dev: boolean;
  https?: boolean;
}): string {
  const script = nonce
    ? `'self' 'nonce-${nonce}' 'strict-dynamic'${dev ? " 'unsafe-eval'" : ""} https://challenges.cloudflare.com`
    : `'self' 'unsafe-inline'${dev ? " 'unsafe-eval'" : ""} https://challenges.cloudflare.com`;
  const directives: Record<string, string> = {
    "default-src": "'self'",
    "script-src": script,
    "style-src": "'self' 'unsafe-inline'",
    "img-src": "'self' data: blob: https://cdn.discordapp.com",
    "font-src": "'self' data:",
    "connect-src": `'self' https://*.ingest.sentry.io https://*.ingest.de.sentry.io${dev ? " ws: wss:" : ""}`,
    "worker-src": "'self' blob:",
    "frame-src": "https://challenges.cloudflare.com",
    "frame-ancestors": "'none'",
    "object-src": "'none'",
    "base-uri": "'self'",
    "form-action": "'self' https://discord.com",
    "upgrade-insecure-requests": "",
  };
  return Object.entries(directives)
    .filter(([k]) => !((dev || !https) && k === "upgrade-insecure-requests"))
    .map(([k, v]) => (v ? `${k} ${v}` : k))
    .join("; ");
}
