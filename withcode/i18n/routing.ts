import { defineRouting } from "next-intl/routing";

export const locales = ["pl", "en"] as const;
export type Locale = (typeof locales)[number];

export const routing = defineRouting({
  locales,
  defaultLocale: "pl",
  localePrefix: "as-needed",
});
