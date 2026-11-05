"use client";
import { useLocale, useTranslations } from "next-intl";
import { useParams } from "next/navigation";
import { Link, usePathname } from "@/i18n/navigation";
import { cn } from "@/lib/utils";

export function LocaleSwitch({ className }: { className?: string }) {
  const locale = useLocale();
  const pathname = usePathname();
  const params = useParams();
  const t = useTranslations("common");
  const other = locale === "pl" ? "en" : "pl";
  void params;
  return (
    <Link
      href={pathname}
      locale={other}
      hrefLang={other}
      aria-label={`${t("language")}: ${t("switchTo")}`}
      className={cn(
        "inline-flex h-9 items-center rounded-full border border-border px-3 font-mono text-xs tracking-wider text-fg-muted uppercase transition-colors hover:border-white/40 hover:text-white",
        className,
      )}
    >
      <span className={cn(locale === "pl" && "text-white")}>PL</span>
      <span className="mx-1.5 text-fg-faint">/</span>
      <span className={cn(locale === "en" && "text-white")}>EN</span>
    </Link>
  );
}
