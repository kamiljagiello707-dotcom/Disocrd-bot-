import type { Metadata } from "next";
import { getFormatter, getTranslations, setRequestLocale } from "next-intl/server";
import { PageHeader } from "@/components/sections/section-heading";
import { Badge } from "@/components/ui/badge";
import { Tooltip } from "@/components/ui/tooltip";
import { getIncidents, getUptime } from "@/lib/content";
import { getBotApi } from "@/lib/bot-api";
import { pageMetadata } from "@/lib/seo/metadata";
import { cn } from "@/lib/utils";

export const revalidate = 60;

export async function generateMetadata({
  params,
}: {
  params: Promise<{ locale: string }>;
}): Promise<Metadata> {
  const { locale } = await params;
  return pageMetadata({
    locale,
    path: "/status",
    titleKey: "status",
    descriptionKey: "statusDescription",
  });
}

const SERVICES = ["BOT", "API", "DATABASE", "SHARDS", "DASHBOARD"] as const;

export default async function StatusPage({ params }: { params: Promise<{ locale: string }> }) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("status");
  const format = await getFormatter();
  const [uptime, incidents, status] = await Promise.all([
    getUptime(),
    getIncidents(),
    getBotApi().getStatus(),
  ]);
  const open = incidents.filter((i) => i.status !== "RESOLVED");
  const overall = open.some((i) => i.severity === "CRITICAL" || i.severity === "MAJOR")
    ? "outage"
    : open.length
      ? "degraded"
      : "ok";

  return (
    <>
      <PageHeader eyebrow={t("eyebrow")} title={t("title")} />
      <section className="container-x pb-16">
        <div
          className={cn(
            "glass flex flex-wrap items-center justify-between gap-6 rounded-xl p-6",
            overall !== "ok" && "border-warning/40",
          )}
        >
          <p className="flex items-center gap-3 text-lg text-white">
            <span
              className={cn(
                "relative flex size-3 rounded-full",
                overall === "ok"
                  ? "bg-success"
                  : overall === "degraded"
                    ? "bg-warning"
                    : "bg-danger",
              )}
            >
              <span
                className={cn(
                  "absolute inset-0 animate-ping rounded-full opacity-60",
                  overall === "ok" ? "bg-success" : "bg-warning",
                )}
              />
            </span>
            {overall === "ok"
              ? t("allOperational")
              : overall === "degraded"
                ? t("degraded")
                : t("outage")}
          </p>
          <dl className="flex gap-8 text-sm">
            <div>
              <dt className="eyebrow">{t("ping")}</dt>
              <dd className="mt-1 font-mono text-white">{status.pingMs} ms</dd>
            </div>
            <div>
              <dt className="eyebrow">{t("shards")}</dt>
              <dd className="mt-1 font-mono text-white">
                {status.shards.filter((s) => s.online).length}/{status.shards.length}
              </dd>
            </div>
            <div>
              <dt className="eyebrow">{t("guilds")}</dt>
              <dd className="mt-1 font-mono text-white">{format.number(status.guilds)}</dd>
            </div>
          </dl>
        </div>
        <p className="mt-3 text-xs text-fg-subtle">
          {t("updated", {
            time: format.dateTime(new Date(status.checkedAt), {
              hour: "2-digit",
              minute: "2-digit",
            }),
          })}
          {status.sample ? " · Sample" : ""}
        </p>
      </section>

      <section className="container-x pb-20" aria-labelledby="uptime">
        <h2 id="uptime" className="eyebrow mb-6">
          {t("uptime90")}
        </h2>
        <ul className="space-y-8">
          {SERVICES.map((s) => {
            const days = uptime[s] ?? [];
            const avg = days.reduce((a, d) => a + d.pct, 0) / Math.max(1, days.length);
            return (
              <li key={s}>
                <div className="mb-3 flex items-center justify-between">
                  <p className="text-sm text-white">{t(`services.${s}`)}</p>
                  <p className="font-mono text-xs text-fg-muted">{avg.toFixed(2)}%</p>
                </div>
                <div
                  className="flex h-9 gap-[2px]"
                  role="img"
                  aria-label={`${t(`services.${s}`)}: ${avg.toFixed(2)}%`}
                >
                  {days.map((d) => (
                    <Tooltip
                      key={d.day}
                      content={`${format.dateTime(new Date(d.day), { day: "numeric", month: "short" })} · ${d.pct.toFixed(2)}%`}
                    >
                      <span
                        tabIndex={-1}
                        className={cn(
                          "h-full flex-1 rounded-[2px] transition-transform hover:scale-y-110",
                          d.pct >= 99.9 ? "bg-white/80" : d.pct >= 99 ? "bg-warning" : "bg-danger",
                        )}
                      />
                    </Tooltip>
                  ))}
                </div>
                <div className="mt-2 flex justify-between text-[11px] text-fg-faint">
                  <span>{t("daysAgo")}</span>
                  <span>{t("today")}</span>
                </div>
              </li>
            );
          })}
        </ul>
      </section>

      <section className="container-x pb-28" aria-labelledby="incidents">
        <h2 id="incidents" className="heading text-3xl text-white">
          {t("incidents")}
        </h2>
        {incidents.length === 0 ? <p className="mt-6 text-fg-muted">{t("noIncidents")}</p> : null}
        <ol className="mt-8 space-y-4">
          {incidents.map((inc) => (
            <li key={inc.id} className="rounded-xl border border-border bg-surface-1 p-6">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <h3 className="text-base font-medium text-white">
                  {locale === "en" ? inc.titleEn : inc.titlePl}
                </h3>
                <div className="flex gap-2">
                  <Badge
                    variant={
                      inc.severity === "MAINTENANCE"
                        ? "default"
                        : inc.severity === "MINOR"
                          ? "warning"
                          : "danger"
                    }
                  >
                    {t(`severity.${inc.severity}`)}
                  </Badge>
                  <Badge variant={inc.status === "RESOLVED" ? "success" : "warning"}>
                    {t(`incidentStatus.${inc.status}`)}
                  </Badge>
                </div>
              </div>
              <p className="mt-1 text-xs text-fg-subtle">
                {format.dateTime(new Date(inc.startedAt), {
                  dateStyle: "medium",
                  timeStyle: "short",
                })}
                {inc.resolvedAt
                  ? ` — ${format.dateTime(new Date(inc.resolvedAt), { timeStyle: "short" })}`
                  : ""}
              </p>
              <ol className="mt-5 space-y-3 border-l border-border pl-5">
                {inc.updates.map((u) => (
                  <li key={u.id} className="relative text-sm">
                    <span
                      className="absolute top-1.5 -left-[1.4rem] size-1.5 rounded-full bg-white"
                      aria-hidden
                    />
                    <span className="font-medium text-white">
                      {t(`incidentStatus.${u.status}`)}
                    </span>
                    <span className="text-fg-subtle">
                      {" "}
                      · {format.dateTime(new Date(u.createdAt), { timeStyle: "short" })}
                    </span>
                    <p className="mt-0.5 text-fg-muted">
                      {locale === "en" ? u.messageEn : u.messagePl}
                    </p>
                  </li>
                ))}
              </ol>
            </li>
          ))}
        </ol>
      </section>
    </>
  );
}
