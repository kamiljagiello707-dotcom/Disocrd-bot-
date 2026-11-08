# 🌊 WaveClient

Klient-mod do **Minecraft 1.21.4 (Fabric)** z rozbudowanym, animowanym ClickGUI w stylu czarno-białym, systemem bindów, edytorem HUD, powiadomieniami i modułami wizualnymi.

![ClickGUI](screenshots/clickgui-ingame.png)
![HUD w grze](screenshots/ingame-hud.png)
![Edytor HUD](screenshots/hud-editor.png)

## ✦ Instalacja
1. Zainstaluj **Fabric Loader** dla 1.21.4 (<https://fabricmc.net/use/installer/>).
2. Wrzuć do `.minecraft/mods`:
   - `WaveClient-1.0.0.jar` (z `build/libs/` po zbudowaniu),
   - **Fabric API** dla 1.21.4.
3. Uruchom grę z profilem Fabric.

### Budowanie
```bash
cd WaveClient
./gradlew build          # gotowy plik: build/libs/WaveClient-1.0.0.jar
./gradlew runClient      # uruchomienie gry w trybie deweloperskim
```
Wymaga Javy 21.

## ✦ Sterowanie
| Klawisz / akcja | Działanie |
|---|---|
| **Prawy Shift** | Otwiera / zamyka ClickGUI |
| **LPM** na module | Włącza / wyłącza moduł |
| **PPM** na module | Rozwija ustawienia (animowane) |
| **ŚPM** na module | Szybkie ustawienie binda |
| **PPM** na nagłówku panelu | Zwija / rozwija kategorię |
| Przeciąganie nagłówka | Przesuwa panel |
| Pisanie / `Ctrl+F` | Wyszukiwarka modułów (podświetla wyniki) |
| Kółko myszy | Przewijanie paneli / zmiana wartości suwaka |
| **PPM** na suwaku | Reset do wartości domyślnej |
| **C** (przytrzymaj) | Zoom (kółkiem zmieniasz przybliżenie) |

### System bindów
- W ustawieniach każdego modułu jest pole **Keybind**: kliknij → naciśnij klawisz (lub boczny przycisk myszy MB3–MB5).
- `Esc` / `Backspace` / `Delete` / PPM — usuwa bind.
- **Bind Mode**: `Toggle` (przełącz) lub `Hold` (aktywny tylko podczas trzymania).
- Bind jest wyświetlany obok nazwy modułu w GUI.

## ✦ GUI
- Czarno-biały motyw z kolorem akcentu do zmiany (moduł **ClickGUI → Accent**, także tryb Rainbow).
- Animacje: otwierania (Scale / Slide / Fade), wypełnienia włączonego modułu, rozwijania ustawień, przełączników, suwaków, listy trybów, hover, zwijania paneli.
- Rozmycie tła, przyciemnienie, podpowiedzi (opisy modułów i ustawień), dźwięki kliknięć.
- Pozycje paneli i wszystkie ustawienia zapisują się w `.minecraft/config/waveclient.json`.

## ✦ Powiadomienia
Przy włączeniu / wyłączeniu modułu wysuwa się czarna karta z ikoną, nazwą, animowanym mini-przełącznikiem i paskiem czasu. Pozycja (BottomRight / TopRight / TopCenter), czas i dźwięk w module **Notifications**.

## ✦ HUD (edytor: przycisk „HUD Editor” na dole GUI)
Przeciągaj elementy myszką, przyciąganie do krawędzi, środka i innych elementów z liniami pomocniczymi, siatka. Kliknięcie „chipa” włącza/wyłącza element, PPM na elemencie go ukrywa.

| Element | Opis |
|---|---|
| Watermark | Logo klienta (style Modern / Minimal / Logo), FPS, godzina, nick |
| ArrayList | Lista włączonych modułów z animacją wjazdu, kolory Wave / White / Accent / Rainbow |
| Info | FPS, ping, prędkość (b/s), serwer, biom |
| Coordinates | XYZ, kierunek, przeliczenie Nether/Overworld |
| Armor | Zbroja + przedmioty w rękach z % wytrzymałości |
| Potions | Aktywne efekty z czasem |
| Keystrokes | WASD, LPM/PPM z CPS, spacja (animowane) |
| Inventory | Podgląd ekwipunku |

## ✦ Moduły
**Visuals:** Fullbright, ESP (gracze/moby/zwierzęta/itemy, kolory Static/Rainbow/Health/Team), Tracers, StorageESP (skrzynie, ender, shulkery, beczki), Zoom, Crosshair (własny celownik), NoHurtCam, NoOverlay (ogień, woda), Ambience (czas i pogoda po stronie klienta).

**Movement:** Sprint, AutoWalk, Parkour, Eagle.

**Player:** AutoRespawn, AntiAFK.

**Client:** ClickGUI, Notifications, HUD Editor.

> ⚠️ Używaj na własnych światach lub serwerach, które na to pozwalają — na wielu serwerach takie modyfikacje są zabronione regulaminem.
