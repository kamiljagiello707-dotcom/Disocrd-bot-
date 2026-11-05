import type { Localized } from "@/lib/utils";

export type Plan = {
  id: "free" | "premium" | "pro";
  name: string;
  monthly: number; // PLN
  yearly: number; // PLN za rok (10 × miesięcznie)
  description: Localized;
  features: Localized[];
  highlighted?: boolean;
};

export const plans: Plan[] = [
  {
    id: "free",
    name: "Free",
    monthly: 0,
    yearly: 0,
    description: {
      pl: "Pełna moderacja dla większości serwerów.",
      en: "Full moderation for most servers.",
    },
    features: [
      { pl: "Wszystkie 6 modułów AutoModu", en: "All 6 AutoMod modules" },
      { pl: "Anty-raid i lockdown", en: "Anti-raid and lockdown" },
      { pl: "Ostrzeżenia, sprawy, 3 progi kar", en: "Warnings, cases, 3 punishment thresholds" },
      { pl: "Logi — 7 dni historii w panelu", en: "Logs — 7 days of history in the dashboard" },
      { pl: "100 zakazanych słów", en: "100 banned words" },
    ],
  },
  {
    id: "premium",
    name: "Premium",
    monthly: 14.99,
    yearly: 149.9,
    highlighted: true,
    description: { pl: "Dla rosnących społeczności.", en: "For growing communities." },
    features: [
      { pl: "Wszystko z Free", en: "Everything in Free" },
      { pl: "1000 zakazanych słów i import z pliku", en: "1000 banned words and file import" },
      { pl: "20 progów automatycznych kar", en: "20 automatic punishment thresholds" },
      { pl: "Captcha obrazkowa w weryfikacji", en: "Image captcha in verification" },
      { pl: "90 dni historii i eksport CSV", en: "90 days of history and CSV export" },
      { pl: "/raid cleanup", en: "/raid cleanup" },
    ],
  },
  {
    id: "pro",
    name: "Pro",
    monthly: 39.99,
    yearly: 399.9,
    description: { pl: "Dla dużych serwerów i sieci.", en: "For large servers and networks." },
    features: [
      { pl: "Wszystko z Premium", en: "Everything in Premium" },
      { pl: "Do 5 serwerów w jednej subskrypcji", en: "Up to 5 servers in one subscription" },
      { pl: "Własny avatar i nazwa bota", en: "Custom bot avatar and name" },
      { pl: "Nielimitowana historia logów", en: "Unlimited log history" },
      { pl: "Priorytetowe wsparcie (odpowiedź < 4 h)", en: "Priority support (reply < 4 h)" },
    ],
  },
];

export type CompareRow = {
  label: Localized;
  values: [string | boolean, string | boolean, string | boolean];
};

export const planComparison: CompareRow[] = [
  { label: { pl: "Moduły AutoModu", en: "AutoMod modules" }, values: ["6", "6", "6"] },
  { label: { pl: "Zakazane słowa", en: "Banned words" }, values: ["100", "1000", "∞"] },
  {
    label: { pl: "Progi automatycznych kar", en: "Auto punishment thresholds" },
    values: ["3", "20", "20"],
  },
  {
    label: { pl: "Historia w panelu", en: "Dashboard history" },
    values: ["7 dni / days", "90 dni / days", "∞"],
  },
  { label: { pl: "Eksport CSV", en: "CSV export" }, values: [false, true, true] },
  { label: { pl: "Captcha obrazkowa", en: "Image captcha" }, values: [false, true, true] },
  {
    label: { pl: "Role reakcyjne (wiadomości)", en: "Reaction role messages" },
    values: ["3", "25", "25"],
  },
  { label: { pl: "Własny wygląd bota", en: "Custom bot branding" }, values: [false, false, true] },
  {
    label: { pl: "Serwery w subskrypcji", en: "Servers per subscription" },
    values: ["1", "1", "5"],
  },
  {
    label: { pl: "Wsparcie", en: "Support" },
    values: ["Discord", "Discord", "Priorytet / Priority"],
  },
];

export const comparison: {
  label: Localized;
  withbot: boolean | Localized;
  typical: boolean | Localized;
}[] = [
  {
    label: { pl: "Czas reakcji AutoModu", en: "AutoMod response time" },
    withbot: { pl: "< 15 ms", en: "< 15 ms" },
    typical: { pl: "100–500 ms", en: "100–500 ms" },
  },
  {
    label: {
      pl: "Filtr odporny na l33t i polskie znaki",
      en: "Filter resistant to l33t and diacritics",
    },
    withbot: true,
    typical: false,
  },
  {
    label: {
      pl: "Anty-raid z lockdownem jednym kliknięciem",
      en: "Anti-raid with one-click lockdown",
    },
    withbot: true,
    typical: { pl: "Płatne", en: "Paid" },
  },
  { label: { pl: "Panel po polsku", en: "Dashboard in Polish" }, withbot: true, typical: false },
  {
    label: { pl: "Historia zmian konfiguracji", en: "Configuration audit log" },
    withbot: true,
    typical: false,
  },
  {
    label: {
      pl: "Automatyczne progi kar w darmowym planie",
      en: "Auto punishment thresholds on the free plan",
    },
    withbot: true,
    typical: { pl: "1 próg", en: "1 threshold" },
  },
  {
    label: { pl: "Brak reklam w wiadomościach bota", en: "No ads in bot messages" },
    withbot: true,
    typical: false,
  },
];
