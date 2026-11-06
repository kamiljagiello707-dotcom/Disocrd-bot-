# FirenClient

Klient Minecraft (Fabric **1.21.4**) z nowoczesnym ClickGUI: ciemny motyw z białymi akcentami, gładka czcionka Inter, ikony, zaokrąglone rogi, cienie i poświaty (własny shader SDF), rozmyte tło i animacje.

## Instalacja
1. Zainstaluj [Fabric Loader](https://fabricmc.net/use/) dla Minecraft 1.21.4.
2. Wrzuć `FirenClient-0.1.0.jar` oraz **Fabric API** (dla 1.21.4) do folderu `.minecraft/mods`.
3. Uruchom grę z profilem Fabric.

## Sterowanie
| Akcja | Klawisz |
|---|---|
| Otwórz / zamknij ClickGUI | **Right Shift** (zmienisz w Opcje → Sterowanie → FirenClient) albo przycisk **FirenClient** w menu ESC |
| Włącz / wyłącz moduł | LPM na module |
| Ustawienia modułu | PPM na module |
| Ustaw bind | ŚPM na module (ESC / Backspace usuwa bind) |
| Rozwiń ustawienia | strzałka obok przełącznika |
| Przesuń okno | przeciągnij logo |
| Przewijanie | kółko myszy |
| Szukaj modułu | zacznij pisać |

Konfiguracja zapisuje się w `.minecraft/config/firenclient.json`.

## Moduły

**Combat**
- **KillAura**: ciche rotacje i move fix, „ludzkie” obracanie, raycast, cooldown lub CPS, Smart HurtTime, Crit sync, AntiBot, filtry. Target ESP w stylach: Ring, Circle, Box, Orbit i Glow, w kolorze motywu albo własnym.
- **TriggerBot**: atakuje, gdy celownik jest na celu, z losowym opóźnieniem i progiem cooldownu.
- **Velocity**: zmniejsza odrzut z ciosów i eksplozji (% poziomo i pionowo).
- **AutoClicker**: losowy CPS dla lewego i prawego przycisku.
- **Criticals**: tryb Packet albo Jump.
- **AutoTotem**: trzyma totem w drugiej ręce.

**Movement**
- **Sprint**: tryb Legit albo Omni.
- **Speed**: tryb Strafe (bhop) albo Ground.
- **Flight**: tryb Motion albo Vanilla, z anti-kickiem.
- **NoFall**, **Step**.

**Render**
- **HUD**: watermark, FPS, XYZ, ArrayList z akcentem, Target HUD.
- **ClickGUI**: motyw Mono, Ocean, Rose, Mint, Sunset, Violet albo Custom z color pickerem; rozmycie tła.
- **ESP**: tryb Box (3D), 2D (z paskiem życia) albo Glow; osobne kolory dla graczy, mobów i zwierząt.
- **Tracers**: kolor według dystansu albo stały; punkt Feet, Body albo Head; grubość linii.
- **Nametags**: nick, HP z kolorem i dystans; zastępuje vanilla nametagi.
- **ChestESP**: skrzynie, Ender chesty, shulkery i beczki.
- **Fullbright**: tryb Gamma albo Night vision.

**Player**
- **AutoTool**, **FastPlace**, **ChestStealer**.

**World**
- **Scaffold**: ciche rotacje, Tower, Keep Y, Extend.
- **Timer**.
- **OreESP**: diamenty, Ancient debris, emeraldy, złoto, żelazo, redstone, lapis i węgiel.

W GUI każde ustawienie koloru ma color picker (pole HSV i pasek odcienia). Opcje z wieloma wartościami wybierasz jako „chipy”.

## Budowanie
```
./gradlew build
```
Gotowy plik: `build/libs/FirenClient-0.1.0.jar`.

## Licencje czcionek
- Inter: SIL Open Font License (`licenses/Inter-OFL.txt`)
- Font Awesome Free (ikony): SIL OFL 1.1 (`licenses/FontAwesome-LICENSE.txt`)
