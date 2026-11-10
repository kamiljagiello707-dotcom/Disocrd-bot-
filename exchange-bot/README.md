# Exchange - tickety

Bot ticketów pod serwer z wymianami. Wiadomości w Components V2, formularze z listami wyboru i wysyłaniem plików.

## Co potrafi

- panel z kategoriami, kursami i kalkulatorem
- wymiana: klient wybiera co wysyła, co chce dostać i kwotę, bot sam liczy prowizję i sprawdza limity
- statusy wymiany (czeka na płatność, płatność potwierdzona, zrealizowana, anulowana)
- klient wrzuca potwierdzenie płatności jako plik prosto z formularza
- przejmowanie ticketów, przypomnienia, dodawanie i usuwanie osób
- transkrypt HTML na logach i w DM po zamknięciu
- ocena 1-5 z komentarzem, zrealizowane wymiany lecą na kanał z opiniami
- blacklista, statystyki obrotu (ogólne i na osobę z obsługi)
- prowizje zmieniane komendą bez restartu

## Komendy

| komenda | opis |
|---|---|
| `/panel` | wysyła panel na kanał |
| `/kursy` | tabela prowizji i limitów |
| `/prowizja ustaw` / `reset` | zmiana prowizji dla pary |
| `/ticket dodaj` / `usun` / `nazwa` / `zamknij` | zarządzanie ticketem |
| `/blacklist dodaj` / `usun` / `lista` | blokada ticketów |
| `/statystyki [osoba]` | obrót, liczba wymian, średnia ocen |

## Odpalenie

1. Bot na https://discord.com/developers/applications, włącz intent **Server Members**.
2. Zaproś z uprawnieniami: Manage Channels, Manage Roles, Send Messages, Attach Files, Read Message History, Manage Messages.
3. `npm install`
4. Skopiuj `.env.example` do `.env` i uzupełnij.
5. W `src/config.js` wpisz ID kategorii, kanału logów, kanału opinii i ról obsługi. Tam też są metody, limity i prowizje.
6. `npm start`, potem `/panel` na wybranym kanale.

Wymaga Node 18.17+ i discord.js 14.28+ (formularze z listami i plikami).
Rola bota musi być wyżej niż role obsługi.
