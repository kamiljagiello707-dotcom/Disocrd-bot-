import { getTranslations } from "next-intl/server";
import { NotFoundStage } from "@/components/sections/not-found-stage";

export default async function NotFound() {
  const t = await getTranslations("notFound");
  const tc = await getTranslations("common");
  return (
    <NotFoundStage
      title={t("title")}
      subtitle={t("subtitle")}
      home={t("home")}
      alt={t("sceneAlt")}
      back={tc("back")}
    />
  );
}
