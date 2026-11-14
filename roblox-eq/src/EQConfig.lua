-- EQConfig (ModuleScript)
-- Miejsce: ReplicatedStorage > EQShared > EQConfig
-- Ustawienia systemu ekwipunku RP.

local Config = {}

-- ── Pojemność ────────────────────────────────────────
Config.MaxWeight = 50            -- maksymalna waga (kg)
Config.MaxStacks = 40            -- maksymalnie tyle różnych stosów przedmiotów

-- ── Klawisze ─────────────────────────────────────────
Config.ToggleKey = Enum.KeyCode.B          -- otwórz / zamknij ekwipunek
Config.PickupKey = Enum.KeyCode.E          -- podnoszenie przedmiotów z ziemi
Config.ShowOpenButton = true               -- przycisk 🎒 na ekranie (ważne na telefonie)
Config.Blur = true                         -- rozmycie tła po otwarciu

-- ── Kategorie (zakładki) ─────────────────────────────
-- Id musi się zgadzać z polem Category w ItemData
Config.Categories = {
	{ Id = "all", Name = "Wszystko" },
	{ Id = "Jedzenie", Name = "Jedzenie" },
	{ Id = "Medyczne", Name = "Medyczne" },
	{ Id = "Narzedzia", Name = "Narzędzia" },
	{ Id = "Rozne", Name = "Różne" },
}

-- ── Zapis danych (DataStore) ─────────────────────────
Config.SaveEnabled = true
Config.DataStoreName = "EQ_RP_v1"          -- zmień nazwę, żeby zresetować wszystkim ekwipunek
Config.AutosaveInterval = 120              -- sekundy

-- ── Przedmioty startowe (dla nowych graczy) ──────────
Config.StarterItems = {
	{ Id = "Telefon", Amount = 1 },
	{ Id = "Chleb", Amount = 2 },
	{ Id = "Woda", Amount = 2 },
}

-- ── Podnoszenie / wyrzucanie / podawanie ─────────────
Config.PickupHoldTime = 0.3
Config.PickupDistance = 8
Config.DropLifetime = 300                  -- po ilu sekundach znika wyrzucony przedmiot
Config.GiveDistance = 8                    -- z jakiej odległości można podać przedmiot graczowi
Config.DemoPickups = true                  -- przedmioty testowe przy spawnie (WYŁĄCZ przed premierą)

-- ── Walka (bronie bez własnych skryptów) ─────────────
Config.PvP = true

-- ── Komendy na czacie (do testów) ────────────────────
-- !daj <Id> [ilość]   !wyczysc   !lista
-- Działają w Studio, dla twórcy gry i dla UserId poniżej.
Config.Admins = {
	-- 123456789,
}

-- ── Rzadkości (kolor paska pod przedmiotem) ──────────
Config.Rarities = {
	Common    = { Name = "ZWYKŁY",     Color = Color3.fromRGB(235, 235, 240), Order = 1 },
	Uncommon  = { Name = "NIEZWYKŁY",  Color = Color3.fromRGB(110, 215, 140), Order = 2 },
	Rare      = { Name = "RZADKI",     Color = Color3.fromRGB(110, 160, 255), Order = 3 },
	Epic      = { Name = "EPICKI",     Color = Color3.fromRGB(185, 120, 255), Order = 4 },
	Legendary = { Name = "LEGENDARNY", Color = Color3.fromRGB(245, 200, 80),  Order = 5 },
}

-- ── Wygląd (biały / szklany styl) ────────────────────
Config.Theme = {
	Panel = Color3.fromRGB(22, 22, 25),
	PanelTransparency = 0.08,
	Text = Color3.fromRGB(246, 246, 248),
	SubText = Color3.fromRGB(175, 175, 184),
	Muted = Color3.fromRGB(125, 125, 134),
	Glass = Color3.fromRGB(255, 255, 255),   -- karty / pola (z dużą przezroczystością)
	Primary = Color3.fromRGB(255, 255, 255), -- przycisk "Użyj"
	PrimaryText = Color3.fromRGB(18, 18, 20),
	Secondary = Color3.fromRGB(58, 58, 64),  -- przycisk "Wyrzuć"
	Danger = Color3.fromRGB(240, 95, 95),
	Good = Color3.fromRGB(120, 220, 150),
}

return Config
