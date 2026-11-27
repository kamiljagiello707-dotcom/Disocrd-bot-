--[[
	PRZYKŁAD (można usunąć): śmietnik do przeszukania (przytrzymaj E).
	Pasek ładowania w trakcie robi atrybut PowiadomienieTrwa na ProximityPrompt,
	a wynik wysyła ten skrypt przez API.
]]

local ReplicatedStorage = game:GetService("ReplicatedStorage")

local Powiadomienia = require(ReplicatedStorage:WaitForChild("Powiadomienia"))

local ZNALEZISKA = { "Pusta puszka", "Stara gazeta", "Klucz do roweru", "Latarka", "Moneta 2 zł", "Rękawiczki robocze" }
local ODNOWIENIE = 60 -- sekundy, po których śmietnik znowu ma coś w środku

local smietnik = script.Parent
local zapytanie = smietnik:WaitForChild("ProximityPrompt")
local ostatnio = -math.huge

zapytanie.Triggered:Connect(function(gracz)
	Powiadomienia.WyslijWPoblizu(smietnik.Position, 30, {
		Tytul = "Śmietnik",
		Tekst = "Ktoś przeszukuje śmietnik obok.",
		Typ = "info",
		Ikona = "szukaj",
		Czas = 3,
	}, gracz)

	if os.clock() - ostatnio < ODNOWIENIE then
		Powiadomienia.Wyslij(gracz, {
			Tytul = "Pusto",
			Tekst = "Ktoś niedawno przeszukał ten śmietnik.",
			Typ = "ostrzezenie",
			Ikona = "skrzynka",
		})
		return
	end
	ostatnio = os.clock()
	if math.random() < 0.65 then
		Powiadomienia.Wyslij(gracz, {
			Tytul = "Znaleziono przedmiot",
			Tekst = ZNALEZISKA[math.random(#ZNALEZISKA)],
			Typ = "sukces",
			Ikona = "szukaj",
		})
	else
		Powiadomienia.Wyslij(gracz, {
			Tytul = "Nic tu nie ma",
			Tekst = "Śmietnik jest pusty.",
			Typ = "info",
			Ikona = "skrzynka",
		})
	end
end)
