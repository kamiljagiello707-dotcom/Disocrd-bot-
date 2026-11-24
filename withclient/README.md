# WithClient

Klient Fabric dla Minecrafta **1.21.1** (Java 21). Wygląd biało-czarny, własne logo i kompaktowe ClickGUI.

## Logo

Nowy znak to „VV” złożone z dwóch nachodzących na siebie V. Lewe V jest białe, prawe szare,
a dookoła biegnie cienka orbita, która nawiązuje do pierwszego logo.
- `with_logo.png` – sam znak (w GUI),
- `with_mark.png` – znak z orbitą,
- `with_app.png` / `mod_icon.png` – ikona aplikacji (ciemny kafelek), widoczna m.in. na liście modów.

## Menu, listy i wczytywanie

WithClient zastępuje ekrany Minecrafta własnymi:
- **Menu główne**:
  - tło: obracająca się panorama Minecrafta z przyciemnieniem, płynącymi światłami i cząsteczkami,
    które łączą się liniami z kursorem,
  - duże logo VV i napis WithClient,
  - lista z białą pigułką (mysz albo ↑ ↓ Enter),
  - karta z profilem, zegarem i datą,
  - **Szybki start**: jednym kliknięciem wchodzisz na ostatni serwer albo ostatni świat,
  - nowości, stan Discorda, przyciski języka i dostępności.
- **Tryb jednoosobowy**: lista światów z ikonami, trybem gry, datą i wersją. Przyciski Graj, Nowy świat,
  Edytuj, Usuń (z potwierdzeniem) i Odśwież. Działa wyszukiwanie i dwuklik.
- **Tryb wieloosobowy**: serwery z ikoną, MOTD, graczami i paskami pingu. Przyciski Dołącz, Bezpośrednio,
  Dodaj, Edytuj, Usuń i Odśwież.
- **Wczytywanie**: przy łączeniu z serwerem, pobieraniu terenu, tworzeniu świata i zapisywaniu.
  Pokazuje logo w wirującym pierścieniu, napis „Wczytywanie…”, status z nazwą serwera albo świata,
  pasek postępu (z procentami przy tworzeniu świata) i wskazówkę. Błąd połączenia też ma własny ekran.

## Status w Discordzie (DiscordRPC)

Gdy grasz, Discord pokazuje **„Gra w WithClient”** z logo. Niżej widać
„WithClient 1.6.0 · Minecraft 1.21.1”, miejsce gry (menu, tryb jednoosobowy, adres serwera) i czas gry.

Discord pokazuje nazwę i logo z **aplikacji Discord**, więc trzeba ją raz założyć (2 minuty):
1. Wejdź na https://discord.com/developers/applications → **New Application**, nazwij ją `WithClient`.
2. Skopiuj **Application ID** (General Information).
3. W **Rich Presence → Art Assets** wgraj `branding/discord_logo.png` pod nazwą **`logo`**.
4. W grze: ClickGUI → Klient → **DiscordRPC** → pole „ID aplikacji” → wklej ID.

Stan połączenia widać w prawym górnym rogu menu głównego. Opcje modułu: duży obraz (logo albo Twój skin),
pokazywanie serwera, nicku i czasu oraz własny opis.

## Efekty w świecie (kosmetyka)

- **TargetESP** (Wizualne): świecący znacznik na przeciwniku, którego bijesz.
  - 7 stylów z dostarczonych tekstur: Diamond, Triad, Shuriken, Brackets, Bold, Soft, Thin.
  - Animacja „namierzania” przy wyborze celu, obrót, pulsowanie, poświata i ciemny „cień” pod spodem,
    dzięki któremu znacznik widać też w dzień.
  - Kolor stały albo tęcza, czerwony błysk przy trafieniu.
  - Rysowany z testem głębi, więc nie widać go przez ściany.
- **JumpCircles** (Kosmetyki): przy skoku pod nogami rozchodzi się krąg.
  - 4 style: Ring, Glow, Soft, Swirl.
  - Możesz ustawić rozmiar, czas, kolor (albo tęczę) i obrót.
- **SwordColor** (Kosmetyki): własny kolor mieczy (opcjonalnie też toporów).
  - Tryby: stały kolor, gradient między dwoma kolorami albo tęcza.
  - Osobno w ręce, na pasku i na ziemi.

## Wizualia i kosmetyka (1.6)

Wszystko działa tylko u Ciebie i zmienia wyłącznie wygląd. Nic nie jest widoczne przez ściany
i nic nie daje przewagi w walce.

**Kamera i widok** (Wizualne)
- **Zoom**: płynne przybliżenie pod klawiszem **C** (zmienisz w Sterowaniu).
  - Kółko myszy zmienia przybliżenie, a czułość myszy spada razem z nim.
  - Możesz włączyć kinową kamerę, ukrywanie ręki, tryb przełączania i pigułkę z wartością (np. „4.0x”).
- **CustomFOV**: własne pole widzenia. FOV nie skacze przy sprincie, opcjonalnie bez efektów mikstur.
- **NoHurtCam**: kamera nie trzęsie się przy obrażeniach (siła 0–100%).
- **Fullbright**: gamma albo noktowizja, z płynnym przejściem.
- **ViewModel**: pozycja (X/Y/Z) i skala przedmiotu w każdej ręce, bez bujania ręki.
- **SwingAnimation** (Kosmetyki): style 1.7, Smooth, Spin, Slide, Push i Chop oraz prędkość machnięcia.

**Świat** (Wizualne / Świat)
- **TimeChanger**: własna godzina, opcjonalnie z cyklem dnia.
- **Weather**: czyste niebo, deszcz, burza albo śnieg, z intensywnością.
- **Ambience**: kolor nieba, mgły i chmur, także w trybie tęczy.
- **CustomSky** (Kosmetyki): własne niebo zamiast nieba z gry:
  - Galaxy: pasmo gwiazd i mgławice,
  - Aurora: falująca zorza,
  - Night, Sunset i Mono,
  - mgła dopasowuje się kolorem, chmury można ukryć.
- **Atmosphere**: świetliki, śnieg, gwiazdy, sakura albo iskry unoszące się wokół Ciebie.
- **BlockOutline**: obrys i wypełnienie bloku z płynnym przesuwaniem między blokami, grubością i tęczą.
- **ItemPhysics**: upuszczone przedmioty leżą płasko na ziemi i kręcą się w locie.
- **Breadcrumbs**: wstęga za Tobą (kolor albo tęcza, zanikanie).

**Walka (tylko efekty)**
- **Crosshair**: własny celownik w 6 stylach (Cross, Dot, Circle, T, Square, Chevron).
  - Obrys, kropka, rozszerzanie w ruchu i kolor po najechaniu na cel.
  - Hitmarker po uderzeniu i cienki wskaźnik ładowania ataku.
- **DamageNumbers**: liczby obrażeń nad przeciwnikiem (Pop, Float, Minimal), kolor zależny od obrażeń.
- **HitParticles** (Kosmetyki): 14 rodzajów cząsteczek przy uderzeniu.
- **HitSound** (Kosmetyki): 8 dźwięków przy uderzeniu, z głośnością i wysokością.
- **KillEffect** (Kosmetyki): efekt przy zabiciu celu:
  - piorun (tylko u Ciebie, bez ognia i obrażeń),
  - dusze, wybuch, krew, fajerwerki albo totem.
- **HitColor**: własny kolor błysku obrażeń (albo tęcza).
- **NoRender**: chowa ogień, wodę, mgłę terenu, paski bossów, animację totemu, dynię, winietę,
  portal, scoreboard i szron.

**Kosmetyki na postaci** (widać je w F5 i w ekwipunku)
- **Cape**: 6 peleryn (WithClient, Mono, Stripes, Minimal, Aurora, Galaxy). Ten sam wzór jest na elytrze.
- **Hat**: China hat (tęczowy, obracany), korona z klejnotami albo cylinder.
- **Halo**: świecąca aureola nad głową.
- **Wings**: skrzydła Angel, Dragon albo Mono, z machaniem.
- **Trails**: 10 rodzajów cząsteczek za Tobą.
- **ItemGlint**: kolor i prędkość połysku zaklęć.
- **NameColor**: kolor, gradient albo tęcza Twojego nicku w TAB.

## ClickGUI (w grze, **prawy Shift**)

Okno ma stałą wielkość na ekranie, niezależnie od skali GUI Minecrafta (zmienisz ją w Wygląd → Skala okna).

- **Nawigacja**: logo, 10 kategorii z licznikami włączonych modułów, strony Wygląd / Konfiguracje / Znajomi
  i profil gracza. Aktywną pozycję zaznacza przesuwana pigułka.
- **Nagłówek**: tytuł, liczba modułów, wyszukiwarka (rozszerza się po kliknięciu) i filtr
  Wszystkie / Aktywne / Ulubione z przesuwanym wskaźnikiem.
- **Moduły**: siatka kart w dwóch kolumnach (ikona, nazwa, opis, klawisz, ulubiony, przełącznik).
  Po wybraniu modułu karty płynnie przechodzą w jedną kolumnę, a z prawej **wysuwa się panel ustawień**.
- **Panel ustawień**: duży przycisk WŁĄCZONY/WYŁĄCZONY, opis i **zwijane grupy** opcji. Rodzaje opcji:
  - przełączniki,
  - suwaki,
  - **zakresy** (dwa uchwyty, np. CPS 9–13),
  - tryby: przełącznik segmentowy albo **lista rozwijana**,
  - **wielokrotny wybór** (np. cele),
  - **kolory** z pickerem i szybkimi odcieniami,
  - **pola tekstowe**,
  - **opcje zależne**, które pojawiają się tylko przy danym trybie, np. Smoothness przy Rotacje = Smooth.

  Na dole panelu są klawisz, ulubiony i przywracanie domyślnych.
- **Animacje i efekty**:
  - wysuwanie panelu,
  - przelewanie siatki,
  - wjazd kart po kolei,
  - przejścia stron,
  - obracające się strzałki,
  - powiadomienia przy przełączaniu,
  - podpowiedzi,
  - **cząsteczki** połączone liniami, reagujące na kursor,
  - **świetlny punkt** okrążający ramkę okna.

Sterowanie:
- **LPM** wybiera moduł albo przełącza go przełącznikiem,
- **PPM** otwiera ustawienia,
- **ŚPM** przypisuje klawisz (Delete usuwa),
- pisanie = wyszukiwanie,
- **Esc** zamyka.

### Wygląd
- **Kolor akcentu**: biały (domyślny), srebrny i 8 kolorów.
- **Styl okna**: Pełny / Szkło / Minimal.
- **Wymiary**: skala okna, zaokrąglenie, przyciemnienie tła.
- **Animacje**: szybkość (Wolne / Normalne / Szybkie / Wył.) i krzywa (Płynna / Sprężysta / Liniowa).
- **Efekty**: cząsteczki, animowana ramka, winieta, rozmycie świata, cień.
- **Interfejs**: opisy i klawisze na liście, podpowiedzi, powiadomienia, dźwięki.

### Konfiguracje i znajomi
- Konfiguracje: zapisz nową, wczytaj, nadpisz, usuń (z potwierdzeniem), otwórz folder.
  Pliki są w `config/withclient/configs/`.
- Znajomi: dodawanie po nicku i usuwanie.

## HUD (działa w grze)

Elementy HUD to moduły z kategorii **HUD**. Włączony moduł pokazuje swój element na ekranie,
a jego opcje zmieniają wygląd. Wszystko jest w stylu biało-czarnym.

| Element | Co pokazuje | Opcje |
|---|---|---|
| **Watermark** | czarna kapsuła na środku u góry: logo + ping, serwer, TPS, nick, godzina (opcjonalnie FPS) | styl Capsule / Stats / Bar / Pill / Logo / Text, własny tekst, tło |
| **ArrayList** | **karta „Moduły” (embed)**: pasek akcentu, nagłówek z liczbą aktywnych, moduły pogrupowane po kategoriach z ikoną i trybem; moduły wjeżdżają i wyjeżdżają | styl Embed / Pills / Classic / Text, grupowanie, nagłówek, sortowanie, animacja, ikony, kolor, ukrywanie wizualnych |
| **Keybinds** | po lewej: nagłówek „Keybinds” z ikoną klawiatury, pod nim pigułki z modułem i kółko z klawiszem (włączone jaśniejsze) | styl Pills / Embed, tylko włączone, limit |
| **TargetHUD** | przeciwnik, którego bijesz: zaokrąglona głowa (albo **obracający się model 3D**), przedmiot w ręce, HP z sercem, pasek z podziałką, śladem obrażeń i absorpcją, odległość, PRZEWAGA / SŁABIEJ, zbroja, trzęsienie i czerwony błysk przy trafieniu, cząsteczki | styl Modern / Model / Compact / Minimal, zasięg |
| **Coordinates** | pigułka z pinezką: „1632, 68, 934” + kierunek; opcjonalnie Nether ↔ świat i biom | styl Pill / Panel |
| **Potions** | efekty z ikoną, poziomem, czasem i paskiem (chowa ikony z Minecrafta) | sortowanie |
| **ArmorHUD** | ciemny panel obok hotbara: zbroja z kreskami wytrzymałości (opcjonalnie przedmiot w ręce) | poziomo / pionowo, procenty |
| **HealthBar** | dwa paski nad hotbarem: zdrowie (czerwony) i głód (pomarańczowy) z liczbą w środku, absorpcja; chowa serca, zbroję i głód Minecrafta | styl Split / Bar / Minimal, ukrywanie serc |
| **Keystrokes** | W A S D, LPM / PPM z CPS, spacja – podświetlają się po wciśnięciu | rozmiar |
| **Compass** | kompas paskowy albo okrągły ze stopniami | |
| **Notifications** | pigułka „Włączono KillAura” z małym przełącznikiem i licznikiem czasu w kółku | styl Pill / Card, czas, pozycja, dźwięk |
| **Spotify** | tekst piosenki w kapsule, karta z **kręcącą się płytą winylową** (okładka w środku), tytuł, wykonawca, poprzedni / pauza / następny, postęp z czasem po obu stronach; opcjonalnie **duży tekst na ekranie** podświetlany w trakcie śpiewania | styl Vinyl / Card / Compact; przyciski klikasz przy otwartym czacie |
| **Hotbar** | własny pasek przedmiotów w ciemnym panelu, obrys wybranego slotu (przesuwany), cienkie kreski wytrzymałości pod przedmiotami, druga ręka obok | styl Panel / Clean, animacja, kreski wytrzymałości, numery |
| **Session** | czas gry, przebyty dystans, śmierci | |

Mniejsze elementy: **FPS** (z wykresem i min/max), **Ping** (paski sygnału), **TPS** (liczone z pakietów
czasu serwera), **Speedometer** (bps / km/h), **CPS**, **Clock**, **ServerInfo**, **Memory**,
**TotemCounter**, **InventoryHUD**.

**Spotify**: Windows czyta systemową sesję multimediów (Spotify, przeglądarka i inne), macOS czyta
Spotify przez AppleScript, a Linux używa `playerctl`. Tekst piosenki pobiera z lrclib.net (można wyłączyć).

HUD ma tę samą wielkość na ekranie przy każdej skali GUI Minecrafta, więc na 1080p z automatyczną skalą
nie zajmuje pół ekranu. Większy albo mniejszy zrobisz w *Klient → HUDEditor → Skala*.

Domyślnie elementy same układają się w stosy przy krawędziach ekranu i omijają się nawzajem.
Unikają też miejsc Minecrafta: czatu, paska przedmiotów i ikon efektów.

### Edytor HUD
Otwierasz go przyciskiem **„Edytuj HUD”** w kategorii HUD albo modułem *HUDEditor* (można mu przypisać klawisz).
- przeciąganie elementów myszą z przyciąganiem do krawędzi, środka ekranu, innych elementów i siatki
  (linie pomocnicze),
- **kółko myszy** = skala elementu, **PPM** = pozycja domyślna,
- panel z przełącznikami wszystkich elementów. Panel można przesunąć za nagłówek, a **H** go zwija,
- „Resetuj układ” i „Gotowe” (albo Esc).

Wspólny wygląd HUD ustawisz w *Klient → HUDEditor*: tło (Dark / Glass / Clear), skala, przezroczystość,
zaokrąglenie, obrys, cień, ikony i kolor akcentu. Pozycje zapisują się w `config/withclient/hud.json`.

## Moduły

149 modułów w 10 kategoriach: Walka, Ruch, Gracz, Wizualne, Świat, HUD, Kosmetyki, Narzędzia,
Exploity i Klient. Każdy ma własną ikonę i zestaw opcji. Stan modułów zapisuje się w
`config/withclient/modules.json`, a wygląd w `gui.json`.

> Działają: HUD, wszystkie wizualia i kosmetyka opisane wyżej, TargetESP, JumpCircles, SwordColor,
> DiscordRPC i menu. Moduły dające przewagę (walka, ruch, ESP przez ściany, XRay itp.) to tylko
> przełączniki w GUI i nic nie robią w grze.

## Budowanie

```
./gradlew build
```

Gotowy plik: `build/libs/withclient-1.6.0.jar`. Wrzuć go do `.minecraft/mods` razem z Fabric API.
Klawisz otwierania zmienisz w Opcje → Sterowanie → WithClient.

## Struktura

```
src/main/java/dev/with/client/
  WithClient.java            – start moda, klawisz GUI
  ui/ClickGui.java           – ClickGUI: nawigacja, siatka, wysuwany panel, strony, efekty
  ui/Theme.java              – wygląd (akcent, styl, skala, animacje, efekty) – gui.json
  ui/Anim.java               – animacje (płynna / sprężysta / liniowa)
  ui/Ui.java                 – paleta, logo, przyciski, przełączniki
  ui/Friends.java
  hud/Hud.java               – elementy HUD, układ (stosy, kotwice, omijanie), powiadomienia
  hud/HudEditor.java         – edytor HUD (przeciąganie, przyciąganie, skala, panel)
  hud/Media.java             – co teraz gra (Windows / macOS / Linux), okładka, tekst piosenki
  render/WorldFx.java        – Target ESP i Jump Circles
  visual/View.java           – zoom, FOV, fullbright, czas, pogoda, kolory nieba, ViewModel, animacje machnięcia
  visual/Effects.java        – cząsteczki, dźwięk i efekt zabicia, liczby obrażeń, ślad, atmosfera, obrys bloku
  visual/CosmeticFeature.java – czapki, aureola i skrzydła na modelu gracza
  visual/Sky.java            – własne niebo (Galaxy, Aurora, Night, Sunset, Mono)
  visual/Tints.java          – peleryny, kolor połysku zaklęć, kolor obrażeń, kolor nicku
  hud/Crosshair.java         – celownik, hitmarker, pigułka zoomu
  discord/DiscordRpc.java    – status w Discordzie przez IPC
  ui/WithMenu.java, MenuBg.java, ListScreen.java, WorldsScreen.java, ServersScreen.java, Loading.java, Recent.java
                             – menu główne, tło, listy światów i serwerów, wczytywanie, szybki start
  module/Category/Module/Modules.java – kategorie, moduły, zapis
  module/Setting.java        – Bool, Num, Range, Mode, Multi, Color, Text, Group (+ warunki widoczności)
  render/R2D.java, Fonts.java, SmoothTextLayer.java – silnik rysowania
  mixin/TextRenderLayerSetMixin.java
  mixin/MouseMixin.java      – CPS (kliknięcia)
  mixin/WorldTimeMixin.java  – TPS (czas świata z serwera)
  mixin/InGameHudMixin.java  – własny hotbar, chowanie ikon efektów, celownik, NoRender
  mixin/GameRenderer/Lightmap/ClientWorld/BackgroundRenderer/WorldRenderer/HeldItemRenderer… – wizualia 1.6
  mixin/ItemRendererMixin.java – kolor mieczy
  mixin/ChatScreenMixin.java – klikanie w HUD przy otwartym czacie
  mixin/MinecraftClientMixin, ScreenMixin, *Accessor – podmiana menu i ekranów wczytywania
src/main/resources/assets/withclient/
  font/, shaders/
  textures/icons/            – logo WithClient + ikony Lucide (l_*, licencja ISC)
  textures/fx/               – tekstury Target ESP i Jump Circles
branding/                    – logo do aplikacji Discord (1024 px) i znak WithClient
```
