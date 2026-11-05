import { getTranslations, setRequestLocale } from "next-intl/server";
import { DashHeader } from "@/components/dashboard/page-header";
import { AntiRaidForm } from "@/components/dashboard/antiraid-form";
import { loadAntiRaid } from "@/lib/dashboard/config";

export default async function AntiRaidPage({
  params,
}: {
  params: Promise<{ locale: string; guildId: string }>;
}) {
  const { locale, guildId } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("dashboard.antiraid");
  const { lockdownActive, lockdownAt, ...initial } = await loadAntiRaid(guildId);
  return (
    <>
      <DashHeader title={t("title")} subtitle={t("subtitle")} />
      <AntiRaidForm
        guildId={guildId}
        initial={initial}
        lockdown={{ active: lockdownActive, at: lockdownAt }}
      />
    </>
  );
}
