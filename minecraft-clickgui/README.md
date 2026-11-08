# Nova ClickGUI (Fabric 1.21.4)

Animowane ClickGUI do własnego klienta: przeciągane panele kategorii, ustawienia modułów,
wyszukiwarka, motyw z gradientem/rainbow, HUD z ArrayListą i zapis configu.

## Sterowanie

| Akcja | Jak |
|---|---|
| Otwórz / zamknij GUI | `Right Shift` (zmienisz w module ClickGUI → Bind) |
| Włącz / wyłącz moduł | LPM na module |
| Rozwiń ustawienia | PPM na module |
| Przesuń panel | przeciągnij nagłówek LPM |
| Zwiń panel | PPM na nagłówku |
| Przewijanie | kółko myszy nad panelem |
| Szukaj | po prostu pisz, `Esc` czyści, `Ctrl+Backspace` kasuje |
| Bind | kliknij `Bind`, wciśnij klawisz (`Esc`/`Del` = brak) |
| Tryby | LPM = następny, PPM = poprzedni |

Motyw (kolor akcentu, rainbow, gradient, przyciemnienie tła, opisy) ustawiasz w module **ClickGUI** (kategoria Misc).
Config zapisuje się do `config/nova.json`.

## Budowanie

```
./gradlew build        # jar w build/libs/
./gradlew runClient    # test w dev
```

## Jak dodać własny moduł

```java
public class MyModule extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "A", "A", "B"));
    public final NumberSetting range = add(new NumberSetting("Range", 4, 1, 6, 0.1));
    public final BooleanSetting extra = add(new BooleanSetting("Extra", true).visibleIf(() -> mode.is("B")));

    public MyModule() { super("MyModule", "Opis w tooltipie", Category.COMBAT); }

    @Override public void onTick() { /* logika */ }
}
```

Potem dopisz `add(new MyModule());` w `ModuleManager`. GUI samo zbuduje panel i komponenty.

## Struktura

- `gui/ClickGuiScreen` – ekran, animacja otwierania/zamykania, wyszukiwarka, tooltip
- `gui/Panel`, `gui/ModuleButton` – panele i przyciski modułów
- `gui/component/*` – toggle, slider, tryb, kolor (hue), bind
- `gui/HudRenderer` – watermark + ArrayList
- `util/Render2D`, `util/Theme`, `util/Animation` – rysowanie, kolory, animacje

Uwaga: przykładowe moduły (KillAura, Flight itd.) to tylko szkielety z ustawieniami do pokazania GUI –
logikę wpinasz sam. Pod 1.21.9+ trzeba zmienić sygnatury `mouseClicked/keyPressed/charTyped` (nowe API `Click`/`KeyInput`).
