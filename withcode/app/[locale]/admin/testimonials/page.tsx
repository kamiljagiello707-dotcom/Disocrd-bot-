import { getTranslations } from "next-intl/server";
import { db } from "@/lib/db";
import { createTestimonial } from "@/lib/actions/admin";
import { AdminCard, AdminField, DeleteButton } from "@/components/admin/ui";
import { Button } from "@/components/ui/button";

export default async function AdminTestimonials() {
  const t = await getTranslations("admin");
  const items = await db.testimonial.findMany({ orderBy: { position: "asc" } });
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <AdminCard title={t("create")}>
        <form action={createTestimonial} className="space-y-3">
          <AdminField label={t("author")} name="author" />
          <AdminField label={t("rolePl")} name="rolePl" />
          <AdminField label={t("roleEn")} name="roleEn" />
          <AdminField label={t("quotePl")} name="quotePl" textarea />
          <AdminField label={t("quoteEn")} name="quoteEn" textarea />
          <Button type="submit" magnetic={false}>
            {t("create")}
          </Button>
        </form>
      </AdminCard>
      <AdminCard title={t("testimonials")}>
        <ul className="divide-y divide-border">
          {items.map((x) => (
            <li key={x.id} className="flex items-center gap-3 py-2.5 text-sm">
              <span className="text-white">{x.author}</span>
              <span className="flex-1 truncate text-fg-muted">{x.quotePl}</span>
              <DeleteButton entity="testimonial" id={x.id} label={t("delete")} />
            </li>
          ))}
        </ul>
      </AdminCard>
    </div>
  );
}
