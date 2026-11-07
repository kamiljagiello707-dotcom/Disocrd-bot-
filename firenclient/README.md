# FirenClient V3

Klient Minecraft (Fabric **1.21.4**) z nowoczesnym ClickGUI: ciemny motyw z białymi akcentami, gładka czcionka Inter, ikony, zaokrąglone rogi, cienie i poświaty (własny shader SDF), rozmyte tło i animacje.

## Instalacja
1. Zainstaluj [Fabric Loader](https://fabricmc.net/use/) dla Minecraft 1.21.4.
2. Wrzuć `FirenClient-2.0.0.jar` oraz **Fabric API** (dla 1.21.4) do folderu `.minecraft/mods`.
3. Uruchom grę z profilem Fabric.

## Sterowanie
| Akcja | Jak |
|---|---|
| Otwórz / zamknij menu | **Right Shift** (zmienisz w Opcje → Sterowanie → FirenClient) albo **ESC → FirenClient** |
| Wybierz moduł | LPM na module na liście |
| Włącz / wyłącz | przełącznik na liście, duży przełącznik w panelu albo PPM na module |
| Bind | ŚPM na module albo przycisk z klawiaturą w panelu (ESC usuwa bind) |
| Ulubione | gwiazdka przy module (ulubione są na górze listy) |
| Przywróć domyślne ustawienia | przycisk ↻ w panelu modułu |
| Szukaj | zacznij pisać |
| Przesuń okno | przeciągnij logo |

## Configi (zakładka Configs)
- Każdy config to osobny plik w `.minecraft/config/firenclient/configs/`.
- **Zapisz jako nowy**: wpisz nazwę i kliknij (albo Enter).
- **Wczytaj**, **Nadpisz** (ikona dyskietki), **Usuń** (kosz).
- **Kopiuj obecny do schowka** / **Importuj ze schowka**: dzielenie się configami z kolegami.
- Aktywny config zapisuje się sam przy zamknięciu menu i gry.
- Znajomi (moduł MiddleClick: ŚPM na graczu) są pomijani przez KillAurę, AutoTrap i AimAssist.

## Moduły

**Combat**
- **AutoTrap**: zamyka najbliższego gracza w obsydianie. Tryby Full (nogi, głowa, sufit), Top (głowa i sufit) i Face (nogi i głowa). Sam stawia podpory, gdy brakuje oparcia, i ma Anti step. Bloki stawia z cichą zmianą slotu i rotacją pakietem. Do wyboru: liczba bloków na tick, opóźnienie, podgląd planowanych bloków.
- **Surround**: obsydian dookoła własnych nóg.
- **AimAssist**: płynne dociąganie celownika.
- **KillAura**: bije sama, także **przez ściany** (osobny zasięg Wall range). Ma predykcję ruchu celu, Auto weapon, pomija znajomych, AntiBot sprawdza listę TAB i świeże encje. ciche rotacje i move fix, „ludzkie” obracanie, raycast, cooldown lub CPS, Smart HurtTime, Crit sync, AntiBot, filtry. Target ESP w stylach: Ring, Circle, Box, Orbit i Glow, w kolorze motywu albo własnym.
- **TriggerBot**: atakuje, gdy celownik jest na celu, z losowym opóźnieniem i progiem cooldownu.
- **Velocity**: zmniejsza odrzut z ciosów i eksplozji (% poziomo i pionowo).
- **AutoClicker**: losowy CPS dla lewego i prawego przycisku.
- **Criticals**: tryb Packet albo Jump.
- **AutoTotem**: trzyma totem w drugiej ręce.

**Movement**
- **Sprint**: tryb Legit albo Omni.
- **Speed**: tryb Strafe (bhop) albo Ground.
- **Flight**: tryb Motion albo Vanilla, z anti-kickiem.
- **NoFall**, **Step**, **NoSlow**, **Parkour**.

**Render (nowe w V3)**
- **ItemESP**: podświetla przedmioty leżące na ziemi.
- **PlayerBeam**: pionowy słup światła nad każdym graczem (kolor osobny dla znajomych).
- **Trail**: zanikający ślad za Twoją postacią.
- **Keystrokes**: podgląd wciskanych klawiszy WASD + mysz + spacja (pozycja X/Y).
- **CPS**: licznik kliknięć na sekundę.
- **Target ESP** w KillAurze: dwa nowe style — **Pulse** (rozchodzące się pierścienie) i **Corners** (narożniki jak w FPS).

**Render**
- **HUD**: watermark, FPS, XYZ, ArrayList z akcentem, Target HUD.
- **ClickGUI**: motyw Mono, Ocean, Rose, Mint, Sunset, Violet albo Custom z color pickerem; rozmycie tła.
- **ESP**: tryb Box (3D), 2D (z paskiem życia) albo Glow; osobne kolory dla graczy, mobów i zwierząt.
- **Tracers**: kolor według dystansu albo stały; punkt Feet, Body albo Head; grubość linii.
- **Nametags**: nick, HP z kolorem i dystans; zastępuje vanilla nametagi.
- **ChestESP**: skrzynie, Ender chesty, shulkery i beczki.
- **Fullbright**: tryb Gamma albo Night vision.
- **Zoom**, **NoHurtCam**.

**Player**
- **AutoArmor**, **AutoTool**, **FastPlace**, **ChestStealer**.

**World**
- **Scaffold**: ciche rotacje, Tower, Keep Y, Extend.
- **Timer**.
- **OreESP**: diamenty, Ancient debris, emeraldy, złoto, żelazo, redstone, lapis i węgiel.

**Misc**
- **FakePlayer**: manekin do testowania KillAury i AutoTrapa (widoczny tylko u Ciebie).
- **MiddleClick**: ŚPM na graczu dodaje go do znajomych albo usuwa.
- **AutoRespawn**, **AntiAFK**.

W GUI każde ustawienie koloru ma color picker (pole HSV i pasek odcienia). Opcje z wieloma wartościami wybierasz jako „chipy”.

## Budowanie
```
./gradlew build
```
Gotowy plik: `build/libs/FirenClient-0.1.0.jar`.

## Licencje czcionek
- Inter: SIL Open Font License (`licenses/Inter-OFL.txt`)
- Font Awesome Free (ikony): SIL OFL 1.1 (`licenses/FontAwesome-LICENSE.txt`)
