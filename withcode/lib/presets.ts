import type { Preset } from "@/components/marketing/discord-mock";

type T = (key: "spam" | "link" | "invite" | "caps" | "mentions" | "word" | "clean") => string;

export function demoPresets(t: T, locale: string): Preset[] {
  const en = locale === "en";
  return [
    {
      id: "spam",
      label: t("spam"),
      messages: Array.from({ length: 5 }, () =>
        en ? "free nitro free nitro" : "darmowe nitro darmowe nitro",
      ),
    },
    {
      id: "link",
      label: t("link"),
      messages: [
        en ? "login here http://free-skins.ru/steam" : "zaloguj się tu http://free-skins.ru/steam",
      ],
    },
    {
      id: "invite",
      label: t("invite"),
      messages: [en ? "join discord.gg/best-server now" : "wbijajcie discord.gg/lepszy-serwer"],
    },
    {
      id: "caps",
      label: t("caps"),
      messages: [en ? "WHY IS NOBODY ANSWERING ME HERE" : "CZEMU NIKT MI TU NIE ODPISUJE"],
    },
    {
      id: "mentions",
      label: t("mentions"),
      messages: ["@Ola @Kuba @Ania @Tomek @Ela @Piotr @Zosia @Bartek @Iga"],
    },
    { id: "word", label: t("word"), messages: [en ? "you are an 1d1ot" : "jesteś 1d10t4"] },
    {
      id: "clean",
      label: t("clean"),
      messages: [en ? "Anyone up for a game tonight?" : "Ktoś gra dziś wieczorem?"],
    },
  ];
}
