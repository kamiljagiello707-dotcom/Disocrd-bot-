# 🎒 System Ekwipunku pod RP (Roblox)

Ekwipunek otwierany klawiszem **X**, z rangami. Każdy gracz startuje jako **Obywatel** z pustym EQ. Po dołączeniu do **OSP** dostaje w EQ cały sprzęt OSP. Przedmioty to zwykłe Toole, więc ich skrypty i animacje działają bez zmian.

## Pliki

| Plik | Do czego |
|---|---|
| `build/SystemEkwipunku.rbxm` | Paczka do wstawienia do gry klienta |
| `build/EkranWczytywania.rbxm` | Ekran wczytywania (LocalScript do ReplicatedFirst) |
| `build/TestEkwipunku.rbxl` | Gotowe miejsce testowe z EQ i ekranem wczytywania: otwórz i kliknij Play |
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

## Budowanie ze źródeł

`narzedzia/budowniczy` to program w Rust (rbx-dom), który składa pliki z `build/`:

```sh
cd narzedzia/budowniczy
cargo run --release -- zbuduj <Items.rbxm> ../..
```

Dla Rojo jest `default.project.json`.
