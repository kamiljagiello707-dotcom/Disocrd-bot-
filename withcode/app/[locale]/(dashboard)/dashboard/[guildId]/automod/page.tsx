import { getTranslations, setRequestLocale } from "next-intl/server";
import { DashHeader } from "@/components/dashboard/page-header";
import { AutoModForm } from "@/components/dashboard/automod-form";
import { loadAutoMod } from "@/lib/dashboard/config";

export default async function AutoModPage({
  params,
}: {
  params: Promise<{ locale: string; guildId: string }>;
}) {
  const { locale, guildId } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("dashboard.automod");
  const initial = await loadAutoMod(guildId);
  return (
    <>
      <DashHeader title={t("title")} subtitle={t("subtitle")} />
      <AutoModForm guildId={guildId} initial={initial} />
    </>
  );
}
