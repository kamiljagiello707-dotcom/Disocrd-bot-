import { getTranslations, setRequestLocale } from "next-intl/server";
import { DashHeader } from "@/components/dashboard/page-header";
import { LogsForm } from "@/components/dashboard/logs-form";
import { loadLogs } from "@/lib/dashboard/config";

export default async function LogsPage({
  params,
}: {
  params: Promise<{ locale: string; guildId: string }>;
}) {
  const { locale, guildId } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("dashboard.logs");
  return (
    <>
      <DashHeader title={t("title")} subtitle={t("subtitle")} />
      <LogsForm guildId={guildId} initial={await loadLogs(guildId)} />
    </>
  );
}
