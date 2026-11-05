import { getTranslations, setRequestLocale } from "next-intl/server";
import { DashHeader } from "@/components/dashboard/page-header";
import { PunishmentsForm } from "@/components/dashboard/punishments-form";
import { loadPunishments } from "@/lib/dashboard/config";

export default async function PunishmentsPage({
  params,
}: {
  params: Promise<{ locale: string; guildId: string }>;
}) {
  const { locale, guildId } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("dashboard.punishments");
  return (
    <>
      <DashHeader title={t("title")} subtitle={t("subtitle")} />
      <PunishmentsForm guildId={guildId} initial={await loadPunishments(guildId)} />
    </>
  );
}
