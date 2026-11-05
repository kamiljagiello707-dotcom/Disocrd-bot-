import { getTranslations } from "next-intl/server";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/ui/button";
import { Discord } from "@/components/ui/icons";
import { SplitReveal } from "@/components/motion/split-reveal";
import { inviteUrl } from "@/lib/utils";

export async function FinalCta(_props: { locale: string }) {
  const t = await getTranslations();
  return (
    <section className="section-y relative overflow-hidden">
      <div
        aria-hidden
        className="absolute inset-0 bg-[radial-gradient(50%_60%_at_50%_100%,rgba(255,255,255,0.09),transparent_70%)]"
      />
      <div className="container-x relative text-center">
        <SplitReveal
          as="h2"
          by="chars"
          className="display mx-auto max-w-[14ch] text-white"
          stagger={0.015}
        >
          {t("home.final.title")}
        </SplitReveal>
        <p className="mx-auto mt-8 max-w-xl text-lg text-fg-muted max-md:text-base">
          {t("home.final.subtitle")}
        </p>
        <div className="mt-10 flex flex-wrap justify-center gap-3">
          <Button asChild size="lg">
            <a href={inviteUrl()} target="_blank" rel="noopener noreferrer">
              <Discord className="size-5" />
              {t("common.addToDiscord")}
            </a>
          </Button>
          <Button asChild size="lg" variant="secondary">
            <Link href="/dashboard" prefetch={false}>
              <span className="relative">{t("common.dashboard")}</span>
            </Link>
          </Button>
        </div>
      </div>
    </section>
  );
}
