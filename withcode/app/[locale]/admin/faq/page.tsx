import { getTranslations } from "next-intl/server";
import { db } from "@/lib/db";
import { createFaq } from "@/lib/actions/admin";
import { AdminCard, AdminField, DeleteButton, inputCls } from "@/components/admin/ui";
import { Button } from "@/components/ui/button";

export default async function AdminFaq() {
  const t = await getTranslations("admin");
  const items = await db.faq.findMany({ orderBy: [{ scope: "asc" }, { position: "asc" }] });
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <AdminCard title={t("create")}>
        <form action={createFaq} className="space-y-3">
          <label className="block text-sm text-white">
            <span className="mb-1.5 block">{t("scope")}</span>
            <select name="scope" className={inputCls}>
              <option value="HOME">HOME</option>
              <option value="PREMIUM">PREMIUM</option>
            </select>
          </label>
          <AdminField label={t("questionPl")} name="questionPl" />
          <AdminField label={t("questionEn")} name="questionEn" />
          <AdminField label={t("answerPl")} name="answerPl" textarea />
          <AdminField label={t("answerEn")} name="answerEn" textarea />
          <Button type="submit" magnetic={false}>
            {t("create")}
          </Button>
        </form>
      </AdminCard>
      <AdminCard title={t("faq")}>
        <ul className="divide-y divide-border">
          {items.map((f) => (
            <li key={f.id} className="flex items-center gap-3 py-2.5 text-sm">
              <span className="font-mono text-[10px] text-fg-subtle">{f.scope}</span>
              <span className="flex-1 text-white">{f.questionPl}</span>
              <DeleteButton entity="faq" id={f.id} label={t("delete")} />
            </li>
          ))}
        </ul>
      </AdminCard>
    </div>
  );
}
