import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { TiltCard } from "@/components/motion/tilt-card";
import { Reveal } from "@/components/motion/reveal";
import { featureIcons, ArrowUpRight } from "@/components/ui/icons";
import { features } from "@/content/features";
import { l } from "@/lib/utils";

export async function FeaturesGrid({ locale, limit }: { locale: string; limit?: number }) {
  const t = await getTranslations();
  const list = limit ? features.slice(0, limit) : features;
  return (
    <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {list.map((f, i) => {
        const Icon = featureIcons[f.icon];
        return (
          <li key={f.slug} className={i === 0 ? "sm:col-span-2 lg:row-span-2" : undefined}>
            <Reveal delay={(i % 4) * 0.06} className="h-full">
              <Link
                href={`/features/${f.slug}`}
                className="group block h-full rounded-lg"
                data-cursor="lg"
              >
                <TiltCard className="flex h-full flex-col p-6 md:p-7">
                  <div className="flex items-start justify-between">
                    <span className="inline-flex size-11 items-center justify-center rounded-md border border-border-strong bg-white/[0.03]">
                      <Icon className="size-5 text-white" />
                    </span>
                    <ArrowUpRight className="size-5 text-fg-faint transition-[color,transform] duration-300 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 group-hover:text-white" />
                  </div>
                  <h3 className={`heading mt-8 text-white ${i === 0 ? "text-3xl" : "text-xl"}`}>
                    {l(f.name, locale)}
                  </h3>
                  <p className="mt-2 text-sm text-fg-muted">{l(f.tagline, locale)}</p>
                  {i === 0 ? (
                    <ul className="mt-8 space-y-2.5 border-t border-border pt-6">
                      {f.bullets.slice(0, 4).map((b) => (
                        <li key={b.pl} className="flex gap-3 text-sm text-fg-muted">
                          <span className="mt-2 h-px w-3 shrink-0 bg-white/50" aria-hidden />
                          {l(b, locale)}
                        </li>
                      ))}
                    </ul>
                  ) : null}
                  <span className="sr-only">{t("common.learnMore")}</span>
                </TiltCard>
              </Link>
            </Reveal>
          </li>
        );
      })}
    </ul>
  );
}
