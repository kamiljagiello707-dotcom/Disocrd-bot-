--[[
	SYSTEM WEZWAŃ 112  |  Rekwizyty
	Modele na miejscu zdarzenia budowane z części (bez modeli z zewnątrz).
	Pozycje są względem miejsca wezwania: X w poprzek drogi, Z wzdłuż drogi
	(ujemne Z = przód miejsca), Y = wysokość nad ziemią.
]]

local Players = game:GetService("Players")
local TweenService = game:GetService("TweenService")

local Rekwizyty = {}

local KORA = Color3.fromRGB(96, 68, 45)
local LISCIE = Color3.fromRGB(62, 112, 52)
local SKORY = {
	Color3.fromRGB(234, 184, 146),
	Color3.fromRGB(204, 142, 105),
	Color3.fromRGB(160, 110, 80),
	Color3.fromRGB(245, 205, 170),
}
local UBRANIA = {
	Color3.fromRGB(45, 70, 120),
	Color3.fromRGB(120, 40, 40),
	Color3.fromRGB(60, 90, 60),
	Color3.fromRGB(70, 70, 75),
	Color3.fromRGB(150, 120, 80),
}
local AUTA = {
	Color3.fromRGB(40, 60, 110),
	Color3.fromRGB(150, 150, 155),
	Color3.fromRGB(30, 30, 34),
	Color3.fromRGB(120, 25, 30),
	Color3.fromRGB(235, 235, 235),
}

local function czesc(rodzic, nazwa, rozmiar, polozenie, kolor, material, opcje)
	local nowa = Instance.new("Part")
	nowa.Name = nazwa
	nowa.Size = rozmiar
	nowa.CFrame = polozenie
	nowa.Color = kolor
	nowa.Material = material or Enum.Material.SmoothPlastic
	nowa.Anchored = true
	nowa.TopSurface = Enum.SurfaceType.Smooth
	nowa.BottomSurface = Enum.SurfaceType.Smooth
	for klucz, wartosc in opcje or {} do
		nowa[klucz] = wartosc
	end
	nowa.Parent = rodzic
	return nowa
end
Rekwizyty.Czesc = czesc

local function losuj(lista)
	return lista[math.random(#lista)]
end

local function model(rodzic, nazwa)
	local nowy = Instance.new("Model")
	nowy.Name = nazwa
	nowy.Parent = rodzic
	return nowy
end

-- Płynnie przesuwa model (PivotTo) w czasie
function Rekwizyty.Przesun(obiekt, cel, czas)
	local wartosc = Instance.new("CFrameValue")
	wartosc.Value = obiekt:GetPivot()
	wartosc.Changed:Connect(function(polozenie)
		if obiekt.Parent then
			obiekt:PivotTo(polozenie)
		end
	end)
	local tween = TweenService:Create(wartosc, TweenInfo.new(czas or 1, Enum.EasingStyle.Quad, Enum.EasingDirection.InOut), {
		Value = cel,
	})
	tween:Play()
	tween.Completed:Connect(function()
		wartosc:Destroy()
	end)
	return tween
end

-- ============================================================
--  Ludzie (prawdziwy awatar R6, a gdy się nie uda: prosta postać z klocków)
-- ============================================================

local function prostaOsoba(nazwa, skora, ubranie)
	local postac = Instance.new("Model")
	postac.Name = nazwa
	local korzen = czesc(postac, "HumanoidRootPart", Vector3.new(2, 2, 1), CFrame.new(0, 3, 0), skora, nil, { Transparency = 1 })
	czesc(postac, "Torso", Vector3.new(2, 2, 1), CFrame.new(0, 3, 0), ubranie)
	czesc(postac, "Head", Vector3.new(1.2, 1.2, 1.2), CFrame.new(0, 4.6, 0), skora)
	czesc(postac, "Left Arm", Vector3.new(1, 2, 1), CFrame.new(-1.5, 3, 0), skora)
	czesc(postac, "Right Arm", Vector3.new(1, 2, 1), CFrame.new(1.5, 3, 0), skora)
	czesc(postac, "Left Leg", Vector3.new(1, 2, 1), CFrame.new(-0.5, 1, 0), Color3.fromRGB(40, 40, 46))
	czesc(postac, "Right Leg", Vector3.new(1, 2, 1), CFrame.new(0.5, 1, 0), Color3.fromRGB(40, 40, 46))
	postac.PrimaryPart = korzen
	return postac, korzen
end

-- poza: "lezy" albo "stoi". Zwraca model i HumanoidRootPart.
function Rekwizyty.Osoba(rodzic, polozenie, poza, nazwa)
	local skora, ubranie = losuj(SKORY), losuj(UBRANIA)
	local opis = Instance.new("HumanoidDescription")
	opis.HeadColor = skora
	opis.LeftArmColor = skora
	opis.RightArmColor = skora
	opis.TorsoColor = ubranie
	opis.LeftLegColor = Color3.fromRGB(40, 40, 46)
	opis.RightLegColor = Color3.fromRGB(40, 40, 46)

	local ok, postac = pcall(function()
		return Players:CreateHumanoidModelFromDescription(opis, Enum.HumanoidRigType.R6)
	end)
	local korzen
	if ok and postac and postac:FindFirstChild("HumanoidRootPart") then
		korzen = postac.HumanoidRootPart
		local humanoid = postac:FindFirstChildOfClass("Humanoid")
		if humanoid then
			humanoid.DisplayDistanceType = Enum.HumanoidDisplayDistanceType.None
		end
	else
		postac, korzen = prostaOsoba(nazwa or "Osoba", skora, ubranie)
	end
	postac.Name = nazwa or "Osoba"
	-- Prawdziwy awatar trzyma się na stawach (zakotwiczony tylko korzeń),
	-- postać z klocków nie ma stawów, więc zostaje cała zakotwiczona
	local maStawy = postac:FindFirstChildWhichIsA("Motor6D", true) ~= nil
	for _, potomek in postac:GetDescendants() do
		if potomek:IsA("BasePart") then
			potomek.Anchored = not maStawy or potomek == korzen
			potomek.CanCollide = false
		end
	end
	if poza == "lezy" then
		postac:PivotTo(polozenie * CFrame.new(0, 0.6, 0) * CFrame.Angles(math.rad(-90), 0, 0))
	else
		postac:PivotTo(polozenie * CFrame.new(0, 3, 0))
	end
	postac.Parent = rodzic
	return postac, korzen
end

-- ============================================================
--  Drzewa
-- ============================================================

-- Drzewo leżące w poprzek drogi: 3 kawałki pnia + korona (gałęzie)
function Rekwizyty.PowaloneDrzewo(w)
	local drzewo = model(w.Model, "PowaloneDrzewo")
	local segmenty, galezie = {}, {}
	for indeks, x in { -9, -1, 7 } do
		table.insert(
			segmenty,
			czesc(
				drzewo,
				"Pien" .. indeks,
				Vector3.new(8, 2.6, 2.6),
				w:Pozycja(x, 1.3, 0) * CFrame.Angles(0, math.rad(3 * indeks), 0),
				KORA,
				Enum.Material.Wood,
				{ Shape = Enum.PartType.Cylinder }
			)
		)
	end
	czesc(drzewo, "Karpa", Vector3.new(1.4, 6, 6), w:Pozycja(-13.6, 3, 0), Color3.fromRGB(85, 62, 42), Enum.Material.Ground, {
		Shape = Enum.PartType.Cylinder,
	})
	czesc(drzewo, "Konar", Vector3.new(6, 0.8, 0.8), w:Pozycja(13, 1.6, 1.6) * CFrame.Angles(0, math.rad(35), 0), KORA, Enum.Material.Wood, {
		Shape = Enum.PartType.Cylinder,
	})
	for indeks, dane in { { 13, 3.2, 0, 7 }, { 16.5, 2.6, 3, 6 }, { 15.5, 3, -3.2, 6 }, { 19, 2.2, 0.5, 5 } } do
		table.insert(
			galezie,
			czesc(
				drzewo,
				"Galezie" .. indeks,
				Vector3.new(dane[4], dane[4], dane[4]),
				w:Pozycja(dane[1], dane[2], dane[3]),
				LISCIE,
				Enum.Material.Grass,
				{ Shape = Enum.PartType.Ball }
			)
		)
	end
	return segmenty, galezie
end

-- Stojące drzewo; zwraca model, położenie gałęzi i niewidoczną podstawę (do E)
function Rekwizyty.DrzewoStojace(rodzic, polozenie)
	local drzewo = model(rodzic, "Drzewo")
	czesc(drzewo, "Pien", Vector3.new(16, 2.2, 2.2), polozenie * CFrame.new(0, 8, 0) * CFrame.Angles(0, 0, math.rad(90)), KORA, Enum.Material.Wood, {
		Shape = Enum.PartType.Cylinder,
	})
	czesc(drzewo, "Galaz", Vector3.new(5, 0.8, 0.8), polozenie * CFrame.new(2.5, 12, 0), KORA, Enum.Material.Wood, {
		Shape = Enum.PartType.Cylinder,
	})
	for indeks, dane in { { 0, 17, 0, 10 }, { 3.5, 15.5, 2, 7 }, { -3.5, 16, -1.5, 7 }, { 1, 20.5, 0, 7 } } do
		czesc(drzewo, "Korona" .. indeks, Vector3.new(dane[4], dane[4], dane[4]), polozenie * CFrame.new(dane[1], dane[2], dane[3]), LISCIE, Enum.Material.Grass, {
			Shape = Enum.PartType.Ball,
		})
	end
	local podstawa = czesc(drzewo, "Podstawa", Vector3.new(3, 5, 3), polozenie * CFrame.new(0, 2.5, 0), KORA, nil, {
		Transparency = 1,
		CanCollide = false,
	})
	return { Model = drzewo, Galaz = polozenie * CFrame.new(4, 12.4, 0), Podstawa = podstawa }
end

-- ============================================================
--  Samochód (rozbity = po wypadku)
-- ============================================================

function Rekwizyty.Samochod(rodzic, polozenie, rozbity)
	local auto = model(rodzic, "Samochod")
	local kolor = losuj(AUTA)
	local szklo = Color3.fromRGB(35, 40, 48)
	local nadwozie = czesc(auto, "Nadwozie", Vector3.new(6, 2, 12), polozenie * CFrame.new(0, 1.9, 0), kolor)
	local kabina = czesc(auto, "Kabina", Vector3.new(5.6, 1.9, 6), polozenie * CFrame.new(0, 3.85, 0.6), kolor)
	czesc(auto, "SzybaPrzod", Vector3.new(5.2, 1.6, 0.2), polozenie * CFrame.new(0, 3.85, -2.45), szklo, Enum.Material.Glass, { Transparency = 0.25 })
	czesc(auto, "SzybaTyl", Vector3.new(5.2, 1.6, 0.2), polozenie * CFrame.new(0, 3.85, 3.65), szklo, Enum.Material.Glass, { Transparency = 0.25 })
	czesc(auto, "SzybaLewa", Vector3.new(0.2, 1.4, 5), polozenie * CFrame.new(-2.85, 3.9, 0.6), szklo, Enum.Material.Glass, { Transparency = 0.25 })
	czesc(auto, "SzybaPrawa", Vector3.new(0.2, 1.4, 5), polozenie * CFrame.new(2.85, 3.9, 0.6), szklo, Enum.Material.Glass, { Transparency = 0.25 })
	local maska = czesc(auto, "Maska", Vector3.new(5.8, 0.3, 3.4), polozenie * CFrame.new(0, 3.05, -4.2), kolor)
	local drzwi = czesc(auto, "Drzwi", Vector3.new(0.3, 1.9, 3.2), polozenie * CFrame.new(-3.15, 2.4, -0.4), kolor:Lerp(Color3.new(0, 0, 0), 0.15))
	for _, x in { -2.2, 2.2 } do
		czesc(auto, "Swiatlo", Vector3.new(1.2, 0.5, 0.2), polozenie * CFrame.new(x, 2.3, -6.05), Color3.fromRGB(255, 250, 230), Enum.Material.Neon)
		czesc(auto, "SwiatloTyl", Vector3.new(1.2, 0.5, 0.2), polozenie * CFrame.new(x, 2.3, 6.05), Color3.fromRGB(200, 20, 20), Enum.Material.Neon)
	end
	for _, x in { -3, 3 } do
		for _, z in { -4, 4 } do
			czesc(auto, "Kolo", Vector3.new(1, 2.4, 2.4), polozenie * CFrame.new(x, 1.2, z), Color3.fromRGB(25, 25, 28), nil, {
				Shape = Enum.PartType.Cylinder,
			})
		end
	end
	if rozbity then
		czesc(auto, "Zgniecenie", Vector3.new(6.2, 1.6, 1.4), polozenie * CFrame.new(0, 1.8, -6.2) * CFrame.Angles(math.rad(12), math.rad(8), 0), kolor:Lerp(Color3.new(0, 0, 0), 0.4), Enum.Material.CorrodedMetal)
		for numer = 1, 6 do
			czesc(auto, "Szklo", Vector3.new(0.5, 0.05, 0.4), polozenie * CFrame.new(math.random(-30, 30) / 10, 0.05, -7 - math.random(0, 25) / 10) * CFrame.Angles(0, math.rad(numer * 47), 0), Color3.fromRGB(200, 220, 230), Enum.Material.Glass, {
				Transparency = 0.4,
				CanCollide = false,
			})
		end
		local dym = Instance.new("Smoke")
		dym.Color = Color3.fromRGB(120, 120, 125)
		dym.Opacity = 0.25
		dym.RiseVelocity = 4
		dym.Size = 3
		dym.Parent = maska
	end
	auto.PrimaryPart = nadwozie
	return { Model = auto, Nadwozie = nadwozie, Kabina = kabina, Maska = maska, Drzwi = drzwi }
end

-- Zwęglenie samochodu po pożarze
function Rekwizyty.Zwegl(auto)
	for _, potomek in auto:GetDescendants() do
		if potomek:IsA("BasePart") and potomek.Material ~= Enum.Material.Glass and potomek.Name ~= "Kolo" then
			potomek.Color = Color3.fromRGB(38, 36, 34)
			potomek.Material = Enum.Material.Slate
		end
	end
end

-- ============================================================
--  Plamy, żar, dym
-- ============================================================

function Rekwizyty.Plama(rodzic, polozenie, srednica)
	return czesc(
		rodzic,
		"Plama",
		Vector3.new(0.12, srednica, srednica),
		polozenie * CFrame.new(0, 0.06, 0) * CFrame.Angles(0, 0, math.rad(90)),
		Color3.fromRGB(18, 18, 22),
		Enum.Material.SmoothPlastic,
		{ Shape = Enum.PartType.Cylinder, Reflectance = 0.35, CanCollide = false }
	)
end

function Rekwizyty.Zar(rodzic, pozycja)
	local zar = czesc(rodzic, "Zar", Vector3.new(3, 0.3, 3), CFrame.new(pozycja + Vector3.new(0, 0.15, 0)), Color3.fromRGB(255, 90, 30), Enum.Material.Neon, {
		Transparency = 0.25,
		CanCollide = false,
	})
	local dym = Instance.new("Smoke")
	dym.Color = Color3.fromRGB(90, 90, 95)
	dym.Opacity = 0.15
	dym.RiseVelocity = 3
	dym.Size = 2
	dym.Parent = zar
	return zar
end

function Rekwizyty.KolumnaDymu(rodzic, polozenie)
	local zrodlo = czesc(rodzic, "KolumnaDymu", Vector3.new(1, 1, 1), polozenie, Color3.new(0, 0, 0), nil, {
		Transparency = 1,
		CanCollide = false,
		CanTouch = false,
		CanQuery = false,
	})
	local dym = Instance.new("Smoke")
	dym.Color = Color3.fromRGB(45, 45, 48)
	dym.Opacity = 0.5
	dym.RiseVelocity = 15
	dym.Size = 22
	dym.Parent = zrodlo
	return zrodlo
end

-- ============================================================
--  Kot, gniazdo szerszeni, słupki
-- ============================================================

function Rekwizyty.Kot(rodzic, polozenie)
	local kot = model(rodzic, "Kot")
	local futro = Color3.fromRGB(230, 140, 50)
	local cialo = czesc(kot, "Cialo", Vector3.new(0.9, 0.9, 1.6), polozenie, futro, Enum.Material.Fabric, { CanCollide = false })
	czesc(kot, "Glowa", Vector3.new(0.9, 0.8, 0.8), polozenie * CFrame.new(0, 0.45, -0.95), futro, Enum.Material.Fabric, { CanCollide = false })
	czesc(kot, "UchoL", Vector3.new(0.25, 0.35, 0.15), polozenie * CFrame.new(-0.28, 1, -0.95), futro, nil, { CanCollide = false })
	czesc(kot, "UchoP", Vector3.new(0.25, 0.35, 0.15), polozenie * CFrame.new(0.28, 1, -0.95), futro, nil, { CanCollide = false })
	czesc(kot, "Ogon", Vector3.new(0.22, 0.22, 1.3), polozenie * CFrame.new(0, 0.5, 1.2) * CFrame.Angles(math.rad(40), 0, 0), futro, Enum.Material.Fabric, { CanCollide = false })
	kot.PrimaryPart = cialo
	return kot
end

function Rekwizyty.Gniazdo(rodzic, polozenie)
	local gniazdo = czesc(rodzic, "Gniazdo", Vector3.new(2.6, 3.2, 2.6), polozenie, Color3.fromRGB(165, 125, 75), Enum.Material.Sand, {
		Shape = Enum.PartType.Ball,
		CanCollide = false,
	})
	local osy = Instance.new("ParticleEmitter")
	osy.Name = "Szerszenie"
	osy.Color = ColorSequence.new(Color3.fromRGB(20, 20, 20))
	osy.Size = NumberSequence.new(0.18)
	osy.Rate = 25
	osy.Lifetime = NumberRange.new(1, 2)
	osy.Speed = NumberRange.new(2, 4)
	osy.SpreadAngle = Vector2.new(180, 180)
	osy.Parent = gniazdo
	return gniazdo
end

function Rekwizyty.Slupki(w, punkty)
	local slupki = {}
	for indeks, punkt in punkty do
		table.insert(
			slupki,
			czesc(w.Model, "Slupek" .. indeks, Vector3.new(0.4, 3.5, 0.4), w:Pozycja(punkt[1], 1.75, punkt[2]), Color3.fromRGB(235, 235, 235))
		)
	end
	return slupki
end

-- ============================================================
--  Podtopienie: woda, pompa, skrzynka elektryczna
-- ============================================================

function Rekwizyty.Podtopienie(w)
	local woda = czesc(w.Model, "Woda", Vector3.new(22, 1.2, 22), w:Pozycja(0, 0.6, 0), Color3.fromRGB(60, 120, 190), Enum.Material.Glass, {
		Transparency = 0.35,
		CanCollide = false,
	})
	local pompa = czesc(w.Model, "Pompa", Vector3.new(3, 2, 2.4), w:Pozycja(14, 1, 0), Color3.fromRGB(200, 40, 40))
	czesc(w.Model, "Silnik", Vector3.new(1.6, 1, 1.6), w:Pozycja(14, 2.5, 0), Color3.fromRGB(35, 35, 38))
	czesc(w.Model, "Waz", Vector3.new(6, 0.5, 0.5), w:Pozycja(10, 0.3, 0), Color3.fromRGB(230, 230, 230), Enum.Material.Fabric, {
		Shape = Enum.PartType.Cylinder,
		CanCollide = false,
	})
	czesc(w.Model, "Slup", Vector3.new(0.4, 4, 0.4), w:Pozycja(-13, 2, 7), Color3.fromRGB(110, 110, 115))
	local skrzynka = czesc(w.Model, "Skrzynka", Vector3.new(1.6, 2.2, 0.8), w:Pozycja(-13, 3.4, 6.5), Color3.fromRGB(150, 150, 155))
	czesc(w.Model, "Ostrzezenie", Vector3.new(0.8, 0.8, 0.05), w:Pozycja(-13, 3.6, 6.07), Color3.fromRGB(245, 200, 40), Enum.Material.Neon)
	return woda, pompa, skrzynka
end

return Rekwizyty
