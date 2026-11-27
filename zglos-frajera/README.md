# 🔔︲ZgłośFrajera

Bot Discord z panelem do zgłaszania oszustów / scammerów (Components V2).

## Pliki
```
index.js        ← cały bot + konfiguracja na górze
src/index.js    ← tylko przekierowanie (gdy hosting ma ustawione src/index.js)
banner.png      ← baner panelu (podmień na swój)
package.json
.env.example    ← zmień nazwę na .env i wpisz TOKEN
```

## Start
1. W `.env` wpisz `TOKEN=...` (albo wklej token w `token: ''` na górze `index.js`).
2. Kanał zgłoszeń: zostaw `reportChannelId` puste – bot sam utworzy prywatny `🚨︲zgłoszenia` (albo wpisz ID swojego kanału).
3. Plik startowy: `index.js` (działa też `src/index.js`).
4. Na Discordzie:
   - `/panel-zglos` – panel „Zgłoś Frajera” (przyciski Zgłoś Oszusta / Scammera)
   - `/panel-oszustow` – „Panel × Oszustów” z przyciskiem **Sprawdź Statystyki** (historia zgłoszeń danej osoby)

Zgłoszenia zapisują się w pliku `zgloszenia.json` (tworzy się sam obok `index.js`) – nie usuwaj go, bo stracisz historię.

CLIENT_ID i GUILD_ID nie są potrzebne – bot sam dodaje komendę na każdym serwerze, na którym jest.
Emotki `emoji_4/9/11` są z Twojego serwera – bot musi na nim być, żeby je wyświetlał.
