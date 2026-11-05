"use client";
import { useLocale } from "next-intl";
import { renderGreeting, sanitizeUserText } from "@/lib/security/sanitize";
import { LogoMark } from "@/components/ui/logo";

type Embed = { title: string | null; description: string | null; imageUrl: string | null } | null;

/** Bezpieczny podgląd w stylu Discorda: tekst wyświetlany jako tekst (bez HTML), wzmianki wyróżnione. */
function Rich({ text }: { text: string }) {
  const parts = text.split(/(@[\p{L}\p{N}_.​]+|\*\*[^*]+\*\*)/gu);
  return (
    <>
      {parts.map((p, i) =>
        p.startsWith("@") ? (
          <span key={i} className="rounded bg-white/15 px-0.5 text-white">
            {p}
          </span>
        ) : p.startsWith("**") && p.endsWith("**") ? (
          <strong key={i} className="font-semibold text-white">
            {p.slice(2, -2)}
          </strong>
        ) : (
          <span key={i}>{p}</span>
        ),
      )}
    </>
  );
}

export function DiscordPreview({
  content,
  embed,
  server,
  memberCount,
}: {
  content: string | null;
  embed: Embed;
  server: string;
  memberCount: number;
}) {
  const locale = useLocale();
  const vars = {
    user: "nowy_członek",
    username: "nowy_członek",
    server,
    memberCount: memberCount + 1,
  };
  const text = content ? renderGreeting(sanitizeUserText(content), vars, locale) : "";
  return (
    <div className="rounded-xl border border-border bg-[#0c0c0c] p-4" aria-live="polite">
      <div className="flex gap-3">
        <LogoMark className="size-10 shrink-0" />
        <div className="min-w-0 flex-1">
          <p className="flex items-center gap-2 text-sm">
            <span className="font-medium text-white">WithBot</span>
            <span className="rounded bg-white px-1 text-[10px] font-semibold text-black">BOT</span>
            <span className="text-[11px] text-fg-faint">
              {new Date().toLocaleTimeString(locale, { hour: "2-digit", minute: "2-digit" })}
            </span>
          </p>
          {text ? (
            <p className="mt-1 text-sm leading-relaxed break-words whitespace-pre-wrap text-fg-muted">
              <Rich text={text} />
            </p>
          ) : null}
          {embed && (embed.title || embed.description || embed.imageUrl) ? (
            <div className="mt-2 max-w-md overflow-hidden rounded border-l-4 border-white bg-surface-2 p-3">
              {embed.title ? (
                <p className="text-sm font-semibold text-white">
                  {renderGreeting(sanitizeUserText(embed.title), vars, locale)}
                </p>
              ) : null}
              {embed.description ? (
                <p className="mt-1 text-sm whitespace-pre-wrap text-fg-muted">
                  <Rich text={renderGreeting(sanitizeUserText(embed.description), vars, locale)} />
                </p>
              ) : null}
              {embed.imageUrl && /^https:\/\//.test(embed.imageUrl) ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={embed.imageUrl}
                  alt=""
                  className="mt-3 max-h-48 rounded object-cover"
                  referrerPolicy="no-referrer"
                />
              ) : null}
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
}
