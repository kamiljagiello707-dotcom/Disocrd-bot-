-- EQConfig (ModuleScript)
-- Miejsce: ReplicatedStorage > EQShared > EQConfig
-- Główne ustawienia systemu ekwipunku.

local Config = {}

-- ── Sloty ────────────────────────────────────────────
Config.HotbarSlots = 9          -- sloty na pasku na dole (klawisze 1-9)
Config.BackpackSlots = 30       -- sloty w plecaku (okno EQ)
Config.MaxSlots = Config.HotbarSlots + Config.BackpackSlots

-- ── Klawisze ─────────────────────────────────────────
Config.ToggleKey = Enum.KeyCode.E          -- otwórz / zamknij EQ
Config.DropKey = Enum.KeyCode.Q            -- wyrzuć 1 szt. trzymanego przedmiotu
Config.PickupKey = Enum.KeyCode.F          -- podnoszenie przedmiotów (ProximityPrompt)

-- ── Zapis danych (DataStore) ─────────────────────────
Config.SaveEnabled = true
Config.DataStoreName = "EQ_v1"             -- zmień nazwę, żeby zresetować wszystkim EQ
Config.AutosaveInterval = 120              -- sekundy

-- ── Przedmioty startowe (dla nowych graczy) ──────────
Config.StarterItems = {
	{ Id = "Miecz", Amount = 1 },
	{ Id = "Jablko", Amount = 5 },
	{ Id = "MiksturaLeczenia", Amount = 2 },
}

-- ── Podnoszenie / wyrzucanie ─────────────────────────
Config.PickupHoldTime = 0.25               -- ile trzeba trzymać klawisz (s)
Config.PickupDistance = 10
Config.DropLifetime = 120                  -- po ilu sekundach znika wyrzucony przedmiot
Config.DemoPickups = true                  -- stawia kilka przedmiotów testowych przy spawnie (wyłącz w grze)

-- ── Walka (bronie bez własnych skryptów) ─────────────
Config.PvP = true                          -- false = bronie ranią tylko NPC

-- ── Komendy na czacie (do testów) ────────────────────
-- !daj <Id> [ilość]   !wyczysc   !lista
-- Działają w Studio, dla twórcy gry i dla ID poniżej.
Config.Admins = {
	-- 123456789,
}

-- ── Rzadkości ────────────────────────────────────────
Config.Rarities = {
	Common    = { Name = "Zwykły",     Color = Color3.fromRGB(176, 182, 194), Order = 1 },
	Uncommon  = { Name = "Niezwykły",  Color = Color3.fromRGB(88, 214, 110),  Order = 2 },
	Rare      = { Name = "Rzadki",     Color = Color3.fromRGB(64, 156, 255),  Order = 3 },
	Epic      = { Name = "Epicki",     Color = Color3.fromRGB(176, 92, 255),  Order = 4 },
	Legendary = { Name = "Legendarny", Color = Color3.fromRGB(255, 170, 40),  Order = 5 },
	Mythic    = { Name = "Mityczny",   Color = Color3.fromRGB(255, 64, 96),   Order = 6 },
}

Config.TypeNames = {
	Tool = "Broń / narzędzie",
	Consumable = "Do użycia",
	Material = "Materiał",
}

-- ── Wygląd ───────────────────────────────────────────
Config.Theme = {
	Background = Color3.fromRGB(18, 20, 28),
	Panel      = Color3.fromRGB(28, 31, 43),
	Slot       = Color3.fromRGB(36, 40, 56),
	SlotStroke = Color3.fromRGB(58, 64, 86),
	Accent     = Color3.fromRGB(255, 60, 90),   -- kolor zaznaczenia / ramek
	Equipped   = Color3.fromRGB(255, 255, 255),
	Text       = Color3.fromRGB(240, 242, 250),
	SubText    = Color3.fromRGB(150, 156, 178),
	Good       = Color3.fromRGB(70, 200, 110),
	Danger     = Color3.fromRGB(230, 70, 80),
	Button     = Color3.fromRGB(48, 53, 74),
}

return Config
