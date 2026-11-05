"use client";
import { useState } from "react";
import { useTranslations } from "next-intl";
import { Link, usePathname } from "@/i18n/navigation";
import {
  ChevronLeft,
  Menu,
  X,
  Chart,
  Shield,
  Zap,
  Gavel,
  History,
  Scroll,
  CheckCircle,
  Wave,
  Tags,
  Key,
  Settings,
} from "@/components/ui/icons";
import { cn } from "@/lib/utils";

const groups = [
  {
    key: "groupProtection",
    items: [
      ["", "overview", Chart],
      ["automod", "automod", Shield],
      ["antiraid", "antiraid", Zap],
    ],
  },
  {
    key: "groupModeration",
    items: [
      ["cases", "cases", Gavel],
      ["punishments", "punishments", History],
      ["logs", "logs", Scroll],
    ],
  },
  {
    key: "groupCommunity",
    items: [
      ["verification", "verification", CheckCircle],
      ["welcome", "welcome", Wave],
      ["roles", "roles", Tags],
    ],
  },
  {
    key: "groupServer",
    items: [
      ["access", "access", Key],
      ["settings", "settings", Settings],
      ["audit", "audit", History],
    ],
  },
] as const;

export function Sidebar({ guildId, guildName }: { guildId: string; guildName: string }) {
  const t = useTranslations("dashboard");
  const tc = useTranslations("common");
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const base = `/dashboard/${guildId}`;
  const isActive = (seg: string) =>
    seg === ""
      ? pathname === base
      : pathname === `${base}/${seg}` || pathname.startsWith(`${base}/${seg}/`);

  const nav = (
    <nav aria-label={guildName} className="flex flex-col gap-6 p-4">
      <Link
        href="/dashboard"
        className="flex items-center gap-2 text-xs text-fg-subtle hover:text-white"
      >
        <ChevronLeft className="size-3.5" /> {t("backToServers")}
      </Link>
      <p className="truncate px-2 font-display text-lg font-semibold text-white">{guildName}</p>
      {groups.map((g) => (
        <div key={g.key}>
          <p className="eyebrow mb-2 px-2">{t(`nav.${g.key}`)}</p>
          <ul className="space-y-0.5">
            {g.items.map(([seg, key, Icon]) => (
              <li key={key}>
                <Link
                  href={seg ? `${base}/${seg}` : base}
                  onClick={() => setOpen(false)}
                  aria-current={isActive(seg) ? "page" : undefined}
                  className={cn(
                    "flex items-center gap-3 rounded-md px-2.5 py-2 text-sm transition-colors",
                    isActive(seg)
                      ? "bg-white text-black"
                      : "text-fg-muted hover:bg-white/[0.05] hover:text-white",
                  )}
                >
                  <Icon className="size-4" />
                  {t(`nav.${key}`)}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </nav>
  );

  return (
    <>
      <button
        type="button"
        className="fixed bottom-4 left-4 z-50 inline-flex size-12 items-center justify-center rounded-full border border-border-strong bg-surface-2 text-white shadow-xl lg:hidden"
        aria-label={open ? tc("closeMenu") : tc("openMenu")}
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
      >
        {open ? <X className="size-5" /> : <Menu className="size-5" />}
      </button>
      <aside className="sticky top-16 hidden h-[calc(100dvh-4rem)] w-64 shrink-0 overflow-y-auto border-r border-border lg:block">
        {nav}
      </aside>
      {open ? (
        <div
          className="fixed inset-0 z-40 overflow-y-auto bg-black pt-16 lg:hidden"
          role="dialog"
          aria-modal="true"
        >
          {nav}
        </div>
      ) : null}
    </>
  );
}
