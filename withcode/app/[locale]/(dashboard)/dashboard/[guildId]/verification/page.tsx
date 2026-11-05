import { getTranslations, setRequestLocale } from "next-intl/server";
import { DashHeader } from "@/components/dashboard/page-header";
import { VerificationForm } from "@/components/dashboard/verification-form";
import { loadVerification } from "@/lib/dashboard/config";

export default async function VerificationPage({
  params,
}: {
  params: Promise<{ locale: string; guildId: string }>;
}) {
  const { locale, guildId } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("dashboard.verification");
  return (
    <>
      <DashHeader title={t("title")} subtitle={t("subtitle")} />
      <VerificationForm guildId={guildId} initial={await loadVerification(guildId)} />
    </>
  );
}
