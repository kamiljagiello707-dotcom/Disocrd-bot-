import { getTranslations } from "next-intl/server";
import { db } from "@/lib/db";
import { createPartner } from "@/lib/actions/admin";
import { AdminCard, AdminField, DeleteButton } from "@/components/admin/ui";
import { Button } from "@/components/ui/button";

export default async function AdminPartners() {
  const t = await getTranslations("admin");
  const items = await db.partner.findMany({ orderBy: { position: "asc" } });
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <AdminCard title={t("create")}>
        <form action={createPartner} className="space-y-3">
          <AdminField label={t("name")} name="name" />
          <AdminField
            label={t("inviteUrl")}
            name="inviteUrl"
            type="url"
            required={false}
            placeholder="https://discord.gg/…"
          />
          <AdminField label={t("members")} name="memberCount" type="number" required={false} />
          <AdminField label={t("descPl")} name="descPl" required={false} />
          <AdminField label={t("descEn")} name="descEn" required={false} />
          <Button type="submit" magnetic={false}>
            {t("create")}
          </Button>
        </form>
      </AdminCard>
      <AdminCard title={t("partners")}>
        <ul className="divide-y divide-border">
          {items.map((p) => (
            <li key={p.id} className="flex items-center gap-3 py-2.5 text-sm">
              <span className="flex-1 text-white">{p.name}</span>
              <span className="text-fg-subtle">{p.memberCount ?? ""}</span>
              <DeleteButton entity="partner" id={p.id} label={t("delete")} />
            </li>
          ))}
        </ul>
      </AdminCard>
    </div>
  );
}
