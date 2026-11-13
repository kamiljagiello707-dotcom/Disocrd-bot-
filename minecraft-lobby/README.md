# LobbyCore – lobby z trybami Anarchia i FFA

Plugin do **Paper/Purpur 1.21.1+** (Java 21). Po wrzuceniu na serwer **sam buduje** lobby i arenę FFA. Nie musisz nic stawiać ręcznie.

## Co jest w środku

**Lobby** (osobny pusty świat `lobby`, pływająca wyspa):
- plac w kształcie róży wiatrów z fontanną i **beaconem** świecącym w niebo
- po lewej pawilon **Anarchii** z działającym portalem do Netheru, po prawej pawilon **FFA** z bramą Endu. Wejście w portal przenosi do trybu.
- NPC przy każdym pawilonie: kliknięcie przenosi do trybu, a NPC odwraca głowę w stronę gracza
- hologramy z liczbą graczy online, aktualizowane na żywo
- wiśnie, kwiaty, latarnie, żywopłot z azalii, **wyrzutnie** (złote płytki)
- **podwójny skok** (spacja w powietrzu)
- ochrona: brak obrażeń, głodu, niszczenia, wyrzucania przedmiotów i mobów. Spadnięcie w pustkę teleportuje na spawn.
- przedmioty w pasku: 🧭 **wybór trybu** (menu), 🙂 **profil** (nick, statystyki FFA, czas gry, portfel z NpcMenu), 🟢 **ukryj/pokaż graczy**

**Anarchia** (zwykły świat `world` + nether + end):
- survival bez zasad
- **osobny ekwipunek**: gdy idziesz do lobby albo FFA, przedmioty, XP, życie i pozycja zapisują się. Po powrocie wszystko wraca.
- pierwsze wejście = losowy teleport (domyślnie w promieniu 3000)

**FFA** (osobny świat `ffa`, okrągła arena z murem, głazami i ruinami):
- gotowy kit z niezniszczalnym sprzętem, do zmiany w configu
- 8 spawnów dookoła, 3 s ochrony po spawnie, natychmiastowy respawn z kitem
- zabójstwa, śmierci, K/D, seria i rekord serii (`stats.yml`)
- po zabójstwie pełne HP. Co 5 zabójstw z rzędu ogłoszenie na czacie.
- **blokada ucieczki**: przez 10 s po ciosie nie zadziała `/spawn`, a wyjście z serwera = śmierć

**Wszędzie**: scoreboard bez migania (osobny dla lobby, anarchii i FFA), nagłówek i stopka tablisty, format czatu z prefiksem trybu, wiadomości wejścia i wyjścia.

## Instalacja
1. Wrzuć `LobbyCore-1.0.0.jar` do `plugins/` (opcjonalnie też `NpcMenu-1.0.0.jar` dla portfela w profilu).
2. Uruchom serwer. Lobby i arena zbudują się same przy pierwszym starcie (kilka sekund).
3. W `plugins/LobbyCore/config.yml` zmień nazwę serwera, IP, Discorda, kit FFA, linie scoreboardu itd.

## Komendy
| Komenda | Co robi |
|---|---|
| `/lobby` (`/spawn`, `/hub`, `/l`) | powrót do lobby |
| `/anarchia`, `/ffa` | szybkie dołączenie do trybu |
| `/la zbuduj lobby` / `/la zbuduj ffa` | przebudowanie od nowa (admin) |
| `/la ustawspawn` | spawn lobby w twoim miejscu |
| `/la dodajspawnffa`, `/la czyscspawnyffa` | własne spawny areny |
| `/la buduj` | tryb budowania: kreatywny, ochrona wyłączona dla ciebie |
| `/la reload` | przeładowanie configu |

Uprawnienie admina: `lobbycore.admin` (domyślnie OP).

## BungeeCord / Velocity
Jeśli Anarchia albo FFA stoją na osobnych serwerach, wpisz ich nazwy w `proxy.anarchia` / `proxy.ffa`.
Portale, NPC i menu będą wtedy przenosić graczy na te serwery, a liczniki online pobiorą się z proxy.

## Podgląd
`podglad/podglad-3d.html` to podgląd 3D zrobiony z bloków, które plugin postawił na testowym serwerze Paper 1.21.1.
