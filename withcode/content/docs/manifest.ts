import type { Localized } from "@/lib/utils";

/** Kolejność i tytuły stron dokumentacji. Plik MDX: content/docs/{pl,en}/{slug}.mdx */
export const docsNav: { group: Localized; items: { slug: string; title: Localized }[] }[] = [
  {
    group: { pl: "Start", en: "Getting started" },
    items: [
      { slug: "introduction", title: { pl: "Wprowadzenie", en: "Introduction" } },
      { slug: "quick-start", title: { pl: "Szybki start", en: "Quick start" } },
    ],
  },
  {
    group: { pl: "Ochrona", en: "Protection" },
    items: [
      { slug: "automod", title: { pl: "Konfiguracja AutoModu", en: "Configuring AutoMod" } },
      { slug: "anti-raid", title: { pl: "Anty-raid i lockdown", en: "Anti-raid and lockdown" } },
      { slug: "punishments", title: { pl: "Ostrzeżenia i kary", en: "Warnings and punishments" } },
    ],
  },
  {
    group: { pl: "Społeczność", en: "Community" },
    items: [
      { slug: "greetings", title: { pl: "Powitania i zmienne", en: "Greetings and variables" } },
      { slug: "dashboard", title: { pl: "Panel i uprawnienia", en: "Dashboard and permissions" } },
    ],
  },
];

export const docSlugs = docsNav.flatMap((g) => g.items.map((i) => i.slug));
