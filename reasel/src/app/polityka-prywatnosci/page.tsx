import type { Metadata } from 'next';
import { LegalPage } from '@/components/ui/LegalPage';
import { siteConfig } from '@/config/site';

export const metadata: Metadata = {
  title: 'Polityka prywatności',
  description: 'Jak Reasel.pl przetwarza dane osobowe i wykorzystuje pliki cookies – informacje zgodne z RODO.',
  alternates: { canonical: '/polityka-prywatnosci' },
};

export default function PrivacyPage() {
  return (
    <LegalPage eyebrow="Dokumenty" title="Polityka prywatności" updated="1 listopada 2026">
      <h2>1. Administrator danych</h2>
      <p>Administratorem Twoich danych osobowych jest Reasel sp. z o.o., {siteConfig.address}. W sprawach dotyczących danych osobowych skontaktuj się z nami: {siteConfig.email}.</p>

      <h2>2. Cele i podstawy przetwarzania</h2>
      <ul>
        <li>realizacja zamówienia i umowy sprzedaży – art. 6 ust. 1 lit. b RODO,</li>
        <li>obowiązki księgowe i podatkowe – art. 6 ust. 1 lit. c RODO,</li>
        <li>obsługa reklamacji i dochodzenie roszczeń – art. 6 ust. 1 lit. f RODO,</li>
        <li>newsletter i marketing – na podstawie zgody, art. 6 ust. 1 lit. a RODO,</li>
        <li>analityka i statystyka – na podstawie zgody wyrażonej w banerze cookies.</li>
      </ul>

      <h2>3. Odbiorcy danych</h2>
      <p>Dane mogą być przekazywane podmiotom wspierającym nas w realizacji zamówień: firmom kurierskim (InPost, DPD), operatorom płatności, dostawcom hostingu i narzędzi e-mail – wyłącznie w zakresie niezbędnym.</p>

      <h2>4. Okres przechowywania</h2>
      <p>Dane związane z zamówieniami przechowujemy przez okres wymagany przepisami podatkowymi (5 lat). Dane przetwarzane na podstawie zgody – do czasu jej wycofania.</p>

      <h2>5. Twoje prawa</h2>
      <p>Masz prawo dostępu do danych, ich sprostowania, usunięcia, ograniczenia przetwarzania, przenoszenia, wniesienia sprzeciwu oraz cofnięcia zgody w dowolnym momencie. Przysługuje Ci także prawo wniesienia skargi do Prezesa UODO.</p>

      <h2>6. Pliki cookies</h2>
      <p>Używamy plików cookies niezbędnych (koszyk, sesja, zapamiętanie zgód) oraz – za Twoją zgodą – analitycznych i marketingowych. Wybór możesz zmienić, usuwając dane strony w przeglądarce – baner pojawi się ponownie.</p>
      <h3>Pamięć lokalna przeglądarki</h3>
      <p>Zawartość koszyka i lista ulubionych przechowywane są w localStorage Twojej przeglądarki i nie są wysyłane na nasze serwery do momentu złożenia zamówienia.</p>
    </LegalPage>
  );
}
