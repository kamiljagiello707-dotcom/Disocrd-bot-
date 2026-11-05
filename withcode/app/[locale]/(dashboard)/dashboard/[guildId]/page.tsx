import { getFormatter, getTranslations, setRequestLocale } from "next-intl/server";
import { DashHeader, Panel } from "@/components/dashboard/page-header";
import { StatsChart } from "@/components/dashboard/stats-chart";
import { Badge } from "@/components/ui/badge";
import { Link } from "@/i18n/navigation";
import { db } from "@/lib/db";
import { getGuildStats } from "@/lib/dashboard/stats";

export default async function OverviewPage({
  params,
}: {
  params: Promise<{ locale: string; guildId: string }>;
}) {
  const { locale, guildId } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("dashboard");
  const format = await getFormatter();
  const [stats, recent] = await Promise.all([
    getGuildStats(guildId, "7d"),
    db.modCase.findMany({ where: { guildId }, orderBy: { createdAt: "desc" }, take: 8 }),
  ]);
  return (
    <>
      <DashHeader title={t("overview.title")} />
      <StatsChart guildId={guildId} initial={stats} />
      <Panel className="mt-6">
        <h2 className="mb-4 text-sm font-medium text-white">{t("overview.recent")}</h2>
        {recent.length === 0 ? (
          <p className="text-sm text-fg-subtle">{t("overview.noRecent")}</p>
        ) : null}
        <ul className="divide-y divide-border">
          {recent.map((c) => (
            <li key={c.id}>
              <Link
                href={`/dashboard/${guildId}/cases/${c.id}`}
                className="flex flex-wrap items-center gap-3 py-3 text-sm hover:text-white"
              >
                <span className="w-12 font-mono text-xs text-fg-subtle">#{c.caseNumber}</span>
                <Badge
                  variant={
                    c.type === "BAN" || c.type === "KICK"
                      ? "danger"
                      : c.type === "TIMEOUT"
                        ? "warning"
                        : "default"
                  }
                >
                  {t(`cases.types.${c.type}`)}
                </Badge>
                <span className="text-white">{c.targetTag ?? c.targetId}</span>
                <span className="min-w-0 flex-1 truncate text-fg-muted">
                  {c.reason ?? t("cases.noReason")}
                </span>
                <time className="text-xs text-fg-subtle">{format.relativeTime(c.createdAt)}</time>
              </Link>
            </li>
          ))}
        </ul>
      </Panel>
    </>
  );
}
