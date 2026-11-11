export const siteConfig = {
  name: 'Reasel.pl',
  shortName: 'REASEL',
  description:
    'Reasel — premium streetwear projektowany w Polsce. Bluzy, t-shirty, spodnie, kurtki i akcesoria w limitowanych dropach. Darmowa dostawa od 299 zł.',
  url: process.env.NEXT_PUBLIC_SITE_URL ?? 'https://reasel.pl',
  locale: 'pl_PL',
  email: 'kontakt@reasel.pl',
  phone: '+48 500 600 700',
  address: 'ul. Przykładowa 12, 00-001 Warszawa',
  social: {
    instagram: 'https://instagram.com/reasel.pl',
    tiktok: 'https://tiktok.com/@reasel.pl',
  },
  /** Próg darmowej dostawy w groszach */
  freeShippingThreshold: 29900,
  currency: 'PLN',
} as const;

export const shippingMethods = [
  {
    id: 'inpost',
    name: 'InPost Paczkomat 24/7',
    description: 'Dostawa w 1–2 dni robocze do wybranego Paczkomatu',
    price: 1399,
  },
  {
    id: 'dpd',
    name: 'Kurier DPD',
    description: 'Dostawa pod wskazany adres w 1–2 dni robocze',
    price: 1699,
  },
] as const;

export type ShippingMethodId = (typeof shippingMethods)[number]['id'];

export const paymentMethods = [
  { id: 'blik', name: 'BLIK', description: 'Szybka płatność kodem z aplikacji bankowej' },
  { id: 'przelew', name: 'Przelew tradycyjny', description: 'Wysyłka po zaksięgowaniu wpłaty' },
  { id: 'karta', name: 'Karta płatnicza', description: 'Visa, Mastercard, Maestro' },
] as const;

export type PaymentMethodId = (typeof paymentMethods)[number]['id'];

export const mainNav = [
  { href: '/sklep', label: 'Sklep' },
  { href: '/sklep?kategoria=bluzy', label: 'Bluzy' },
  { href: '/sklep?kategoria=kurtki', label: 'Kurtki' },
  { href: '/o-nas', label: 'O nas' },
  { href: '/kontakt', label: 'Kontakt' },
] as const;
