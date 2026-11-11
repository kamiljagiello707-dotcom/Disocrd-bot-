import type { Metadata } from 'next';
import { LegalPage } from '@/components/ui/LegalPage';
import { siteConfig } from '@/config/site';
import { TransitionLink } from '@/components/ui/TransitionLink';

export const metadata: Metadata = {
  title: 'Regulamin sklepu',
  description: 'Regulamin sklepu internetowego Reasel.pl – zasady składania zamówień, płatności, dostawy, odstąpienia od umowy i reklamacji.',
  alternates: { canonical: '/regulamin' },
};

export default function TermsPage() {
  return (
    <LegalPage eyebrow="Dokumenty" title="Regulamin" updated="1 listopada 2026">
      <h2>§1. Postanowienia ogólne</h2>
      <ol>
        <li>Sklep internetowy dostępny pod adresem {siteConfig.url} prowadzony jest przez Reasel sp. z o.o. z siedzibą w Warszawie ({siteConfig.address}), NIP 000-000-00-00 (dalej: „Sprzedawca”).</li>
        <li>Kontakt ze Sprzedawcą: e-mail {siteConfig.email}, tel. {siteConfig.phone}.</li>
        <li>Regulamin określa zasady korzystania ze sklepu, składania zamówień, dostawy, płatności, odstąpienia od umowy oraz rozpatrywania reklamacji.</li>
      </ol>

      <h2>§2. Składanie zamówień</h2>
      <ol>
        <li>Zamówienia można składać 24 godziny na dobę, 7 dni w tygodniu.</li>
        <li>W celu złożenia zamówienia należy dodać produkty do koszyka, podać dane niezbędne do realizacji, wybrać sposób dostawy i płatności oraz zaakceptować regulamin.</li>
        <li>Umowa sprzedaży zostaje zawarta z chwilą otrzymania przez Klienta e-maila potwierdzającego przyjęcie zamówienia.</li>
      </ol>

      <h2>§3. Ceny i płatności</h2>
      <ol>
        <li>Ceny podane są w złotych polskich (PLN) i zawierają podatek VAT.</li>
        <li>Dostępne metody płatności: BLIK, przelew tradycyjny, karta płatnicza (Visa, Mastercard, Maestro).</li>
        <li>Przy wyborze przelewu tradycyjnego Klient zobowiązany jest do zapłaty w terminie 3 dni roboczych.</li>
      </ol>

      <h2>§4. Dostawa</h2>
      <ol>
        <li>Dostawa realizowana jest na terytorium Polski za pośrednictwem InPost (Paczkomaty 24/7) oraz kuriera DPD.</li>
        <li>Koszt dostawy: InPost – 13,99 zł, DPD – 16,99 zł. Przy zamówieniach od 299 zł dostawa jest bezpłatna.</li>
        <li>Zamówienia wysyłamy w ciągu 1 dnia roboczego od zaksięgowania płatności.</li>
      </ol>

      <h2>§5. Prawo odstąpienia od umowy</h2>
      <p>Konsument może odstąpić od umowy w terminie 30 dni od otrzymania towaru, bez podania przyczyny. Szczegóły procedury opisuje strona <TransitionLink href="/zwroty">Zwroty i reklamacje</TransitionLink>.</p>

      <h2>§6. Reklamacje</h2>
      <p>Sprzedawca odpowiada za zgodność towaru z umową na zasadach określonych w ustawie o prawach konsumenta. Reklamację można złożyć mailowo lub przez formularz kontaktowy. Odpowiedź udzielana jest w terminie 14 dni.</p>

      <h2>§7. Postanowienia końcowe</h2>
      <p>W sprawach nieuregulowanych zastosowanie mają przepisy prawa polskiego, w szczególności Kodeksu cywilnego oraz ustawy o prawach konsumenta. Konsument może skorzystać z pozasądowych sposobów rozpatrywania sporów, m.in. platformy ODR.</p>
    </LegalPage>
  );
}
