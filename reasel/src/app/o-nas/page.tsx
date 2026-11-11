import type { Metadata } from 'next';
import Image from 'next/image';
import { PageHeader } from '@/components/ui/PageHeader';
import { SplitText } from '@/components/ui/SplitText';
import { Marquee } from '@/components/ui/Marquee';
import { TransitionLink } from '@/components/ui/TransitionLink';
import { unsplash } from '@/lib/format';

export const metadata: Metadata = {
  title: 'O nas — manifest marki',
  description: 'Reasel to warszawska marka premium streetwear. Szyjemy w Polsce, w małych seriach, z certyfikowanych materiałów.',
  alternates: { canonical: '/o-nas' },
};

const stats = [
  { value: '2019', label: 'Rok założenia' },
  { value: '100%', label: 'Szyte w Polsce' },
  { value: '42', label: 'Limitowane dropy' },
  { value: '68k', label: 'Klientów w Reasel Club' },
];

const values = [
  { title: 'Lokalnie', text: 'Współpracujemy z pięcioma szwalniami w promieniu 200 km od Warszawy. Znamy z imienia każdą osobę, która szyje nasze ubrania.' },
  { title: 'Świadomie', text: 'Bawełna organiczna GOTS, poliester z recyklingu, zero plastiku w opakowaniach. Krótkie serie zamiast nadprodukcji.' },
  { title: 'Na lata', text: 'Projektujemy rzeczy, które mają się zestarzeć z godnością. Dożywotnio naprawiamy szwy w naszych bluzach.' },
];

export default function AboutPage() {
  return (
    <div className="relative z-10 bg-ink">
      <PageHeader eyebrow="Manifest" title="Ubrania bez kompromisów" description="Reasel powstał z frustracji. Szukaliśmy streetwearu, który wygląda jak z Tokio, a jakością dorównuje włoskim pracowniom – i nie mogliśmy go znaleźć. Więc zaczęliśmy go szyć." />

      <section className="container-x grid gap-6 pb-24 md:grid-cols-12">
        <div className="relative aspect-[4/5] overflow-hidden rounded-3xl md:col-span-7 md:aspect-[16/11]">
          <Image src={unsplash('photo-1523381210434-271e8be1f52b', 1600)} alt="T-shirty Reasel na drewnianych wieszakach w pracowni" fill priority sizes="(min-width: 768px) 58vw, 100vw" className="object-cover" />
        </div>
        <div className="relative aspect-[4/5] overflow-hidden rounded-3xl md:col-span-5">
          <Image src={unsplash('photo-1611312449408-fcece27cdbb7', 1000)} alt="Wieszak z kolekcją ubrań w showroomie" fill sizes="(min-width: 768px) 42vw, 100vw" className="object-cover" />
        </div>
      </section>

      <section className="border-y border-white/10 py-20" aria-label="Liczby">
        <dl className="container-x grid grid-cols-2 gap-10 md:grid-cols-4">
          {stats.map((s) => (
            <div key={s.label}>
              <dt className="eyebrow">{s.label}</dt>
              <dd className="h-display mt-2 text-5xl text-acid md:text-7xl">{s.value}</dd>
            </div>
          ))}
        </dl>
      </section>

      <section className="container-x py-24 md:py-32" aria-labelledby="values-title">
        <SplitText id="values-title" text="W co wierzymy" className="h-display mb-16 text-display-md" />
        <div className="grid gap-12 md:grid-cols-3">
          {values.map((v, i) => (
            <article key={v.title} className="border-t border-white/15 pt-6">
              <p className="eyebrow">0{i + 1}</p>
              <h3 className="h-display mt-3 text-3xl">{v.title}</h3>
              <p className="mt-4 leading-relaxed text-ink-200">{v.text}</p>
            </article>
          ))}
        </div>
      </section>

      <Marquee items={['Warszawa', 'Łódź', 'Kraków', 'Szyte w Polsce']} className="border-y border-white/10 py-8 text-6xl md:text-8xl" outline />

      <section className="container-x py-24 text-center md:py-32">
        <p className="h-display mx-auto max-w-4xl text-display-md">
          Nie robimy sezonów. Robimy <span className="text-acid">dropy</span>, które warto mieć.
        </p>
        <TransitionLink href="/sklep" className="btn-primary mt-12">
          Zobacz kolekcję
        </TransitionLink>
      </section>
    </div>
  );
}
