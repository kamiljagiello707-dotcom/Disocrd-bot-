--[[
	SYSTEM POWIADOMIEŃ  |  Start serwera (zdarzenie + powiadomienia z atrybutów ProximityPrompt).
	Ustawienia: ReplicatedStorage > Powiadomienia > Ustawienia
]]

local ReplicatedStorage = game:GetService("ReplicatedStorage")

require(ReplicatedStorage:WaitForChild("Powiadomienia")).StartSerwer()
