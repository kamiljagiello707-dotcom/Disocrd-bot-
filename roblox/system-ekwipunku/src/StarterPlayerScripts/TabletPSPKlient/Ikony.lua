--[[
	TABLET KIEROWNIKA PSP  |  Ikony
	Proste ikony liniowe z ramek GUI (bez obrazków i emoji), 28x28.
	Ikony.rysuj("brama", rodzic, kolor, rozmiar) - rozmiar w pikselach (domyślnie 28)
]]

local Ikony = {}

local function kontur(kontener, kolor, x, y, szerokosc, wysokosc, promien)
	local ramka = Instance.new("Frame")
	ramka.BackgroundTransparency = 1
	ramka.Position = UDim2.fromOffset(x, y)
	ramka.Size = UDim2.fromOffset(szerokosc, wysokosc)
	local obrys = Instance.new("UIStroke")
	obrys.Color = kolor
	obrys.Thickness = 2
	obrys.Parent = ramka
	if promien and promien > 0 then
		local naroznik = Instance.new("UICorner")
		naroznik.CornerRadius = UDim.new(0, promien)
		naroznik.Parent = ramka
	end
	ramka.Parent = kontener
	return ramka
end

local function pelny(kontener, kolor, x, y, szerokosc, wysokosc, promien, obrot)
	local ramka = Instance.new("Frame")
	ramka.BackgroundColor3 = kolor
	ramka.BorderSizePixel = 0
	ramka.Position = UDim2.fromOffset(x, y)
	ramka.Size = UDim2.fromOffset(szerokosc, wysokosc)
	ramka.Rotation = obrot or 0
	if promien and promien > 0 then
		local naroznik = Instance.new("UICorner")
		naroznik.CornerRadius = UDim.new(0, promien)
		naroznik.Parent = ramka
	end
	ramka.Parent = kontener
	return ramka
end

local RYSUNKI = {
	-- Brama garażowa: rama i listwy
	brama = function(k, c)
		kontur(k, c, 3, 5, 22, 19, 3)
		pelny(k, c, 7, 10, 14, 2, 1)
		pelny(k, c, 7, 14, 14, 2, 1)
		pelny(k, c, 7, 18, 14, 2, 1)
	end,
	-- Syrena: kopuła, podstawa i promienie
	syrena = function(k, c)
		kontur(k, c, 8, 9, 12, 12, 6)
		pelny(k, c, 5, 20, 18, 3, 1)
		pelny(k, c, 13, 2, 2, 5, 1)
		pelny(k, c, 4, 6, 2, 5, 1, -40)
		pelny(k, c, 22, 6, 2, 5, 1, 40)
	end,
	-- Załoga: dwie postacie
	zaloga = function(k, c)
		kontur(k, c, 6, 4, 8, 8, 4)
		kontur(k, c, 3, 15, 14, 9, 5)
		kontur(k, c, 17, 6, 6, 6, 3)
		kontur(k, c, 18, 15, 8, 8, 4)
	end,
	-- Wóz strażacki: skrzynia, kabina, koła
	pojazd = function(k, c)
		kontur(k, c, 2, 8, 15, 11, 2)
		kontur(k, c, 17, 11, 8, 8, 2)
		pelny(k, c, 5, 19, 6, 6, 3)
		pelny(k, c, 17, 19, 6, 6, 3)
		pelny(k, c, 4, 5, 9, 2, 1)
	end,
	-- Komunikat: dymek z kropkami
	komunikat = function(k, c)
		kontur(k, c, 3, 5, 22, 15, 5)
		pelny(k, c, 7, 18, 5, 5, 1, 45)
		pelny(k, c, 8, 11, 3, 3, 2)
		pelny(k, c, 13, 11, 3, 3, 2)
		pelny(k, c, 18, 11, 3, 3, 2)
	end,
	-- Żarówka
	swiatlo = function(k, c)
		kontur(k, c, 7, 3, 14, 14, 7)
		pelny(k, c, 10, 18, 8, 3, 1)
		pelny(k, c, 11, 22, 6, 2, 1)
	end,
	-- Strzałka w prawo (chevron)
	dalej = function(k, c)
		pelny(k, c, 10, 8, 10, 2, 1, 45)
		pelny(k, c, 10, 18, 10, 2, 1, -45)
	end,
}

function Ikony.rysuj(nazwa, rodzic, kolor, rozmiar)
	local kontener = Instance.new("Frame")
	kontener.Name = "Ikona"
	kontener.BackgroundTransparency = 1
	kontener.Size = UDim2.fromOffset(28, 28)
	kontener.AnchorPoint = Vector2.new(0.5, 0.5)
	kontener.Position = UDim2.fromScale(0.5, 0.5)
	local rysunek = RYSUNKI[nazwa]
	if rysunek then
		rysunek(kontener, kolor or Color3.new(1, 1, 1))
	end
	if rozmiar and rozmiar ~= 28 then
		local skala = Instance.new("UIScale")
		skala.Scale = rozmiar / 28
		skala.Parent = kontener
	end
	kontener.Parent = rodzic
	return kontener
end

return Ikony
