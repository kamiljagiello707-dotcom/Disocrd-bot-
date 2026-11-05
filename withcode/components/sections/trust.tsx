import { getTranslations } from "next-intl/server";
import { Counter } from "@/components/motion/counter";

export async function Trust({
  locale,
  servers,
  users,
  threats,
  sample,
}: {
  locale: string;
  servers: number;
  users: number;
  threats: number;
  sample: boolean;
}) {
  const t = await getTranslations();
  const items = [
    { value: servers, label: t("home.trust.servers") },
    { value: users, label: t("home.trust.users") },
    { value: threats, label: t("home.trust.threats") },
  ];
  return (
    <section
      className="relative border-y border-border bg-black/40 backdrop-blur-sm"
      aria-label={t("home.trust.label")}
    >
      <div className="container-x py-12 md:py-16">
        <p className="eyebrow mb-8 text-center">
          {t("home.trust.label")}
          {sample ? <span className="ml-2 text-fg-faint">· {t("common.sampleData")}</span> : null}
        </p>
        <dl className="grid grid-cols-1 gap-8 text-center sm:grid-cols-3">
          {items.map((i) => (
            <div key={i.label}>
              <dt className="sr-only">{i.label}</dt>
              <dd className="heading text-3xl text-white">
                <Counter value={i.value} locale={locale} compact suffix="+" />
              </dd>
              <dd className="mt-2 text-sm text-fg-muted">{i.label}</dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}
