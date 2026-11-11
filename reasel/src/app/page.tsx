import { Hero } from '@/components/sections/Hero';
import { NewCollection } from '@/components/sections/NewCollection';
import { Bestsellers } from '@/components/sections/Bestsellers';
import { BrandStory } from '@/components/sections/BrandStory';
import { Lookbook } from '@/components/sections/Lookbook';
import { Newsletter } from '@/components/sections/Newsletter';
import { Marquee } from '@/components/ui/Marquee';
import { JsonLd } from '@/components/ui/JsonLd';
import { getProductsByTag } from '@/lib/catalog';
import { lookbook } from '@/lib/products';
import { organizationJsonLd } from '@/lib/seo';

/**
 * Strona główna: Hero 3D → Nowa kolekcja (karuzela 3D) → Bestsellery →
 * Historia marki (scroll-story) → Lookbook → Newsletter → Stopka (layout).
 */
export default async function HomePage() {
  const [newest, bestsellers] = await Promise.all([getProductsByTag('nowosc', 8), getProductsByTag('bestseller', 4)]);

  return (
    <>
      <JsonLd data={organizationJsonLd()} />
      <Hero />
      <div className="relative z-10 -rotate-2 border-y border-acid/40 bg-acid py-4 text-ink">
        <Marquee items={['Nowa kolekcja FW26', 'Darmowa dostawa od 299 zł', '30 dni na zwrot', 'Szyte w Polsce']} className="text-2xl md:text-4xl" speed={70} />
      </div>
      <NewCollection products={newest} />
      <Bestsellers products={bestsellers} />
      <BrandStory />
      <Lookbook images={lookbook} />
      <Newsletter />
    </>
  );
}
