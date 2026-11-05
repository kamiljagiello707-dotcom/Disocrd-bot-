"use client";
import { useEffect, useMemo, useRef, useState } from "react";
import { useTranslations } from "next-intl";
import { AnimatePresence, motion } from "motion/react";
import {
  decide,
  evaluate,
  type EngineConfig,
  type EngineMessage,
  type Violation,
} from "@/lib/automod/engine";
import type { AutoModModuleKey } from "@/lib/validators/automod";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { LogoMark } from "@/components/ui/logo";
import { cn } from "@/lib/utils";

type ChatMessage = {
  id: number;
  author: "you" | "bot";
  content: string;
  deleted?: boolean;
  violations?: Violation[];
  at: number;
};

type LogEntry = {
  id: number;
  text: string;
  module?: AutoModModuleKey;
  kind: "deleted" | "warn" | "timeout" | "allow";
};

const MODULE_LABEL: Record<AutoModModuleKey, string> = {
  ANTI_SPAM: "Anti-spam",
  ANTI_LINK: "Anti-link",
  ANTI_INVITE: "Anti-invite",
  WORD_FILTER: "Word filter",
  ANTI_CAPS: "Anti-caps",
  ANTI_MENTION: "Anti-mention",
};

export type Preset = { id: string; label: string; messages: string[] };

let seq = 0;
const now = () => Date.now();

export function DiscordMock({
  modules,
  presets,
  compact = false,
  bannedWords = ["idiota", "idiot", "kup*", "buy*", "scam"],
}: {
  modules: AutoModModuleKey[];
  presets: Preset[];
  compact?: boolean;
  bannedWords?: string[];
}) {
  const t = useTranslations("demo");
  const tm = useTranslations("dashboard.automod.modules");
  const [enabled, setEnabled] = useState<Record<AutoModModuleKey, boolean>>(
    () => Object.fromEntries(modules.map((m) => [m, true])) as Record<AutoModModuleKey, boolean>,
  );
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [log, setLog] = useState<LogEntry[]>([]);
  const [input, setInput] = useState("");
  const strikes = useRef(0);
  const history = useRef<EngineMessage[]>([]);
  const listRef = useRef<HTMLDivElement>(null);

  const config: EngineConfig = useMemo(
    () => ({
      enabled,
      sensitivity: 60,
      bannedWords,
      allowedDomains: ["youtube.com", "github.com", "withcode.pl"],
    }),
    [enabled, bannedWords],
  );

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: "smooth" });
  }, [messages]);

  const send = (content: string, atArg?: number) => {
    const at = atArg ?? now();
    const text = content.trim().slice(0, 400);
    if (!text) return;
    const msg: EngineMessage = { authorId: "you", content: text, timestamp: at };
    const violations = evaluate(msg, history.current, config);
    history.current = [...history.current.slice(-20), msg];
    const id = ++seq;
    setMessages((m) => [...m.slice(-30), { id, author: "you", content: text, at, violations }]);
    if (violations.length === 0) {
      setLog((l) => [{ id, text: t("allowed"), kind: "allow" as const }, ...l].slice(0, 30));
      return;
    }
    const decision = decide(violations, strikes.current);
    strikes.current += 1;
    // krótka pauza, żeby było widać, co się stało
    window.setTimeout(() => {
      setMessages((m) => m.map((x) => (x.id === id ? { ...x, deleted: true } : x)));
      const first = violations[0];
      const reason = first ? `${MODULE_LABEL[first.module]} · ${first.reason}` : "";
      const botText =
        decision === "timeout"
          ? `⏱ @${t("you")} ${t("timedOut")} (10 min) — ${reason}`
          : decision === "warn"
            ? `⚠ @${t("you")} ${t("warned")} — ${reason}`
            : `🛡 ${t("deleted")} — ${reason}`;
      setMessages((m) => [...m, { id: ++seq, author: "bot", content: botText, at: now() }]);
      setLog((l) =>
        [
          {
            id,
            text: `${decision.toUpperCase()} · ${violations.map((v) => MODULE_LABEL[v.module]).join(", ")}`,
            ...(first ? { module: first.module } : {}),
            kind:
              decision === "delete"
                ? ("deleted" as const)
                : decision === "warn"
                  ? ("warn" as const)
                  : ("timeout" as const),
          },
          ...l,
        ].slice(0, 30),
      );
    }, 450);
  };

  const runPreset = (p: Preset) => {
    const base = now();
    p.messages.forEach((m, i) => window.setTimeout(() => send(m, base + i * 250), i * 250));
  };

  const reset = () => {
    setMessages([]);
    setLog([]);
    strikes.current = 0;
    history.current = [];
  };

  return (
    <div className={cn("grid gap-4", !compact && "lg:grid-cols-[1fr_20rem]")}>
      <div className="overflow-hidden rounded-xl border border-border bg-[#0c0c0c]">
        <div className="flex items-center justify-between border-b border-border px-4 py-3">
          <p className="flex items-center gap-2 text-sm font-medium text-white">
            <span className="text-lg text-fg-subtle">#</span>
            {t("channel")}
          </p>
          <button
            type="button"
            onClick={reset}
            className="text-xs text-fg-subtle transition-colors hover:text-white"
          >
            {t("reset")}
          </button>
        </div>
        <div
          ref={listRef}
          className={cn("space-y-1 overflow-y-auto px-2 py-4", compact ? "h-72" : "h-[26rem]")}
          role="log"
          aria-live="polite"
          aria-label={`#${t("channel")}`}
          data-lenis-prevent
        >
          <AnimatePresence initial={false}>
            {messages.map((m) => (
              <motion.div
                key={m.id}
                layout
                initial={{ opacity: 0, y: 10 }}
                animate={m.deleted ? { opacity: 0.35, x: [0, -4, 4, 0] } : { opacity: 1, y: 0 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.35 }}
                className="flex gap-3 rounded-md px-2 py-1.5 hover:bg-white/[0.02]"
              >
                {m.author === "bot" ? (
                  <LogoMark className="mt-0.5 size-9 shrink-0" />
                ) : (
                  <span className="mt-0.5 flex size-9 shrink-0 items-center justify-center rounded-full bg-surface-3 text-sm text-white">
                    {t("you").slice(0, 1)}
                  </span>
                )}
                <div className="min-w-0">
                  <p className="flex items-center gap-2 text-sm">
                    <span className="font-medium text-white">
                      {m.author === "bot" ? "WithBot" : t("you")}
                    </span>
                    {m.author === "bot" ? (
                      <span className="rounded bg-white px-1 py-px text-[10px] font-semibold text-black">
                        {t("botTag")}
                      </span>
                    ) : null}
                    <time className="text-[11px] text-fg-faint">
                      {new Date(m.at).toLocaleTimeString([], {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </time>
                  </p>
                  <p
                    className={cn(
                      "text-sm break-words text-fg-muted",
                      m.deleted && "line-through",
                      m.author === "bot" && "text-white",
                    )}
                  >
                    {m.content}
                  </p>
                </div>
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
        <form
          className="flex gap-2 border-t border-border p-3"
          onSubmit={(e) => {
            e.preventDefault();
            send(input);
            setInput("");
          }}
        >
          <label htmlFor={`demo-input-${compact ? "c" : "f"}`} className="sr-only">
            {t("placeholder")}
          </label>
          <input
            id={`demo-input-${compact ? "c" : "f"}`}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            maxLength={400}
            placeholder={t("placeholder")}
            className="h-10 min-w-0 flex-1 rounded-md bg-surface-2 px-3 text-sm text-white placeholder:text-fg-faint focus-visible:ring-2 focus-visible:ring-white/30 focus-visible:outline-none"
          />
          <Button type="submit" size="sm" magnetic={false}>
            {t("send")}
          </Button>
        </form>
        <div className="flex flex-wrap gap-2 border-t border-border p-3">
          <span className="sr-only">{t("presets")}</span>
          {presets.map((p) => (
            <button
              key={p.id}
              type="button"
              onClick={() => runPreset(p)}
              className="rounded-full border border-border px-3 py-1.5 text-xs text-fg-muted transition-colors hover:border-white/40 hover:text-white"
            >
              {p.label}
            </button>
          ))}
        </div>
      </div>

      {!compact ? (
        <aside className="flex flex-col gap-4">
          <div className="rounded-xl border border-border bg-surface-1 p-4">
            <h3 className="eyebrow mb-3">{t("rules")}</h3>
            <ul className="space-y-2.5">
              {modules.map((m) => (
                <li key={m} className="flex items-center justify-between gap-3">
                  <span className="text-sm text-white" id={`mod-${m}`}>
                    {tm(`${m}.name`)}
                  </span>
                  <Switch
                    aria-labelledby={`mod-${m}`}
                    checked={enabled[m]}
                    onCheckedChange={(v) => setEnabled((e) => ({ ...e, [m]: v }))}
                  />
                </li>
              ))}
            </ul>
          </div>
          <div className="flex-1 rounded-xl border border-border bg-surface-1 p-4">
            <h3 className="eyebrow mb-3">{t("log")}</h3>
            {log.length === 0 ? (
              <p className="text-sm text-fg-subtle">{t("logEmpty")}</p>
            ) : (
              <ul
                className="max-h-72 space-y-2 overflow-y-auto font-mono text-xs"
                data-lenis-prevent
              >
                {log.map((l) => (
                  <li
                    key={`${l.id}-${l.kind}`}
                    className={cn(
                      "rounded-md border px-2.5 py-2",
                      l.kind === "allow"
                        ? "border-border text-fg-subtle"
                        : l.kind === "timeout"
                          ? "border-danger/30 text-danger"
                          : l.kind === "warn"
                            ? "border-warning/30 text-warning"
                            : "border-border-strong text-white",
                    )}
                  >
                    {l.text}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </aside>
      ) : null}
    </div>
  );
}
