# VescaClient

Osobny projekt (Fabric, Minecraft **1.21.1**, Java 21) z biało-czarnym wyglądem.
Zastępuje menu główne, listę serwerów, listę światów i ekrany wczytywania Minecrafta.

## Co jest w środku

### Tło (wspólne dla wszystkich ekranów)
- nocne niebo z migoczącymi gwiazdami i co kilka sekund spadającą gwiazdą
- srebrna, falująca **zorza polarna**
- dwie warstwy **gór z bloków** z mgłą i unoszącymi się świetlikami
- lekka paralaksa za myszką
- animacja płynnie trwa dalej przy przechodzeniu między ekranami

### Menu główne
- logo i napis **VESCA CLIENT**
- lista: Tryb jednoosobowy, Tryb wieloosobowy, Minecraft Realms, Ustawienia, Wyjdź z gry
- zaznaczony wiersz to biała pigułka z czarnym tekstem, opisem i strzałką; działa też klawiatura (↑ ↓ Tab Enter)
- szklana karta: głowa skina, powitanie zależne od pory dnia, zegar z sekundami, data, pasek postępu dnia, nowości
- dół: wersja, przyciski Język i Dostępność, „Copyright Mojang AB”

### Tryb wieloosobowy (lista serwerów)
- ikony serwerów (favicon), nazwa, MOTD, adres, liczba graczy i paski pingu
- przyciski: **Dołącz**, Bezpośrednio, Dodaj, Edytuj, Usuń (z potwierdzeniem), Odśwież
- wyszukiwanie: wystarczy zacząć pisać; dwuklik albo Enter dołącza; prawy przycisk otwiera edycję; Esc wraca

### Tryb jednoosobowy (lista światów)
- ikona świata, nazwa, tryb gry, data ostatniej gry, wersja albo znacznik HARDCORE
- przyciski: **Graj**, Nowy świat, Edytuj, Usuń (z potwierdzeniem), Odśwież
- wyszukiwanie, dwuklik, klawiatura jak wyżej

### Ekran wczytywania
Pokazuje się przy łączeniu z serwerem, pobieraniu terenu, przygotowywaniu świata i zapisywaniu:
logo VescaClient z obracającym się pierścieniem, napis **„Wczytywanie…”** (albo „Zapisywanie…”),
status i nazwa serwera/świata, pasek postępu (przy tworzeniu świata z procentami), przycisk „Anuluj”
i losowa wskazówka na dole.

Napisy są po polsku albo po angielsku, zależnie od języka gry. Układ dopasowuje się do skali GUI.

## Budowanie

```
./gradlew build
```

Gotowy plik: `build/libs/vescaclient-1.1.0.jar` → wrzuć do `.minecraft/mods` razem z Fabric API.

> Nie instaluj go razem ze starym `vescclient-*.jar`: oba podmieniają menu główne.

## Struktura

```
src/main/java/dev/vesca/client/
  VescaClient.java              – start moda (rejestracja shaderów)
  ui/Scene.java                 – wspólne animowane tło
  ui/Ui.java                    – paleta biało-czarna, logo, przyciski, klawisze
  ui/VescaMenu.java             – menu główne
  ui/VescaScreen.java           – baza ekranów z listą (panel, nagłówek, wyszukiwarka, lista, przyciski)
  ui/ServersScreen.java         – lista serwerów
  ui/WorldsScreen.java          – lista światów
  ui/Loading.java               – ekran wczytywania
  ui/Anim.java                  – płynne animacje
  render/R2D.java               – rysowanie 2D (zaokrąglenia SDF, cienie, poświaty, ikony)
  render/Fonts.java             – czcionka Inter
  render/SmoothTextLayer.java   – wygładzony tekst
  mixin/MinecraftClientMixin    – podmiana menu, listy serwerów i listy światów
  mixin/ScreenMixin             – ekrany wczytywania rysowane po swojemu
  mixin/*Accessor               – odczyt statusu / postępu z ekranów Minecrafta
  mixin/TextRenderLayerSetMixin
src/main/resources/assets/vescaclient/
  font/      – Inter (TTF)
  shaders/   – shadery UI
  textures/  – ikony (Phosphor, MIT) i tekstury UI
```
