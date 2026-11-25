# 🎒 System Ekwipunku pod RP (Roblox)

Ekwipunek otwierany klawiszem **X**, z rangami. Każdy gracz startuje jako **Obywatel** z pustym EQ. Po dołączeniu do **OSP** dostaje w EQ cały sprzęt OSP. Przedmioty to zwykłe Toole, więc ich skrypty i animacje działają bez zmian.

## Pliki

| Plik | Do czego |
|---|---|
| `build/SystemEkwipunku.rbxm` | Paczka do wstawienia do gry klienta |
| `build/EkranWczytywania.rbxm` | Ekran wczytywania (LocalScript do ReplicatedFirst) |
| `build/SystemAlarmowyOSP.rbxm` | System alarmowania OSP z przykładową remizą |
| `build/SystemMentora.rbxm` | Mentor dla nowych graczy |
| `build/TabletKierownikaPSP.rbxm` | Tablet kierownika PSP z przykładową remizą |
| `build/SystemWezwan112.rbxm` | System wezwań 112 (10 rodzajów zdarzeń) |
| `build/TestEkwipunku.rbxl` | Gotowe miejsce testowe ze wszystkim: otwórz i kliknij Play |
| `przedmioty/OSP.rbxm` | Przedmioty OSP klienta (oczyszczone) |
| `src/` | Kod źródłowy (Luau) |

> `build/` i `przedmioty/` nie są w repozytorium (jest publiczne, a to przedmioty klienta). Powstają po zbudowaniu, patrz niżej.

## Instalacja w grze

1. W Studio: **File → Insert from File** (albo przeciągnij) `SystemEkwipunku.rbxm`.
2. Rozłóż zawartość folderów:
   - `Do ReplicatedStorage` → `Ekwipunek` do **ReplicatedStorage**
   - `Do ServerScriptService` → `EkwipunekSerwer` do **ServerScriptService**
   - `Do StarterPlayerScripts` → `EkwipunekKlient` do **StarterPlayer → StarterPlayerScripts**
   - `Do ServerStorage` → `EkwipunekPrzedmioty` do **ServerStorage**
   - `Do Workspace (przyklad)` → `StanowiskoOSP` do **Workspace** (opcjonalnie, do testów)
3. Usuń pusty folder `SystemEkwipunku`.
4. **Game Settings → Avatar → R6.** Przedmioty OSP są zrobione pod R6 (używają `Torso` i `Right Arm`), na R15 ręce się nie ułożą.
5. Zapis rangi: **Game Settings → Security → Enable Studio Access to API Services** (bez tego ranga działa, ale nie zapisuje się w Studio).

## Jak gracz dostaje rangę OSP

Dowolny z tych sposobów (wszystko na serwerze):

```lua
-- 1. Z innego skryptu
local Rangi = require(game.ServerScriptService.EkwipunekSerwer.Rangi)
Rangi.UstawRange(gracz, "OSP")

-- 2. Atrybutem
gracz:SetAttribute("Ranga", "OSP")
```

3. Przeniesienie gracza do drużyny (Team) o nazwie `OSP`. Wyjście z niej oznacza powrót do rangi Obywatel.
4. Komenda admina na czacie: `/ranga <nick> OSP`, cofnięcie: `/ranga <nick> Obywatel`.
   Adminem jest właściciel gry, osoby z listy `Admini` w Konfiguracji, a w Studio każdy.
5. Przykładowe stanowisko `StanowiskoOSP`: podejdź i przytrzymaj **E**.

## Ustawienia

Wszystko jest w `ReplicatedStorage → Ekwipunek → Konfiguracja`:
- klawisz, maksymalna waga, ranga startowa, admini, zapis rangi, połączenie z drużynami
- **nowa ranga** (np. PSP, Policja): dopisz ją w `Rangi` i dodaj folder `ServerStorage → EkwipunekPrzedmioty → <nazwa>` z Toolami
- **opisy przedmiotów**: nazwa, kategoria, rzadkość (kolor paska), waga, opis, ikona (bez ikony EQ pokazuje obracający się model 3D)

## Co jest w EQ

- wyszukiwarka, zakładki kategorii, siatka z ilością (`x2`) i paskiem rzadkości
- podgląd 3D każdego przedmiotu, w panelu szczegółów obracający się model
- **Weź do ręki / Schowaj** (albo podwójne kliknięcie), **Wyrzuć**; przedmiotów służbowych nie da się wyrzucić
- pasek wagi z limitem; przedmiot podniesiony ponad limit wypada na ziemię
- znaczek rangi, powiadomienia, rozmycie tła, animacja otwierania
- na telefonie przycisk 🎒 po lewej stronie ekranu

## Zmiany w przedmiotach klienta

- **Radio**: usunięty zaciemniony (obfuscated) skrypt „Script - obfuscated - dont working”. Był wyłączony, ale w takim kodzie zwykle ukrywa się backdoor.
- **Szczotka**: `EventScript` pozwalał każdemu (także exploiterom) wyłączyć dowolną część mapy. Teraz działa tylko na częściach `Door` blisko szczotki trzymanej w ręce.

Przedmioty, które potrzebują systemów spoza paczki:
- **Radio**: `ReplicatedStorage.Pegasus.RadioGui` (system radia Pegasus)
- **Dron**: system „Reaper Drone System”
- **MRE**: wartości `HungerVal` i `ThirstVal` w graczu (system głodu)

## Ekran wczytywania

Wstaw `EkranWczytywania.rbxm` i przeciągnij `EkranWczytywania` do **ReplicatedFirst**. Działa też bez ekwipunku.

- nazwa gry, podtytuł, wersja, logo, obraz tła, muzyka, kolor akcentu i porady są w module `Ustawienia` w środku skryptu
- wczytuje obrazki, dźwięki i modele z mapy (pasek z liczbą zasobów), czeka na postać
- trwa co najmniej `MinimalnyCzas` (4 s), najwyżej `MaksymalnyCzas` (30 s); po 8 s pojawia się **Pomiń**
- na końcu przycisk **GRAJ** (albo Enter / spacja); `PrzyciskGraj = false`, żeby ekran znikał sam
- na czas wczytywania chowa listę graczy, czat, zdrowie i emotki (lista w `UkryjInterfejs`)
- błąd w trakcie wczytywania nigdy nie zablokuje gracza: ekran i tak zniknie
- EQ nie otwiera się pod ekranem wczytywania, a jego powiadomienia czekają, aż ekran zniknie

## System alarmowania OSP

Wstaw `SystemAlarmowyOSP.rbxm` i rozłóż foldery jak przy EQ (`AlarmOSP` do ReplicatedStorage, `AlarmSerwer` do ServerScriptService, `AlarmKlient` do StarterPlayerScripts). Przykładowa remiza (`RemizaAlarm`) i punkty (`MiejscaZdarzen`) są w folderze `Do Workspace (przyklad)`.

- **Panel dyspozytora**: przy komputerze w remizie (przytrzymaj E) albo komendą `/alarm`. Jednostki, rodzaj zdarzenia, miejsce, opis i alarmy w toku na jednym ekranie. Alarm wysyła się **przytrzymaniem** przycisku przez 1 s.
- **Pager strażaka**: karta alarmu z dźwiękiem i błyskiem ekranu, przyciski **JADĘ / NIE JADĘ**, odległość do miejsca na żywo.
- **Znacznik**: po kliknięciu JADĘ nad miejscem zdarzenia pojawia się słup światła z odległością (widzi go tylko ten gracz), a znika po dojechaniu.
- **Dyspozytor widzi**, ile osób jedzie, i może zakończyć alarm. Alarm kończy się też sam po `CzasAlarmu`.
- **Remiza**: syrena (`AlarmSyrena`) wyje, a lampy (`AlarmSwiatlo`) migają przez `CzasSyreny`.
- Opis wpisany przez gracza przechodzi przez filtr tekstu Roblox (wymóg Roblox dla tekstu widocznego dla innych).

Obiekty na mapie oznacza się Tagami (Properties → Tags): `AlarmKomputer`, `AlarmSyrena`, `AlarmSwiatlo`, `MiejsceZdarzenia` (nazwa części = adres). Atrybut `Jednostka` na syrenie lub lampie ogranicza ją do jednej jednostki.

Ustawienia (`ReplicatedStorage → AlarmOSP → Ustawienia`): jednostki (po randze z EQ albo drużynie), kto może alarmować, rodzaje zdarzeń, **ID dźwięku syreny** (domyślnie pusty), dźwięk pagera, czasy i limity.

Alarm z innego skryptu:

```lua
local Alarmy = require(game.ServerScriptService.AlarmSerwer.Alarmy)
Alarmy.Wyslij({ Jednostki = { "OSP" }, Rodzaj = "Pożar", Miejsce = workspace.Dom, Opis = "Pali się dom" })
```

## Mentor (pomoc dla nowych graczy)

Wstaw `SystemMentora.rbxm` i rozłóż foldery (`Mentor` do ReplicatedStorage, `MentorSerwer` do ServerScriptService, `MentorKlient` do StarterPlayerScripts). Z folderu `Do Workspace (przyklad)` weź `MentorPunkt` (tu stanie mentor) i `Remiza OSP` (cel przycisku drogi).

- Mentor (awatar R6) staje na części z Tagiem `Mentor`, przodem tam, gdzie przód części. Ma plakietkę z imieniem i animację stania.
- Wygląd: `WygladZGracza = UserId` (domyślnie 0 = właściciel gry).
- **E** otwiera rozmowę: kamera najeżdża na mentora, mentor macha, a obok jest menu z dymkiem (tekst pisze się na żywo), listą rozdziałów (✓ przy przeczytanych), krokami i przyciskami Wstecz / Dalej (albo strzałki na klawiaturze).
- **Pokaż drogę**: słup światła prowadzi do części z Tagiem `MentorCel` (nazwa części = `Cel` w rozdziale). Opcjonalnie `PozwolNaTeleport = true` dodaje „Zabierz mnie tam”.
- **Nowi gracze** (DataStore) widzą „!” nad mentorem, podpowiedź i słup światła prowadzący do niego.

Wszystkie teksty są w `ReplicatedStorage → Mentor → Ustawienia`. Rozdział to `Ikona`, `Tytul`, `Tekst` (dymek), `Kroki` (lista) i opcjonalny `Przycisk`. W tekstach działają `{nick}` i `{serwer}`.

## Tablet kierownika PSP

Wstaw `TabletKierownikaPSP.rbxm` i rozłóż foldery (`TabletPSP` do ReplicatedStorage, `TabletPSPSerwer` do ServerScriptService, `TabletPSPKlient` do StarterPlayerScripts). Folder `Kierownik PSP` (z narzędziem „Tablet PSP”) wrzuć do `ServerStorage → EkwipunekPrzedmioty`, a `PojazdyPSP` do ServerStorage. Bez systemu ekwipunku tablet wrzuca się do StarterPack.

Tablet otwiera się po wzięciu go do ręki: wysuwa się z dołu ekranu, ma pasek statusu (godzina, zasięg, bateria), a na Stronie Głównej logo, czerwony napis **ZARZĄDZAJ SWOJĄ PSP** i 6 aplikacji z informacją na żywo:

| Aplikacja | Co robi | Na mapie (Tag) |
|---|---|---|
| Bramy | Otwiera i zamyka bramy (płynnie do góry), pojedynczo albo wszystkie naraz | `BramaPSP` |
| Syrena | Duży przycisk syreny i lamp ostrzegawczych, odliczanie | `AlarmSyrena`, `AlarmSwiatlo` |
| Załoga | Lista strażaków w służbie, przyjmowanie graczy z pobliża, zwalnianie | ranga z EQ albo drużyna |
| Pojazdy | Wóz z `ServerStorage → PojazdyPSP` wyjeżdża na wolne miejsce przed garażem | `MiejscePojazduPSP` |
| Komunikaty | Wiadomość do całej załogi (przez filtr tekstu Roblox) | — |
| Oświetlenie | Światła w strefach remizy (atrybut `Strefa`) | `SwiatloPSP` |

Ustawienia (`ReplicatedStorage → TabletPSP → Ustawienia`): nazwa jednostki, napis, **logo** (`rbxassetid://...`; bez niego jest wbudowany znak „PSP”), kolor, kto jest kierownikiem, rangi załogi, kolejność i nazwy aplikacji, dźwięki i czasy. Do EQ doszły rangi `PSP` i `Kierownik PSP`.

## System wezwań 112

Wstaw `SystemWezwan112.rbxm` i rozłóż foldery (`Wezwania112` do ReplicatedStorage, `Wezwania112Serwer` do ServerScriptService, `Wezwania112Klient` do StarterPlayerScripts). `MiejscaWezwan` to przykładowe punkty: ustaw je na swojej mapie.

**Miejsca:** część z Tagiem `MiejsceWezwania`. Nazwa części to adres, przód części to kierunek drogi, a atrybut `Rodzaje` mówi, co może tu wypaść (np. `Drzewo, Olej, Wypadek`).

| Id | Wezwanie | Zadania |
|---|---|---|
| `Drzewo` | Powalone drzewo | przetnij 3 części pnia piłą (animacja cięcia), usuń gałęzie |
| `PozarLasu` | Pożar lasu | ugaś ogień (rozprzestrzenia się co 25 s, do 14 ognisk), dogaś pogorzelisko |
| `PozarBudynku` | Pożar budynku | ugaś ogień, wynieś poszkodowanego, pierwsza pomoc |
| `Wypadek` | Wypadek drogowy | odłącz akumulator, wytnij drzwi Szczękami Życia, pomoc kierowcy, sorbent na wyciek |
| `PozarAuta` | Pożar samochodu | ugaś auto (zwęgla się), sprawdź kamerą termowizyjną |
| `Olej` | Plama oleju | posyp sorbentem, zamieć szczotką |
| `Poszkodowany` | Osoba poszkodowana | pierwsza pomoc, szyny Kramera |
| `Kot` | Kot na drzewie | zdejmij kota, oddaj właścicielce |
| `Podtopienie` | Podtopienie | odłącz prąd, uruchom pompę, woda opada sama |
| `Szerszenie` | Gniazdo szerszeni | odgrodź taśmą, zdejmij gniazdo bosakiem |

- **Gaszenie:** prądownice klienta naprawdę gaszą ogień (ich woda zmniejsza i niszczy `Fire`). Można też przytrzymać E z gaśniczym narzędziem w ręce.
- **Ikona 112** nad miejscem zdarzenia jest widoczna z daleka (tylko dla strażaków) i pokazuje odległość. W lewym dolnym rogu jest lista wezwań: kliknięcie włącza słup światła (nawigację), a przy najbliższym wezwaniu widać listę zadań z postępem i potrzebnym narzędziem.
- **Koniec:** po wszystkich zadaniach strażacy dostają powiadomienie „Pomyślnie zakończono wezwanie”, a uczestnicy +1 do licznika `Wezwania` w tabeli graczy.
- **Wezwania** pojawiają się same (co 4–8 min, gdy jest strażak w grze) albo z komendy `/wezwanie [rodzaj | lista | stop]`. Jeśli jest system alarmowania OSP, przychodzi też pager.

Ustawienia (`ReplicatedStorage → Wezwania112 → Ustawienia`): kto jest strażakiem, nazwy narzędzi dla każdej grupy (albo `WymagajNarzedzi = false`), automatyczne wezwania i czasy, nazwy, opisy i włączanie rodzajów, dźwięki i animacje.

## Budowanie ze źródeł

`narzedzia/budowniczy` to program w Rust (rbx-dom), który składa pliki z `build/`:

```sh
cd narzedzia/budowniczy
cargo run --release -- zbuduj <Items.rbxm> ../..
```

Dla Rojo jest `default.project.json`.
