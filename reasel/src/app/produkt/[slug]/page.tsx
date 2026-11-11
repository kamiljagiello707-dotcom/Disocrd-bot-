import type { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { getProductBySlug, getProducts, getRelatedProducts, categoryLabel } from '@/lib/catalog';
import { breadcrumbJsonLd, productJsonLd } from '@/lib/seo';
import { formatPrice } from '@/lib/format';
import { JsonLd } from '@/components/ui/JsonLd';
import { SplitText } from '@/components/ui/SplitText';
import { ProductClient } from '@/components/product/ProductClient';
import { ProductCard } from '@/components/product/ProductCard';

interface Params {
  params: { slug: string };
}

/** Statyczne generowanie wszystkich stron produktów (SSG). */
export async function generateStaticParams() {
  const products = await getProducts();
  return products.map((p) => ({ slug: p.slug }));
}

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const product = await getProductBySlug(params.slug);
  if (!product) return { title: 'Produkt nie istnieje' };
  const title = `${product.name} — ${formatPrice(product.price)}`;
  return {
    title,
    description: `${product.shortDescription} ${product.composition}. Darmowa dostawa od 299 zł, 30 dni na zwrot.`,
    alternates: { canonical: `/produkt/${product.slug}` },
    openGraph: {
      title,
      description: product.shortDescription,
      url: `/produkt/${product.slug}`,
      images: product.images.map((i) => ({ url: i.src, alt: i.alt, width: 1200, height: 1500 })),
    },
    twitter: { card: 'summary_large_image', title, description: product.shortDescription, images: [product.images[0].src] },
  };
}

export default async function ProductPage({ params }: Params) {
  const product = await getProductBySlug(params.slug);
  if (!product) notFound();
  const related = await getRelatedProducts(product, 4);

  return (
    <>
      <JsonLd data={productJsonLd(product)} />
      <JsonLd
        data={breadcrumbJsonLd([
          { name: 'Start', path: '/' },
          { name: categoryLabel(product.category), path: `/sklep?kategoria=${product.category}` },
          { name: product.name, path: `/produkt/${product.slug}` },
        ])}
      />
      <div className="relative z-10 bg-ink">
        <ProductClient product={product} />

        <section className="container-x border-t border-white/10 py-24" aria-labelledby="related-title">
          <p className="eyebrow mb-4">Może Ci się spodobać</p>
          <SplitText id="related-title" text="Podobne produkty" className="h-display mb-12 text-display-md" />
          <ul className="grid grid-cols-2 gap-x-4 gap-y-12 md:grid-cols-4 md:gap-x-6">
            {related.map((p) => (
              <li key={p.id}>
                <ProductCard product={p} sizes="(min-width: 768px) 25vw, 50vw" />
              </li>
            ))}
          </ul>
        </section>
      </div>
    </>
  );
}
