import { createHmac, timingSafeEqual } from "node:crypto";

export const SIGNATURE_HEADER = "x-withbot-signature";
export const TIMESTAMP_HEADER = "x-withbot-timestamp";
export const NONCE_HEADER = "x-withbot-nonce";
const TOLERANCE_SEC = 300;

export function sign(secret: string, timestamp: string, body: string): string {
  return `sha256=${createHmac("sha256", secret).update(`${timestamp}.${body}`).digest("hex")}`;
}

export type VerifyResult = { ok: true } | { ok: false; reason: "missing" | "expired" | "invalid" };

/** Weryfikacja podpisu HMAC-SHA256 w czasie stałym + okno czasowe ±5 min. */
export function verifySignature(
  secret: string,
  {
    signature,
    timestamp,
    body,
    now = Date.now(),
  }: { signature: string | null; timestamp: string | null; body: string; now?: number },
): VerifyResult {
  if (!signature || !timestamp) return { ok: false, reason: "missing" };
  const ts = Number(timestamp);
  if (!Number.isFinite(ts) || Math.abs(now / 1000 - ts) > TOLERANCE_SEC)
    return { ok: false, reason: "expired" };
  const expected = Buffer.from(sign(secret, timestamp, body));
  const given = Buffer.from(signature);
  if (expected.length !== given.length || !timingSafeEqual(expected, given))
    return { ok: false, reason: "invalid" };
  return { ok: true };
}
