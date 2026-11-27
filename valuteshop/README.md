# 📁︲ValuteShop

Bot Discord dla sklepu z walutą (anarchia.gg / minestar.gg / Rapy.gg) – Components V2.

## Pliki
```
index.js        ← cały bot + konfiguracja na górze
src/index.js    ← przekierowanie (gdy hosting ma ustawione src/index.js)
banner.jpg      ← baner paneli (podmień na swój)
package.json
.env.example    ← zmień nazwę na .env i wpisz TOKEN
```

## Start
1. Token: w `.env` wpisz `TOKEN=...` albo wklej go w `token: ''` na górze `index.js`.
2. Na discord.com/developers → Bot włącz **Message Content Intent** (bez tego transkrypty będą puste).
3. (zalecane) W `sellerRoleIds` wpisz ID roli sprzedawcy – inaczej tickety obsługują tylko administratorzy.
4. Plik startowy: `index.js`.

Kategoria `🎫︲Tickety` i prywatny kanał `📁︲logi-ticketów` tworzą się same (albo podaj swoje ID w configu).

## Komendy
- `/panel-ticketow` – panel ticketów (Zakup / Reklamacja / Pomoc Ogólna)
- `/panel-klienta` – Panel Klienta (ile wydałeś / historia zakupów)
- `/zakup dodaj klient kwota [opis]` – ręczne dodanie zakupu
- `/zakup usun numer` – usunięcie zakupu

## Ticket
Podsumowanie z przyciskami **Zamknij** / **Przejmij** / **Ustawienia** (tylko sprzedawcy).
Ustawienia: dodaj / usuń osobę, wezwij klienta (PV), oznacz jako zrealizowane (przy Zakupie podajesz kwotę –
trafia do historii klienta i zarobku sprzedawcy). Po zamknięciu klient dostaje PV z transkryptem HTML, a kopia idzie na logi.

Dane: `zakupy.json`, `tickety.json` (tworzą się same) – nie usuwaj ich.
