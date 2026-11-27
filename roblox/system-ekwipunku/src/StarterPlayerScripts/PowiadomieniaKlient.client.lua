--[[
	SYSTEM POWIADOMIEŃ  |  Start u gracza (karty w prawym górnym rogu).
]]

local ReplicatedStorage = game:GetService("ReplicatedStorage")

require(ReplicatedStorage:WaitForChild("Powiadomienia")).StartKlient()
