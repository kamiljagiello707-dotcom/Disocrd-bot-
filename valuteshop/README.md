# 📁︲ValuteShop

Bot Discord dla sklepu z walutą na anarchia.gg – Components V2.

## Pliki
```
index.js        ← cały bot + konfiguracja na górze
src/index.js    ← przekierowanie (gdy hosting ma ustawione src/index.js)
package.json
.env.example    ← zmień nazwę na .env i wpisz TOKEN
```

## Start
1. Token: w `.env` wpisz `TOKEN=...` albo wklej go w `token: ''` na górze `index.js`.
2. (opcjonalnie) W `sellerRoleIds` wpisz ID roli sprzedawcy – inaczej zakupy dodają tylko administratorzy.
3. Plik startowy: `index.js`.

## Komendy
- `/panel-klienta` – wysyła Panel Klienta (menu: **Sprawdź ile wydałeś** / **Sprawdź swoją Historię**)
- `/zakup dodaj klient kwota [opis]` – sprzedawca dodaje zakup (np. kwota `25.50`, opis `10kk waluty`)
- `/zakup usun numer` – usuwa zakup dodany przez pomyłkę

Zakupy zapisują się w `zakupy.json` (tworzy się sam obok `index.js`) – nie usuwaj go.
