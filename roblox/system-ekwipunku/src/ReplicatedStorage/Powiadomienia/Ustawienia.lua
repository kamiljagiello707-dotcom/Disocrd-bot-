--[[
	SYSTEM POWIADOMIEŃ  |  USTAWIENIA
]]

local Ustawienia = {}

-- Gdzie pokazują się powiadomienia: "PrawaGora" albo "PrawyDol"
Ustawienia.Pozycja = "PrawaGora"
-- Odstęp od góry ekranu (pod paskiem Roblox). Jeśli w grze jest widoczna lista
-- graczy (PlayerList), zwiększ np. do 220 albo ustaw Pozycja = "PrawyDol".
Ustawienia.OdstepOdGory = 64

Ustawienia.Szerokosc = 340 -- szerokość karty w pikselach
Ustawienia.MaksNaEkranie = 4 -- ile kart naraz (starsze znikają)
Ustawienia.CzasDomyslny = 5 -- ile sekund karta jest widoczna

-- Kolory typów powiadomień
Ustawienia.Typy = {
	info = Color3.fromRGB(125, 165, 255),
	sukces = Color3.fromRGB(95, 215, 135),
	ostrzezenie = Color3.fromRGB(245, 165, 70),
	blad = Color3.fromRGB(240, 80, 65),
	akcja = Color3.fromRGB(235, 235, 240),
}

-- Ikona dla typu, gdy powiadomienie nie poda własnej
Ustawienia.IkonyTypow = {
	info = "info",
	sukces = "ok",
	ostrzezenie = "uwaga",
	blad = "blad",
	akcja = "info",
}

-- Dźwięki ("" = cisza)
Ustawienia.Dzwieki = {
	info = "",
	sukces = "",
	ostrzezenie = "rbxasset://sounds/electronicpingshort.wav",
	blad = "rbxasset://sounds/electronicpingshort.wav",
	akcja = "",
}
Ustawienia.Glosnosc = 0.4

-- Powiadomienie przy wsiadaniu do pojazdu (VehicleSeat) i wysiadaniu
Ustawienia.PowiadomOPojazdach = true

-- Domyślny zasięg (w studach) powiadomień "w pobliżu" z atrybutów ProximityPrompt
Ustawienia.ZasiegWPoblizu = 40

return Ustawienia
