# WithClient

Nowy klient (Fabric, Minecraft **1.21.1**, Java 21) z własnym logo i dużym, dopracowanym interfejsem.
Oparty na dojrzałym silniku renderowania (SDF, gładkie czcionki Inter) z poprzednich projektów.

## ClickGUI (w grze, **prawy Shift**)

Duże, trójkolumnowe okno:

- **Nawigacja** (lewa): logo WithClient + wersja, lista 7 kategorii (Walka, Ruch, Gracz, Wizualne,
  Świat, Exploity, Klient) z licznikami włączonych modułów, strony klienta (Wygląd, Konfiguracje,
  Znajomi) i profil gracza na dole. Aktywną pozycję zaznacza przesuwana pigułka w kolorze akcentu.
- **Lista modułów** (środek): wyszukiwarka, wiersze z ikoną, nazwą, klawiszem i przełącznikiem.
  Kliknięcie otwiera ustawienia po prawej.
- **Panel ustawień** (prawa): wybrany moduł z dużym przełącznikiem WŁ./WYŁ., opisem, klawiszem,
  ulubionym i **wszystkimi opcjami**: przełączniki, suwaki, tryby (chipsy) i **kolory z pełnym
  pickerem** (kwadrat S/V, pasek barwy, pasek przezroczystości, podgląd i HEX), „Przywróć domyślne".

Sterowanie: **LPM** włącza/wyłącza, **klik** wybiera moduł i pokazuje opcje, **ŚPM** przypisuje
klawisz (Delete usuwa), pisanie = wyszukiwanie, **Esc** zamyka.

### Strony klienta
- **Wygląd**: kolor akcentu (biały + 8 kolorów), animacje, rozmiar okna, przyciemnienie tła,
  rozmycie, cień, dźwięki.
- **Konfiguracje**: zapisz / wczytaj / nadpisz / usuń (z potwierdzeniem) – `config/withclient/configs/`.
- **Znajomi**: dodawanie po nicku i usuwanie.

## Moduły

~58 modułów w 7 kategoriach, każdy z wieloma opcjami (tryby, suwaki, przełączniki, kolory).
Zapis w `config/withclient/modules.json`, motyw w `gui.json`.

> Moduły to na razie przełączniki z ustawieniami w GUI – bez działania w grze. Działają tylko
> rzeczy wizualne/kosmetyczne i sam interfejs.

## Budowanie

```
./gradlew build
```

Gotowy plik: `build/libs/withclient-1.0.0.jar` → do `.minecraft/mods` razem z Fabric API.
Klawisz otwierania zmienisz w Opcje → Sterowanie → WithClient.

## Struktura

```
src/main/java/dev/with/client/
  WithClient.java            – start moda, klawisz GUI
  ui/ClickGui.java           – duże trójkolumnowe GUI + color picker
  ui/Theme.java              – motyw (akcent, skala, efekty) – gui.json
  ui/Ui.java                 – paleta, logo, przyciski, przełączniki
  ui/Friends.java, Anim.java
  module/Category/Setting/Module/Modules.java – system modułów (Bool/Num/Mode/Color)
  render/R2D.java, Fonts.java, SmoothTextLayer.java – silnik
  mixin/TextRenderLayerSetMixin.java
src/main/resources/assets/withclient/
  font/, shaders/, textures/ (logo WithClient + ikony Phosphor, MIT)
```
