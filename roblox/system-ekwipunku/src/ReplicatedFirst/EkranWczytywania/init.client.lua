--[[
	EKRAN WCZYTYWANIA  |  LocalScript w ReplicatedFirst
	Ustawienia są w module "Ustawienia" w środku tego skryptu.

	Gdy ekran jest widoczny, gracz ma atrybut EkranWczytywania = true
	(ekwipunek czeka z otwieraniem i powiadomieniami do końca wczytywania).
]]

local ReplicatedFirst = game:GetService("ReplicatedFirst")
local Players = game:GetService("Players")
local ContentProvider = game:GetService("ContentProvider")
local TweenService = game:GetService("TweenService")
local RunService = game:GetService("RunService")
local StarterGui = game:GetService("StarterGui")
local Lighting = game:GetService("Lighting")
local SoundService = game:GetService("SoundService")
local UserInputService = game:GetService("UserInputService")

pcall(function()
	ReplicatedFirst:RemoveDefaultLoadingScreen()
end)

local Ustawienia = require(script:WaitForChild("Ustawienia"))
local gracz = Players.LocalPlayer
gracz:SetAttribute("EkranWczytywania", true)

local start = os.clock()

-- ============================================================
--  Wygląd (te same kolory co w ekwipunku)
-- ============================================================

local KOLOR = {
	Okno = Color3.fromRGB(24, 24, 27),
	Slot = Color3.fromRGB(43, 43, 48),
	Bialy = Color3.fromRGB(245, 245, 247),
	Tekst = Color3.fromRGB(236, 236, 240),
	TekstSlaby = Color3.fromRGB(150, 150, 160),
	TekstCiemny = Color3.fromRGB(22, 22, 25),
}
local AKCENT = Ustawienia.KolorAkcentu or Color3.fromRGB(235, 75, 65)
local CZCIONKA = "rbxasset://fonts/families/GothamSSm.json"

local function czcionka(waga)
	return Font.new(CZCIONKA, waga or Enum.FontWeight.Regular)
end

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
	return nowy("UICorner", { CornerRadius = promien, Parent = rodzic })
end

local function obrysuj(rodzic, przezroczystosc)
	return nowy("UIStroke", {
		Color = Color3.new(1, 1, 1),
		Transparency = przezroczystosc,
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
	}
	for klucz, wartosc in wlasciwosci do
		pelne[klucz] = wartosc
	end
	return nowy("TextLabel", pelne)
end

local function odstep(wysokosc, kolejnosc, rodzic)
	return nowy("Frame", {
		BackgroundTransparency = 1,
		Size = UDim2.fromOffset(1, wysokosc),
		LayoutOrder = kolejnosc,
		Parent = rodzic,
	})
end

local function animuj(obiekt, wlasciwosci, info)
	local tween = TweenService:Create(obiekt, info, wlasciwosci)
	tween:Play()
	return tween
end

-- ============================================================
--  Interfejs Roblox (lista graczy, czat...) na czas wczytywania
-- ============================================================

local ukryte = {}

local function ukryjInterfejs()
	for _, nazwa in Ustawienia.UkryjInterfejs or {} do
		local ok, typ = pcall(function()
			return Enum.CoreGuiType[nazwa]
		end)
		-- Plecakiem (Backpack) zarządza ekwipunek, więc go nie ruszamy
		if ok and typ and typ ~= Enum.CoreGuiType.Backpack and typ ~= Enum.CoreGuiType.All then
			local okStan, wlaczony = pcall(function()
				return StarterGui:GetCoreGuiEnabled(typ)
			end)
			if not okStan or wlaczony then
				local okZmiana = pcall(function()
					StarterGui:SetCoreGuiEnabled(typ, false)
				end)
				if okZmiana then
					table.insert(ukryte, typ)
				end
			end
		end
	end
end

local function przywrocInterfejs()
	for _, typ in ukryte do
		pcall(function()
			StarterGui:SetCoreGuiEnabled(typ, true)
		end)
	end
	table.clear(ukryte)
end

ukryjInterfejs()

-- ============================================================
--  Budowa ekranu
-- ============================================================

local ekran = nowy("ScreenGui", {
	Name = "EkranWczytywania",
	IgnoreGuiInset = true,
	ResetOnSpawn = false,
	DisplayOrder = 100,
	ZIndexBehavior = Enum.ZIndexBehavior.Sibling,
	Parent = gracz:WaitForChild("PlayerGui"),
})

local rozmycie = nowy("BlurEffect", { Name = "EkranWczytywaniaRozmycie", Size = 24, Parent = Lighting })

-- Tło
local tlo = nowy("Frame", {
	Size = UDim2.fromScale(1, 1),
	BackgroundColor3 = Color3.new(1, 1, 1),
	BorderSizePixel = 0,
	Parent = ekran,
})
nowy("UIGradient", {
	Rotation = 90,
	Color = ColorSequence.new(Color3.fromRGB(28, 28, 33), Color3.fromRGB(10, 10, 12)),
	Parent = tlo,
})

if Ustawienia.TloObraz and Ustawienia.TloObraz ~= "" then
	nowy("ImageLabel", {
		Size = UDim2.fromScale(1, 1),
		BackgroundTransparency = 1,
		Image = Ustawienia.TloObraz,
		ScaleType = Enum.ScaleType.Crop,
		Parent = tlo,
	})
	local przyciemnienie = nowy("Frame", {
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = Color3.new(0, 0, 0),
		BackgroundTransparency = 0.3,
		BorderSizePixel = 0,
		Parent = tlo,
	})
	nowy("UIGradient", {
		Rotation = 90,
		Transparency = NumberSequence.new({
			NumberSequenceKeypoint.new(0, 0.4),
			NumberSequenceKeypoint.new(0.6, 0.2),
			NumberSequenceKeypoint.new(1, 0),
		}),
		Parent = przyciemnienie,
	})
end

-- Pływające bańki w tle: { x, y, rozmiar (część wysokości ekranu) }
local BANKI = {
	{ 0.1, 0.3, 0.62 },
	{ 0.92, 0.16, 0.5 },
	{ 0.06, 0.95, 0.5 },
	{ 0.9, 0.9, 0.72 },
	{ 0.55, -0.08, 0.3 },
}
for indeks, banka in BANKI do
	local kolo = nowy("Frame", {
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.fromScale(banka[1], banka[2]),
		Size = UDim2.fromScale(banka[3], banka[3]),
		SizeConstraint = Enum.SizeConstraint.RelativeYY,
		BackgroundColor3 = Color3.new(1, 1, 1),
		BackgroundTransparency = 0.955,
		BorderSizePixel = 0,
		Parent = tlo,
	})
	zaokraglij(kolo, UDim.new(0.5, 0))
	local kierunek = indeks % 2 == 0 and 1 or -1
	animuj(
		kolo,
		{ Position = UDim2.fromScale(banka[1] + 0.02 * kierunek, banka[2] - 0.035) },
		TweenInfo.new(6 + indeks * 1.3, Enum.EasingStyle.Sine, Enum.EasingDirection.InOut, -1, true)
	)
end

-- Skala zależna od ekranu (projekt pod 1280x720)
local skale = {}
local function dodajSkale(obiekt)
	local skala = nowy("UIScale", { Parent = obiekt })
	table.insert(skale, skala)
	return skala
end
local function przeliczSkale()
	local kamera = workspace.CurrentCamera
	local rozmiar = kamera and kamera.ViewportSize or Vector2.new(1280, 720)
	local wartosc = math.clamp(math.min(rozmiar.X / 1280, rozmiar.Y / 720), 0.45, 1.6)
	for _, skala in skale do
		skala.Scale = wartosc
	end
end

-- Środek: logo, nazwa, pasek
local srodek = nowy("Frame", {
	AnchorPoint = Vector2.new(0.5, 0.5),
	Position = UDim2.fromScale(0.5, 0.46),
	Size = UDim2.fromOffset(720, 420),
	BackgroundTransparency = 1,
	Parent = ekran,
})
dodajSkale(srodek)
nowy("UIListLayout", {
	HorizontalAlignment = Enum.HorizontalAlignment.Center,
	VerticalAlignment = Enum.VerticalAlignment.Center,
	SortOrder = Enum.SortOrder.LayoutOrder,
	Parent = srodek,
})

if Ustawienia.Logo and Ustawienia.Logo ~= "" then
	nowy("ImageLabel", {
		Size = UDim2.fromOffset(112, 112),
		BackgroundTransparency = 1,
		Image = Ustawienia.Logo,
		ScaleType = Enum.ScaleType.Fit,
		LayoutOrder = 1,
		Parent = srodek,
	})
	odstep(14, 2, srodek)
end

local tytul = napis({
	Text = Ustawienia.NazwaGry,
	FontFace = czcionka(Enum.FontWeight.Heavy),
	TextScaled = true,
	TextTransparency = 1,
	Size = UDim2.fromOffset(720, 66),
	LayoutOrder = 3,
	Parent = srodek,
})
nowy("UITextSizeConstraint", { MaxTextSize = 60, MinTextSize = 24, Parent = tytul })

local podtytul = napis({
	Text = Ustawienia.Podtytul,
	FontFace = czcionka(Enum.FontWeight.Medium),
	TextColor3 = KOLOR.TekstSlaby,
	TextSize = 18,
	TextTransparency = 1,
	Size = UDim2.fromOffset(720, 26),
	LayoutOrder = 4,
	Parent = srodek,
})

odstep(40, 5, srodek)

local pasek = nowy("Frame", {
	Size = UDim2.fromOffset(460, 6),
	BackgroundColor3 = Color3.new(1, 1, 1),
	BackgroundTransparency = 0.88,
	BorderSizePixel = 0,
	LayoutOrder = 6,
	Parent = srodek,
})
zaokraglij(pasek, UDim.new(0, 3))
local wypelnienie = nowy("Frame", {
	Size = UDim2.fromScale(0, 1),
	BackgroundColor3 = KOLOR.Bialy,
	BorderSizePixel = 0,
	Parent = pasek,
})
zaokraglij(wypelnienie, UDim.new(0, 3))
-- Błysk przesuwający się po pasku
local blysk = nowy("UIGradient", {
	Transparency = NumberSequence.new({
		NumberSequenceKeypoint.new(0, 0.3),
		NumberSequenceKeypoint.new(0.5, 0),
		NumberSequenceKeypoint.new(1, 0.3),
	}),
	Offset = Vector2.new(-1, 0),
	Parent = wypelnienie,
})
animuj(blysk, { Offset = Vector2.new(1, 0) }, TweenInfo.new(1.6, Enum.EasingStyle.Linear, Enum.EasingDirection.In, -1))

odstep(12, 7, srodek)

local wierszStatusu = nowy("Frame", {
	Size = UDim2.fromOffset(460, 18),
	BackgroundTransparency = 1,
	LayoutOrder = 8,
	Parent = srodek,
})
-- Kręcące się kółko: obrys z gradientem, który się obraca
local kolko = nowy("Frame", {
	AnchorPoint = Vector2.new(0, 0.5),
	Position = UDim2.new(0, 1, 0.5, 0),
	Size = UDim2.fromOffset(13, 13),
	BackgroundTransparency = 1,
	Parent = wierszStatusu,
})
zaokraglij(kolko, UDim.new(0.5, 0))
local obrysKolka = nowy("UIStroke", { Color = KOLOR.Bialy, Thickness = 2, Parent = kolko })
local gradientKolka = nowy("UIGradient", {
	Transparency = NumberSequence.new({
		NumberSequenceKeypoint.new(0, 0),
		NumberSequenceKeypoint.new(0.5, 0.15),
		NumberSequenceKeypoint.new(1, 1),
	}),
	Parent = obrysKolka,
})
local status = napis({
	Text = "Łączenie z serwerem",
	TextColor3 = KOLOR.TekstSlaby,
	TextSize = 13,
	TextXAlignment = Enum.TextXAlignment.Left,
	Position = UDim2.fromOffset(24, 0),
	Size = UDim2.new(1, -80, 1, 0),
	Parent = wierszStatusu,
})
local procent = napis({
	Text = "0%",
	FontFace = czcionka(Enum.FontWeight.Bold),
	TextSize = 13,
	TextXAlignment = Enum.TextXAlignment.Right,
	AnchorPoint = Vector2.new(1, 0),
	Position = UDim2.fromScale(1, 0),
	Size = UDim2.new(0, 60, 1, 0),
	Parent = wierszStatusu,
})

odstep(30, 9, srodek)

-- Miejsce na przycisk GRAJ (stały rozmiar, żeby nic nie skakało)
local miejsceGraj = nowy("Frame", {
	Size = UDim2.fromOffset(210, 46),
	BackgroundTransparency = 1,
	LayoutOrder = 10,
	Parent = srodek,
})
local przyciskGraj = nowy("TextButton", {
	Text = "GRAJ",
	FontFace = czcionka(Enum.FontWeight.Heavy),
	TextSize = 16,
	TextColor3 = KOLOR.TekstCiemny,
	BackgroundColor3 = KOLOR.Bialy,
	AutoButtonColor = false,
	AnchorPoint = Vector2.new(0.5, 0.5),
	Position = UDim2.fromScale(0.5, 0.5),
	Size = UDim2.fromScale(1, 1),
	Visible = false,
	Parent = miejsceGraj,
})
zaokraglij(przyciskGraj, UDim.new(0.5, 0))
local poswiata = nowy("UIStroke", {
	Color = KOLOR.Bialy,
	Thickness = 0,
	Transparency = 0.5,
	ApplyStrokeMode = Enum.ApplyStrokeMode.Border,
	Parent = przyciskGraj,
})

-- Góra: wersja
local gora = nowy("Frame", {
	AnchorPoint = Vector2.new(0.5, 0),
	Position = UDim2.new(0.5, 0, 0, 18),
	Size = UDim2.fromOffset(1240, 20),
	BackgroundTransparency = 1,
	Parent = ekran,
})
dodajSkale(gora)
napis({
	Text = Ustawienia.Wersja or "",
	TextColor3 = KOLOR.TekstSlaby,
	TextSize = 12,
	TextXAlignment = Enum.TextXAlignment.Right,
	Size = UDim2.fromScale(1, 1),
	Parent = gora,
})

-- Dół: gracz, porada, pomiń
local dol = nowy("Frame", {
	AnchorPoint = Vector2.new(0.5, 1),
	Position = UDim2.new(0.5, 0, 1, -26),
	Size = UDim2.fromOffset(1240, 56),
	BackgroundTransparency = 1,
	Parent = ekran,
})
dodajSkale(dol)

local awatar = nowy("ImageLabel", {
	Position = UDim2.fromOffset(0, 4),
	Size = UDim2.fromOffset(48, 48),
	BackgroundColor3 = KOLOR.Slot,
	Image = "",
	Parent = dol,
})
zaokraglij(awatar, UDim.new(0.5, 0))
obrysuj(awatar, 0.8)
napis({
	Text = "Witaj,",
	TextColor3 = KOLOR.TekstSlaby,
	TextSize = 12,
	TextXAlignment = Enum.TextXAlignment.Left,
	Position = UDim2.fromOffset(60, 9),
	Size = UDim2.fromOffset(300, 14),
	Parent = dol,
})
napis({
	Text = gracz.DisplayName,
	FontFace = czcionka(Enum.FontWeight.Bold),
	TextSize = 17,
	TextXAlignment = Enum.TextXAlignment.Left,
	TextTruncate = Enum.TextTruncate.AtEnd,
	Position = UDim2.fromOffset(60, 25),
	Size = UDim2.fromOffset(240, 20),
	Parent = dol,
})
task.spawn(function()
	local ok, obraz = pcall(function()
		return Players:GetUserThumbnailAsync(gracz.UserId, Enum.ThumbnailType.HeadShot, Enum.ThumbnailSize.Size100x100)
	end)
	if ok and awatar.Parent then
		awatar.Image = obraz
	end
end)

local karta = nowy("Frame", {
	AnchorPoint = Vector2.new(0.5, 0),
	Position = UDim2.fromScale(0.5, 0),
	Size = UDim2.fromOffset(560, 56),
	BackgroundColor3 = KOLOR.Okno,
	BackgroundTransparency = 0.35,
	Parent = dol,
})
zaokraglij(karta, UDim.new(0, 12))
obrysuj(karta, 0.9)
napis({
	Text = "PORADA",
	FontFace = czcionka(Enum.FontWeight.Bold),
	TextColor3 = AKCENT,
	TextSize = 10,
	TextXAlignment = Enum.TextXAlignment.Left,
	Position = UDim2.fromOffset(18, 10),
	Size = UDim2.fromOffset(200, 12),
	Parent = karta,
})
local porada = napis({
	Text = "",
	TextScaled = true,
	TextXAlignment = Enum.TextXAlignment.Left,
	Position = UDim2.fromOffset(18, 25),
	Size = UDim2.new(1, -36, 0, 20),
	Parent = karta,
})
nowy("UITextSizeConstraint", { MaxTextSize = 14, MinTextSize = 10, Parent = porada })

local przyciskPomin = nowy("TextButton", {
	Text = "Pomiń  ›",
	FontFace = czcionka(Enum.FontWeight.Bold),
	TextSize = 13,
	TextColor3 = KOLOR.Tekst,
	BackgroundColor3 = KOLOR.Slot,
	BackgroundTransparency = 0.3,
	AutoButtonColor = false,
	AnchorPoint = Vector2.new(1, 0.5),
	Position = UDim2.fromScale(1, 0.5),
	Size = UDim2.fromOffset(104, 36),
	Visible = false,
	Parent = dol,
})
zaokraglij(przyciskPomin, UDim.new(0.5, 0))
obrysuj(przyciskPomin, 0.85)

przeliczSkale()
local kamera = workspace.CurrentCamera
if kamera then
	kamera:GetPropertyChangedSignal("ViewportSize"):Connect(przeliczSkale)
end

-- ============================================================
--  Muzyka
-- ============================================================

local muzyka
if Ustawienia.Muzyka and Ustawienia.Muzyka ~= "" then
	muzyka = nowy("Sound", {
		Name = "MuzykaWczytywania",
		SoundId = Ustawienia.Muzyka,
		Looped = true,
		Volume = 0,
		Parent = SoundService,
	})
	muzyka:Play()
	animuj(muzyka, { Volume = Ustawienia.GlosnoscMuzyki or 0.4 }, TweenInfo.new(1.5))
end

-- ============================================================
--  Animacje: pasek, kółko, porady, wejście
-- ============================================================

local cel = 0 -- prawdziwy postęp 0..1
local pokazany = 0 -- postęp na pasku (wygładzony)
local bazaStatusu = "Łączenie z serwerem"
local gotowe = false
local pominieto = false
local zakonczono = false

local function ustawStatus(tekst)
	bazaStatusu = tekst
end

local polaczenieKlatki = RunService.RenderStepped:Connect(function(krok)
	-- Pasek nie może być szybszy niż MinimalnyCzas
	local limitCzasu = math.clamp((os.clock() - start) / math.max(Ustawienia.MinimalnyCzas or 0, 0.1), 0, 1)
	local docelowy = math.min(cel, limitCzasu)
	pokazany += (docelowy - pokazany) * math.min(1, krok * 5)
	if docelowy >= 1 and pokazany > 0.995 then
		pokazany = 1
	end
	wypelnienie.Size = UDim2.fromScale(pokazany, 1)
	procent.Text = math.floor(pokazany * 100) .. "%"

	gradientKolka.Rotation = (gradientKolka.Rotation + krok * 360) % 360
	if gotowe then
		status.Text = bazaStatusu
	else
		status.Text = bazaStatusu .. string.rep(".", math.floor(os.clock() * 3) % 4)
	end
end)

local PORADY = Ustawienia.Porady or {}
local numerPorady = #PORADY > 0 and math.random(1, #PORADY) or 0
if numerPorady > 0 then
	porada.Text = PORADY[numerPorady]
end
task.spawn(function()
	while not zakonczono and #PORADY > 1 do
		task.wait(Ustawienia.ZmianaPoradyCo or 5)
		if zakonczono then
			break
		end
		animuj(porada, { TextTransparency = 1 }, TweenInfo.new(0.25)).Completed:Wait()
		numerPorady = numerPorady % #PORADY + 1
		porada.Text = PORADY[numerPorady]
		animuj(porada, { TextTransparency = 0 }, TweenInfo.new(0.25))
	end
end)

-- Wejście: nazwa i podtytuł pojawiają się płynnie
animuj(tytul, { TextTransparency = 0 }, TweenInfo.new(0.8, Enum.EasingStyle.Quad, Enum.EasingDirection.Out))
task.delay(0.25, function()
	animuj(podtytul, { TextTransparency = 0 }, TweenInfo.new(0.8, Enum.EasingStyle.Quad, Enum.EasingDirection.Out))
end)

if (Ustawienia.PominPo or 0) > 0 then
	task.delay(Ustawienia.PominPo, function()
		if not gotowe and not zakonczono then
			przyciskPomin.Visible = true
		end
	end)
end

-- ============================================================
--  Wczytywanie
-- ============================================================

local KLASY_Z_ZASOBAMI = {
	"Decal", "ImageLabel", "ImageButton", "MeshPart", "FileMesh", "Sound", "Animation",
	"SurfaceAppearance", "ParticleEmitter", "Beam", "Trail", "Sky", "Clothing", "VideoFrame",
}

local function maZasoby(obiekt)
	for _, klasa in KLASY_Z_ZASOBAMI do
		if obiekt:IsA(klasa) then
			return true
		end
	end
	return false
end

local function koniecCzasu()
	return os.clock() - start > (Ustawienia.MaksymalnyCzas or 30)
end

-- Czeka, aż warunek będzie spełniony (albo gracz pominie, albo minie czas)
local function czekaj(warunek, limit)
	local poczatek = os.clock()
	while not warunek() do
		if pominieto or koniecCzasu() or (limit and os.clock() - poczatek > limit) then
			return false
		end
		task.wait(0.1)
	end
	return true
end

local function wczytajZasoby()
	local lista = {}
	for _, nazwa in Ustawienia.Wczytaj or {} do
		local ok, usluga = pcall(function()
			return game:GetService(nazwa)
		end)
		if ok and usluga then
			for _, obiekt in usluga:GetDescendants() do
				if maZasoby(obiekt) then
					table.insert(lista, obiekt)
				end
			end
		end
	end

	local razem = #lista
	local PACZKA = 20
	for indeks = 1, razem, PACZKA do
		if pominieto or zakonczono then
			return
		end
		local koniec = math.min(indeks + PACZKA - 1, razem)
		pcall(function()
			ContentProvider:PreloadAsync(table.move(lista, indeks, koniec, 1, {}))
		end)
		cel = 0.15 + 0.75 * (koniec / razem)
		ustawStatus(("Wczytywanie świata (%d / %d)"):format(koniec, razem))
	end
end

local function przebieg()
	cel = 0.05
	czekaj(function()
		return game:IsLoaded()
	end)

	cel = 0.15
	ustawStatus("Wczytywanie świata")
	local zasobyGotowe = false
	task.spawn(function()
		pcall(wczytajZasoby)
		zasobyGotowe = true
	end)
	czekaj(function()
		return zasobyGotowe
	end)

	cel = 0.92
	ustawStatus("Przygotowanie postaci")
	czekaj(function()
		return gracz.Character ~= nil
	end, 15)

	cel = 1
	ustawStatus("Wczytywanie")
	czekaj(function()
		return pokazany >= 1
	end, 10)
end

-- ============================================================
--  Koniec
-- ============================================================

local function wygas(korzen, info)
	for _, obiekt in korzen:GetDescendants() do
		if obiekt:IsA("GuiObject") then
			TweenService:Create(obiekt, info, { BackgroundTransparency = 1 }):Play()
			if obiekt:IsA("TextLabel") or obiekt:IsA("TextButton") then
				TweenService:Create(obiekt, info, { TextTransparency = 1 }):Play()
			elseif obiekt:IsA("ImageLabel") or obiekt:IsA("ImageButton") then
				TweenService:Create(obiekt, info, { ImageTransparency = 1 }):Play()
			end
		elseif obiekt:IsA("UIStroke") then
			TweenService:Create(obiekt, info, { Transparency = 1 }):Play()
		end
	end
end

local function zakoncz()
	if zakonczono then
		return
	end
	zakonczono = true
	przyciskPomin.Visible = false

	local info = TweenInfo.new(0.8, Enum.EasingStyle.Quad, Enum.EasingDirection.Out)
	wygas(ekran, info)
	for _, skala in skale do
		animuj(skala, { Scale = skala.Scale * 1.04 }, info)
	end
	animuj(rozmycie, { Size = 0 }, TweenInfo.new(1.4, Enum.EasingStyle.Quad, Enum.EasingDirection.Out))
	if muzyka then
		animuj(muzyka, { Volume = 0 }, TweenInfo.new(1.2))
	end

	przywrocInterfejs()
	gracz:SetAttribute("EkranWczytywania", false)

	task.wait(1.4)
	polaczenieKlatki:Disconnect()
	ekran:Destroy()
	rozmycie:Destroy()
	if muzyka then
		muzyka:Destroy()
	end
end

local function pokazGraj()
	gotowe = true
	przyciskPomin.Visible = false
	ustawStatus("Gotowe!")
	kolko.Visible = false
	status.Position = UDim2.fromOffset(0, 0)

	przyciskGraj.Visible = true
	przyciskGraj.Size = UDim2.fromScale(0.8, 0.8)
	przyciskGraj.TextTransparency = 1
	przyciskGraj.BackgroundTransparency = 1
	local info = TweenInfo.new(0.35, Enum.EasingStyle.Back, Enum.EasingDirection.Out)
	animuj(przyciskGraj, { Size = UDim2.fromScale(1, 1), TextTransparency = 0, BackgroundTransparency = 0 }, info)
	-- Pulsująca poświata
	animuj(
		poswiata,
		{ Thickness = 6, Transparency = 1 },
		TweenInfo.new(1.4, Enum.EasingStyle.Quad, Enum.EasingDirection.Out, -1)
	)
end

przyciskPomin.Activated:Connect(function()
	pominieto = true
end)
przyciskGraj.Activated:Connect(zakoncz)
UserInputService.InputBegan:Connect(function(wejscie)
	if not przyciskGraj.Visible or zakonczono then
		return
	end
	local klawisz = wejscie.KeyCode
	if klawisz == Enum.KeyCode.Return or klawisz == Enum.KeyCode.Space or klawisz == Enum.KeyCode.ButtonA then
		zakoncz()
	end
end)
for _, guzik in { przyciskGraj, przyciskPomin } do
	local bazowy = guzik.BackgroundColor3
	guzik.MouseEnter:Connect(function()
		animuj(guzik, { BackgroundColor3 = bazowy:Lerp(Color3.new(0.6, 0.6, 0.6), 0.25) }, TweenInfo.new(0.12))
	end)
	guzik.MouseLeave:Connect(function()
		animuj(guzik, { BackgroundColor3 = bazowy }, TweenInfo.new(0.12))
	end)
end

-- Błąd w wczytywaniu nigdy nie może zablokować gracza na tym ekranie
local ok, blad = pcall(przebieg)
if not ok then
	warn("[EkranWczytywania] " .. tostring(blad))
end

if ok and not pominieto and Ustawienia.PrzyciskGraj then
	pokazGraj()
else
	zakoncz()
end
