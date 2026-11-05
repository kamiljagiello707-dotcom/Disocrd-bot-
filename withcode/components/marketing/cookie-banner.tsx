"use client";
import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { Link } from "@/i18n/navigation";

export type Consent = { necessary: true; analytics: boolean; marketing: boolean; v: 1; at: string };
const COOKIE = "wc_consent";
const OPEN_EVENT = "wc:open-cookie-settings";

export function readConsent(): Consent | null {
  if (typeof document === "undefined") return null;
  const raw = document.cookie.split("; ").find((c) => c.startsWith(`${COOKIE}=`));
  if (!raw) return null;
  try {
    const parsed = JSON.parse(decodeURIComponent(raw.slice(COOKIE.length + 1))) as Partial<Consent>;
    if (parsed.v !== 1) return null;
    return {
      necessary: true,
      analytics: Boolean(parsed.analytics),
      marketing: Boolean(parsed.marketing),
      v: 1,
      at: String(parsed.at),
    };
  } catch {
    return null;
  }
}

function writeConsent(c: Omit<Consent, "necessary" | "v" | "at">) {
  const value: Consent = { necessary: true, v: 1, at: new Date().toISOString(), ...c };
  const secure = location.protocol === "https:" ? "; Secure" : "";
  document.cookie = `${COOKIE}=${encodeURIComponent(JSON.stringify(value))}; Path=/; Max-Age=${60 * 60 * 24 * 180}; SameSite=Lax${secure}`;
  window.dispatchEvent(new CustomEvent("wc:consent", { detail: value }));
}

export function CookieSettingsButton({ label }: { label: string }) {
  return (
    <button
      type="button"
      className="underline decoration-fg-faint underline-offset-4 hover:text-white"
      onClick={() => window.dispatchEvent(new Event(OPEN_EVENT))}
    >
      {label}
    </button>
  );
}

/** Skrypt inline: ustawia html[data-consent] przed pierwszym malowaniem, żeby baner nie mrugał. */
export const consentBootScript = `try{if(document.cookie.split('; ').some(function(c){return c.indexOf('${COOKIE}=')===0}))document.documentElement.dataset.consent='1'}catch(e){}`;

/**
 * Baner zgód zgodny z RODO: brak domyślnych zgód, „Tylko niezbędne” równorzędne z „Akceptuję”.
 * Renderowany w HTML od razu (bez czekania na JS → brak opóźnionego LCP/CLS); ukrywa go CSS,
 * gdy zgoda jest już zapisana (html[data-consent]).
 */
export function CookieBanner() {
  const t = useTranslations("cookies");
  const [custom, setCustom] = useState(false);
  const [analytics, setAnalytics] = useState(false);
  const [marketing, setMarketing] = useState(false);

  useEffect(() => {
    const existing = readConsent();
    if (!existing) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect -- odczyt cookie możliwy tylko po hydratacji
    setAnalytics(existing.analytics);
    setMarketing(existing.marketing);
  }, []);

  useEffect(() => {
    const onOpen = () => {
      setCustom(true);
      delete document.documentElement.dataset.consent;
    };
    window.addEventListener(OPEN_EVENT, onOpen);
    return () => window.removeEventListener(OPEN_EVENT, onOpen);
  }, []);

  const done = (a: boolean, m: boolean) => {
    writeConsent({ analytics: a, marketing: m });
    document.documentElement.dataset.consent = "1";
    setCustom(false);
  };

  return (
    <section
      role="dialog"
      aria-labelledby="cookie-title"
      aria-describedby="cookie-body"
      className="cookie-banner glass fixed inset-x-3 bottom-3 z-[var(--z-toast)] animate-[cookie-in_600ms_var(--ease-out-expo)_both] rounded-xl bg-surface-1/90 p-5 sm:inset-x-auto sm:bottom-5 sm:left-5 sm:max-w-md"
    >
      <h2 id="cookie-title" className="text-sm font-semibold text-white">
        {t("title")}
      </h2>
      <p id="cookie-body" className="mt-2 text-sm leading-relaxed text-fg-muted">
        {t("body")}{" "}
        <Link href="/legal/cookies" className="text-white underline underline-offset-4">
          {t("policy")}
        </Link>
      </p>
      {custom ? (
        <ul className="mt-4 space-y-3 border-t border-border pt-4">
          {(
            [
              ["necessary", true, () => undefined, true],
              ["analytics", analytics, setAnalytics, false],
              ["marketing", marketing, setMarketing, false],
            ] as const
          ).map(([key, value, set, disabled]) => (
            <li key={key} className="flex items-start justify-between gap-4">
              <div>
                <p className="text-sm text-white" id={`ck-${key}`}>
                  {t(key)}
                </p>
                <p className="text-xs text-fg-subtle">{t(`${key}Desc`)}</p>
              </div>
              <Switch
                aria-labelledby={`ck-${key}`}
                checked={value}
                disabled={disabled}
                onCheckedChange={(v) => set(v)}
              />
            </li>
          ))}
        </ul>
      ) : null}
      <div className="mt-5 flex flex-wrap gap-2">
        {custom ? (
          <Button size="sm" onClick={() => done(analytics, marketing)}>
            {t("save")}
          </Button>
        ) : (
          <Button size="sm" onClick={() => done(true, true)}>
            {t("acceptAll")}
          </Button>
        )}
        <Button size="sm" variant="secondary" onClick={() => done(false, false)}>
          <span className="relative">{t("rejectAll")}</span>
        </Button>
        {!custom ? (
          <Button size="sm" variant="ghost" onClick={() => setCustom(true)}>
            {t("customize")}
          </Button>
        ) : null}
      </div>
    </section>
  );
}
