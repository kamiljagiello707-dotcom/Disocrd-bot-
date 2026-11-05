import { describe, expect, it } from "vitest";
import { sign, verifySignature } from "@/lib/security/hmac";
import { renderGreeting, sanitizeUserText } from "@/lib/security/sanitize";
import { csvCell } from "@/lib/dashboard/cases";
import { buildCsp } from "@/lib/security/csp";

describe("HMAC webhook signatures", () => {
  const secret = "test-secret-1234567890";
  const body = JSON.stringify({ type: "heartbeat", pingMs: 40 });
  const now = Date.now();
  const ts = String(Math.floor(now / 1000));

  it("accepts a valid signature", () => {
    expect(
      verifySignature(secret, { signature: sign(secret, ts, body), timestamp: ts, body, now }),
    ).toEqual({ ok: true });
  });
  it("rejects tampered body and wrong secret", () => {
    expect(
      verifySignature(secret, {
        signature: sign(secret, ts, body),
        timestamp: ts,
        body: body + " ",
        now,
      }).ok,
    ).toBe(false);
    expect(
      verifySignature(secret, {
        signature: sign("other-secret-123456", ts, body),
        timestamp: ts,
        body,
        now,
      }).ok,
    ).toBe(false);
  });
  it("rejects stale timestamps and missing headers", () => {
    const old = String(Math.floor(now / 1000) - 3600);
    expect(
      verifySignature(secret, { signature: sign(secret, old, body), timestamp: old, body, now }),
    ).toEqual({ ok: false, reason: "expired" });
    expect(verifySignature(secret, { signature: null, timestamp: ts, body, now })).toEqual({
      ok: false,
      reason: "missing",
    });
  });
});

describe("sanitization", () => {
  it("strips HTML and neutralizes mass mentions", () => {
    const out = sanitizeUserText("<img src=x onerror=alert(1)>Hej @everyone <b>tu</b> & @here");
    expect(out).not.toMatch(/<|onerror/);
    expect(out).toContain("tu & ");
    expect(out).not.toMatch(/@everyone|@here/);
  });
  it("renders greeting variables", () => {
    expect(
      renderGreeting(
        "Cześć {user} na {server}! #{memberCount} {unknown}",
        { user: "u", username: "ola", server: "WC", memberCount: 1234 },
        "pl",
      ),
    ).toBe(
      "Cześć @ola na WC! #1234 {unknown}".replace(
        "1234",
        new Intl.NumberFormat("pl-PL").format(1234),
      ),
    );
  });
});

describe("csv export", () => {
  it("escapes quotes, separators and formula injection", () => {
    expect(csvCell('a "b", c')).toBe('"a ""b"", c"');
    expect(csvCell("=HYPERLINK(1)")).toBe("'=HYPERLINK(1)");
    expect(csvCell(null)).toBe("");
  });
});

describe("CSP", () => {
  it("uses nonce + strict-dynamic when nonce given, no unsafe-eval in prod", () => {
    const csp = buildCsp({ nonce: "abc", dev: false });
    expect(csp).toContain("'nonce-abc'");
    expect(csp).toContain("'strict-dynamic'");
    expect(csp).not.toContain("unsafe-eval");
    expect(csp).toContain("frame-ancestors 'none'");
  });
});
