import "server-only";
import { createCipheriv, createDecipheriv, createHash, randomBytes } from "node:crypto";

/** AES-256-GCM dla tokenów Discorda w bazie. Klucz z TOKEN_ENCRYPTION_KEY (dowolny ciąg → SHA-256). */
function key(): Buffer | null {
  const raw = process.env.TOKEN_ENCRYPTION_KEY;
  return raw ? createHash("sha256").update(raw).digest() : null;
}

export function encryptToken(value: string | null | undefined): string | null | undefined {
  const k = key();
  if (!value || !k || value.startsWith("enc:")) return value;
  const iv = randomBytes(12);
  const cipher = createCipheriv("aes-256-gcm", k, iv);
  const data = Buffer.concat([cipher.update(value, "utf8"), cipher.final()]);
  return `enc:${Buffer.concat([iv, cipher.getAuthTag(), data]).toString("base64url")}`;
}

export function decryptToken(value: string | null | undefined): string | null {
  if (!value) return null;
  if (!value.startsWith("enc:")) return value;
  const k = key();
  if (!k) return null;
  try {
    const buf = Buffer.from(value.slice(4), "base64url");
    const decipher = createDecipheriv("aes-256-gcm", k, buf.subarray(0, 12));
    decipher.setAuthTag(buf.subarray(12, 28));
    return Buffer.concat([decipher.update(buf.subarray(28)), decipher.final()]).toString("utf8");
  } catch {
    return null;
  }
}
