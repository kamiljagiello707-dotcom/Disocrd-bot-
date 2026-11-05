import { getTranslations } from "next-intl/server";
import { db } from "@/lib/db";
import { createChangelog } from "@/lib/actions/admin";
import { AdminCard, AdminField, DeleteButton } from "@/components/admin/ui";
import { Button } from "@/components/ui/button";

export default async function AdminChangelog() {
  const t = await getTranslations("admin");
  const entries = await db.changelogEntry.findMany({
    orderBy: { publishedAt: "desc" },
    include: { _count: { select: { items: true } } },
  });
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <AdminCard title={t("create")}>
        <form action={createChangelog} className="space-y-3">
          <AdminField label={t("version")} name="version" placeholder="2.5.0" />
          <AdminField label={t("titlePl")} name="titlePl" />
          <AdminField label={t("titleEn")} name="titleEn" />
          <AdminField
            label={t("items")}
            name="items"
            textarea
            placeholder="NEW | Nowa funkcja | New feature"
          />
          <Button type="submit" magnetic={false}>
            {t("create")}
          </Button>
        </form>
      </AdminCard>
      <AdminCard title={t("changelog")}>
        <ul className="divide-y divide-border">
          {entries.map((e) => (
            <li key={e.id} className="flex items-center justify-between gap-3 py-2.5 text-sm">
              <span className="font-mono text-white">v{e.version}</span>
              <span className="flex-1 truncate text-fg-muted">
                {e.titlePl} · {e._count.items}
              </span>
              <DeleteButton entity="changelog" id={e.id} label={t("delete")} />
            </li>
          ))}
        </ul>
      </AdminCard>
    </div>
  );
}
