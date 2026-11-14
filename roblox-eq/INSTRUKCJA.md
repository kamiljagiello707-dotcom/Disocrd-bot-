# 🎒 System EQ (ekwipunek) do Roblox Studio — pełna instrukcja

Gotowy system ekwipunku: pasek na dole (1–9), okno plecaka (klawisz **E**), przeciąganie przedmiotów, stosy, rzadkości z kolorami, okno szczegółów, używanie, wyrzucanie, podnoszenie z ziemi, bronie, zapis postępu (DataStore) i obsługa telefonów.

---

## ✦ Co jest w środku

| Funkcja | Opis |
|---|---|
| 🧰 Pasek 1–9 | Klawisze 1–9 albo kliknięcie = weź do ręki / użyj |
| 🎒 Plecak 30 slotów | Klawisz **E** albo przycisk 🎒 (działa też na telefonie) |
| 🖱️ Przeciąganie | Przenoszenie, zamiana miejscami, łączenie stosów |
| ✂️ Dzielenie stosu | Prawy przycisk myszy na slocie albo przycisk „Podziel” |
| ⬇️ Wyrzucanie | **Q** = 1 szt. trzymanego przedmiotu, upuszczenie poza okno = cały stos |
| ✋ Podnoszenie | Klawisz **F** przy przedmiocie na ziemi |
| 🔍 Szukaj + ⇅ Sortuj | Wyszukiwanie po nazwie, sortowanie plecaka wg rzadkości |
| 💎 Rzadkości | Zwykły, Niezwykły, Rzadki, Epicki, Legendarny, Mityczny (kolory ramek) |
| ⚔️ Bronie | Działają od razu (zadają obrażenia) — nie trzeba robić modelu |
| 🧪 Mikstury | Leczenie, szybkość, skok (na czas) |
| 💾 Zapis | Ekwipunek zapisuje się po wyjściu, co 2 min i przy wyłączeniu serwera |
| 🛡️ Bezpieczny | Wszystko liczy serwer — exploiter nie doda sobie przedmiotów |
| 📱 Telefon | UI samo dopasowuje się do rozmiaru ekranu |

**Skróty:** `E` otwórz/zamknij • `1–9` pasek • `Q` wyrzuć • `F` podnieś • `Esc` zamknij • 2× klik = użyj • najedź na przedmiot w plecaku + cyfra = przenieś na pasek.

---

## ✦ Instalacja — sposób 1 (najszybszy, ~1 min)

W folderze `modele/` są 3 gotowe pliki. W Roblox Studio:

1. Otwórz swoją grę (albo nowe miejsce **Baseplate**).
2. Włącz okno **Explorer** (zakładka *View → Explorer*).
3. Przeciągnij pliki z folderu na komputerze **na odpowiednie miejsce w Explorerze**:

| Plik | Upuść na |
|---|---|
| `EQShared.rbxmx` | **ReplicatedStorage** |
| `EQSystem.rbxmx` | **ServerScriptService** |
| `EQClient.rbxmx` | **StarterPlayer → StarterPlayerScripts** |

> Zamiast przeciągania możesz kliknąć PPM na ReplicatedStorage (itd.) → **Insert from File...** i wybrać plik.

4. Przejdź do kroku **„Włącz zapis”** niżej.

---

## ✦ Instalacja — sposób 2 (ręcznie, kopiuj-wklej)

Pliki z kodem są w folderze `src/`. W Explorerze utwórz dokładnie taką strukturę (PPM → *Insert Object*):

```
ReplicatedStorage
└── 📁 EQShared                (Folder)
    ├── 📜 EQConfig            (ModuleScript)  ← src/EQConfig.lua
    └── 📜 ItemData            (ModuleScript)  ← src/ItemData.lua

ServerScriptService
└── 📁 EQSystem                (Folder)
    ├── 📜 EQService           (ModuleScript)  ← src/EQService.lua
    └── 📜 EQServer            (Script)        ← src/EQServer.server.lua

StarterPlayer
└── StarterPlayerScripts
    └── 📜 EQClient            (LocalScript)   ← src/EQClient.client.lua

ServerStorage
└── 📁 EQTools                 (Folder, opcjonalnie – własne modele broni)
```

Dla każdego skryptu: otwórz go (2× klik), usuń domyślny tekst `print("Hello world!")` i wklej całą zawartość pliku.

⚠️ **Nazwy muszą się zgadzać co do litery** (`EQShared`, `EQConfig`, `ItemData`, `EQSystem`, `EQService`). Typy też: **ModuleScript**, **Script** i **LocalScript** to trzy różne rzeczy.

---

## ✦ Włącz zapis (DataStore)

1. **Opublikuj grę**: *File → Publish to Roblox* (DataStore działa tylko w opublikowanej grze).
2. *Home → Game Settings → Security* → włącz **Enable Studio Access to API Services** → *Save*.

Bez tego system i tak działa, ale ekwipunek nie zapisuje się między wejściami (w Output zobaczysz ostrzeżenie `[EQ]`).

---

## ✦ Test

1. Kliknij **Play** (F5).
2. W **Output** (*View → Output*) powinno być: `[EQ] System ekwipunku uruchomiony (39 slotów)`.
3. Na dole masz pasek z mieczem, jabłkami i miksturami. Wokół spawnu leży 10 przedmiotów testowych — podejdź i wciśnij **F**.
4. Wciśnij **E** — otworzy się plecak. Przeciągaj, klikaj, sortuj.
5. Wciśnij **1** — miecz w ręce, klik = zamach (zadaje obrażenia graczom i NPC).

**Komendy na czacie** (działają w Studio i dla twórcy gry):

| Komenda | Co robi |
|---|---|
| `!daj Diament 10` | daje przedmiot (ID z `ItemData`) |
| `!lista` | pokazuje ID wszystkich przedmiotów |
| `!wyczysc` | czyści ekwipunek |

Gdy skończysz testy, w `EQConfig` ustaw `Config.DemoPickups = false`, żeby przedmioty testowe nie pojawiały się w grze.

---

## ✦ Dodawanie własnych przedmiotów

Otwórz **ItemData** i dopisz nowy wpis (skopiuj istniejący). Przykład:

```lua
Banan = {
	Name = "Banan",
	Description = "Żółty i zdrowy.",
	Rarity = "Uncommon",      -- Common / Uncommon / Rare / Epic / Legendary / Mythic
	Type = "Consumable",      -- Tool / Consumable / Material
	Emoji = "🍌",
	MaxStack = 16,
	Heal = 30,
},
```

**Własny obrazek zamiast emoji:** wgraj obrazek (*View → Asset Manager → Images*), PPM → *Copy Asset ID* i dodaj `Icon = "rbxassetid://TWOJE_ID",`.

**Pola dla broni (`Type = "Tool"`)**: `Damage`, `Cooldown`, `Range`, `Color`.
**Pola dla mikstur (`Type = "Consumable"`)**: `Heal`, `SpeedBoost`, `JumpBoost`, `Duration`.

---

## ✦ Własne modele broni / narzędzi

Domyślnie system sam robi prosty miecz. Żeby użyć **swojego Toola** (np. z Toolboxa):

1. W **ServerStorage** utwórz folder **EQTools**.
2. Wrzuć do niego Tool i nazwij go **tak samo jak ID** przedmiotu (np. `Miecz`)
   albo dodaj w ItemData pole `ToolName = "NazwaToola"`.
3. Gotowe — po wybraniu przedmiotu gracz dostanie Twój Tool (z jego własnymi skryptami i animacjami).

---

## ✦ Przedmioty do podniesienia na mapie

Dowolny **Part** albo **Model** w Workspace może być przedmiotem do podniesienia. Zaznacz go → okno **Properties** → na samym dole **Attributes → +** i dodaj:

| Atrybut | Typ | Przykład | Opis |
|---|---|---|---|
| `EQItem` | string | `Diament` | ID przedmiotu (wymagane) |
| `EQAmount` | number | `3` | ilość (domyślnie 1) |
| `EQRespawn` | number | `30` | po ilu sekundach pojawi się znowu (brak = znika na zawsze) |

System sam doda podpowiedź „Podnieś [F]”.

---

## ✦ Używanie EQ w innych skryptach (sklepy, questy, skrzynki)

W dowolnym **Script** (po stronie serwera):

```lua
local EQ = require(game.ServerScriptService.EQSystem.EQService)

-- dać przedmiot (zwraca ile się NIE zmieściło)
local reszta = EQ.AddItem(player, "Zloto", 5)

-- zabrać przedmiot (true = zabrano, false = za mało)
if EQ.RemoveItem(player, "Drewno", 10) then
	EQ.AddItem(player, "Kilof", 1) -- crafting!
end

EQ.CountItem(player, "Diament")      -- ile sztuk ma gracz
EQ.HasItem(player, "Kilof")          -- czy ma
EQ.HasSpace(player, "Drewno", 64)    -- czy się zmieści
EQ.Notify(player, "Kupiono!", Color3.fromRGB(80, 220, 120))
EQ.SpawnItem("Jablko", 3, Vector3.new(0, 5, 0), 20) -- przedmiot na ziemi (respawn 20 s)

-- zdarzenia
EQ.ItemAdded.Event:Connect(function(player, id, amount) print(player.Name, "+", amount, id) end)
```

**Przykład — sklep na przycisk (ClickDetector):** wstaw Part, do niego `ClickDetector` i **Script**:

```lua
local EQ = require(game.ServerScriptService.EQSystem.EQService)
script.Parent.ClickDetector.MouseClick:Connect(function(player)
	if EQ.RemoveItem(player, "Zloto", 3) then
		EQ.AddItem(player, "OgnistyMiecz", 1)
		EQ.Notify(player, "🔥 Kupiono Ognisty miecz!", Color3.fromRGB(255, 170, 40))
	else
		EQ.Notify(player, "Potrzebujesz 3x Sztabka złota", Color3.fromRGB(230, 70, 80))
	end
end)
```

---

## ✦ Ustawienia (EQConfig)

| Ustawienie | Domyślnie | Opis |
|---|---|---|
| `HotbarSlots` | 9 | sloty na pasku (max 9) |
| `BackpackSlots` | 30 | sloty w plecaku |
| `ToggleKey` / `DropKey` / `PickupKey` | E / Q / F | klawisze |
| `DataStoreName` | `EQ_v1` | zmień (np. na `EQ_v2`), żeby zresetować wszystkim ekwipunek |
| `StarterItems` | miecz, jabłka, mikstury | co dostaje nowy gracz |
| `DropLifetime` | 120 | po ilu sekundach znika wyrzucony przedmiot |
| `DemoPickups` | true | przedmioty testowe przy spawnie — **wyłącz przed premierą** |
| `PvP` | true | `false` = bronie ranią tylko NPC |
| `Admins` | – | UserId osób, które mogą używać komend `!daj` itd. |
| `Theme` | ciemny + czerwony | kolory UI (np. zmień `Accent`) |

---

## ✦ Problemy?

| Problem | Rozwiązanie |
|---|---|
| Nic się nie pojawia | Sprawdź **Output**. Najczęściej zła nazwa albo typ skryptu (patrz struktura wyżej). |
| `Infinite yield possible on ... EQShared` | Folder `EQShared` nie jest w **ReplicatedStorage** albo ma inną nazwę. |
| `Infinite yield ... EQRemotes` | Skrypt serwera nie działa — `EQServer` musi być typu **Script** w `ServerScriptService/EQSystem`. |
| Nie zapisuje się | Opublikuj grę i włącz *Enable Studio Access to API Services*. |
| Widać stary plecak Robloxa | `EQClient` musi być **LocalScript** w **StarterPlayerScripts**. |
| `[EQ] Nieznany przedmiot na mapie` | Atrybut `EQItem` ma ID, którego nie ma w `ItemData` (wielkość liter ma znaczenie). |
| Komendy `!daj` nie działają | Działają tylko w Studio, dla twórcy gry albo dla ID z `Config.Admins`. |
