const plnFormatter = new Intl.NumberFormat('pl-PL', {
  style: 'currency',
  currency: 'PLN',
  minimumFractionDigits: 2,
});

/** Formatuje cenę w groszach do postaci „349,00 zł”. */
export function formatPrice(grosze: number): string {
  return plnFormatter.format(grosze / 100);
}

/** Zamienia polskie znaki i wielkość liter – do wyszukiwania bez diakrytyków. */
export function normalizeText(value: string): string {
  return value
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/ł/g, 'l');
}

/** Poprawna odmiana słowa przez liczbę: 1 produkt, 2 produkty, 5 produktów. */
export function plural(count: number, one: string, few: string, many: string): string {
  if (count === 1) return one;
  const mod10 = count % 10;
  const mod100 = count % 100;
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few;
  return many;
}

/** Buduje URL zdjęcia z Unsplash w zadanym rozmiarze. */
export function unsplash(id: string, width = 1200, height?: number): string {
  const h = height ? `&h=${height}` : '';
  return `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${width}${h}&q=80`;
}

export function cn(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(' ');
}
