# 🎫 FutureShop ˈ Bot ticketów

Bot ticketów do Discorda (discord.js v14) w nowym układzie wiadomości Discorda (Components V2): czerwony kontener z ramką nagłówka, separatorami, menu, banerem i stopką.

## ✦ Funkcje
- 📩 Panel z menu wyboru kategorii (Zamówienie, Pomoc, Reklamacja, Partnerstwo, Odbiór nagrody)
- 📝 Formularz (modal) z pytaniami dla każdej kategorii
- 🔐 Prywatny kanał `||🛒ˈzamówienie•nick` widoczny tylko dla klienta i obsługi
- 📁 Menu **Wybierz akcję ticketa**: Przejmij, Przypomnij klientowi, Dodaj/Usuń osobę, Transkrypt, Zamknij, Zamknij z powodem
- 📤 Logi otwarcia/zamknięcia z transkryptem HTML w stylu Discorda (avatary, kolory ról, wzmianki, załączniki)
- 💌 DM do klienta po zamknięciu z transkryptem i oceną ⭐ 1–5 (oceny trafiają na logi)
- 🔢 Numeracja ticketów `#0001`, limit otwartych ticketów na osobę
- ⚙️ Komendy: `/panel`, `/ticket dodaj`, `/ticket usun`, `/ticket nazwa`, `/ticket zamknij`

## ✦ Instalacja
1. Utwórz bota na <https://discord.com/developers/applications>, włącz intent **Server Members**.
2. Zaproś bota z uprawnieniami: Manage Channels, Manage Roles, Send Messages, Embed Links, Attach Files, Read Message History.
3. `npm install`
4. Skopiuj `.env.example` → `.env` i uzupełnij `TOKEN`, `CLIENT_ID`, `GUILD_ID`.
5. W `src/config.js` wpisz ID kategorii, kanału logów i ról obsługi. Możesz tam też zmienić nazwę sklepu, kolor, znak `ˈ`, kategorie i pytania. Baner to plik `assets/banner.png` (podmień na swój) albo link w `bannerUrl`.
6. `npm start`, a potem na wybranym kanale wpisz `/panel`.

> Rola bota musi być **wyżej** niż role obsługi na liście ról.

## ✦ Reasel.pl – sklep 3D
W katalogu [`reasel/`](reasel/README.md) znajduje się sklep internetowy z odzieżą streetwear (Next.js 14, React Three Fiber, GSAP). Instrukcja uruchomienia i wdrożenia na Vercel: `reasel/README.md`.
