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

### Powitanie na serwerze
Przy **pierwszym** wejściu na dany serwer na środku ekranu pojawia się **„Hello VescaClient”**
(logo, litery wjeżdżają po kolei, błysk, linia i podpis „Pierwszy raz na …”), a po ok. 5 sekundach płynnie znika.
Odwiedzone serwery są zapisane w `config/vescaclient/seen_servers.txt`, więc na tym samym serwerze
powitanie już się nie pokaże (także po restarcie gry). Usuń ten plik, żeby zobaczyć je znowu.

### ClickGUI (w grze, **prawy Shift**)
Biało-czarne, z własnym logo VescaClient i białymi ikonami Phosphor; układ wzorowany na prostych,
listowych klientach (stały pasek z logo i wersją, sekcje FUNKCJE / INNE, dwie kolumny wierszy).
- **stały pasek boczny**: logo + „Wersja", sekcje FUNKCJE (Walka, Ruch, Gracz, Wizualne, Świat, Klient, Ulubione)
  i INNE (Znajomi, Konfiguracje, Motyw), na dole Twój skin; aktywną stronę zaznacza pigułka w kolorze akcentu
- **moduły**: dwie kolumny prostych wierszy (nazwa + opis, klawisz, kropka stanu); w nagłówku kategoria,
  a po najechaniu modułu jego opis; wyszukiwarka rozwija się z ikonki
- **panel ustawień** (PPM na module) wysuwa się z prawej: włączony, klawisz, ulubiony, przełączniki, suwaki, tryby, „Przywróć domyślne"
- **Motyw**: kolor akcentu (biały + 8 kolorów), animacje, rozmiar GUI, pasek boczny (po najechaniu / rozwinięty / zwinięty),
  rozmycie tła, cień, dźwięki, przyciemnienie tła, skrót do trybu 3D
- **Konfiguracje**: zapisz / wczytaj / nadpisz / usuń; **Znajomi**: dodawanie po nicku i usuwanie
- **3D GUI** (Wizualne → 3D GUI, domyślnie wyłączone): okno jako pochylony panel z perspektywą; klikanie działa normalnie
- **LPM** włącza/wyłącza, **PPM** ustawienia, **środkowy** klawisz (Delete usuwa), pisanie = szukanie, **Esc** zamyka
- zapis: moduły w `config/vescaclient/modules.json`, motyw w `gui.json`; klawisz otwierania w Opcje → Sterowanie → VescaClient

> Moduły to na razie tylko przełączniki i ustawienia w GUI – nie mają żadnego działania w grze.

Napisy są po polsku albo po angielsku, zależnie od języka gry. Układ dopasowuje się do skali GUI.

## Budowanie

```
./gradlew build
```

Gotowy plik: `build/libs/vescaclient-1.5.0.jar` → wrzuć do `.minecraft/mods` razem z Fabric API.

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
  ui/Welcome.java               – powitanie „Hello VescaClient” przy pierwszym wejściu na serwer
  ui/ClickGui.java              – ClickGUI (prawy Shift): pasek, strony, panel ustawień, tryb 3D
  ui/Theme.java                 – ustawienia motywu (gui.json)
  ui/Friends.java               – lista znajomych (friends.txt)
  module/Category.java          – kategorie
  module/Module.java            – moduł (nazwa, ikona, opis, stan, klawisz, ustawienia)
  module/Setting.java           – ustawienia: przełącznik, suwak, tryb
  module/Modules.java           – lista modułów, klawisze, zapis do modules.json
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
  textures/  – ikony (Phosphor, MIT; g_* = ikony ClickGUI) i tekstury UI
  lang/      – nazwa klawisza w Opcje → Sterowanie
```
