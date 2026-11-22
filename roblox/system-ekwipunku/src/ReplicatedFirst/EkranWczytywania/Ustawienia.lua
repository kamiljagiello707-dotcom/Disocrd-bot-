--[[
	EKRAN WCZYTYWANIA  |  USTAWIENIA
	Zmień nazwę gry, obrazki, muzykę i porady. Kodu niżej nie trzeba ruszać.
]]

local Ustawienia = {}

-- ============================================================
--  NAPISY
-- ============================================================

Ustawienia.NazwaGry = "MIASTO RP"
Ustawienia.Podtytul = "Polski serwer Roleplay"
Ustawienia.Wersja = "v1.0"

-- ============================================================
--  OBRAZKI I MUZYKA  ("" = brak)
-- ============================================================

-- Logo nad nazwą gry, np. "rbxassetid://1234567890"
Ustawienia.Logo = ""

-- Obraz w tle (np. zdjęcie mapy). Bez niego jest ciemne tło z bańkami.
Ustawienia.TloObraz = ""

-- Muzyka w trakcie wczytywania, np. "rbxassetid://1234567890"
Ustawienia.Muzyka = ""
Ustawienia.GlosnoscMuzyki = 0.4

-- Kolor dodatków (napis PORADA, kropka statusu). Czerwony jak OSP.
Ustawienia.KolorAkcentu = Color3.fromRGB(235, 75, 65)

-- ============================================================
--  ZACHOWANIE
-- ============================================================

-- Ekran trwa co najmniej tyle sekund (żeby pasek nie mignął)
Ustawienia.MinimalnyCzas = 4

-- Po tylu sekundach ekran kończy wczytywanie, nawet jeśli coś jeszcze się ładuje
Ustawienia.MaksymalnyCzas = 30

-- Po tylu sekundach pojawia się przycisk "Pomiń" (0 = nigdy)
Ustawienia.PominPo = 8

-- true = na końcu gracz klika GRAJ; false = ekran znika sam
Ustawienia.PrzyciskGraj = true

-- Usługi, których obrazki, dźwięki i modele mają się wczytać przed grą
Ustawienia.Wczytaj = { "Workspace", "ReplicatedStorage", "Lighting", "StarterGui" }

-- Elementy interfejsu Roblox ukryte na czas wczytywania.
-- Jeśli gra sama wyłącza któryś z nich (np. własny pasek zdrowia), usuń go stąd.
Ustawienia.UkryjInterfejs = { "PlayerList", "Chat", "Health", "EmotesMenu" }

-- ============================================================
--  PORADY  (zmieniają się na dole ekranu)
-- ============================================================

Ustawienia.Porady = {
	"Naciśnij X, aby otworzyć ekwipunek.",
	"Grasz postacią: zachowuj się tak, jak ona zachowałaby się naprawdę.",
	"Sprzęt służbowy dostajesz po dołączeniu do służby, np. do OSP.",
	"Zasady serwera znajdziesz na naszym Discordzie.",
	"Nie wiesz, co robić? Zapytaj innych graczy na czacie.",
}

-- Co ile sekund zmienia się porada
Ustawienia.ZmianaPoradyCo = 5

return Ustawienia
