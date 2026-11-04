# 🎫 NightShop ✦ Bot ticketów

Bot ticketów do Discorda (discord.js v14) w stylu serwerów sklepowych.

## ✦ Funkcje
- 📩 Panel z menu wyboru kategorii (Zakup, Pomoc, Reklamacja, Partnerstwo, Odbiór nagrody)
- 📝 Formularz (modal) z pytaniami dla każdej kategorii
- 🔐 Prywatny kanał `🛒・zakup-nick` widoczny tylko dla klienta i obsługi
- 🙋 Przycisk **Przejmij**, 🔒 **Zamknij** (z potwierdzeniem i opcjonalnym powodem), 📄 **Transkrypt**
- 📤 Logi otwarcia/zamknięcia z transkryptem `.txt`
- 💌 DM do klienta po zamknięciu z transkryptem i oceną ⭐ 1–5 (oceny trafiają na logi)
- 🔢 Numeracja ticketów `#0001`, limit otwartych ticketów na osobę
- ⚙️ Komendy: `/panel`, `/ticket dodaj`, `/ticket usun`, `/ticket nazwa`, `/ticket zamknij`

## ✦ Instalacja
1. Utwórz bota na <https://discord.com/developers/applications>, włącz intent **Server Members**.
2. Zaproś bota z uprawnieniami: Manage Channels, Manage Roles, Send Messages, Embed Links, Attach Files, Read Message History.
3. `npm install`
4. Skopiuj `.env.example` → `.env` i uzupełnij `TOKEN`, `CLIENT_ID`, `GUILD_ID`.
5. W `src/config.js` wpisz ID kategorii, kanału logów i ról obsługi. Możesz tam też zmienić nazwę sklepu, kolor, baner i kategorie.
6. `npm start`, a potem na wybranym kanale wpisz `/panel`.

> Rola bota musi być **wyżej** niż role obsługi na liście ról.
