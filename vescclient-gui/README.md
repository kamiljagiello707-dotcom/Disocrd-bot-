# VescClient GUI

Nowy, osobny projekt (Fabric, Minecraft **1.21.1**, Java 21). Na razie zawiera jedną rzecz:
**własne menu główne „Aurora”**, które zastępuje ekran tytułowy Minecrafta.

## Co jest w menu

- **Tło**
  - nocne niebo z migoczącymi gwiazdami i co kilka sekund spadającą gwiazdą
  - animowana **zorza polarna**: 3 falujące wstęgi (turkus, fiolet, róż)
  - dwie warstwy **gór z bloków** na dole, ze świecącą krawędzią i mgłą między nimi
  - unoszące się świetliki
  - lekka paralaksa: warstwy przesuwają się za myszką
- **Lewa strona**
  - logo „V” + napis **VESC CLIENT**
  - lista: Tryb jednoosobowy, Tryb wieloosobowy, Minecraft Realms, Ustawienia, Wyjdź z gry
  - przesuwane podświetlenie z opisem i strzałką; przy „Wyjdź” na czerwono
  - obsługa klawiatury: **↑ / ↓ / Tab** i **Enter**
- **Prawa strona**: szklana karta z głową Twojego skina, powitaniem zależnym od pory dnia,
  dużym zegarem z sekundami, datą, paskiem postępu dnia i listą nowości
- **Dół**: wersja, przyciski Język i Dostępność, link „Copyright Mojang AB”
- Polski albo angielski tekst, zależnie od języka gry
- Układ dopasowuje się do skali GUI; przy małym oknie karta po prawej się chowa

## Budowanie

```
./gradlew build
```

Gotowy plik: `build/libs/vescclient-gui-1.0.0.jar` → wrzuć do `.minecraft/mods` razem z Fabric API.

> Nie instaluj go razem ze starym `vescclient-*.jar`: oba podmieniają menu główne.

## Struktura

```
src/main/java/dev/vesc/gui/
  VescGui.java                 – start moda (rejestracja shaderów)
  ui/VescMenu.java             – menu główne „Aurora”
  ui/Anim.java                 – płynne animacje
  render/R2D.java              – rysowanie 2D (zaokrąglenia SDF, cienie, poświaty, ikony)
  render/Fonts.java            – czcionka Inter
  render/SmoothTextLayer.java  – wygładzony tekst
  mixin/MinecraftClientMixin   – podmiana ekranu tytułowego
  mixin/TextRenderLayerSetMixin
src/main/resources/assets/vescgui/
  font/      – Inter (TTF)
  shaders/   – shadery UI
  textures/  – ikony (Phosphor, MIT) i tekstury UI
```
