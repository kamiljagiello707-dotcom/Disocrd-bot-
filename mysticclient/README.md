# ✦ MysticClient

Własny klient utility do **Minecraft 1.21.1** (mod Fabric).

## Moduły
| Kategoria | Moduł | Klawisz | Opis |
|---|---|---|---|
| Movement | Fly | `G` | Latanie jak w kreatywnym |
| Movement | Speed | – | Szybsze chodzenie |
| Movement | AutoSprint | – | Automatyczny sprint |
| Movement | Step | – | Wchodzenie na pełne bloki bez skoku |
| Player | NoFall | – | Brak obrażeń od upadku |
| Player | FastPlace | – | Brak opóźnienia przy stawianiu bloków |
| Combat | AutoTotem | – | Totem automatycznie w drugiej ręce |
| Render | Fullbright | `B` | Pełna jasność |
| Render | ESP | `Y` | Obrys graczy i potworów przez ściany |
| Render | Zoom | `C` | Przybliżenie ×4 |

- **Prawy Shift** – menu (ClickGUI): LPM włącza/wyłącza moduł, PPM ustawia klawisz (Delete = brak), nagłówki paneli można przeciągać, PPM na nagłówku zwija panel.
- HUD: logo w lewym górnym rogu i lista włączonych modułów po prawej.
- Ustawienia zapisują się w `.minecraft/config/mysticclient.json`.

## Instalacja
1. Zainstaluj [Fabric Loader](https://fabricmc.net/use/installer/) dla 1.21.1.
2. Wrzuć do `.minecraft/mods/`: **Fabric API** (dla 1.21.1) oraz `mysticclient-1.0.0.jar`.
3. Uruchom profil Fabric w launcherze.

## Budowanie
```bash
cd mysticclient
./gradlew build        # jar: build/libs/mysticclient-1.0.0.jar
./gradlew runClient    # odpala grę z modem do testów
```
Wymaga Javy 21.

## Dodawanie własnego modułu
Utwórz klasę w `src/main/java/pl/mystic/client/module/modules/` dziedziczącą po `Module`, nadpisz `onTick()` / `onEnable()` / `onDisable()` i dopisz `add(new TwojModul());` w `ModuleManager`.

> ⚠️ Moduły typu Fly/Speed/NoFall działają w singleplayerze i na Twoich serwerach. Na publicznych serwerach z anticheatem dostaniesz kicka/bana – klient nie ma obejść anticheata.
