import { getTranslations, setRequestLocale } from "next-intl/server";
import { DashHeader } from "@/components/dashboard/page-header";
import { SettingsForm } from "@/components/dashboard/settings-form";
import { loadSettings } from "@/lib/dashboard/config";

export default async function SettingsPage({
  params,
}: {
  params: Promise<{ locale: string; guildId: string }>;
}) {
  const { locale, guildId } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("dashboard.settings");
  return (
    <>
      <DashHeader title={t("title")} subtitle={t("subtitle")} />
      <SettingsForm guildId={guildId} initial={await loadSettings(guildId)} />
    </>
  );
}
