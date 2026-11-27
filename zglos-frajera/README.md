# 🔔︲ZgłośFrajera

Bot Discord z panelem do zgłaszania oszustów / scammerów, zrobiony w **Components V2**.

## Panel
Nagłówek → separator → opis → separator → przyciski **Zgłoś Oszusta** / **Zgłoś Scammera** (szare) → separator → baner → separator → stopka.

Kliknięcie przycisku otwiera formularz (nick frajera, co się stało, strata, dowody), a zgłoszenie trafia na kanał `reportChannelId`.

## Instalacja
1. `cd zglos-frajera && npm install`
2. Skopiuj `.env.example` → `.env` i uzupełnij `TOKEN`, `CLIENT_ID`, `GUILD_ID`.
3. W `src/config.js` wpisz `reportChannelId`. Baner podmień w `assets/banner.png` albo podaj link w `bannerUrl`.
4. `npm start`, a potem na kanale wpisz `/panel-zglos`.

> Emotki `emoji_4/9/11` są z Twojego serwera – bot musi na nim być, żeby je wyświetlał.
