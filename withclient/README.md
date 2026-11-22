# WithClient

Klient Fabric dla Minecrafta **1.21.1** (Java 21). Wygląd biało-czarny, własne logo i kompaktowe ClickGUI.

## Logo

Nowy znak to „VV” złożone z dwóch nachodzących na siebie V. Lewe V jest białe, prawe szare,
a dookoła biegnie cienka orbita, która nawiązuje do pierwszego logo.
- `with_logo.png` – sam znak (w GUI),
- `with_mark.png` – znak z orbitą,
- `with_app.png` / `mod_icon.png` – ikona aplikacji (ciemny kafelek), widoczna m.in. na liście modów.

## ClickGUI (w grze, **prawy Shift**)

Okno ma stałą wielkość na ekranie, niezależnie od skali GUI Minecrafta (zmienisz ją w Wygląd → Skala okna).

- **Nawigacja**: logo, 10 kategorii z licznikami włączonych modułów, strony Wygląd / Konfiguracje / Znajomi
  i profil gracza. Aktywną pozycję zaznacza przesuwana pigułka.
- **Nagłówek**: tytuł, liczba modułów, wyszukiwarka (rozszerza się po kliknięciu) i filtr
  Wszystkie / Aktywne / Ulubione z przesuwanym wskaźnikiem.
- **Moduły**: siatka kart w dwóch kolumnach (ikona, nazwa, opis, klawisz, ulubiony, przełącznik).
  Po wybraniu modułu karty płynnie przechodzą w jedną kolumnę, a z prawej **wysuwa się panel ustawień**.
- **Panel ustawień**: duży przycisk WŁĄCZONY/WYŁĄCZONY, opis i **zwijane grupy** opcji. Rodzaje opcji:
  - przełączniki,
  - suwaki,
  - **zakresy** (dwa uchwyty, np. CPS 9–13),
  - tryby: przełącznik segmentowy albo **lista rozwijana**,
  - **wielokrotny wybór** (np. cele),
  - **kolory** z pickerem i szybkimi odcieniami,
  - **pola tekstowe**,
  - **opcje zależne**, które pojawiają się tylko przy danym trybie, np. Smoothness przy Rotacje = Smooth.

  Na dole panelu są klawisz, ulubiony i przywracanie domyślnych.
- **Animacje i efekty**:
  - wysuwanie panelu,
  - przelewanie siatki,
  - wjazd kart po kolei,
  - przejścia stron,
  - obracające się strzałki,
  - powiadomienia przy przełączaniu,
  - podpowiedzi,
  - **cząsteczki** połączone liniami, reagujące na kursor,
  - **świetlny punkt** okrążający ramkę okna.

Sterowanie:
- **LPM** wybiera moduł albo przełącza go przełącznikiem,
- **PPM** otwiera ustawienia,
- **ŚPM** przypisuje klawisz (Delete usuwa),
- pisanie = wyszukiwanie,
- **Esc** zamyka.

### Wygląd
- **Kolor akcentu**: biały (domyślny), srebrny i 8 kolorów.
- **Styl okna**: Pełny / Szkło / Minimal.
- **Wymiary**: skala okna, zaokrąglenie, przyciemnienie tła.
- **Animacje**: szybkość (Wolne / Normalne / Szybkie / Wył.) i krzywa (Płynna / Sprężysta / Liniowa).
- **Efekty**: cząsteczki, animowana ramka, winieta, rozmycie świata, cień.
- **Interfejs**: opisy i klawisze na liście, podpowiedzi, powiadomienia, dźwięki.

### Konfiguracje i znajomi
- Konfiguracje: zapisz nową, wczytaj, nadpisz, usuń (z potwierdzeniem), otwórz folder.
  Pliki są w `config/withclient/configs/`.
- Znajomi: dodawanie po nicku i usuwanie.

## HUD (działa w grze)

Elementy HUD to moduły z kategorii **HUD**. Włączony moduł pokazuje swój element na ekranie,
a jego opcje zmieniają wygląd. Wszystko jest w stylu biało-czarnym.

| Element | Co pokazuje | Opcje |
|---|---|---|
| **Watermark** | logo WithClient + FPS, ping, serwer, czas, nick | styl Bar / Pill / Logo / Text, własny tekst, tło |
| **ArrayList** | włączone moduły z trybem (np. „KillAura Single”), wjeżdżają i wyjeżdżają | sortowanie, animacja Slide / Fade / None, tło, pasek, kolor, ukrywanie wizualnych |
| **Keybinds** | moduły z przypisanym klawiszem, kropka stanu i klawisz | tylko włączone, nagłówek, limit |
| **TargetHUD** | przeciwnik, którego bijesz albo w którego celujesz: głowa / ikona moba, HP z animowanym paskiem, odległość, zbroja, PRZEWAGA / SŁABIEJ | styl Modern / Compact / Minimal, zasięg |
| **Coordinates** | X Y Z, przeliczenie Nether ↔ świat, kierunek, biom | |
| **Potions** | efekty z ikoną, poziomem, czasem i paskiem (chowa ikony z Minecrafta) | sortowanie |
| **ArmorHUD** | zbroja + przedmiot w ręce z paskiem wytrzymałości | poziomo / pionowo, procenty |
| **HealthBar** | pasek zdrowia nad paskiem przedmiotów (absorpcja, głód) | Bar / Minimal |
| **Keystrokes** | W A S D, LPM / PPM z CPS, spacja – podświetlają się po wciśnięciu | rozmiar |
| **Compass** | kompas paskowy albo okrągły ze stopniami | |
| **Notifications** | powiadomienie przy przełączeniu modułu klawiszem | czas, pozycja, dźwięk |
| **Session** | czas gry, przebyty dystans, śmierci | |

Mniejsze elementy: **FPS** (z wykresem i min/max), **Ping** (paski sygnału), **TPS** (liczone z pakietów
czasu serwera), **Speedometer** (bps / km/h), **CPS**, **Clock**, **ServerInfo**, **Memory**,
**TotemCounter**, **InventoryHUD**.

Domyślnie elementy same układają się w stosy przy krawędziach ekranu i omijają się nawzajem.
Unikają też miejsc Minecrafta: czatu, paska przedmiotów i ikon efektów.

### Edytor HUD
Otwierasz go przyciskiem **„Edytuj HUD”** w kategorii HUD albo modułem *HUDEditor* (można mu przypisać klawisz).
- przeciąganie elementów myszą z przyciąganiem do krawędzi, środka ekranu, innych elementów i siatki
  (linie pomocnicze),
- **kółko myszy** = skala elementu, **PPM** = pozycja domyślna,
- panel z przełącznikami wszystkich elementów. Panel można przesunąć za nagłówek, a **H** go zwija,
- „Resetuj układ” i „Gotowe” (albo Esc).

Wspólny wygląd HUD ustawisz w *Klient → HUDEditor*: tło (Dark / Glass / Clear), skala, przezroczystość,
zaokrąglenie, obrys, cień, ikony i kolor akcentu. Pozycje zapisują się w `config/withclient/hud.json`.

## Moduły

141 modułów w 10 kategoriach: Walka, Ruch, Gracz, Wizualne, Świat, HUD, Kosmetyki, Narzędzia,
Exploity i Klient. Każdy ma własną ikonę i zestaw opcji. Stan modułów zapisuje się w
`config/withclient/modules.json`, a wygląd w `gui.json`.

> Moduły HUD działają w grze. Pozostałe moduły to na razie przełączniki z ustawieniami w GUI, bez działania w grze.

## Budowanie

```
./gradlew build
```

Gotowy plik: `build/libs/withclient-1.2.0.jar`. Wrzuć go do `.minecraft/mods` razem z Fabric API.
Klawisz otwierania zmienisz w Opcje → Sterowanie → WithClient.

## Struktura

```
src/main/java/dev/with/client/
  WithClient.java            – start moda, klawisz GUI
  ui/ClickGui.java           – ClickGUI: nawigacja, siatka, wysuwany panel, strony, efekty
  ui/Theme.java              – wygląd (akcent, styl, skala, animacje, efekty) – gui.json
  ui/Anim.java               – animacje (płynna / sprężysta / liniowa)
  ui/Ui.java                 – paleta, logo, przyciski, przełączniki
  ui/Friends.java
  hud/Hud.java               – elementy HUD, układ (stosy, kotwice, omijanie), powiadomienia
  hud/HudEditor.java         – edytor HUD (przeciąganie, przyciąganie, skala, panel)
  module/Category/Module/Modules.java – kategorie, moduły, zapis
  module/Setting.java        – Bool, Num, Range, Mode, Multi, Color, Text, Group (+ warunki widoczności)
  render/R2D.java, Fonts.java, SmoothTextLayer.java – silnik rysowania
  mixin/TextRenderLayerSetMixin.java
  mixin/MouseMixin.java      – CPS (kliknięcia)
  mixin/WorldTimeMixin.java  – TPS (czas świata z serwera)
  mixin/StatusEffectOverlayMixin.java – chowa ikony efektów, gdy włączone Potions
src/main/resources/assets/withclient/
  font/, shaders/
  textures/icons/            – logo WithClient + ikony Lucide (l_*, licencja ISC)
```
