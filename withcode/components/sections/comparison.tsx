import { getTranslations } from "next-intl/server";
import { comparison } from "@/content/pricing";
import { Check, X } from "@/components/ui/icons";
import { l } from "@/lib/utils";
import { SectionHeading } from "./section-heading";

export async function Comparison({ locale }: { locale: string }) {
  const t = await getTranslations("home.comparison");
  const cell = (v: boolean | { pl: string; en: string }, strong: boolean) =>
    typeof v === "boolean" ? (
      v ? (
        <Check
          className={strong ? "mx-auto size-5 text-white" : "mx-auto size-5 text-fg-muted"}
          aria-label="✓"
        />
      ) : (
        <X className="mx-auto size-4 text-fg-faint" aria-label="✗" />
      )
    ) : (
      <span className={strong ? "text-white" : "text-fg-muted"}>{l(v, locale)}</span>
    );
  return (
    <section className="section-y" aria-labelledby="cmp-title">
      <div className="container-x">
        <SectionHeading eyebrow={t("eyebrow")} title={t("title")} subtitle={t("subtitle")} />
        <div className="glass mt-14 overflow-x-auto rounded-xl">
          <table className="w-full min-w-[560px] text-sm">
            <caption className="sr-only">{t("title")}</caption>
            <thead>
              <tr className="border-b border-border text-left">
                <th scope="col" className="px-6 py-5 font-medium text-fg-subtle">
                  {t("feature")}
                </th>
                <th
                  scope="col"
                  className="w-40 bg-white/[0.04] px-6 py-5 text-center font-semibold text-white"
                >
                  {t("withbot")}
                </th>
                <th scope="col" className="w-40 px-6 py-5 text-center font-medium text-fg-subtle">
                  {t("typical")}
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {comparison.map((row) => (
                <tr key={row.label.pl}>
                  <th scope="row" className="px-6 py-4 text-left font-normal text-fg-muted">
                    {l(row.label, locale)}
                  </th>
                  <td className="bg-white/[0.04] px-6 py-4 text-center">
                    {cell(row.withbot, true)}
                  </td>
                  <td className="px-6 py-4 text-center">{cell(row.typical, false)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}
