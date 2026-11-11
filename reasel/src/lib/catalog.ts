import { products } from './products';
import type { Category, Product } from './types';

/**
 * WARSTWA DOSTĘPU DO DANYCH
 * -------------------------
 * Wszystkie strony pobierają produkty wyłącznie przez te funkcje.
 * Aby podpiąć Shopify Storefront API / Stripe / CMS, zamień implementację
 * na zapytania `fetch` (funkcje są już asynchroniczne) i zmapuj odpowiedź
 * na typ `Product` z `lib/types.ts`.
 */

export const categories: { slug: Category; label: string; description: string }[] = [
  { slug: 'bluzy', label: 'Bluzy', description: 'Ciężkie bluzy z kapturem i crewnecki' },
  { slug: 't-shirty', label: 'T-shirty', description: 'Koszulki heavyweight i graficzne' },
  { slug: 'spodnie', label: 'Spodnie', description: 'Cargo, denim selvedge i wide leg' },
  { slug: 'kurtki', label: 'Kurtki', description: 'Ramoneski, parki i bombery' },
  { slug: 'akcesoria', label: 'Akcesoria', description: 'Czapki, plecaki i skórzane dodatki' },
];

export function categoryLabel(slug: Category): string {
  return categories.find((c) => c.slug === slug)?.label ?? slug;
}

export async function getProducts(): Promise<Product[]> {
  return products;
}

export async function getProductBySlug(slug: string): Promise<Product | undefined> {
  return products.find((p) => p.slug === slug);
}

export async function getProductsByTag(tag: Product['tags'][number], limit?: number): Promise<Product[]> {
  const list = products.filter((p) => p.tags.includes(tag));
  return typeof limit === 'number' ? list.slice(0, limit) : list;
}

/** Podobne produkty: najpierw ta sama kategoria, potem pozostałe bestsellery. */
export async function getRelatedProducts(product: Product, limit = 4): Promise<Product[]> {
  const sameCategory = products.filter((p) => p.category === product.category && p.id !== product.id);
  const others = products.filter(
    (p) => p.category !== product.category && p.tags.includes('bestseller') && p.id !== product.id,
  );
  return [...sameCategory, ...others].slice(0, limit);
}

/** Lekka wersja katalogu dla wyszukiwarki (renderowanej po stronie klienta). */
export function getSearchIndex() {
  return products.map((p) => ({
    id: p.id,
    slug: p.slug,
    name: p.name,
    category: p.category,
    price: p.price,
    image: p.images[0],
    colors: p.colors.map((c) => c.name),
    text: [p.name, p.shortDescription, p.category, ...p.colors.map((c) => c.name), p.composition].join(' '),
  }));
}

export type SearchEntry = ReturnType<typeof getSearchIndex>[number];
