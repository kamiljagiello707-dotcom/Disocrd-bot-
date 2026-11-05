"use client";
import { useState } from "react";
import { useTranslations } from "next-intl";
import { AnimatePresence, motion } from "motion/react";
import type { Plan } from "@/content/pricing";
import { Button } from "@/components/ui/button";
import { Check } from "@/components/ui/icons";
import { Link } from "@/i18n/navigation";
import { cn, inviteUrl, l } from "@/lib/utils";

function Price({ value, locale }: { value: string; locale: string }) {
  return (
    <span className="relative inline-flex overflow-hidden" aria-live="polite">
      <AnimatePresence mode="popLayout" initial={false}>
        {value.split("").map((ch, i) => (
          <motion.span
            key={`${i}-${ch}`}
            initial={{ y: "100%", opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            exit={{ y: "-100%", opacity: 0 }}
            transition={{ duration: 0.45, delay: i * 0.03, ease: [0.16, 1, 0.3, 1] }}
            className="inline-block tabular-nums"
          >
            {ch}
          </motion.span>
        ))}
      </AnimatePresence>
      <span className="sr-only">{locale === "en" ? "PLN" : "zł"}</span>
    </span>
  );
}

export function Pricing({ plans, locale }: { plans: Plan[]; locale: string }) {
  const t = useTranslations("premium");
  const [yearly, setYearly] = useState(true);
  const fmt = (n: number) =>
    new Intl.NumberFormat(locale === "en" ? "en-US" : "pl-PL", {
      minimumFractionDigits: n % 1 ? 2 : 0,
      maximumFractionDigits: 2,
    }).format(n);

  return (
    <div>
      <div className="flex justify-center">
        <div
          role="radiogroup"
          aria-label={t("eyebrow")}
          className="relative inline-flex rounded-full border border-border bg-surface-1 p-1"
        >
          {[false, true].map((y) => (
            <button
              key={String(y)}
              role="radio"
              aria-checked={yearly === y}
              type="button"
              onClick={() => setYearly(y)}
              className={cn(
                "relative z-10 rounded-full px-5 py-2 text-sm transition-colors duration-300",
                yearly === y ? "text-black" : "text-fg-muted hover:text-white",
              )}
            >
              {yearly === y ? (
                <motion.span
                  layoutId="billing-pill"
                  className="absolute inset-0 -z-10 rounded-full bg-white"
                  transition={{ type: "spring", stiffness: 400, damping: 34 }}
                />
              ) : null}
              {y ? t("yearly") : t("monthly")}
              {y ? (
                <span className={cn("ml-2 text-xs", yearly ? "text-black/70" : "text-fg-subtle")}>
                  {t("save")}
                </span>
              ) : null}
            </button>
          ))}
        </div>
      </div>
      <ul className="mt-14 grid gap-4 lg:grid-cols-3">
        {plans.map((p) => {
          const perMonth = yearly ? p.yearly / 12 : p.monthly;
          return (
            <li
              key={p.id}
              className={cn(
                "relative flex flex-col rounded-xl p-7",
                p.highlighted ? "bg-white text-black" : "glass text-white",
              )}
            >
              {p.highlighted ? (
                <span className="absolute -top-3 left-7 rounded-full border border-black/10 bg-black px-3 py-1 font-mono text-[10px] tracking-wider text-white uppercase">
                  {t("popular")}
                </span>
              ) : null}
              <h3 className="heading text-2xl">{p.name}</h3>
              <p className={cn("mt-2 text-sm", p.highlighted ? "text-black/70" : "text-fg-muted")}>
                {l(p.description, locale)}
              </p>
              <p className="mt-8 flex items-baseline gap-2">
                <span className="heading text-5xl">
                  <Price value={fmt(Math.round(perMonth * 100) / 100)} locale={locale} />
                </span>
                <span className={cn("text-sm", p.highlighted ? "text-black/70" : "text-fg-subtle")}>
                  zł {t("perMonth")}
                </span>
              </p>
              <p
                className={cn(
                  "mt-1 h-5 text-xs",
                  p.highlighted ? "text-black/65" : "text-fg-subtle",
                )}
              >
                {yearly && p.yearly > 0 ? t("billedYearly", { price: `${fmt(p.yearly)} zł` }) : ""}
              </p>
              <ul className="mt-8 flex-1 space-y-3">
                {p.features.map((f) => (
                  <li key={f.pl} className="flex gap-3 text-sm">
                    <Check
                      className={cn(
                        "mt-0.5 size-4 shrink-0",
                        p.highlighted ? "text-black" : "text-white",
                      )}
                    />
                    <span className={p.highlighted ? "text-black/80" : "text-fg-muted"}>
                      {l(f, locale)}
                    </span>
                  </li>
                ))}
              </ul>
              <div className="mt-10">
                {p.id === "free" ? (
                  <Button asChild variant="secondary" className="w-full">
                    <a href={inviteUrl()} target="_blank" rel="noopener noreferrer">
                      <span className="relative">{t("currentFree")}</span>
                    </a>
                  </Button>
                ) : (
                  <Button
                    asChild
                    variant={p.highlighted ? "outline" : "primary"}
                    className={cn(
                      "w-full",
                      p.highlighted &&
                        "border-black bg-black text-white hover:bg-black/85 hover:text-white",
                    )}
                  >
                    <Link href={`/contact?topic=PREMIUM&plan=${p.id}`}>
                      {t("choose", { plan: p.name })}
                    </Link>
                  </Button>
                )}
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
