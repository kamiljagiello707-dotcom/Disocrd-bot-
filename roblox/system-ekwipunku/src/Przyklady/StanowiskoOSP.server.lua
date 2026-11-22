--[[
	PRZYKŁAD (można usunąć): stanowisko zapisu do OSP.
	Gracz podchodzi, przytrzymuje E i dołącza do OSP albo z niej odchodzi.

	Jeśli gra ma już swój sposób przyjmowania do OSP (np. whitelistę),
	wystarczy w tamtym skrypcie (na serwerze) wywołać:
		require(game.ServerScriptService.EkwipunekSerwer.Rangi).UstawRange(gracz, "OSP")
]]

local ServerScriptService = game:GetService("ServerScriptService")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local Rangi = require(ServerScriptService:WaitForChild("EkwipunekSerwer"):WaitForChild("Rangi"))
local Konfiguracja = require(ReplicatedStorage:WaitForChild("Ekwipunek"):WaitForChild("Konfiguracja"))

local RANGA = "OSP"
local zapytanie = script.Parent:WaitForChild("ProximityPrompt")

zapytanie.Triggered:Connect(function(gracz)
	if Rangi.PobierzRange(gracz) == RANGA then
		Rangi.UstawRange(gracz, Konfiguracja.DomyslnaRanga)
	else
		Rangi.UstawRange(gracz, RANGA)
	end
end)
