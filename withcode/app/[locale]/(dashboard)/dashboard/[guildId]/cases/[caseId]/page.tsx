import { notFound } from "next/navigation";
import { getFormatter, getTranslations, setRequestLocale } from "next-intl/server";
import { DashHeader, Panel } from "@/components/dashboard/page-header";
import { RevokeButton } from "@/components/dashboard/revoke-button";
import { Badge } from "@/components/ui/badge";
import { Link } from "@/i18n/navigation";
import { db } from "@/lib/db";
import { getGuildAccess } from "@/lib/permissions";
import { canEdit } from "@/lib/permissions/resolve";

export default async function CasePage({
  params,
}: {
  params: Promise<{ locale: string; guildId: string; caseId: string }>;
}) {
  const { locale, guildId, caseId } = await params;
  setRequestLocale(locale);
  const [c, access] = await Promise.all([
    db.modCase.findFirst({ where: { id: caseId, guildId } }),
    getGuildAccess(guildId),
  ]);
  if (!c) notFound();
  const t = await getTranslations("dashboard.cases");
  const td = await getTranslations("dashboard.durations");
  const format = await getFormatter();
  const revocable =
    !c.revokedAt && ["WARN", "TIMEOUT", "BAN", "SOFTBAN"].includes(c.type) && canEdit(access.level);
  const durationLabel = c.duration
    ? ["60", "300", "600", "3600", "21600", "86400", "604800"].includes(String(c.duration))
      ? td(String(c.duration) as "60")
      : `${c.duration}s`
    : "—";
  const rows: [string, React.ReactNode][] = [
    [t("type"), <Badge key="t">{t(`types.${c.type}`)}</Badge>],
    [
      t("user"),
      <span key="u" className="font-mono text-white">
        {c.targetTag ?? ""} ({c.targetId})
      </span>,
    ],
    [t("moderator"), c.moderatorTag ?? c.moderatorId],
    [t("source"), t(`sources.${c.source}`)],
    [t("reason"), c.reason ?? t("noReason")],
    [t("duration"), durationLabel],
    [
      t("expires"),
      c.expiresAt ? format.dateTime(c.expiresAt, { dateStyle: "medium", timeStyle: "short" }) : "—",
    ],
    [t("date"), format.dateTime(c.createdAt, { dateStyle: "full", timeStyle: "short" })],
  ];
  return (
    <>
      <Link
        href={`/dashboard/${guildId}/cases`}
        className="mb-6 inline-flex text-xs text-fg-subtle hover:text-white"
      >
        ← {t("title")}
      </Link>
      <DashHeader
        title={t("details", { number: c.caseNumber })}
        actions={revocable ? <RevokeButton guildId={guildId} caseId={c.id} /> : null}
      />
      {c.revokedAt ? (
        <p className="mb-4 rounded-lg border border-success/30 bg-success/10 px-4 py-3 text-sm text-success">
          {t("revokedBy", {
            user: c.revokedBy ?? "?",
            time: format.dateTime(c.revokedAt, { dateStyle: "medium", timeStyle: "short" }),
          })}
          {c.revokeReason ? ` — ${c.revokeReason}` : ""}
        </p>
      ) : null}
      <Panel>
        <dl className="divide-y divide-border">
          {rows.map(([k, v]) => (
            <div key={k} className="grid gap-1 py-3 sm:grid-cols-[12rem_1fr]">
              <dt className="text-sm text-fg-subtle">{k}</dt>
              <dd className="text-sm text-fg-muted">{v}</dd>
            </div>
          ))}
        </dl>
      </Panel>
    </>
  );
}
