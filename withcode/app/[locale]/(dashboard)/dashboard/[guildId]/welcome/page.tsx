import { getTranslations, setRequestLocale } from "next-intl/server";
import { DashHeader } from "@/components/dashboard/page-header";
import { WelcomeForm } from "@/components/dashboard/welcome-form";
import { loadGreetings } from "@/lib/dashboard/config";

export default async function WelcomePage({
  params,
}: {
  params: Promise<{ locale: string; guildId: string }>;
}) {
  const { locale, guildId } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("dashboard.welcome");
  return (
    <>
      <DashHeader title={t("title")} subtitle={t("subtitle")} />
      <WelcomeForm guildId={guildId} initial={await loadGreetings(guildId)} />
    </>
  );
}
