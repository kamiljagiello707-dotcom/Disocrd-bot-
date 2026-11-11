# Fuzir Client

Klient do Minecrafta 1.21.4 (Fabric) z czarno-białym ClickGUI, HUD i modułami.

## Instalacja

1. Zainstaluj [Fabric Loader](https://fabricmc.net/use/installer/) dla wersji **1.21.4**.
2. Wrzuć do folderu `.minecraft/mods`:
   - `fuzir-client-1.0.0.jar` (z `build/libs` albo z wydania),
   - [Fabric API](https://modrinth.com/mod/fabric-api) dla 1.21.4.
3. Uruchom grę z profilem Fabric.

## Sterowanie

| Klawisz | Akcja |
|---|---|
| Prawy Shift | otwiera / zamyka menu (zmienisz w Opcje → Sterowanie → Fuzir Client) |
| Lewy klik na karcie | włącza / wyłącza moduł |
| Prawy klik albo ⋮ | rozwija ustawienia modułu |
| Pisanie w menu / Ctrl+F | wyszukiwanie modułów, Enter włącza jedyny wynik |
| Strzałki ↑ ↓ | zmiana kategorii |
| Esc | zamyka menu (albo czyści wyszukiwanie) |
| Klawisz w ustawieniach | kliknij i wciśnij klawisz; Esc / Backspace / prawy klik usuwa |

Ustawienia zapisują się w `.minecraft/config/fuzir-client.json`.

## Moduły

| Kategoria | Moduł | Co robi |
|---|---|---|
| Ruch | Sprint | bieganie bez trzymania klawisza |
| Ruch | AutoWalk | chodzenie do przodu, opcjonalnie wypływanie w wodzie |
| Render | Fullbright | pełna jasność (noktowizja tylko po stronie klienta) |
| Render | Zoom | przybliżenie pod klawiszem C, płynne, z kamerą filmową |
| Render | NoWeather | ukrywa deszcz i burzę |
| HUD | Watermark | logo, FPS i godzina (styl pełny / minimalny) |
| HUD | Lista modułów | włączone moduły z animacją w prawym górnym rogu |
| HUD | Koordynaty | XYZ, przelicznik Netheru, kierunek |
| HUD | Informacje | ping, prędkość, adres serwera |
| HUD | Klawisze | podgląd WASD, myszy i spacji |
| HUD | Powiadomienia | dymki po przełączeniu modułu |
| Klient | Ustawienia | rozmycie tła, dźwięki, szybkość animacji, skala menu i HUD |

## Budowanie

Wymaga JDK 21.

```
./gradlew build
```

Gotowy plik: `build/libs/fuzir-client-1.0.0.jar`.

`./gradlew runAutotest` uruchamia grę z modułem testowym (`src/testmod`), który sam tworzy świat, przechodzi przez menu i zapisuje zrzuty ekranu do `run/screenshots`. Moduł testowy nie trafia do jara.

## Dodawanie modułu

1. Klasa w `src/main/java/tech/fuzir/client/module/impl`, dziedzicząca po `Module`.
2. Ustawienia przez `add(new BoolSetting(...))`, `NumberSetting`, `ModeSetting`, `KeySetting`; menu rysuje je samo.
3. Logika w `onTick()`, `onEnable()`, `onDisable()`.
4. Rejestracja w `ModuleManager.init()`.
