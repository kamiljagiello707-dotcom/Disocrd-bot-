# NpcMenu – NPC z menu (Nazwa + Portfel)

Plugin do serwera **Paper/Purpur 1.21+** (Java 21). Klikasz NPC prawym przyciskiem i otwiera się menu:

| Slot | Przedmiot | Co pokazuje |
|---|---|---|
| 🟨 Nazwa | głowa gracza | nick gracza |
| 💰 Portfel | sztabka złota | saldo gracza, np. `100 zł` |
| ❌ Zamknij | bariera | zamyka menu |

Z menu nie da się wyciągnąć przedmiotów, a NPC nie bierze obrażeń, nie rusza się i nie otwiera handlu.

## Instalacja
1. `mvn package` (albo weź gotowy plik `NpcMenu-1.0.0.jar`)
2. Wrzuć `target/NpcMenu-1.0.0.jar` do folderu `plugins/` i zrestartuj serwer.

## Komendy
| Komenda | Opis | Uprawnienie |
|---|---|---|
| `/npc stworz [nazwa]` | tworzy NPC w twoim miejscu (kolory `&`) | `npcmenu.admin` |
| `/npc usun` | usuwa najbliższego NPC (do 5 kratek) | `npcmenu.admin` |
| `/portfel [gracz]` | pokazuje saldo | – |
| `/portfel dodaj/zabierz/ustaw <gracz> <kwota>` | zmienia saldo | `npcmenu.admin` |

## Konfiguracja
`plugins/NpcMenu/config.yml`: waluta, saldo startowe, tytuł menu i opisy przedmiotów (`{gracz}`, `{saldo}`, `{waluta}`).
Salda graczy zapisują się w `plugins/NpcMenu/portfel.yml`.
