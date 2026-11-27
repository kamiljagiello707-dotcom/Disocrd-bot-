--[[
	PRZYKŁAD (można usunąć): drzwi samochodu otwierane przez E.
	Pokazuje użycie API: powiadomienie dla gracza i dla osób w pobliżu.
	Atrybut KatOtwarcia na drzwiach: kąt i kierunek otwarcia (np. -70 albo 70).
]]

local ReplicatedStorage = game:GetService("ReplicatedStorage")
local TweenService = game:GetService("TweenService")

local Powiadomienia = require(ReplicatedStorage:WaitForChild("Powiadomienia"))

local drzwi = script.Parent
local zapytanie = drzwi:WaitForChild("ProximityPrompt")
local pojazd = drzwi:FindFirstAncestorOfClass("Model")
local nazwaPojazdu = pojazd and pojazd.Name or "Pojazd"

local zamkniete = drzwi.CFrame
local zawias = zamkniete * CFrame.new(0, 0, -drzwi.Size.Z / 2)
local kat = drzwi:GetAttribute("KatOtwarcia") or 70
local otwarte = false

zapytanie.Triggered:Connect(function(gracz)
	otwarte = not otwarte
	local cel = otwarte and (zawias * CFrame.Angles(0, math.rad(kat), 0) * CFrame.new(0, 0, drzwi.Size.Z / 2)) or zamkniete
	TweenService:Create(drzwi, TweenInfo.new(0.45, Enum.EasingStyle.Quad, Enum.EasingDirection.Out), { CFrame = cel }):Play()
	zapytanie.ActionText = otwarte and "Zamknij drzwi" or "Otwórz drzwi"

	Powiadomienia.Wyslij(gracz, {
		Tytul = otwarte and "Drzwi otwarte" or "Drzwi zamknięte",
		Tekst = "Pojazd: " .. nazwaPojazdu,
		Typ = "akcja",
		Ikona = "auto",
		Czas = 3,
	})
	Powiadomienia.WyslijWPoblizu(drzwi.Position, 40, {
		Tytul = nazwaPojazdu,
		Tekst = otwarte and "Ktoś otworzył drzwi pojazdu w pobliżu." or "Ktoś zamknął drzwi pojazdu w pobliżu.",
		Typ = "info",
		Ikona = "drzwi",
		Czas = 3,
	}, gracz)
end)
