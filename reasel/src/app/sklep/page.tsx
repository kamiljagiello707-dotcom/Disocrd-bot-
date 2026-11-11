import type { Metadata } from 'next';
import { categories, getProducts } from '@/lib/catalog';
import type { Category } from '@/lib/types';
import { ShopClient } from '@/components/shop/ShopClient';
import { PageHeader } from '@/components/ui/PageHeader';

export const metadata: Metadata = {
  title: 'Sklep — bluzy, t-shirty, spodnie, kurtki i akcesoria',
  description:
    'Pełna kolekcja Reasel: ciężkie bluzy oversize, t-shirty heavyweight, cargo, jeansy selvedge, kurtki i akcesoria. Filtruj po rozmiarze, kolorze i cenie.',
  alternates: { canonical: '/sklep' },
};

export default async function ShopPage({ searchParams }: { searchParams: { kategoria?: string } }) {
  const products = await getProducts();
  // Filtr kategorii renderowany po stronie serwera – siatka jest w HTML (SEO, brak przeskoku układu)
  const initialCategory = categories.some((c) => c.slug === searchParams.kategoria) ? (searchParams.kategoria as Category) : null;
  return (
    <>
      <PageHeader eyebrow={`Sklep · ${products.length} produktów`} title="Kolekcja" description="Wszystko, co obecnie szyjemy. Filtruj, sortuj i znajdź swój rozmiar." />
      <ShopClient products={products} initialCategory={initialCategory} />
    </>
  );
}
