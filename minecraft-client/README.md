# Test Client (Fabric 1.21.1)

Prywatny klient testowy z ClickGUI, na własny serwer.

## Build
```
./gradlew build
```
Jar: `build/libs/test-client-1.0.0.jar` → wrzuć do `.minecraft/mods` razem z Fabric API (1.21.1).
Szybki test w dev: `./gradlew runClient`.

## Sterowanie
- **Right Shift**: otwiera ClickGUI (zmienisz w Opcje → Sterowanie)
- LPM na module: włącz/wyłącz
- PPM na module: rozwiń ustawienia + bind
- Bind: LPM, potem naciśnij klawisz (ESC/Delete usuwa bind), PPM też czyści
- Nagłówek panelu: przeciągnij LPM, zwiń PPM

Config zapisuje się do `config/testclient.json`.

## Moduły
| Kategoria | Moduł | Opis |
|---|---|---|
| Movement | Sprint | auto-sprint |
| Movement | Flight | latanie (Speed, tryb Vanilla) |
| Render | Fullbright | klientowy Night Vision |
| Render | HUD | watermark, ArrayList, koordynaty |
| Player | NoFall | brak obrażeń od upadku |

## Własny serwer
Żeby serwer Cię nie kickował, ustaw `allow-flight=true` w `server.properties`
i wyłącz anticheat albo daj sobie jego uprawnienie bypass.

## Nowy moduł
Utwórz klasę dziedziczącą po `Module`, dodaj ustawienia przez `add(...)`
i zarejestruj ją w `ModuleManager`. Pojawi się w GUI sama.
