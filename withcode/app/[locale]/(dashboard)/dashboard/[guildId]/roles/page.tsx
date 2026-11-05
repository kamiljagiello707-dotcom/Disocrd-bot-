import { getTranslations, setRequestLocale } from "next-intl/server";
import { DashHeader } from "@/components/dashboard/page-header";
import { RolesForm } from "@/components/dashboard/roles-form";
import { loadRoles } from "@/lib/dashboard/config";

export default async function RolesPage({
  params,
}: {
  params: Promise<{ locale: string; guildId: string }>;
}) {
  const { locale, guildId } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("dashboard.roles");
  return (
    <>
      <DashHeader title={t("title")} subtitle={t("subtitle")} />
      <RolesForm guildId={guildId} initial={await loadRoles(guildId)} />
    </>
  );
}
