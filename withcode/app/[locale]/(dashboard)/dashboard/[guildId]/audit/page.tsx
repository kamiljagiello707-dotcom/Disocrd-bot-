import { getFormatter, getTranslations, setRequestLocale } from "next-intl/server";
import { DashHeader, Panel } from "@/components/dashboard/page-header";
import { db } from "@/lib/db";
import type { DiffEntry } from "@/lib/audit";

const fmt = (v: unknown) => {
  const s = typeof v === "string" ? v : JSON.stringify(v);
  return s.length > 120 ? `${s.slice(0, 117)}…` : s;
};

export default async function AuditPage({
  params,
}: {
  params: Promise<{ locale: string; guildId: string }>;
}) {
  const { locale, guildId } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("dashboard.audit");
  const format = await getFormatter();
  const logs = await db.configAuditLog.findMany({
    where: { guildId },
    orderBy: { createdAt: "desc" },
    take: 100,
    include: { actor: true },
  });
  return (
    <>
      <DashHeader title={t("title")} subtitle={t("subtitle")} />
      {logs.length === 0 ? (
        <Panel>
          <p className="text-sm text-fg-subtle">{t("empty")}</p>
        </Panel>
      ) : null}
      <ol className="space-y-3">
        {logs.map((l) => {
          const diff = (Array.isArray(l.diff) ? l.diff : []) as unknown as DiffEntry[];
          return (
            <li key={l.id}>
              <Panel>
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="text-sm">
                    <span className="font-medium text-white">
                      {l.actor.name ?? l.actor.discordId}
                    </span>{" "}
                    <span className="text-fg-muted">{t("changed", { section: l.section })}</span>
                  </p>
                  <time className="text-xs text-fg-subtle" dateTime={l.createdAt.toISOString()}>
                    {format.dateTime(l.createdAt, { dateStyle: "medium", timeStyle: "short" })}
                  </time>
                </div>
                <ul className="mt-3 space-y-1.5 font-mono text-xs">
                  {diff.slice(0, 12).map((d) => (
                    <li
                      key={d.path}
                      className="grid gap-1 rounded-md bg-black/50 px-3 py-2 md:grid-cols-[14rem_1fr_1fr]"
                    >
                      <span className="text-fg-subtle">{d.path}</span>
                      <span className="text-danger/90 line-through decoration-danger/40">
                        {fmt(d.before)}
                      </span>
                      <span className="text-success">{fmt(d.after)}</span>
                    </li>
                  ))}
                  {diff.length > 12 ? (
                    <li className="text-fg-subtle">+{diff.length - 12}</li>
                  ) : null}
                </ul>
              </Panel>
            </li>
          );
        })}
      </ol>
    </>
  );
}
