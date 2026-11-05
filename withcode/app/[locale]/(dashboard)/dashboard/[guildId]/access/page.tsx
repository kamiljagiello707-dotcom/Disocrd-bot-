import { getTranslations, setRequestLocale } from "next-intl/server";
import { DashHeader } from "@/components/dashboard/page-header";
import { AccessForm } from "@/components/dashboard/access-form";
import { loadAccess } from "@/lib/dashboard/config";
import { getGuildAccess } from "@/lib/permissions";
import { canManageAccess } from "@/lib/permissions/resolve";

export default async function AccessPage({
  params,
}: {
  params: Promise<{ locale: string; guildId: string }>;
}) {
  const { locale, guildId } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("dashboard.access");
  const { level } = await getGuildAccess(guildId);
  return (
    <>
      <DashHeader title={t("title")} subtitle={t("subtitle")} />
      <AccessForm
        guildId={guildId}
        initial={await loadAccess(guildId)}
        canManage={canManageAccess(level)}
      />
    </>
  );
}
