--[[
	PRZYKŁAD DO TESTÓW (można usunąć): stanowisko, przy którym gracz
	zostaje kierownikiem PSP albo wraca do rangi Obywatel.
	Kierownik dostaje Tablet PSP przez ekwipunek (folder rangi "Kierownik PSP").
]]

local ServerScriptService = game:GetService("ServerScriptService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local Rangi = require(ServerScriptService:WaitForChild("EkwipunekSerwer"):WaitForChild("Rangi"))
local Konfiguracja = require(ReplicatedStorage:WaitForChild("Ekwipunek"):WaitForChild("Konfiguracja"))

local RANGA = "Kierownik PSP"
local zapytanie = script.Parent:WaitForChild("ProximityPrompt")

zapytanie.Triggered:Connect(function(gracz)
	if Rangi.PobierzRange(gracz) == RANGA then
		Rangi.UstawRange(gracz, Konfiguracja.DomyslnaRanga)
	else
		Rangi.UstawRange(gracz, RANGA)
	end
end)
