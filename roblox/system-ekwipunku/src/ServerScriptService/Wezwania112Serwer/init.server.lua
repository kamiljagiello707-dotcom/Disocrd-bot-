--[[
	SYSTEM WEZWAŃ 112  |  Start serwera.
	Wezwania = silnik, Rekwizyty = modele na miejscu zdarzenia, Rodzaje = 10 rodzajów wezwań.
	Ustawienia: ReplicatedStorage > Wezwania112 > Ustawienia
]]

local Wezwania = require(script:WaitForChild("Wezwania"))
local Rekwizyty = require(script:WaitForChild("Rekwizyty"))
local Rodzaje = require(script:WaitForChild("Rodzaje"))

Rodzaje.Zarejestruj(Wezwania, Rekwizyty)
Wezwania.Start()
