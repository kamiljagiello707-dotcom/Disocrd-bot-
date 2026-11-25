--[[
	TABLET KIEROWNIKA PSP  |  Strona gracza
	Tablet otwiera się, gdy gracz weźmie do ręki Tool o nazwie z Ustawienia.NazwaTabletu.
]]

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")

local gracz = Players.LocalPlayer
local folderWspolny = ReplicatedStorage:WaitForChild("TabletPSP")
local Ustawienia = require(folderWspolny:WaitForChild("Ustawienia"))
local zdarzenia = folderWspolny:WaitForChild("Zdarzenia")
local remoteZadanie = zdarzenia:WaitForChild("Zadanie")
local remotePowiadomienie = zdarzenia:WaitForChild("Powiadomienie")

local W = require(script:WaitForChild("Wyglad"))
local Tablet = require(script:WaitForChild("Tablet"))
local K = W.KOLOR
local AKCENT = Ustawienia.KolorAkcentu or K.Czerwony

local ekran = W.nowy("ScreenGui", {
	Name = "TabletPSP",
	ResetOnSpawn = false,
	IgnoreGuiInset = true,
	ZIndexBehavior = Enum.ZIndexBehavior.Sibling,
	DisplayOrder = 28,
	Parent = gracz:WaitForChild("PlayerGui"),
})

-- ============================================================
--  Powiadomienia (styl jak na telefonie)
-- ============================================================

local stos = W.nowy("Frame", {
	BackgroundTransparency = 1,
	AnchorPoint = Vector2.new(0.5, 0),
	Position = UDim2.new(0.5, 0, 0, 16),
	Size = UDim2.fromOffset(440, 400),
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
local function karta(tytul, tresc, kolor, czas)
	numer += 1
	local ramka = W.nowy("CanvasGroup", {
		Size = UDim2.fromOffset(420, 0),
		AutomaticSize = Enum.AutomaticSize.Y,
		BackgroundColor3 = K.Okno,
		BackgroundTransparency = 0.06,
		GroupTransparency = 1,
		LayoutOrder = numer,
		Parent = stos,
	})
	W.zaokraglij(ramka, 16)
	W.obrysuj(ramka, 0.86)
	W.nowy("UIPadding", {
		PaddingTop = UDim.new(0, 12),
		PaddingBottom = UDim.new(0, 12),
		PaddingLeft = UDim.new(0, 14),
		PaddingRight = UDim.new(0, 14),
		Parent = ramka,
	})
	local znak = W.nowy("Frame", {
		Size = UDim2.fromOffset(34, 34),
		BackgroundColor3 = kolor,
		Parent = ramka,
	})
	W.zaokraglij(znak, 9)
	W.napis({
		Text = "PSP",
		FontFace = W.czcionka(Enum.FontWeight.Heavy),
		TextSize = 11,
		TextColor3 = Color3.new(1, 1, 1),
		TextXAlignment = Enum.TextXAlignment.Center,
		Size = UDim2.fromScale(1, 1),
		Parent = znak,
	})
	W.napis({
		Text = tytul,
		FontFace = W.czcionka(Enum.FontWeight.Bold),
		TextSize = 13,
		Position = UDim2.fromOffset(46, 0),
		Size = UDim2.new(1, -46, 0, 16),
		Parent = ramka,
	})
	W.napis({
		Text = tresc,
		TextSize = 13,
		TextColor3 = K.TekstSlaby,
		TextWrapped = true,
		AutomaticSize = Enum.AutomaticSize.Y,
		Position = UDim2.fromOffset(46, 18),
		Size = UDim2.new(1, -46, 0, 16),
		Parent = ramka,
	})
	W.animuj(ramka, { GroupTransparency = 0 }, TweenInfo.new(0.25))
	task.delay(czas or 4, function()
		W.animuj(ramka, { GroupTransparency = 1 }, TweenInfo.new(0.4)).Completed:Wait()
		ramka:Destroy()
	end)
end

local function powiadom(tresc, rodzaj)
	local kolor = rodzaj == "blad" and K.Czerwony or (rodzaj == "sukces" and Color3.fromRGB(46, 160, 90) or AKCENT)
	karta("Tablet PSP", tresc, kolor, 4)
end

remotePowiadomienie.OnClientEvent:Connect(function(rodzaj, dane)
	if rodzaj == "Komunikat" and typeof(dane) == "table" then
		karta("Komunikat od: " .. tostring(dane.Autor), tostring(dane.Tekst), AKCENT, 9)
	elseif rodzaj == "Info" then
		karta(Ustawienia.NazwaJednostki or "PSP", tostring(dane), AKCENT, 6)
	end
end)

-- ============================================================
--  Tablet
-- ============================================================

local function odloz()
	local postac = gracz.Character
	local humanoid = postac and postac:FindFirstChildOfClass("Humanoid")
	if humanoid then
		humanoid:UnequipTools()
	end
	Tablet.Zamknij()
end

Tablet.Inicjuj(ekran, {
	Ustawienia = Ustawienia,
	Powiadom = powiadom,
	Odloz = odloz,
	Zadanie = function(akcja, a, b)
		local ok, wynik, wiadomosc, dane = pcall(function()
			return remoteZadanie:InvokeServer(akcja, a, b)
		end)
		if not ok then
			return false, "Brak połączenia z serwerem.", nil
		end
		return wynik, wiadomosc, dane
	end,
})

local function czyTablet(obiekt)
	return obiekt:IsA("Tool") and obiekt.Name == Ustawienia.NazwaTabletu
end

local function podlaczPostac(postac)
	Tablet.Zamknij()
	postac.ChildAdded:Connect(function(obiekt)
		if czyTablet(obiekt) then
			Tablet.Otworz()
		end
	end)
	postac.ChildRemoved:Connect(function(obiekt)
		if czyTablet(obiekt) then
			Tablet.Zamknij()
		end
	end)
	for _, obiekt in postac:GetChildren() do
		if czyTablet(obiekt) then
			Tablet.Otworz()
		end
	end
end

gracz.CharacterAdded:Connect(podlaczPostac)
if gracz.Character then
	podlaczPostac(gracz.Character)
end
