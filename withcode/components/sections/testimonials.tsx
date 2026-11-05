import { getTranslations } from "next-intl/server";
import type { TestimonialItem } from "@/lib/content";
import { SectionHeading } from "./section-heading";

export async function Testimonials({
  locale,
  items,
}: {
  locale: string;
  items: TestimonialItem[];
}) {
  const t = await getTranslations("home.testimonials");
  const loop = [...items, ...items];
  return (
    <section className="section-y overflow-hidden" aria-labelledby="t-title">
      <div className="container-x">
        <SectionHeading eyebrow={t("eyebrow")} title={t("title")} />
      </div>
      <div className="group relative mt-14 [mask-image:linear-gradient(90deg,transparent,#000_10%,#000_90%,transparent)]">
        <ul className="flex w-max animate-[marquee_60s_linear_infinite] gap-4 group-hover:[animation-play-state:paused] motion-reduce:animate-none">
          {loop.map((item, i) => (
            <li
              key={`${item.id}-${i}`}
              aria-hidden={i >= items.length}
              className="glass w-[min(26rem,82vw)] shrink-0 rounded-lg p-7"
            >
              <blockquote>
                <p className="text-base leading-relaxed text-white">
                  „{locale === "en" ? item.quoteEn : item.quotePl}”
                </p>
                <footer className="mt-6 flex items-center gap-3">
                  <span
                    className="flex size-10 items-center justify-center rounded-full border border-border-strong bg-surface-3 font-display text-sm text-white"
                    aria-hidden
                  >
                    {item.author.slice(0, 1)}
                  </span>
                  <span>
                    <cite className="block text-sm font-medium text-white not-italic">
                      {item.author}
                    </cite>
                    <span className="text-xs text-fg-subtle">
                      {locale === "en" ? item.roleEn : item.rolePl}
                    </span>
                  </span>
                </footer>
              </blockquote>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
