-- ItemData (ModuleScript)
-- Miejsce: ReplicatedStorage > EQShared > ItemData
-- Lista wszystkich przedmiotów. Klucz (np. "Miecz") to ID przedmiotu.
--
-- Pola:
--   Name        nazwa widoczna w grze
--   Description opis
--   Rarity      Common / Uncommon / Rare / Epic / Legendary / Mythic
--   Type        "Tool" (do trzymania w ręce), "Consumable" (do zjedzenia/wypicia), "Material"
--   Emoji       ikonka, gdy nie ma obrazka
--   Icon        (opcjonalnie) "rbxassetid://123456" – obrazek zamiast emoji
--   MaxStack    ile sztuk mieści się w jednym slocie (narzędzia: 1)
--
-- Tool:
--   ToolName    (opcjonalnie) nazwa Toola w ServerStorage > EQTools. Brak = bot zrobi prosty model sam
--   Damage      obrażenia (tylko dla automatycznego modelu)
--   Cooldown    przerwa między ciosami (s)
--   Range       zasięg ciosu (studs)
--   Color       kolor automatycznego modelu
--
-- Consumable:
--   Heal        ile HP leczy
--   SpeedBoost  + do szybkości chodzenia
--   JumpBoost   + do wysokości skoku
--   Duration    ile sekund trwa boost

return {
	-- ── Bronie / narzędzia ──
	Miecz = {
		Name = "Żelazny miecz",
		Description = "Solidny miecz dla każdego wojownika.",
		Rarity = "Common",
		Type = "Tool",
		Emoji = "🗡️",
		MaxStack = 1,
		Damage = 15,
		Cooldown = 0.6,
		Range = 6,
		Color = Color3.fromRGB(190, 196, 206),
	},
	OgnistyMiecz = {
		Name = "Ognisty miecz",
		Description = "Ostrze wykute w sercu wulkanu. Pali wszystko, czego dotknie.",
		Rarity = "Legendary",
		Type = "Tool",
		Emoji = "🔥",
		MaxStack = 1,
		Damage = 40,
		Cooldown = 0.5,
		Range = 7,
		Color = Color3.fromRGB(255, 110, 30),
	},
	Kilof = {
		Name = "Kilof",
		Description = "Przydatny do kopania. Można też nim przywalić.",
		Rarity = "Uncommon",
		Type = "Tool",
		Emoji = "⛏️",
		MaxStack = 1,
		Damage = 8,
		Cooldown = 0.8,
		Range = 5,
		Color = Color3.fromRGB(120, 90, 60),
	},

	-- ── Do użycia ──
	Jablko = {
		Name = "Jabłko",
		Description = "Soczyste jabłko. Leczy trochę zdrowia.",
		Rarity = "Common",
		Type = "Consumable",
		Emoji = "🍎",
		MaxStack = 16,
		Heal = 20,
	},
	MiksturaLeczenia = {
		Name = "Mikstura leczenia",
		Description = "Czerwony eliksir, który szybko stawia na nogi.",
		Rarity = "Rare",
		Type = "Consumable",
		Emoji = "🧪",
		MaxStack = 5,
		Heal = 60,
	},
	MiksturaSzybkosci = {
		Name = "Mikstura szybkości",
		Description = "Po wypiciu biegasz jak błyskawica.",
		Rarity = "Epic",
		Type = "Consumable",
		Emoji = "⚡",
		MaxStack = 5,
		SpeedBoost = 12,
		Duration = 15,
	},
	MiksturaSkoku = {
		Name = "Mikstura skoku",
		Description = "Nogi jak sprężyny.",
		Rarity = "Uncommon",
		Type = "Consumable",
		Emoji = "🦘",
		MaxStack = 5,
		JumpBoost = 15,
		Duration = 15,
	},

	-- ── Materiały ──
	Drewno = {
		Name = "Drewno",
		Description = "Podstawowy materiał budowlany.",
		Rarity = "Common",
		Type = "Material",
		Emoji = "🪵",
		MaxStack = 64,
	},
	Kamien = {
		Name = "Kamień",
		Description = "Twardy i ciężki.",
		Rarity = "Common",
		Type = "Material",
		Emoji = "🪨",
		MaxStack = 64,
	},
	Zloto = {
		Name = "Sztabka złota",
		Description = "Błyszczy się. Cenna.",
		Rarity = "Rare",
		Type = "Material",
		Emoji = "🪙",
		MaxStack = 64,
	},
	Diament = {
		Name = "Diament",
		Description = "Bardzo rzadki kamień szlachetny.",
		Rarity = "Epic",
		Type = "Material",
		Emoji = "💎",
		MaxStack = 32,
	},
	Gwiazda = {
		Name = "Odłamek gwiazdy",
		Description = "Spadł z nieba. Nikt nie wie, do czego służy...",
		Rarity = "Mythic",
		Type = "Material",
		Emoji = "🌟",
		MaxStack = 16,
	},
}
