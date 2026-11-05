import { getFormatter, getTranslations } from "next-intl/server";
import { db } from "@/lib/db";
import { addIncidentUpdate, createIncident } from "@/lib/actions/admin";
import { AdminCard, AdminField, DeleteButton, inputCls } from "@/components/admin/ui";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

export default async function AdminIncidents() {
  const t = await getTranslations("admin");
  const ts = await getTranslations("status");
  const format = await getFormatter();
  const incidents = await db.incident.findMany({
    orderBy: { startedAt: "desc" },
    take: 30,
    include: { updates: { orderBy: { createdAt: "desc" }, take: 1 } },
  });
  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_1.4fr]">
      <AdminCard title={t("create")}>
        <form action={createIncident} className="space-y-3">
          <AdminField label={t("titlePl")} name="titlePl" />
          <AdminField label={t("titleEn")} name="titleEn" />
          <label className="block text-sm text-white">
            <span className="mb-1.5 block">{t("severity")}</span>
            <select name="severity" className={inputCls}>
              {(["MINOR", "MAJOR", "CRITICAL", "MAINTENANCE"] as const).map((s) => (
                <option key={s} value={s}>
                  {ts(`severity.${s}`)}
                </option>
              ))}
            </select>
          </label>
          <AdminField
            label="Services (BOT,API,DATABASE,SHARDS,DASHBOARD)"
            name="services"
            placeholder="BOT,SHARDS"
          />
          <AdminField label={t("messagePl")} name="messagePl" textarea />
          <AdminField label={t("messageEn")} name="messageEn" textarea />
          <Button type="submit" magnetic={false}>
            {t("create")}
          </Button>
        </form>
      </AdminCard>
      <div className="space-y-3">
        {incidents.map((i) => (
          <AdminCard key={i.id}>
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-sm font-medium text-white">{i.titlePl}</p>
                <p className="mt-1 text-xs text-fg-subtle">
                  {format.dateTime(i.startedAt, { dateStyle: "medium", timeStyle: "short" })}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Badge variant={i.status === "RESOLVED" ? "success" : "warning"}>
                  {ts(`incidentStatus.${i.status}`)}
                </Badge>
                <DeleteButton entity="incident" id={i.id} label={t("delete")} />
              </div>
            </div>
            {i.status !== "RESOLVED" ? (
              <form
                action={addIncidentUpdate}
                className="mt-4 grid gap-2 border-t border-border pt-4"
              >
                <input type="hidden" name="id" value={i.id} />
                <select name="status" className={inputCls} aria-label={t("status")}>
                  {(["IDENTIFIED", "MONITORING", "RESOLVED"] as const).map((s) => (
                    <option key={s} value={s}>
                      {ts(`incidentStatus.${s}`)}
                    </option>
                  ))}
                </select>
                <input
                  name="messagePl"
                  required
                  placeholder={t("messagePl")}
                  className={inputCls}
                  aria-label={t("messagePl")}
                />
                <input
                  name="messageEn"
                  required
                  placeholder={t("messageEn")}
                  className={inputCls}
                  aria-label={t("messageEn")}
                />
                <Button type="submit" size="sm" variant="outline" magnetic={false}>
                  {t("addUpdate")}
                </Button>
              </form>
            ) : null}
          </AdminCard>
        ))}
      </div>
    </div>
  );
}
