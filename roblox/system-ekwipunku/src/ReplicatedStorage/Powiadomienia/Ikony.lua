--[[
	SYSTEM POWIADOMIEŃ  |  Ikony liniowe z ramek GUI (28x28, bez obrazków)
	Nazwy: info, ok, uwaga, blad, drzwi, auto, szukaj, klucz, skrzynka, osoba, telefon
]]

local Ikony = {}

local CZCIONKA = "rbxasset://fonts/families/GothamSSm.json"

local function kontur(k, c, x, y, szerokosc, wysokosc, promien)
	local ramka = Instance.new("Frame")
	ramka.BackgroundTransparency = 1
	ramka.Position = UDim2.fromOffset(x, y)
	ramka.Size = UDim2.fromOffset(szerokosc, wysokosc)
	local obrys = Instance.new("UIStroke")
	obrys.Color = c
	obrys.Thickness = 2
	obrys.Parent = ramka
	if promien and promien > 0 then
		local naroznik = Instance.new("UICorner")
		naroznik.CornerRadius = UDim.new(0, promien)
		naroznik.Parent = ramka
	end
	ramka.Parent = k
	return ramka
end

local function pelny(k, c, x, y, szerokosc, wysokosc, promien, obrot)
	local ramka = Instance.new("Frame")
	ramka.BackgroundColor3 = c
	ramka.BorderSizePixel = 0
	ramka.Position = UDim2.fromOffset(x, y)
	ramka.Size = UDim2.fromOffset(szerokosc, wysokosc)
	ramka.Rotation = obrot or 0
	if promien and promien > 0 then
		local naroznik = Instance.new("UICorner")
		naroznik.CornerRadius = UDim.new(0, promien)
		naroznik.Parent = ramka
	end
	ramka.Parent = k
	return ramka
end

local function znak(k, c, tekst, rozmiar)
	local napis = Instance.new("TextLabel")
	napis.BackgroundTransparency = 1
	napis.Size = UDim2.fromScale(1, 1)
	napis.Text = tekst
	napis.TextColor3 = c
	napis.TextSize = rozmiar or 15
	napis.FontFace = Font.new(CZCIONKA, Enum.FontWeight.Heavy)
	napis.Parent = k
end

local RYSUNKI = {
	info = function(k, c)
		kontur(k, c, 3, 3, 22, 22, 11)
		znak(k, c, "i", 14)
	end,
	ok = function(k, c)
		kontur(k, c, 3, 3, 22, 22, 11)
		pelny(k, c, 8, 15, 6, 2, 1, 45)
		pelny(k, c, 11, 12, 11, 2, 1, -50)
	end,
	uwaga = function(k, c)
		kontur(k, c, 3, 3, 22, 22, 11)
		znak(k, c, "!", 15)
	end,
	blad = function(k, c)
		kontur(k, c, 3, 3, 22, 22, 11)
		pelny(k, c, 8, 13, 12, 2, 1, 45)
		pelny(k, c, 8, 13, 12, 2, 1, -45)
	end,
	drzwi = function(k, c)
		kontur(k, c, 7, 3, 14, 22, 2)
		pelny(k, c, 16, 13, 3, 3, 2)
		pelny(k, c, 4, 24, 20, 2, 1)
	end,
	auto = function(k, c)
		kontur(k, c, 2, 12, 24, 8, 3)
		kontur(k, c, 7, 6, 13, 7, 3)
		pelny(k, c, 5, 19, 6, 6, 3)
		pelny(k, c, 17, 19, 6, 6, 3)
	end,
	szukaj = function(k, c)
		kontur(k, c, 4, 4, 14, 14, 7)
		pelny(k, c, 15, 19, 9, 3, 1, 45)
	end,
	klucz = function(k, c)
		kontur(k, c, 3, 9, 10, 10, 5)
		pelny(k, c, 12, 13, 13, 2, 1)
		pelny(k, c, 20, 15, 2, 4, 1)
		pelny(k, c, 23, 15, 2, 3, 1)
	end,
	skrzynka = function(k, c)
		kontur(k, c, 3, 9, 22, 15, 2)
		kontur(k, c, 2, 5, 24, 5, 2)
		pelny(k, c, 12, 13, 4, 2, 1)
	end,
	osoba = function(k, c)
		kontur(k, c, 9, 3, 10, 10, 5)
		kontur(k, c, 5, 16, 18, 9, 6)
	end,
	telefon = function(k, c)
		kontur(k, c, 8, 2, 12, 24, 3)
		pelny(k, c, 12, 21, 4, 2, 1)
	end,
}

function Ikony.Istnieje(nazwa)
	return RYSUNKI[nazwa] ~= nil
end

function Ikony.rysuj(nazwa, rodzic, kolor, rozmiar)
	local kontener = Instance.new("Frame")
	kontener.Name = "Ikona"
	kontener.BackgroundTransparency = 1
	kontener.Size = UDim2.fromOffset(28, 28)
	kontener.AnchorPoint = Vector2.new(0.5, 0.5)
	kontener.Position = UDim2.fromScale(0.5, 0.5)
	local rysunek = RYSUNKI[nazwa] or RYSUNKI.info
	rysunek(kontener, kolor or Color3.new(1, 1, 1))
	if rozmiar and rozmiar ~= 28 then
		local skala = Instance.new("UIScale")
		skala.Scale = rozmiar / 28
		skala.Parent = kontener
	end
	kontener.Parent = rodzic
	return kontener
end

return Ikony
