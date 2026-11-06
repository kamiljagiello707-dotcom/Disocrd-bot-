# FirenClient

Klient Minecraft (Fabric **1.21.4**) z nowoczesnym ClickGUI: ciemny motyw z białymi akcentami, gładka czcionka Inter, ikony, zaokrąglone rogi, cienie i poświaty (własny shader SDF), rozmyte tło i animacje.

## Instalacja
1. Zainstaluj [Fabric Loader](https://fabricmc.net/use/) dla Minecraft 1.21.4.
2. Wrzuć `FirenClient-0.1.0.jar` oraz **Fabric API** (dla 1.21.4) do folderu `.minecraft/mods`.
3. Uruchom grę z profilem Fabric.

## Sterowanie
| Akcja | Klawisz |
|---|---|
| Otwórz / zamknij ClickGUI | **Right Shift** |
| Włącz / wyłącz moduł | LPM na module |
| Ustawienia modułu | PPM na module |
| Ustaw bind | ŚPM na module (ESC / Backspace usuwa bind) |
| Rozwiń ustawienia | strzałka obok przełącznika |
| Przesuń okno | przeciągnij logo |
| Przewijanie | kółko myszy |
| Szukaj modułu | zacznij pisać |

Konfiguracja zapisuje się w `.minecraft/config/firenclient.json`.

## Działające moduły
- **KillAura**:
  - **Cel:** tryby Single (trzyma cel), Switch (z opóźnieniem) i Multi; priorytet Distance / Health / Angle / HurtTime; osobny zasięg ataku i celowania (Aim range); limit FOV.
  - **Rotacje:** Silent (kamera stoi, obraca się tylko to, co widzi serwer) albo Client; Move fix (ruch zgodny z rotacją serwera, klawisze dobierane pod kamerę); płynne obracanie ze zmienną prędkością, dryfem punktu celowania i mikro-drganiami; dopasowanie do czułości myszy; punkt celowania Nearest / Center / Head.
  - **Atak:** Raycast (cios tylko gdy promień faktycznie trafia w hitbox, nie przez ściany); timing Cooldown (próg %) albo CPS min/max z losowością; Smart HurtTime; Crit sync (czeka na fazę opadania skoku, żeby ciosy były krytyczne).
  - **Filtry:** gracze, moby, zwierzęta, niewidzialni, drużyna, AntiBot (gracz musi być na liście TAB), tylko z bronią, pauza przy jedzeniu.
  - **Target ESP:** animowany pierścień wokół celu; Target HUD z paskiem życia.
- **Sprint**, **Fullbright**, **ClickGUI** i **HUD** (watermark, FPS, XYZ, animowana ArrayList, Target HUD z paskiem życia celu KillAury).

Pozostałe moduły mają gotowe ustawienia w GUI, ale ich logika nie jest jeszcze zaimplementowana.

## Budowanie
```
./gradlew build
```
Gotowy plik: `build/libs/FirenClient-0.1.0.jar`.

## Licencje czcionek
- Inter: SIL Open Font License (`licenses/Inter-OFL.txt`)
- Font Awesome Free (ikony): SIL OFL 1.1 (`licenses/FontAwesome-LICENSE.txt`)
