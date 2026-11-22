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

## Moduły

135 modułów w 10 kategoriach: Walka, Ruch, Gracz, Wizualne, Świat, HUD, Kosmetyki, Narzędzia,
Exploity i Klient. Każdy ma własną ikonę i zestaw opcji. Stan modułów zapisuje się w
`config/withclient/modules.json`, a wygląd w `gui.json`.

> Moduły to na razie przełączniki z ustawieniami w GUI, bez działania w grze. Działa tylko interfejs.

## Budowanie

```
./gradlew build
```

Gotowy plik: `build/libs/withclient-1.1.0.jar`. Wrzuć go do `.minecraft/mods` razem z Fabric API.
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
  module/Category/Module/Modules.java – kategorie, moduły, zapis
  module/Setting.java        – Bool, Num, Range, Mode, Multi, Color, Text, Group (+ warunki widoczności)
  render/R2D.java, Fonts.java, SmoothTextLayer.java – silnik rysowania
  mixin/TextRenderLayerSetMixin.java
src/main/resources/assets/withclient/
  font/, shaders/
  textures/icons/            – logo WithClient + ikony Lucide (l_*, licencja ISC)
```
