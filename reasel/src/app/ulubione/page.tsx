import type { Metadata } from 'next';
import { PageHeader } from '@/components/ui/PageHeader';
import { FavoritesList } from '@/components/shop/FavoritesList';
import { getProducts } from '@/lib/catalog';

export const metadata: Metadata = {
  title: 'Ulubione',
  robots: { index: false, follow: true },
};

export default async function FavoritesPage() {
  const products = await getProducts();
  return (
    <div className="relative z-10 min-h-screen bg-ink">
      <PageHeader eyebrow="Twoja lista" title="Ulubione" />
      <FavoritesList products={products} />
    </div>
  );
}
