/**
 * Silnik reguł AutoModu — czysta logika bez zależności od Discorda.
 * Ten sam kod napędza interaktywne demo na stronie i jest pokryty testami.
 */
import type { AutoModModuleKey } from "@/lib/validators/automod";

export type EngineMessage = {
  authorId: string;
  content: string;
  timestamp: number;
  mentionCount?: number;
};

export type EngineConfig = {
  enabled: Partial<Record<AutoModModuleKey, boolean>>;
  sensitivity: number; // 0–100
  bannedWords: string[];
  allowedDomains: string[];
};

export type Violation = {
  module: AutoModModuleKey;
  reason: string;
};

const INVITE_RE = /(discord\.gg|discord(?:app)?\.com\/invite)\/[a-z0-9-]+/i;
const URL_RE = /\bhttps?:\/\/([^\s/]+)[^\s]*/gi;
const MENTION_RE = /<@[!&]?\d+>|@(everyone|here)|@\w{2,32}/g;

const LEET: Record<string, string> = {
  "0": "o",
  "1": "i",
  "3": "e",
  "4": "a",
  "5": "s",
  "7": "t",
  "@": "a",
  $: "s",
};

export function normalizeText(input: string): string {
  return input
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/ł/g, "l")
    .replace(/[013457@$]/g, (c) => LEET[c] ?? c)
    .replace(/(.)\1{2,}/g, "$1$1");
}

/** Mapuje czułość 0–100 na próg: wyższa czułość = niższy próg. */
export function scale(sensitivity: number, strict: number, lenient: number): number {
  const s = Math.min(100, Math.max(0, sensitivity)) / 100;
  return Math.round(lenient + (strict - lenient) * s);
}

export function wordToRegex(word: string): RegExp {
  const escaped = normalizeText(word)
    .split("*")
    .map((p) => p.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"))
    .join("[\\p{L}\\p{N}]*");
  return new RegExp(`(^|[^\\p{L}\\p{N}])${escaped}($|[^\\p{L}\\p{N}])`, "u");
}

export function capsRatio(text: string): { ratio: number; letters: number } {
  const letters = [...text].filter((c) => c.toLowerCase() !== c.toUpperCase());
  if (letters.length === 0) return { ratio: 0, letters: 0 };
  const upper = letters.filter((c) => c === c.toUpperCase()).length;
  return { ratio: upper / letters.length, letters: letters.length };
}

export function extractDomains(text: string): string[] {
  const out: string[] = [];
  for (const m of text.matchAll(URL_RE)) {
    if (m[1]) out.push(m[1].toLowerCase().replace(/^www\./, ""));
  }
  return out;
}

export function evaluate(
  message: EngineMessage,
  history: EngineMessage[],
  config: EngineConfig,
): Violation[] {
  const v: Violation[] = [];
  const on = (m: AutoModModuleKey) => config.enabled[m] === true;
  const s = config.sensitivity;

  if (on("ANTI_SPAM")) {
    const windowMs = 5000;
    const max = scale(s, 3, 7);
    const recent = history.filter(
      (h) => h.authorId === message.authorId && message.timestamp - h.timestamp <= windowMs,
    );
    const dupes = recent.filter(
      (h) => normalizeText(h.content) === normalizeText(message.content),
    ).length;
    if (recent.length + 1 > max) v.push({ module: "ANTI_SPAM", reason: `rate>${max}/5s` });
    else if (dupes + 1 >= scale(s, 2, 4)) v.push({ module: "ANTI_SPAM", reason: "duplicate" });
  }

  if (on("ANTI_INVITE") && INVITE_RE.test(message.content)) {
    v.push({ module: "ANTI_INVITE", reason: "invite" });
  }

  if (on("ANTI_LINK")) {
    const blocked = extractDomains(message.content).filter(
      (d) =>
        !INVITE_RE.test(d) && !config.allowedDomains.some((a) => d === a || d.endsWith(`.${a}`)),
    );
    if (blocked.length > 0) v.push({ module: "ANTI_LINK", reason: blocked[0] ?? "link" });
  }

  if (on("WORD_FILTER") && config.bannedWords.length > 0) {
    const normalized = normalizeText(message.content);
    const hit = config.bannedWords.find((w) => wordToRegex(w).test(normalized));
    if (hit) v.push({ module: "WORD_FILTER", reason: hit });
  }

  if (on("ANTI_CAPS")) {
    const { ratio, letters } = capsRatio(message.content);
    const minLen = 8;
    const maxRatio = scale(s, 60, 90) / 100;
    if (letters >= minLen && ratio > maxRatio) {
      v.push({ module: "ANTI_CAPS", reason: `${Math.round(ratio * 100)}%` });
    }
  }

  if (on("ANTI_MENTION")) {
    const count = message.mentionCount ?? (message.content.match(MENTION_RE) ?? []).length;
    const max = scale(s, 3, 8);
    if (count > max) v.push({ module: "ANTI_MENTION", reason: `${count}>${max}` });
  }

  return v;
}

export type Decision = "allow" | "delete" | "warn" | "timeout";

/** Prosta polityka eskalacji dla dema: 1. naruszenie = usuń, 2. = ostrzeż, 3+ = timeout. */
export function decide(violations: Violation[], priorStrikes: number): Decision {
  if (violations.length === 0) return "allow";
  if (priorStrikes >= 2) return "timeout";
  if (priorStrikes >= 1) return "warn";
  return "delete";
}
