-- ItemData (ModuleScript)
-- Miejsce: ReplicatedStorage > EQShared > ItemData
-- Lista przedmiotów. Klucz (np. "Chleb") to ID przedmiotu.
--
-- Pola:
--   Name        nazwa w grze
--   Description opis
--   Category    Jedzenie / Medyczne / Narzedzia / Rozne   (zakładki z EQConfig)
--   Rarity      Common / Uncommon / Rare / Epic / Legendary
--   Type        "Consumable" (do zjedzenia / użycia), "Tool" (do trzymania w ręce), "Item" (zwykły)
--   Weight      waga 1 sztuki w kg
--   MaxStack    ile sztuk w jednym stosie
--   Icon        "rbxassetid://ID" – biała ikona (patrz INSTRUKCJA, folder ikony/)
--   Emoji       zapasowa ikonka, gdy Icon jest puste
--
-- Consumable:  Heal (HP), SpeedBoost, JumpBoost, Duration (s)
-- Tool:        ToolName (Tool w ServerStorage > EQTools), Damage, Cooldown, Range,
--              Light = true (latarka – klik włącza/wyłącza światło)

return {
	-- ── Jedzenie ──
	Chleb = {
		Name = "Chleb", Description = "Świeży bochenek. Zaspokaja głód.",
		Category = "Jedzenie", Rarity = "Common", Type = "Consumable",
		Weight = 0.5, MaxStack = 10, Heal = 10, Emoji = "🍞", Icon = "",
	},
	Woda = {
		Name = "Woda", Description = "Butelka czystej wody.",
		Category = "Jedzenie", Rarity = "Common", Type = "Consumable",
		Weight = 0.5, MaxStack = 10, Heal = 5, Emoji = "💧", Icon = "",
	},
	Pizza = {
		Name = "Pizza", Description = "Kawałek gorącej pizzy.",
		Category = "Jedzenie", Rarity = "Common", Type = "Consumable",
		Weight = 0.4, MaxStack = 8, Heal = 20, Emoji = "🍕", Icon = "",
	},
	Kawa = {
		Name = "Kawa", Description = "Mocna kawa. Na chwilę dodaje energii.",
		Category = "Jedzenie", Rarity = "Uncommon", Type = "Consumable",
		Weight = 0.3, MaxStack = 5, SpeedBoost = 6, Duration = 20, Emoji = "☕", Icon = "",
	},

	-- ── Medyczne ──
	Apteczka = {
		Name = "Apteczka", Description = "Przywraca sporo zdrowia.",
		Category = "Medyczne", Rarity = "Rare", Type = "Consumable",
		Weight = 2.0, MaxStack = 3, Heal = 75, Emoji = "🩹", Icon = "",
	},
	Bandaz = {
		Name = "Bandaż", Description = "Zatrzymuje krwawienie. Leczy trochę zdrowia.",
		Category = "Medyczne", Rarity = "Common", Type = "Consumable",
		Weight = 0.2, MaxStack = 10, Heal = 20, Emoji = "🩹", Icon = "",
	},
	Tabletki = {
		Name = "Tabletki przeciwbólowe", Description = "Szybka ulga w bólu.",
		Category = "Medyczne", Rarity = "Uncommon", Type = "Consumable",
		Weight = 0.1, MaxStack = 10, Heal = 15, Emoji = "💊", Icon = "",
	},

	-- ── Narzędzia ──
	Latarka = {
		Name = "Latarka", Description = "Oświetla drogę w ciemności. Kliknij, aby włączyć.",
		Category = "Narzedzia", Rarity = "Common", Type = "Tool",
		Weight = 0.5, MaxStack = 1, Light = true, Emoji = "🔦", Icon = "",
	},
	Telefon = {
		Name = "Telefon", Description = "Twój smartfon. Zawsze pod ręką.",
		Category = "Narzedzia", Rarity = "Rare", Type = "Tool",
		Weight = 0.3, MaxStack = 1, Emoji = "📱", Icon = "",
	},
	Klucze = {
		Name = "Klucze", Description = "Pęk kluczy. Do czego pasują?",
		Category = "Narzedzia", Rarity = "Common", Type = "Item",
		Weight = 0.1, MaxStack = 5, Emoji = "🔑", Icon = "",
	},
	Kij = {
		Name = "Kij bejsbolowy", Description = "Do sportu. Oczywiście.",
		Category = "Narzedzia", Rarity = "Uncommon", Type = "Tool",
		Weight = 1.5, MaxStack = 1, Damage = 15, Cooldown = 0.8, Range = 6,
		Color = Color3.fromRGB(150, 110, 70), Emoji = "🏏", Icon = "",
	},

	-- ── Różne ──
	SztabkaZlota = {
		Name = "Sztabka złota", Description = "Ciężka i bardzo cenna.",
		Category = "Rozne", Rarity = "Legendary", Type = "Item",
		Weight = 1.0, MaxStack = 10, Emoji = "🪙", Icon = "",
	},
	Portfel = {
		Name = "Portfel", Description = "Dokumenty i trochę gotówki.",
		Category = "Rozne", Rarity = "Common", Type = "Item",
		Weight = 0.2, MaxStack = 1, Emoji = "👛", Icon = "",
	},
	Zegarek = {
		Name = "Zegarek", Description = "Elegancki zegarek na rękę.",
		Category = "Rozne", Rarity = "Epic", Type = "Item",
		Weight = 0.1, MaxStack = 1, Emoji = "⌚", Icon = "",
	},
}
