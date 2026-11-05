"use client";
import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Search } from "@/components/ui/icons";

const TYPES = ["WARN", "TIMEOUT", "KICK", "BAN", "SOFTBAN", "UNBAN", "NOTE"] as const;

export function CasesFilters() {
  const t = useTranslations("dashboard.cases");
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();
  const [q, setQ] = useState(params.get("q") ?? "");

  const update = (key: string, value: string | null) => {
    const sp = new URLSearchParams(params.toString());
    if (value) sp.set(key, value);
    else sp.delete(key);
    sp.delete("page");
    router.replace(`${pathname}?${sp.toString()}`);
  };

  useEffect(() => {
    const id = setTimeout(() => {
      if ((params.get("q") ?? "") !== q) update("q", q || null);
    }, 300);
    return () => clearTimeout(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [q]);

  const sel = "h-10 rounded-md border border-border-strong bg-surface-1 px-3 text-sm text-white";
  return (
    <div className="mb-4 flex flex-wrap gap-2">
      <div className="relative min-w-64 flex-1">
        <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-fg-subtle" />
        <input
          type="search"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder={t("search")}
          aria-label={t("search")}
          className="h-10 w-full rounded-md border border-border-strong bg-surface-1 pr-3 pl-9 text-sm text-white placeholder:text-fg-faint focus-visible:border-white focus-visible:outline-none"
        />
      </div>
      <select
        aria-label={t("type")}
        className={sel}
        value={params.get("type") ?? ""}
        onChange={(e) => update("type", e.target.value || null)}
      >
        <option value="">{t("allTypes")}</option>
        {TYPES.map((x) => (
          <option key={x} value={x}>
            {t(`types.${x}`)}
          </option>
        ))}
      </select>
      <select
        aria-label={t("status")}
        className={sel}
        value={params.get("status") ?? "all"}
        onChange={(e) => update("status", e.target.value === "all" ? null : e.target.value)}
      >
        <option value="all">{t("all")}</option>
        <option value="active">{t("active")}</option>
        <option value="revoked">{t("revoked")}</option>
      </select>
    </div>
  );
}
