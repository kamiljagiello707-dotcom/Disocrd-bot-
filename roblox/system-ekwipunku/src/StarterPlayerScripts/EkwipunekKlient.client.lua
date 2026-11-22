--[[
	SYSTEM EKWIPUNKU  |  Okno ekwipunku (strona gracza)

	Otwieranie: klawisz z Konfiguracja.Klawisz (domyślnie X),
	na telefonie przycisk z plecakiem po lewej stronie ekranu.
	Przedmioty to zwykłe Toole z plecaka gracza, więc ich własne
	skrypty i animacje działają tak samo jak z paska Roblox.
]]

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local UserInputService = game:GetService("UserInputService")
local TweenService = game:GetService("TweenService")
local RunService = game:GetService("RunService")
local StarterGui = game:GetService("StarterGui")
local Lighting = game:GetService("Lighting")

local gracz = Players.LocalPlayer
local folderWspolny = ReplicatedStorage:WaitForChild("Ekwipunek")
local Konfiguracja = require(folderWspolny:WaitForChild("Konfiguracja"))
local Przedmioty = require(folderWspolny:WaitForChild("Przedmioty"))
local zdarzenia = folderWspolny:WaitForChild("Zdarzenia")
local remoteAkcja = zdarzenia:WaitForChild("Akcja")
local remotePowiadomienie = zdarzenia:WaitForChild("Powiadomienie")

-- ============================================================
--  Wygląd
-- ============================================================

local KOLOR = {
	Okno = Color3.fromRGB(24, 24, 27),
	Panel = Color3.fromRGB(33, 33, 37),
	Slot = Color3.fromRGB(43, 43, 48),
	SlotNajechany = Color3.fromRGB(54, 54, 60),
	SlotWybrany = Color3.fromRGB(70, 70, 78),
	Bialy = Color3.fromRGB(245, 245, 247),
	Tekst = Color3.fromRGB(236, 236, 240),
	TekstSlaby = Color3.fromRGB(150, 150, 160),
	TekstCiemny = Color3.fromRGB(22, 22, 25),
	Zielony = Color3.fromRGB(95, 215, 135),
	Czerwony = Color3.fromRGB(240, 85, 75),
	Pomaranczowy = Color3.fromRGB(245, 165, 70),
}

local CZCIONKA = "rbxasset://fonts/families/GothamSSm.json"
local function czcionka(waga)
	return Font.new(CZCIONKA, waga or Enum.FontWeight.Regular)
end

local SZEROKOSC, WYSOKOSC = 724, 500
local MARGINES = 22
local KOMORKA = 78
local ODSTEP = 8
local SZEROKOSC_SIATKI = 436
local SZEROKOSC_SZCZEGOLOW = SZEROKOSC - MARGINES * 2 - SZEROKOSC_SIATKI - 18
local GORA_SIATKI, WYSOKOSC_SIATKI = 140, 290
local POLE_WIDZENIA = 30

local SZYBKO = TweenInfo.new(0.12, Enum.EasingStyle.Quad, Enum.EasingDirection.Out)

local POLSKIE = {
	{ "ą", "Ą" }, { "ć", "Ć" }, { "ę", "Ę" }, { "ł", "Ł" }, { "ń", "Ń" },
	{ "ó", "Ó" }, { "ś", "Ś" }, { "ź", "Ź" }, { "ż", "Ż" },
}

local function duze(tekst)
	tekst = string.upper(tekst)
	for _, para in POLSKIE do
		tekst = string.gsub(tekst, para[1], para[2])
	end
	return tekst
end

local function male(tekst)
	tekst = string.lower(tekst)
	for _, para in POLSKIE do
		tekst = string.gsub(tekst, para[2], para[1])
	end
	return tekst
end

local function hex(kolor)
	return "#" .. kolor:ToHex()
end

-- ============================================================
--  Tworzenie elementów
-- ============================================================

local function nowy(klasa, wlasciwosci)
	local obiekt = Instance.new(klasa)
	local rodzic
	for klucz, wartosc in wlasciwosci or {} do
		if klucz == "Parent" then
			rodzic = wartosc
		else
			obiekt[klucz] = wartosc
		end
	end
	if rodzic then
		obiekt.Parent = rodzic
	end
	return obiekt
end

local function zaokraglij(rodzic, promien)
	return nowy("UICorner", { CornerRadius = UDim.new(0, promien), Parent = rodzic })
end

local function obrysuj(rodzic, przezroczystosc, kolor)
	return nowy("UIStroke", {
		Color = kolor or Color3.new(1, 1, 1),
		Transparency = przezroczystosc or 0.9,
		Thickness = 1,
		ApplyStrokeMode = Enum.ApplyStrokeMode.Border,
		Parent = rodzic,
	})
end

local function napis(wlasciwosci)
	local pelne = {
		BackgroundTransparency = 1,
		TextColor3 = KOLOR.Tekst,
		FontFace = czcionka(),
		TextSize = 14,
		TextXAlignment = Enum.TextXAlignment.Left,
	}
	for klucz, wartosc in wlasciwosci do
		pelne[klucz] = wartosc
	end
	return nowy("TextLabel", pelne)
end

local function przycisk(wlasciwosci)
	local pelne = {
		AutoButtonColor = false,
		BorderSizePixel = 0,
		FontFace = czcionka(Enum.FontWeight.Bold),
		TextSize = 14,
	}
	for klucz, wartosc in wlasciwosci do
		pelne[klucz] = wartosc
	end
	return nowy("TextButton", pelne)
end

local function animuj(obiekt, wlasciwosci, info)
	local tween = TweenService:Create(obiekt, info or SZYBKO, wlasciwosci)
	tween:Play()
	return tween
end

-- ============================================================
--  Podgląd 3D przedmiotów
-- ============================================================

local NIEPOTRZEBNE = {
	"LuaSourceContainer", "Sound", "ParticleEmitter", "Beam", "Trail", "Fire", "Smoke",
	"Sparkles", "Light", "LayerCollector", "BodyMover", "ProximityPrompt", "ClickDetector",
	"RemoteEvent", "RemoteFunction", "BindableEvent", "BindableFunction", "ValueBase",
}

local szablony = {} -- [nazwa Toola] = { Model, Widok } albo false

local function granice(model, uklad)
	local minimum, maksimum
	for _, czesc in model:GetDescendants() do
		if czesc:IsA("BasePart") and czesc.Transparency < 0.98 then
			local polowa = czesc.Size / 2
			for _, x in { -1, 1 } do
				for _, y in { -1, 1 } do
					for _, z in { -1, 1 } do
						local rog = czesc.CFrame * Vector3.new(polowa.X * x, polowa.Y * y, polowa.Z * z)
						local lokalny = uklad:PointToObjectSpace(rog)
						minimum = minimum and minimum:Min(lokalny) or lokalny
						maksimum = maksimum and maksimum:Max(lokalny) or lokalny
					end
				end
			end
		end
	end
	return minimum, maksimum
end

-- Ustawia kamerę tak, żeby przedmiot był widoczny w całości, bokiem
local function przygotujWidok(model)
	local uchwyt = model:FindFirstChild("Handle")
	local odniesienie
	if uchwyt and uchwyt:IsA("BasePart") then
		odniesienie = uchwyt.CFrame
	else
		local pierwsza = model:FindFirstChildWhichIsA("BasePart", true)
		odniesienie = pierwsza and pierwsza.CFrame or CFrame.new()
	end

	local minimum, maksimum = granice(model, odniesienie)
	if not minimum then
		return nil
	end
	local rozmiar = maksimum - minimum
	local srodek = odniesienie * ((minimum + maksimum) / 2)

	local osie = {
		{ odniesienie.RightVector, rozmiar.X },
		{ odniesienie.UpVector, rozmiar.Y },
		{ -odniesienie.LookVector, rozmiar.Z },
	}
	table.sort(osie, function(a, b)
		return a[2] > b[2]
	end)
	local dluga, srednia, krotka = osie[1][1], osie[2][1], osie[3][1]
	if srednia.Y < 0 then
		srednia = -srednia
	end
	if krotka:Dot(Vector3.new(0.3, 0.4, 1)) < 0 then
		krotka = -krotka
	end

	local kierunek = (krotka + srednia * 0.35 + dluga * 0.3).Unit
	local promien = math.max(rozmiar.Magnitude / 2, 0.25)
	local odleglosc = promien / math.sin(math.rad(POLE_WIDZENIA / 2))

	return function(kat, zblizenie)
		local obrot = CFrame.fromAxisAngle(srednia, kat)
		local oko = srodek + obrot:VectorToWorldSpace(kierunek) * odleglosc * (zblizenie or 1)
		return CFrame.lookAt(oko, srodek, srednia)
	end
end

local function szablonPodgladu(narzedzie)
	local zapisany = szablony[narzedzie.Name]
	if zapisany ~= nil then
		return zapisany or nil
	end

	local wynik = false
	local archiwalny = narzedzie.Archivable
	narzedzie.Archivable = true
	local ok, kopia = pcall(function()
		return narzedzie:Clone()
	end)
	narzedzie.Archivable = archiwalny

	if ok and kopia then
		-- Usuwamy skrypty, dźwięki i efekty: podgląd ma być tylko modelem
		for _, obiekt in kopia:GetDescendants() do
			for _, klasa in NIEPOTRZEBNE do
				if obiekt:IsA(klasa) then
					obiekt:Destroy()
					break
				end
			end
		end
		local model = Instance.new("Model")
		for _, dziecko in kopia:GetChildren() do
			dziecko.Parent = model
		end
		kopia:Destroy()

		local widok = przygotujWidok(model)
		if widok then
			wynik = { Model = model, Widok = widok }
		else
			model:Destroy()
		end
	end

	szablony[narzedzie.Name] = wynik
	return wynik or nil
end

-- Wstawia ikonę przedmiotu do kontenera. Zwraca dane podglądu 3D (albo nil).
local function ustawIkone(kontener, narzedzie, dane)
	kontener:ClearAllChildren()

	if dane.Ikona and dane.Ikona ~= "" then
		nowy("ImageLabel", {
			BackgroundTransparency = 1,
			Size = UDim2.fromScale(1, 1),
			Image = dane.Ikona,
			ScaleType = Enum.ScaleType.Fit,
			Parent = kontener,
		})
		return nil
	end

	local szablon = narzedzie and szablonPodgladu(narzedzie)
	if szablon then
		local okno = nowy("ViewportFrame", {
			BackgroundTransparency = 1,
			Size = UDim2.fromScale(1, 1),
			Ambient = Color3.fromRGB(175, 175, 180),
			LightColor = Color3.fromRGB(255, 255, 255),
			LightDirection = Vector3.new(-0.4, -1, -0.6),
			Parent = kontener,
		})
		szablon.Model:Clone().Parent = okno
		local kamera = nowy("Camera", { FieldOfView = POLE_WIDZENIA, Parent = okno })
		kamera.CFrame = szablon.Widok(0, 0.9)
		okno.CurrentCamera = kamera
		return { Kamera = kamera, Widok = szablon.Widok, Kat = 0 }
	end

	napis({
		Text = Konfiguracja.IkonyKategorii[dane.Kategoria] or "📦",
		TextScaled = true,
		TextXAlignment = Enum.TextXAlignment.Center,
		Size = UDim2.fromScale(1, 1),
		Parent = kontener,
	})
	return nil
end

-- ============================================================
--  Budowa okna
-- ============================================================

if Konfiguracja.UkryjDomyslnyPlecak then
	task.spawn(function()
		for _ = 1, 10 do
			local ok = pcall(function()
				StarterGui:SetCoreGuiEnabled(Enum.CoreGuiType.Backpack, false)
			end)
			if ok then
				break
			end
			task.wait(0.5)
		end
	end)
end

local ekran = nowy("ScreenGui", {
	Name = "Ekwipunek",
	ResetOnSpawn = false,
	IgnoreGuiInset = true,
	ZIndexBehavior = Enum.ZIndexBehavior.Sibling,
	DisplayOrder = 20,
	Parent = gracz:WaitForChild("PlayerGui"),
})

local rozmycie = nowy("BlurEffect", { Name = "EkwipunekRozmycie", Size = 0, Parent = Lighting })

-- Przyciemnienie tła: kliknięcie obok okna zamyka EQ, Modal odblokowuje myszkę
local tlo = przycisk({
	Name = "Tlo",
	Text = "",
	Modal = true,
	BackgroundColor3 = Color3.new(0, 0, 0),
	BackgroundTransparency = 1,
	Size = UDim2.fromScale(1, 1),
	Visible = false,
	ZIndex = 0,
	Parent = ekran,
})

local okno = nowy("Frame", {
	Name = "Okno",
	ZIndex = 2,
	AnchorPoint = Vector2.new(0.5, 0.5),
	Position = UDim2.fromScale(0.5, 0.5),
	Size = UDim2.fromOffset(SZEROKOSC, WYSOKOSC),
	BackgroundColor3 = KOLOR.Okno,
	BackgroundTransparency = 0.06,
	Visible = false,
	Parent = ekran,
})
zaokraglij(okno, 16)
obrysuj(okno, 0.86)
local skala = nowy("UIScale", { Parent = okno })
-- Okno pochłania kliknięcia, żeby nie zamykało się przez tło
nowy("TextButton", {
	Text = "",
	BackgroundTransparency = 1,
	Size = UDim2.fromScale(1, 1),
	ZIndex = 0,
	AutoButtonColor = false,
	Parent = okno,
})

-- Nagłówek: tytuł + ranga
local naglowek = nowy("Frame", {
	BackgroundTransparency = 1,
	Position = UDim2.fromOffset(MARGINES, 16),
	Size = UDim2.fromOffset(SZEROKOSC_SIATKI, 30),
	Parent = okno,
})
nowy("UIListLayout", {
	FillDirection = Enum.FillDirection.Horizontal,
	VerticalAlignment = Enum.VerticalAlignment.Center,
	Padding = UDim.new(0, 12),
	SortOrder = Enum.SortOrder.LayoutOrder,
	Parent = naglowek,
})
napis({
	Text = "EKWIPUNEK",
	FontFace = czcionka(Enum.FontWeight.Heavy),
	TextSize = 22,
	AutomaticSize = Enum.AutomaticSize.X,
	Size = UDim2.fromOffset(0, 30),
	LayoutOrder = 1,
	Parent = naglowek,
})
local odznaka = nowy("Frame", {
	AutomaticSize = Enum.AutomaticSize.X,
	Size = UDim2.fromOffset(0, 22),
	BackgroundTransparency = 0.85,
	LayoutOrder = 2,
	Parent = naglowek,
})
zaokraglij(odznaka, 11)
local obrysOdznaki = obrysuj(odznaka, 0.5)
nowy("UIPadding", { PaddingLeft = UDim.new(0, 10), PaddingRight = UDim.new(0, 10), Parent = odznaka })
local napisOdznaki = napis({
	FontFace = czcionka(Enum.FontWeight.Bold),
	TextSize = 11,
	AutomaticSize = Enum.AutomaticSize.X,
	Size = UDim2.fromScale(0, 1),
	Parent = odznaka,
})

local zamknijPrzycisk = przycisk({
	Text = "✕",
	TextSize = 16,
	TextColor3 = KOLOR.Tekst,
	AnchorPoint = Vector2.new(1, 0),
	Position = UDim2.new(1, -MARGINES + 4, 0, 14),
	Size = UDim2.fromOffset(34, 34),
	BackgroundColor3 = KOLOR.Slot,
	Parent = okno,
})
zaokraglij(zamknijPrzycisk, 9)
obrysuj(zamknijPrzycisk, 0.88)

-- Wyszukiwarka
local poleSzukaj = nowy("Frame", {
	Position = UDim2.fromOffset(MARGINES, 58),
	Size = UDim2.fromOffset(240, 32),
	BackgroundColor3 = KOLOR.Slot,
	Parent = okno,
})
zaokraglij(poleSzukaj, 8)
obrysuj(poleSzukaj, 0.92)
local szukaj = nowy("TextBox", {
	BackgroundTransparency = 1,
	Position = UDim2.fromOffset(12, 0),
	Size = UDim2.new(1, -24, 1, 0),
	ClearTextOnFocus = false,
	Text = "",
	PlaceholderText = "Szukaj...",
	PlaceholderColor3 = KOLOR.TekstSlaby,
	TextColor3 = KOLOR.Tekst,
	FontFace = czcionka(),
	TextSize = 13,
	TextXAlignment = Enum.TextXAlignment.Left,
	Parent = poleSzukaj,
})
local licznik = napis({
	Position = UDim2.fromOffset(MARGINES + 252, 58),
	Size = UDim2.fromOffset(SZEROKOSC - MARGINES * 2 - 252, 32),
	TextXAlignment = Enum.TextXAlignment.Right,
	TextColor3 = KOLOR.TekstSlaby,
	TextSize = 12,
	Parent = okno,
})

-- Zakładki kategorii
local zakladki = nowy("ScrollingFrame", {
	BackgroundTransparency = 1,
	BorderSizePixel = 0,
	Position = UDim2.fromOffset(MARGINES, 100),
	Size = UDim2.fromOffset(SZEROKOSC - MARGINES * 2, 28),
	CanvasSize = UDim2.new(),
	AutomaticCanvasSize = Enum.AutomaticSize.X,
	ScrollingDirection = Enum.ScrollingDirection.X,
	ScrollBarThickness = 0,
	Parent = okno,
})
nowy("UIListLayout", {
	FillDirection = Enum.FillDirection.Horizontal,
	Padding = UDim.new(0, 6),
	SortOrder = Enum.SortOrder.LayoutOrder,
	Parent = zakladki,
})

-- Siatka przedmiotów
local siatka = nowy("ScrollingFrame", {
	BackgroundTransparency = 1,
	BorderSizePixel = 0,
	Position = UDim2.fromOffset(MARGINES, GORA_SIATKI),
	Size = UDim2.fromOffset(SZEROKOSC_SIATKI, WYSOKOSC_SIATKI),
	CanvasSize = UDim2.new(),
	AutomaticCanvasSize = Enum.AutomaticSize.Y,
	ScrollingDirection = Enum.ScrollingDirection.Y,
	ScrollBarThickness = 4,
	ScrollBarImageColor3 = KOLOR.Bialy,
	ScrollBarImageTransparency = 0.7,
	VerticalScrollBarInset = Enum.ScrollBarInset.Always,
	Parent = okno,
})
nowy("UIPadding", {
	PaddingTop = UDim.new(0, 2),
	PaddingBottom = UDim.new(0, 2),
	PaddingLeft = UDim.new(0, 2),
	PaddingRight = UDim.new(0, 2),
	Parent = siatka,
})
nowy("UIGridLayout", {
	CellSize = UDim2.fromOffset(KOMORKA, KOMORKA),
	CellPadding = UDim2.fromOffset(ODSTEP, ODSTEP),
	SortOrder = Enum.SortOrder.LayoutOrder,
	Parent = siatka,
})

local pusto = nowy("Frame", {
	BackgroundTransparency = 1,
	Position = UDim2.fromOffset(MARGINES, GORA_SIATKI),
	Size = UDim2.fromOffset(SZEROKOSC_SIATKI, WYSOKOSC_SIATKI),
	Visible = false,
	Parent = okno,
})
local pustoTytul = napis({
	Text = "",
	FontFace = czcionka(Enum.FontWeight.Bold),
	TextSize = 16,
	TextXAlignment = Enum.TextXAlignment.Center,
	Position = UDim2.new(0, 0, 0.5, -26),
	Size = UDim2.new(1, 0, 0, 22),
	Parent = pusto,
})
local pustoOpis = napis({
	Text = "",
	TextColor3 = KOLOR.TekstSlaby,
	TextSize = 13,
	TextWrapped = true,
	TextXAlignment = Enum.TextXAlignment.Center,
	TextYAlignment = Enum.TextYAlignment.Top,
	Position = UDim2.new(0, 30, 0.5, 2),
	Size = UDim2.new(1, -60, 0, 40),
	Parent = pusto,
})

-- Panel szczegółów
local szczegoly = nowy("Frame", {
	Position = UDim2.fromOffset(SZEROKOSC - MARGINES - SZEROKOSC_SZCZEGOLOW, GORA_SIATKI),
	Size = UDim2.fromOffset(SZEROKOSC_SZCZEGOLOW, WYSOKOSC_SIATKI),
	BackgroundColor3 = KOLOR.Panel,
	BackgroundTransparency = 0.15,
	Parent = okno,
})
zaokraglij(szczegoly, 12)
obrysuj(szczegoly, 0.9)

local brakWyboru = napis({
	Text = "Wybierz przedmiot",
	TextColor3 = KOLOR.TekstSlaby,
	TextSize = 13,
	TextXAlignment = Enum.TextXAlignment.Center,
	Size = UDim2.fromScale(1, 1),
	Parent = szczegoly,
})

local trescSzczegolow = nowy("Frame", {
	BackgroundTransparency = 1,
	Size = UDim2.fromScale(1, 1),
	Visible = false,
	Parent = szczegoly,
})
local ikonaDuza = nowy("Frame", {
	BackgroundTransparency = 1,
	AnchorPoint = Vector2.new(0.5, 0),
	Position = UDim2.new(0.5, 0, 0, 12),
	Size = UDim2.fromOffset(100, 76),
	Parent = trescSzczegolow,
})
local nazwaSz = napis({
	FontFace = czcionka(Enum.FontWeight.Bold),
	TextScaled = true,
	TextXAlignment = Enum.TextXAlignment.Center,
	Position = UDim2.fromOffset(12, 92),
	Size = UDim2.new(1, -24, 0, 20),
	Parent = trescSzczegolow,
})
nowy("UITextSizeConstraint", { MaxTextSize = 16, MinTextSize = 10, Parent = nazwaSz })
local rzadkoscSz = napis({
	FontFace = czcionka(Enum.FontWeight.Bold),
	TextSize = 11,
	TextXAlignment = Enum.TextXAlignment.Center,
	Position = UDim2.fromOffset(12, 114),
	Size = UDim2.new(1, -24, 0, 13),
	Parent = trescSzczegolow,
})
local opisSz = napis({
	TextColor3 = KOLOR.TekstSlaby,
	TextScaled = true,
	TextWrapped = true,
	TextXAlignment = Enum.TextXAlignment.Center,
	Position = UDim2.fromOffset(14, 131),
	Size = UDim2.new(1, -28, 0, 38),
	Parent = trescSzczegolow,
})
nowy("UITextSizeConstraint", { MaxTextSize = 12, MinTextSize = 9, Parent = opisSz })
local infoSz = napis({
	TextColor3 = KOLOR.TekstSlaby,
	TextSize = 11,
	TextXAlignment = Enum.TextXAlignment.Center,
	Position = UDim2.fromOffset(12, 172),
	Size = UDim2.new(1, -24, 0, 13),
	Parent = trescSzczegolow,
})
local kategoriaSz = napis({
	TextColor3 = KOLOR.TekstSlaby,
	TextSize = 11,
	RichText = true,
	TextXAlignment = Enum.TextXAlignment.Center,
	Position = UDim2.fromOffset(12, 187),
	Size = UDim2.new(1, -24, 0, 13),
	Parent = trescSzczegolow,
})

local przyciskGlowny = przycisk({
	Text = "Weź do ręki",
	TextColor3 = KOLOR.TekstCiemny,
	BackgroundColor3 = KOLOR.Bialy,
	Position = UDim2.fromOffset(12, 206),
	Size = UDim2.new(1, -24, 0, 32),
	Parent = trescSzczegolow,
})
zaokraglij(przyciskGlowny, 8)

local przyciskWyrzuc = przycisk({
	Text = "Wyrzuć",
	TextColor3 = KOLOR.Tekst,
	BackgroundColor3 = KOLOR.SlotNajechany,
	Position = UDim2.fromOffset(12, 246),
	Size = UDim2.new(1, -24, 0, 32),
	Parent = trescSzczegolow,
})
zaokraglij(przyciskWyrzuc, 8)
obrysuj(przyciskWyrzuc, 0.92)

-- Dół: pasek wagi i podpowiedź
local torWagi = nowy("Frame", {
	Position = UDim2.fromOffset(MARGINES, 448),
	Size = UDim2.fromOffset(SZEROKOSC_SIATKI, 6),
	BackgroundColor3 = KOLOR.Slot,
	BorderSizePixel = 0,
	Parent = okno,
})
zaokraglij(torWagi, 3)
local paskWagi = nowy("Frame", {
	Size = UDim2.fromScale(0, 1),
	BackgroundColor3 = KOLOR.Bialy,
	BorderSizePixel = 0,
	Parent = torWagi,
})
zaokraglij(paskWagi, 3)
local napisWagi = napis({
	Position = UDim2.fromOffset(MARGINES, 460),
	Size = UDim2.fromOffset(SZEROKOSC_SIATKI, 16),
	TextColor3 = KOLOR.TekstSlaby,
	TextSize = 12,
	Parent = okno,
})
napis({
	Text = ("[%s] zamknij"):format(Konfiguracja.Klawisz.Name),
	AnchorPoint = Vector2.new(1, 0),
	Position = UDim2.new(1, -MARGINES, 0, 460),
	Size = UDim2.fromOffset(200, 16),
	TextColor3 = KOLOR.TekstSlaby,
	TextSize = 12,
	TextXAlignment = Enum.TextXAlignment.Right,
	Parent = okno,
})

-- Powiadomienia (widoczne też przy zamkniętym EQ)
local stosPowiadomien = nowy("Frame", {
	BackgroundTransparency = 1,
	AnchorPoint = Vector2.new(0.5, 0),
	Position = UDim2.new(0.5, 0, 0, 64),
	Size = UDim2.fromOffset(640, 300),
	ZIndex = 5,
	Parent = ekran,
})
nowy("UIListLayout", {
	Padding = UDim.new(0, 8),
	HorizontalAlignment = Enum.HorizontalAlignment.Center,
	SortOrder = Enum.SortOrder.LayoutOrder,
	Parent = stosPowiadomien,
})

-- ============================================================
--  Stan
-- ============================================================

local stan = {
	otwarte = false,
	kategoria = "Wszystko",
	fraza = "",
	wybrany = nil,
}
local grupy = {} -- [klucz] = { Klucz, Narzedzia, Dane, WRece }
local kolejnosc = {} -- grupy posortowane
local sloty = {} -- [klucz] = slot
local przyciskiZakladek = {} -- [kategoria] = przycisk
local listaZakladek = ""
local podgladSzczegolow = nil
local pokazanyKlucz = nil
local bazowaSkala = 1
local ostatnieKlikniecie = { klucz = nil, czas = 0 }

local function indeksKategorii(kategoria)
	return table.find(Konfiguracja.Kategorie, kategoria) or (#Konfiguracja.Kategorie + 1)
end

local function pasuje(grupa)
	if stan.kategoria ~= "Wszystko" and grupa.Dane.Kategoria ~= stan.kategoria then
		return false
	end
	if stan.fraza ~= "" then
		return string.find(male(grupa.Dane.Nazwa), stan.fraza, 1, true) ~= nil
	end
	return true
end

local function zbierzPrzedmioty()
	grupy, kolejnosc = {}, {}
	local postac = gracz.Character
	for _, narzedzie in Przedmioty.Narzedzia(gracz) do
		local klucz = narzedzie.Name
		local grupa = grupy[klucz]
		if not grupa then
			grupa = { Klucz = klucz, Narzedzia = {}, Dane = Przedmioty.Dane(narzedzie), WRece = false }
			grupy[klucz] = grupa
			table.insert(kolejnosc, grupa)
		end
		table.insert(grupa.Narzedzia, narzedzie)
		if postac and narzedzie.Parent == postac then
			grupa.WRece = true
		end
	end
	table.sort(kolejnosc, function(a, b)
		local ka, kb = indeksKategorii(a.Dane.Kategoria), indeksKategorii(b.Dane.Kategoria)
		if ka ~= kb then
			return ka < kb
		end
		return a.Dane.Nazwa < b.Dane.Nazwa
	end)
end

-- ============================================================
--  Powiadomienia
-- ============================================================

local numerPowiadomienia = 0

local function pokazPowiadomienie(tresc, rodzaj)
	numerPowiadomienia += 1
	local kolor = KOLOR.Bialy
	if rodzaj == "blad" then
		kolor = KOLOR.Czerwony
	elseif rodzaj == "sukces" then
		kolor = KOLOR.Zielony
	end

	local dymek = nowy("CanvasGroup", {
		AutomaticSize = Enum.AutomaticSize.X,
		Size = UDim2.fromOffset(0, 38),
		BackgroundColor3 = KOLOR.Okno,
		BackgroundTransparency = 0.08,
		GroupTransparency = 1,
		LayoutOrder = numerPowiadomienia,
		Parent = stosPowiadomien,
	})
	zaokraglij(dymek, 10)
	obrysuj(dymek, 0.86)
	nowy("UIPadding", { PaddingLeft = UDim.new(0, 14), PaddingRight = UDim.new(0, 16), Parent = dymek })
	nowy("UIListLayout", {
		FillDirection = Enum.FillDirection.Horizontal,
		VerticalAlignment = Enum.VerticalAlignment.Center,
		Padding = UDim.new(0, 10),
		SortOrder = Enum.SortOrder.LayoutOrder,
		Parent = dymek,
	})
	local kropka = nowy("Frame", {
		Size = UDim2.fromOffset(8, 8),
		BackgroundColor3 = kolor,
		BorderSizePixel = 0,
		LayoutOrder = 1,
		Parent = dymek,
	})
	zaokraglij(kropka, 4)
	napis({
		Text = tresc,
		FontFace = czcionka(Enum.FontWeight.Medium),
		TextSize = 13,
		AutomaticSize = Enum.AutomaticSize.X,
		Size = UDim2.fromOffset(0, 38),
		LayoutOrder = 2,
		Parent = dymek,
	})

	animuj(dymek, { GroupTransparency = 0 }, TweenInfo.new(0.25))
	task.delay(4, function()
		local znikanie = animuj(dymek, { GroupTransparency = 1 }, TweenInfo.new(0.4))
		znikanie.Completed:Wait()
		dymek:Destroy()
	end)
end

-- Gdy w grze jest ekran wczytywania, powiadomienia czekają, aż zniknie
local function czekajNaWczytanie()
	local poczatek = os.clock()
	while gracz:GetAttribute("EkranWczytywania") == true and os.clock() - poczatek < 90 do
		task.wait(0.25)
	end
end

remotePowiadomienie.OnClientEvent:Connect(function(tresc, rodzaj)
	czekajNaWczytanie()
	pokazPowiadomienie(tresc, rodzaj)
end)

-- ============================================================
--  Sloty
-- ============================================================

local function stylSlotu(slot, wybrany)
	slot.Wybrany = wybrany
	local tloSlotu = KOLOR.Slot
	if wybrany then
		tloSlotu = KOLOR.SlotWybrany
	elseif slot.Najechany then
		tloSlotu = KOLOR.SlotNajechany
	end
	animuj(slot.Przycisk, { BackgroundColor3 = tloSlotu })
	slot.Obrys.Transparency = wybrany and 0.55 or 0.9
end

local wybierz -- zdefiniowane niżej
local wezLubSchowaj -- zdefiniowane niżej

local function utworzSlot(grupa)
	local slot = { Klucz = grupa.Klucz, Najechany = false, Wybrany = false }

	slot.Przycisk = przycisk({
		Name = grupa.Klucz,
		Text = "",
		BackgroundColor3 = KOLOR.Slot,
		Parent = siatka,
	})
	zaokraglij(slot.Przycisk, 10)
	slot.Obrys = obrysuj(slot.Przycisk, 0.9)

	slot.Ikona = nowy("Frame", {
		BackgroundTransparency = 1,
		AnchorPoint = Vector2.new(0.5, 0),
		Position = UDim2.new(0.5, 0, 0, 7),
		Size = UDim2.fromOffset(46, 36),
		Parent = slot.Przycisk,
	})
	slot.Ilosc = napis({
		FontFace = czcionka(Enum.FontWeight.Bold),
		TextSize = 11,
		TextXAlignment = Enum.TextXAlignment.Right,
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.new(1, -7, 0, 5),
		Size = UDim2.fromOffset(30, 12),
		Parent = slot.Przycisk,
	})
	slot.Nazwa = napis({
		TextScaled = true,
		TextWrapped = true,
		TextXAlignment = Enum.TextXAlignment.Center,
		Position = UDim2.fromOffset(4, 45),
		Size = UDim2.new(1, -8, 0, 22),
		Parent = slot.Przycisk,
	})
	nowy("UITextSizeConstraint", { MaxTextSize = 11, MinTextSize = 7, Parent = slot.Nazwa })
	slot.Pasek = nowy("Frame", {
		AnchorPoint = Vector2.new(0.5, 0),
		Position = UDim2.new(0.5, 0, 0, 70),
		Size = UDim2.new(1, -22, 0, 3),
		BorderSizePixel = 0,
		Parent = slot.Przycisk,
	})
	zaokraglij(slot.Pasek, 2)
	slot.WRece = nowy("Frame", {
		Position = UDim2.fromOffset(7, 7),
		Size = UDim2.fromOffset(7, 7),
		BackgroundColor3 = KOLOR.Zielony,
		BorderSizePixel = 0,
		Visible = false,
		Parent = slot.Przycisk,
	})
	zaokraglij(slot.WRece, 4)

	ustawIkone(slot.Ikona, grupa.Narzedzia[1], grupa.Dane)

	slot.Przycisk.MouseEnter:Connect(function()
		slot.Najechany = true
		stylSlotu(slot, slot.Wybrany)
	end)
	slot.Przycisk.MouseLeave:Connect(function()
		slot.Najechany = false
		stylSlotu(slot, slot.Wybrany)
	end)
	slot.Przycisk.Activated:Connect(function()
		local teraz = os.clock()
		local podwojne = ostatnieKlikniecie.klucz == slot.Klucz and teraz - ostatnieKlikniecie.czas < 0.4
		ostatnieKlikniecie.klucz = slot.Klucz
		ostatnieKlikniecie.czas = teraz
		wybierz(slot.Klucz)
		if podwojne then
			wezLubSchowaj()
		end
	end)

	return slot
end

local function aktualizujSlot(slot, grupa, indeks)
	slot.Przycisk.LayoutOrder = indeks
	slot.Ilosc.Text = "x" .. #grupa.Narzedzia
	slot.Nazwa.Text = grupa.Dane.Nazwa
	slot.Pasek.BackgroundColor3 = Przedmioty.KolorRzadkosci(grupa.Dane.Rzadkosc)
	slot.WRece.Visible = grupa.WRece
end

-- ============================================================
--  Zakładki
-- ============================================================

local odswiez -- zdefiniowane niżej

local function stylZakladki(przyciskZakladki, aktywna)
	animuj(przyciskZakladki, {
		BackgroundColor3 = aktywna and KOLOR.Bialy or KOLOR.Slot,
		TextColor3 = aktywna and KOLOR.TekstCiemny or KOLOR.Tekst,
	})
end

local function zbudujZakladki()
	local kategorie = { "Wszystko" }
	for _, kategoria in Konfiguracja.Kategorie do
		table.insert(kategorie, kategoria)
	end
	-- Kategorie spoza listy (np. przedmioty z innych systemów) też dostają zakładkę
	for _, grupa in kolejnosc do
		if not table.find(kategorie, grupa.Dane.Kategoria) then
			table.insert(kategorie, grupa.Dane.Kategoria)
		end
	end

	local podpis = table.concat(kategorie, "|")
	if podpis == listaZakladek then
		return
	end
	listaZakladek = podpis

	for _, stary in przyciskiZakladek do
		stary:Destroy()
	end
	table.clear(przyciskiZakladek)
	if not table.find(kategorie, stan.kategoria) then
		stan.kategoria = "Wszystko"
	end

	for indeks, kategoria in kategorie do
		local zakladka = przycisk({
			Text = kategoria,
			TextSize = 12,
			AutomaticSize = Enum.AutomaticSize.X,
			Size = UDim2.fromOffset(0, 28),
			BackgroundColor3 = KOLOR.Slot,
			TextColor3 = KOLOR.Tekst,
			LayoutOrder = indeks,
			Parent = zakladki,
		})
		zaokraglij(zakladka, 14)
		obrysuj(zakladka, 0.92)
		nowy("UIPadding", { PaddingLeft = UDim.new(0, 14), PaddingRight = UDim.new(0, 14), Parent = zakladka })
		stylZakladki(zakladka, kategoria == stan.kategoria)
		zakladka.Activated:Connect(function()
			stan.kategoria = kategoria
			for nazwa, inny in przyciskiZakladek do
				stylZakladki(inny, nazwa == kategoria)
			end
			odswiez()
		end)
		przyciskiZakladek[kategoria] = zakladka
	end
end

-- ============================================================
--  Panel szczegółów
-- ============================================================

local function ustawPrzyciskAktywny(przyciskDoUstawienia, aktywny)
	przyciskDoUstawienia.Active = aktywny
	przyciskDoUstawienia.TextTransparency = aktywny and 0 or 0.55
	przyciskDoUstawienia.BackgroundTransparency = aktywny and 0 or 0.4
end

local function pokazSzczegoly()
	local grupa = stan.wybrany and grupy[stan.wybrany]
	if not grupa then
		trescSzczegolow.Visible = false
		brakWyboru.Visible = true
		brakWyboru.Text = #kolejnosc == 0 and "Brak przedmiotów" or "Wybierz przedmiot"
		if pokazanyKlucz then
			pokazanyKlucz = nil
			podgladSzczegolow = nil
			ikonaDuza:ClearAllChildren()
		end
		return
	end

	trescSzczegolow.Visible = true
	brakWyboru.Visible = false
	local dane = grupa.Dane

	if pokazanyKlucz ~= grupa.Klucz then
		pokazanyKlucz = grupa.Klucz
		podgladSzczegolow = ustawIkone(ikonaDuza, grupa.Narzedzia[1], dane)
	end

	nazwaSz.Text = dane.Nazwa
	rzadkoscSz.Text = duze(dane.Rzadkosc)
	rzadkoscSz.TextColor3 = Przedmioty.KolorRzadkosci(dane.Rzadkosc)
	opisSz.Text = dane.Opis ~= "" and dane.Opis or "Brak opisu."
	infoSz.Text = ("Ilość: %d  •  Waga: %.1f kg"):format(#grupa.Narzedzia, dane.Waga)

	local linia = "Kategoria: " .. dane.Kategoria
	if dane.Ranga then
		local ustawienia = Konfiguracja.Rangi[dane.Ranga]
		local kolor = ustawienia and ustawienia.Kolor or KOLOR.Tekst
		linia ..= ('  •  <font color="%s">%s</font>'):format(hex(kolor), dane.Ranga)
	end
	kategoriaSz.Text = linia

	local postac = gracz.Character
	local humanoid = postac and postac:FindFirstChildOfClass("Humanoid")
	przyciskGlowny.Text = grupa.WRece and "Schowaj" or "Weź do ręki"
	ustawPrzyciskAktywny(przyciskGlowny, humanoid ~= nil and humanoid.Health > 0)

	local sluzbowy = dane.Ranga ~= nil and not Konfiguracja.MoznaWyrzucacSluzbowe
	local zablokowany = grupa.Narzedzia[1]:GetAttribute("MozeWyrzucic") == false
	local bezUchwytu = grupa.Narzedzia[1]:FindFirstChild("Handle") == nil
	if sluzbowy then
		przyciskWyrzuc.Text = "Przedmiot służbowy"
	elseif zablokowany or bezUchwytu then
		przyciskWyrzuc.Text = "Nie można wyrzucić"
	else
		przyciskWyrzuc.Text = "Wyrzuć"
	end
	ustawPrzyciskAktywny(przyciskWyrzuc, not (sluzbowy or zablokowany or bezUchwytu))
end

-- ============================================================
--  Odświeżanie
-- ============================================================

local function aktualizujRange()
	local ranga = Przedmioty.Ranga(gracz)
	local ustawienia = Konfiguracja.Rangi[ranga]
	local kolor = ustawienia and ustawienia.Kolor or KOLOR.Tekst
	napisOdznaki.Text = duze(ranga)
	napisOdznaki.TextColor3 = kolor
	odznaka.BackgroundColor3 = kolor
	obrysOdznaki.Color = kolor
end

odswiez = function()
	if not stan.otwarte then
		return
	end

	zbierzPrzedmioty()
	zbudujZakladki()

	local widoczne = 0
	local pierwszy
	for indeks, grupa in kolejnosc do
		local slot = sloty[grupa.Klucz]
		if not slot then
			slot = utworzSlot(grupa)
			sloty[grupa.Klucz] = slot
		end
		aktualizujSlot(slot, grupa, indeks)
		local widac = pasuje(grupa)
		slot.Przycisk.Visible = widac
		if widac then
			widoczne += 1
			pierwszy = pierwszy or grupa
		end
	end
	for klucz, slot in sloty do
		if not grupy[klucz] then
			slot.Przycisk:Destroy()
			sloty[klucz] = nil
		end
	end

	local wybrana = stan.wybrany and grupy[stan.wybrany]
	if not wybrana or not pasuje(wybrana) then
		stan.wybrany = pierwszy and pierwszy.Klucz or nil
	end
	for klucz, slot in sloty do
		local czyWybrany = klucz == stan.wybrany
		if slot.Wybrany ~= czyWybrany then
			stylSlotu(slot, czyWybrany)
		end
	end
	pokazSzczegoly()

	-- Pusty ekwipunek albo brak wyników
	pusto.Visible = widoczne == 0
	if #kolejnosc == 0 then
		pustoTytul.Text = "Twój ekwipunek jest pusty"
		pustoOpis.Text = ("Ranga: %s. Przedmioty pojawią się tutaj, gdy je zdobędziesz."):format(Przedmioty.Ranga(gracz))
	else
		pustoTytul.Text = "Nic nie znaleziono"
		pustoOpis.Text = "Zmień kategorię albo wyszukiwaną frazę."
	end

	local liczbaSztuk = 0
	for _, grupa in kolejnosc do
		liczbaSztuk += #grupa.Narzedzia
	end
	licznik.Text = ("Przedmioty: %d"):format(liczbaSztuk)

	-- Waga
	local waga = Przedmioty.LacznaWaga(gracz)
	local limit = Konfiguracja.MaksymalnaWaga
	local udzial = limit > 0 and math.clamp(waga / limit, 0, 1) or 0
	local kolorWagi = KOLOR.Bialy
	if waga > limit then
		kolorWagi = KOLOR.Czerwony
	elseif udzial > 0.85 then
		kolorWagi = KOLOR.Pomaranczowy
	end
	animuj(paskWagi, { Size = UDim2.fromScale(udzial, 1), BackgroundColor3 = kolorWagi }, TweenInfo.new(0.25))
	napisWagi.Text = ("%.1f / %g kg"):format(waga, limit)

	aktualizujRange()
end

local zaplanowane = false
local function zaplanujOdswiezenie()
	if zaplanowane then
		return
	end
	zaplanowane = true
	task.defer(function()
		zaplanowane = false
		odswiez()
	end)
end

wybierz = function(klucz)
	stan.wybrany = klucz
	for innyKlucz, slot in sloty do
		stylSlotu(slot, innyKlucz == klucz)
	end
	pokazSzczegoly()
end

-- ============================================================
--  Otwieranie / zamykanie
-- ============================================================

local function przeliczSkale()
	local kamera = workspace.CurrentCamera
	local ekranRozmiar = kamera and kamera.ViewportSize or Vector2.new(1280, 720)
	local naWysokosc = UserInputService.TouchEnabled and 0.94 or 0.75
	bazowaSkala = math.clamp(
		math.min(ekranRozmiar.X * 0.94 / SZEROKOSC, ekranRozmiar.Y * naWysokosc / WYSOKOSC, 1.3),
		0.4,
		1.3
	)
	if stan.otwarte then
		skala.Scale = bazowaSkala
	end
end

local function otworz()
	if stan.otwarte then
		return
	end
	stan.otwarte = true
	przeliczSkale()
	odswiez()

	tlo.Visible = true
	okno.Visible = true
	skala.Scale = bazowaSkala * 0.94
	animuj(skala, { Scale = bazowaSkala }, TweenInfo.new(0.22, Enum.EasingStyle.Back, Enum.EasingDirection.Out))
	animuj(tlo, { BackgroundTransparency = 0.55 }, TweenInfo.new(0.2))
	animuj(rozmycie, { Size = 14 }, TweenInfo.new(0.2))
end

local function zamknij()
	if not stan.otwarte then
		return
	end
	stan.otwarte = false
	szukaj:ReleaseFocus()

	local chowanie = animuj(skala, { Scale = bazowaSkala * 0.94 }, TweenInfo.new(0.14, Enum.EasingStyle.Quad, Enum.EasingDirection.In))
	animuj(tlo, { BackgroundTransparency = 1 }, TweenInfo.new(0.14))
	animuj(rozmycie, { Size = 0 }, TweenInfo.new(0.14))
	chowanie.Completed:Connect(function()
		if not stan.otwarte then
			okno.Visible = false
			tlo.Visible = false
		end
	end)
end

local function przelacz()
	if gracz:GetAttribute("EkranWczytywania") == true then
		return
	end
	if stan.otwarte then
		zamknij()
	else
		otworz()
	end
end

-- ============================================================
--  Akcje
-- ============================================================

wezLubSchowaj = function()
	local grupa = stan.wybrany and grupy[stan.wybrany]
	if not grupa then
		return
	end
	local postac = gracz.Character
	local humanoid = postac and postac:FindFirstChildOfClass("Humanoid")
	if not humanoid or humanoid.Health <= 0 then
		return
	end

	if grupa.WRece then
		humanoid:UnequipTools()
		return
	end

	local plecak = gracz:FindFirstChildOfClass("Backpack")
	for _, narzedzie in grupa.Narzedzia do
		if narzedzie.Parent == plecak then
			humanoid:EquipTool(narzedzie)
			if Konfiguracja.ZamykajPoWzieciu then
				zamknij()
			end
			return
		end
	end
end

local function wyrzuc()
	local grupa = stan.wybrany and grupy[stan.wybrany]
	if not grupa or not przyciskWyrzuc.Active then
		return
	end
	-- Najpierw wyrzucamy sztukę z plecaka, a dopiero potem tę z ręki
	local doWyrzucenia = grupa.Narzedzia[1]
	for _, narzedzie in grupa.Narzedzia do
		if narzedzie.Parent ~= gracz.Character then
			doWyrzucenia = narzedzie
			break
		end
	end
	remoteAkcja:FireServer("Wyrzuc", doWyrzucenia)
end

przyciskGlowny.Activated:Connect(function()
	if przyciskGlowny.Active then
		wezLubSchowaj()
	end
end)
przyciskWyrzuc.Activated:Connect(wyrzuc)
zamknijPrzycisk.Activated:Connect(zamknij)
tlo.Activated:Connect(zamknij)

for _, guzik in { przyciskGlowny, przyciskWyrzuc, zamknijPrzycisk } do
	local bazowy = guzik.BackgroundColor3
	guzik.MouseEnter:Connect(function()
		if guzik.Active then
			animuj(guzik, { BackgroundColor3 = bazowy:Lerp(Color3.new(0.5, 0.5, 0.5), 0.2) })
		end
	end)
	guzik.MouseLeave:Connect(function()
		animuj(guzik, { BackgroundColor3 = bazowy })
	end)
end

szukaj:GetPropertyChangedSignal("Text"):Connect(function()
	stan.fraza = male(szukaj.Text)
	odswiez()
end)

UserInputService.InputBegan:Connect(function(wejscie, przetworzone)
	if przetworzone then
		return
	end
	if wejscie.KeyCode == Konfiguracja.Klawisz then
		przelacz()
	end
end)

-- Przycisk na telefonie
if UserInputService.TouchEnabled then
	local mobilny = przycisk({
		Text = "🎒",
		TextSize = 26,
		AnchorPoint = Vector2.new(0, 0.5),
		Position = UDim2.new(0, 16, 0.5, 0),
		Size = UDim2.fromOffset(56, 56),
		BackgroundColor3 = KOLOR.Okno,
		BackgroundTransparency = 0.15,
		Parent = ekran,
	})
	zaokraglij(mobilny, 28)
	obrysuj(mobilny, 0.8)
	mobilny.Activated:Connect(przelacz)
end

-- Obracanie modelu 3D w panelu szczegółów
RunService.RenderStepped:Connect(function(krok)
	if stan.otwarte and podgladSzczegolow then
		podgladSzczegolow.Kat += krok * 0.6
		podgladSzczegolow.Kamera.CFrame = podgladSzczegolow.Widok(podgladSzczegolow.Kat, 1)
	end
end)

-- ============================================================
--  Obserwowanie plecaka i postaci
-- ============================================================

local polaczenia = {}

local function obserwuj(kontener)
	local function zmiana(obiekt)
		if obiekt:IsA("Tool") then
			zaplanujOdswiezenie()
		end
	end
	table.insert(polaczenia, kontener.ChildAdded:Connect(zmiana))
	table.insert(polaczenia, kontener.ChildRemoved:Connect(zmiana))
end

local function podlaczGracza()
	for _, polaczenie in polaczenia do
		polaczenie:Disconnect()
	end
	table.clear(polaczenia)

	local plecak = gracz:FindFirstChildOfClass("Backpack")
	if plecak then
		obserwuj(plecak)
	end
	local postac = gracz.Character
	if postac then
		obserwuj(postac)
		local humanoid = postac:FindFirstChildOfClass("Humanoid") or postac:WaitForChild("Humanoid", 5)
		if humanoid then
			table.insert(polaczenia, humanoid.Died:Connect(zaplanujOdswiezenie))
		end
	end
	zaplanujOdswiezenie()
end

gracz.CharacterAdded:Connect(podlaczGracza)
gracz.ChildAdded:Connect(function(obiekt)
	if obiekt:IsA("Backpack") then
		podlaczGracza()
	end
end)
gracz:GetAttributeChangedSignal(Przedmioty.ATRYBUT_GRACZA):Connect(function()
	aktualizujRange()
	zaplanujOdswiezenie()
end)

local kamera = workspace.CurrentCamera
if kamera then
	kamera:GetPropertyChangedSignal("ViewportSize"):Connect(przeliczSkale)
end

aktualizujRange()
podlaczGracza()

task.delay(3, function()
	czekajNaWczytanie()
	task.wait(1)
	pokazPowiadomienie(("Naciśnij %s, aby otworzyć ekwipunek."):format(Konfiguracja.Klawisz.Name), "info")
end)
