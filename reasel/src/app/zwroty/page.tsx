import type { Metadata } from 'next';
import { LegalPage } from '@/components/ui/LegalPage';
import { siteConfig } from '@/config/site';

export const metadata: Metadata = {
  title: 'Zwroty i reklamacje',
  description: '30 dni na zwrot w Reasel.pl. Sprawdź, jak zwrócić lub zareklamować produkt.',
  alternates: { canonical: '/zwroty' },
};

export default function ReturnsPage() {
  return (
    <LegalPage eyebrow="Pomoc" title="Zwroty i reklamacje" updated="1 listopada 2026">
      <h2>30 dni na zwrot</h2>
      <p>Masz aż 30 dni od otrzymania paczki na zwrot produktów bez podania przyczyny. Produkty muszą być nienoszone, z metkami i w oryginalnym opakowaniu.</p>

      <h2>Jak zwrócić produkt?</h2>
      <ol>
        <li>Napisz do nas na {siteConfig.email} z numerem zamówienia (np. RSL-XXXXXX) lub skorzystaj z formularza kontaktowego.</li>
        <li>Otrzymasz bezpłatną etykietę zwrotną InPost – nadasz paczkę w dowolnym Paczkomacie.</li>
        <li>Zwrot środków nastąpi w ciągu 14 dni od otrzymania paczki, tą samą metodą płatności.</li>
      </ol>

      <h2>Wymiana rozmiaru</h2>
      <p>Wymiana na inny rozmiar jest bezpłatna. W zgłoszeniu podaj rozmiar, na który chcesz wymienić produkt – wyślemy go, gdy tylko odbierzemy zwrot.</p>

      <h2>Reklamacje</h2>
      <p>Jeśli produkt ma wadę, prześlij zdjęcia i opis problemu. Rozpatrzymy reklamację w ciągu 14 dni. W przypadku uznania reklamacji naprawimy, wymienimy produkt lub zwrócimy pieniądze – zgodnie z Twoim wyborem i przepisami ustawy o prawach konsumenta.</p>

      <h2>Dożywotnia naprawa szwów</h2>
      <p>W ramach programu Reasel Care naprawiamy rozprute szwy w bluzach i t-shirtach Reasel bezpłatnie, bez względu na datę zakupu.</p>
    </LegalPage>
  );
}
