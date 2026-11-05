import { getFormatter, getTranslations } from "next-intl/server";
import { db } from "@/lib/db";
import { addBlacklist } from "@/lib/actions/admin";
import { AdminCard, AdminField, DeleteButton, inputCls } from "@/components/admin/ui";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

export default async function AdminBlacklist() {
  const t = await getTranslations("admin");
  const format = await getFormatter();
  const items = await db.blacklistEntry.findMany({ orderBy: { createdAt: "desc" } });
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <AdminCard title={t("create")}>
        <form action={addBlacklist} className="space-y-3">
          <label className="block text-sm text-white">
            <span className="mb-1.5 block">{t("type")}</span>
            <select name="type" className={inputCls}>
              <option value="GUILD">GUILD</option>
              <option value="USER">USER</option>
            </select>
          </label>
          <AdminField label={t("targetId")} name="targetId" placeholder="123456789012345678" />
          <AdminField label={t("reason")} name="reason" required={false} />
          <Button type="submit" magnetic={false}>
            {t("create")}
          </Button>
        </form>
      </AdminCard>
      <AdminCard title={t("blacklist")}>
        <ul className="divide-y divide-border">
          {items.map((b) => (
            <li key={b.id} className="flex items-center gap-3 py-2.5 text-sm">
              <Badge>{b.type}</Badge>
              <span className="font-mono text-white">{b.targetId}</span>
              <span className="flex-1 truncate text-fg-muted">{b.reason}</span>
              <span className="text-xs text-fg-subtle">
                {format.dateTime(b.createdAt, { dateStyle: "short" })}
              </span>
              <DeleteButton entity="blacklist" id={b.id} label={t("delete")} />
            </li>
          ))}
        </ul>
      </AdminCard>
    </div>
  );
}
