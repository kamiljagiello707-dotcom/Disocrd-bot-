--[[
	SYSTEM ALARMOWANIA OSP  |  Strona gracza
	- Panel: okno dyspozytora (komputer w remizie albo komenda /alarm)
	- Pager: powiadomienie o alarmie u strażaków + znacznik miejsca zdarzenia
]]

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local gracz = Players.LocalPlayer
local folderWspolny = ReplicatedStorage:WaitForChild("AlarmOSP")
local Ustawienia = require(folderWspolny:WaitForChild("Ustawienia"))
local zdarzenia = folderWspolny:WaitForChild("Zdarzenia")
local remoteZdarzenie = zdarzenia:WaitForChild("Zdarzenie")
local remoteWyslij = zdarzenia:WaitForChild("Wyslij")

local W = require(script:WaitForChild("Wyglad"))
local Panel = require(script:WaitForChild("Panel"))
local Pager = require(script:WaitForChild("Pager"))

local ekran = W.nowy("ScreenGui", {
	Name = "AlarmOSP",
	ResetOnSpawn = false,
	IgnoreGuiInset = true,
	ZIndexBehavior = Enum.ZIndexBehavior.Sibling,
	DisplayOrder = 30,
	Parent = gracz:WaitForChild("PlayerGui"),
})

-- ============================================================
--  Krótkie powiadomienia na górze ekranu
-- ============================================================

local stos = W.nowy("Frame", {
	BackgroundTransparency = 1,
	AnchorPoint = Vector2.new(0.5, 0),
	Position = UDim2.new(0.5, 0, 0, 112),
	Size = UDim2.fromOffset(640, 200),
	ZIndex = 10,
	Parent = ekran,
})
W.nowy("UIListLayout", {
	Padding = UDim.new(0, 8),
	HorizontalAlignment = Enum.HorizontalAlignment.Center,
	SortOrder = Enum.SortOrder.LayoutOrder,
	Parent = stos,
})

local numer = 0
local function powiadom(tresc, rodzaj)
	numer += 1
	local kolor = W.KOLOR.Bialy
	if rodzaj == "blad" then
		kolor = W.KOLOR.Czerwony
	elseif rodzaj == "sukces" then
		kolor = W.KOLOR.Zielony
	end
	local dymek = W.nowy("CanvasGroup", {
		AutomaticSize = Enum.AutomaticSize.X,
		Size = UDim2.fromOffset(0, 38),
		BackgroundColor3 = W.KOLOR.Okno,
		BackgroundTransparency = 0.08,
		GroupTransparency = 1,
		LayoutOrder = numer,
		Parent = stos,
	})
	W.zaokraglij(dymek, 10)
	W.obrysuj(dymek, 0.86)
	W.nowy("UIPadding", { PaddingLeft = UDim.new(0, 14), PaddingRight = UDim.new(0, 16), Parent = dymek })
	W.nowy("UIListLayout", {
		FillDirection = Enum.FillDirection.Horizontal,
		VerticalAlignment = Enum.VerticalAlignment.Center,
		Padding = UDim.new(0, 10),
		SortOrder = Enum.SortOrder.LayoutOrder,
		Parent = dymek,
	})
	local kropka = W.nowy("Frame", {
		Size = UDim2.fromOffset(8, 8),
		BackgroundColor3 = kolor,
		BorderSizePixel = 0,
		LayoutOrder = 1,
		Parent = dymek,
	})
	W.zaokraglij(kropka, 4)
	W.napis({
		Text = tresc,
		FontFace = W.czcionka(Enum.FontWeight.Medium),
		TextSize = 13,
		AutomaticSize = Enum.AutomaticSize.X,
		Size = UDim2.fromOffset(0, 38),
		LayoutOrder = 2,
		Parent = dymek,
	})
	W.animuj(dymek, { GroupTransparency = 0 }, TweenInfo.new(0.25))
	task.delay(4, function()
		W.animuj(dymek, { GroupTransparency = 1 }, TweenInfo.new(0.4)).Completed:Wait()
		dymek:Destroy()
	end)
end

-- Gdy w grze jest ekran wczytywania, alarm pokaże się dopiero po nim
local function czekajNaWczytanie()
	local poczatek = os.clock()
	while gracz:GetAttribute("EkranWczytywania") == true and os.clock() - poczatek < 90 do
		task.wait(0.25)
	end
end

local kontekst = {
	Ustawienia = Ustawienia,
	Powiadom = powiadom,
	Wyslij = function(dane)
		local ok, wynik, tresc = pcall(function()
			return remoteWyslij:InvokeServer(dane)
		end)
		if not ok then
			return false, "Brak połączenia z serwerem."
		end
		return wynik, tresc
	end,
	Zdarzenie = function(...)
		remoteZdarzenie:FireServer(...)
	end,
}

Panel.Inicjuj(ekran, kontekst)
Pager.Inicjuj(ekran, kontekst)

remoteZdarzenie.OnClientEvent:Connect(function(akcja, a, b)
	if akcja == "Panel" then
		Panel.Otworz(a)
	elseif akcja == "Dane" then
		Panel.Dane(a)
	elseif akcja == "Aktywne" then
		Panel.Aktywne(a)
	elseif akcja == "Nowy" then
		task.spawn(function()
			czekajNaWczytanie()
			Pager.Nowy(a)
		end)
	elseif akcja == "Aktualizacja" then
		Pager.Aktualizuj(a)
	elseif akcja == "Koniec" then
		Pager.Koniec(a)
	elseif akcja == "Komunikat" then
		powiadom(a, b)
	end
end)
