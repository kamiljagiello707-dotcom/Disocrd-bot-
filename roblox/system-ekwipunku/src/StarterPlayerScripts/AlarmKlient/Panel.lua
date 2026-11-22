--[[
	SYSTEM ALARMOWANIA OSP  |  Panel dyspozytora
	Wszystko na jednym ekranie: jednostki, rodzaj, miejsce, opis i alarmy w toku.
	Alarm wysyła się przytrzymaniem przycisku, żeby nie zaalarmować przez przypadek.
]]

local Lighting = game:GetService("Lighting")
local UserInputService = game:GetService("UserInputService")

local W = require(script.Parent:WaitForChild("Wyglad"))
local K = W.KOLOR

local Panel = {}

local SZEROKOSC, WYSOKOSC = 860, 520
local CZAS_PRZYTRZYMANIA = 1

local ctx, Ustawienia
local tlo, okno, skala, rozmycie
local listaJednostek, siatkaRodzajow, listaMiejsc, poleOpisu, licznikOpisu
local podsTyp, podsJednostki, podsMiejsce, podsLudzie
local przyciskAlarm, wypelnienieAlarmu, napisAlarmu, podpowiedzAlarmu
local listaAktywnych, brakAktywnych

local stan = { otwarte = false, jednostki = {}, rodzaj = nil, miejsce = nil, wysylanie = false }
local dane = { Jednostki = {}, Miejsca = {}, Aktywne = {} }
local wierszeJednostek = {}
local kafelkiRodzajow = {}
local wierszeMiejsc = {}
local bazowaSkala = 1
local trzymanie = nil
local pierwszeOtwarcie = true

-- ============================================================
--  Pomocnicze
-- ============================================================

local function etykieta(tekst, x, y, szerokosc, rodzic)
	return W.napis({
		Text = tekst,
		FontFace = W.czcionka(Enum.FontWeight.Bold),
		TextSize = 11,
		TextColor3 = K.TekstSlaby,
		Position = UDim2.fromOffset(x, y),
		Size = UDim2.fromOffset(szerokosc, 16),
		Parent = rodzic or okno,
	})
end

local function przewijanie(x, y, szerokosc, wysokosc, odstep)
	local ramka = W.nowy("ScrollingFrame", {
		BackgroundTransparency = 1,
		BorderSizePixel = 0,
		Position = UDim2.fromOffset(x, y),
		Size = UDim2.fromOffset(szerokosc, wysokosc),
		CanvasSize = UDim2.new(),
		AutomaticCanvasSize = Enum.AutomaticSize.Y,
		ScrollingDirection = Enum.ScrollingDirection.Y,
		ScrollBarThickness = 3,
		ScrollBarImageColor3 = K.Bialy,
		ScrollBarImageTransparency = 0.7,
		Parent = okno,
	})
	W.nowy("UIListLayout", {
		Padding = UDim.new(0, odstep),
		SortOrder = Enum.SortOrder.LayoutOrder,
		Parent = ramka,
	})
	W.nowy("UIPadding", { PaddingTop = UDim.new(0, 1), PaddingLeft = UDim.new(0, 1), Parent = ramka })
	return ramka
end

local function wybraneJednostki()
	local lista = {}
	for _, jednostka in dane.Jednostki do
		if stan.jednostki[jednostka.Nazwa] then
			table.insert(lista, jednostka.Nazwa)
		end
	end
	return lista
end

local function moznaWyslac()
	return #wybraneJednostki() > 0 and stan.rodzaj ~= nil and not stan.wysylanie
end

-- ============================================================
--  Podsumowanie
-- ============================================================

local function odswiezPodsumowanie()
	if stan.rodzaj then
		local rodzaj = W.znajdzRodzaj(Ustawienia, stan.rodzaj)
		podsTyp.Text = rodzaj.Ikona .. "  " .. rodzaj.Nazwa
		podsTyp.TextColor3 = rodzaj.Kolor
	else
		podsTyp.Text = "Nie wybrano rodzaju"
		podsTyp.TextColor3 = K.TekstSlaby
	end

	local jednostki = wybraneJednostki()
	podsJednostki.Text = #jednostki > 0 and table.concat(jednostki, ", ") or "Nie wybrano jednostek"
	podsMiejsce.Text = "📍  " .. (stan.miejsce or "Bez wskazania miejsca")

	local ludzie = 0
	for _, jednostka in dane.Jednostki do
		if stan.jednostki[jednostka.Nazwa] then
			ludzie += jednostka.WGrze
		end
	end
	podsLudzie.Text = ("Strażaków w grze: %d"):format(ludzie)

	local mozna = moznaWyslac()
	W.kolorPrzycisku(przyciskAlarm, mozna and K.Czerwony or K.Slot)
	napisAlarmu.TextColor3 = mozna and K.Bialy or K.TekstSlaby
	if stan.wysylanie then
		podpowiedzAlarmu.Text = "Wysyłanie..."
	elseif #jednostki == 0 then
		podpowiedzAlarmu.Text = "Zaznacz co najmniej jedną jednostkę"
	elseif not stan.rodzaj then
		podpowiedzAlarmu.Text = "Wybierz rodzaj zdarzenia"
	else
		podpowiedzAlarmu.Text = "Przytrzymaj przycisk przez 1 sekundę"
	end
end

-- ============================================================
--  Jednostki
-- ============================================================

local function stylJednostki(nazwa)
	local wiersz = wierszeJednostek[nazwa]
	local zaznaczona = stan.jednostki[nazwa] == true
	W.kolorPrzycisku(wiersz.Przycisk, zaznaczona and K.SlotWybrany or K.Slot)
	wiersz.Obrys.Transparency = zaznaczona and 0.55 or 0.9
	wiersz.Pole.BackgroundColor3 = zaznaczona and K.Bialy or K.Okno
	wiersz.Znak.Visible = zaznaczona
end

local function rysujJednostki()
	for _, wiersz in wierszeJednostek do
		wiersz.Przycisk:Destroy()
	end
	table.clear(wierszeJednostek)

	for indeks, jednostka in dane.Jednostki do
		local przycisk = W.przycisk({
			Text = "",
			Size = UDim2.new(1, -6, 0, 50),
			LayoutOrder = indeks,
			Parent = listaJednostek,
		})
		W.zaokraglij(przycisk, 10)
		local obrys = W.obrysuj(przycisk, 0.9)
		local pole = W.nowy("Frame", {
			Position = UDim2.fromOffset(14, 15),
			Size = UDim2.fromOffset(20, 20),
			BackgroundColor3 = K.Okno,
			Parent = przycisk,
		})
		W.zaokraglij(pole, 5)
		W.obrysuj(pole, 0.7)
		local znak = W.napis({
			Text = "✓",
			FontFace = W.czcionka(Enum.FontWeight.Bold),
			TextColor3 = K.TekstCiemny,
			TextXAlignment = Enum.TextXAlignment.Center,
			Size = UDim2.fromScale(1, 1),
			Visible = false,
			Parent = pole,
		})
		W.napis({
			Text = jednostka.Nazwa,
			FontFace = W.czcionka(Enum.FontWeight.Bold),
			TextTruncate = Enum.TextTruncate.AtEnd,
			Position = UDim2.fromOffset(46, 8),
			Size = UDim2.new(1, -56, 0, 18),
			Parent = przycisk,
		})
		local kropka = W.nowy("Frame", {
			Position = UDim2.fromOffset(46, 31),
			Size = UDim2.fromOffset(6, 6),
			BackgroundColor3 = jednostka.WGrze > 0 and K.Zielony or K.TekstSlaby,
			BorderSizePixel = 0,
			Parent = przycisk,
		})
		W.zaokraglij(kropka, 3)
		W.napis({
			Text = jednostka.WGrze == 1 and "1 strażak w grze" or ("Strażaków w grze: %d"):format(jednostka.WGrze),
			TextSize = 11,
			TextColor3 = K.TekstSlaby,
			Position = UDim2.fromOffset(58, 27),
			Size = UDim2.new(1, -68, 0, 14),
			Parent = przycisk,
		})
		W.najechanie(przycisk)
		przycisk.Activated:Connect(function()
			stan.jednostki[jednostka.Nazwa] = not stan.jednostki[jednostka.Nazwa] or nil
			stylJednostki(jednostka.Nazwa)
			odswiezPodsumowanie()
		end)
		wierszeJednostek[jednostka.Nazwa] = { Przycisk = przycisk, Obrys = obrys, Pole = pole, Znak = znak }
	end

	-- Jedna jednostka w grze? Od razu zaznaczona.
	if pierwszeOtwarcie and #dane.Jednostki == 1 then
		stan.jednostki[dane.Jednostki[1].Nazwa] = true
	end
	for nazwa in stan.jednostki do
		if not wierszeJednostek[nazwa] then
			stan.jednostki[nazwa] = nil
		end
	end
	for nazwa in wierszeJednostek do
		stylJednostki(nazwa)
	end
end

-- ============================================================
--  Rodzaje zdarzeń
-- ============================================================

local function stylRodzajow()
	for nazwa, kafelek in kafelkiRodzajow do
		local wybrany = stan.rodzaj == nazwa
		W.kolorPrzycisku(kafelek.Przycisk, wybrany and K.Slot:Lerp(kafelek.Kolor, 0.22) or K.Slot)
		kafelek.Obrys.Color = wybrany and kafelek.Kolor or Color3.new(1, 1, 1)
		kafelek.Obrys.Transparency = wybrany and 0.1 or 0.9
	end
end

local function zbudujRodzaje()
	for indeks, rodzaj in Ustawienia.Rodzaje do
		local przycisk = W.przycisk({ Text = "", LayoutOrder = indeks, Parent = siatkaRodzajow })
		W.zaokraglij(przycisk, 10)
		local obrys = W.obrysuj(przycisk, 0.9)
		W.napis({
			Text = rodzaj.Ikona,
			TextSize = 20,
			TextXAlignment = Enum.TextXAlignment.Center,
			Position = UDim2.fromOffset(8, 0),
			Size = UDim2.new(0, 30, 1, 0),
			Parent = przycisk,
		})
		W.napis({
			Text = rodzaj.Nazwa,
			FontFace = W.czcionka(Enum.FontWeight.Bold),
			TextSize = 12,
			TextWrapped = true,
			Position = UDim2.fromOffset(42, 0),
			Size = UDim2.new(1, -50, 1, 0),
			Parent = przycisk,
		})
		W.najechanie(przycisk)
		przycisk.Activated:Connect(function()
			stan.rodzaj = rodzaj.Nazwa
			stylRodzajow()
			odswiezPodsumowanie()
		end)
		kafelkiRodzajow[rodzaj.Nazwa] = { Przycisk = przycisk, Obrys = obrys, Kolor = rodzaj.Kolor }
	end
	stylRodzajow()
end

-- ============================================================
--  Miejsca zdarzeń
-- ============================================================

local function stylMiejsc()
	for nazwa, przycisk in wierszeMiejsc do
		local wybrane = (stan.miejsce or "") == nazwa
		W.kolorPrzycisku(przycisk, wybrane and K.Bialy or K.Slot)
		przycisk.TextColor3 = wybrane and K.TekstCiemny or K.Tekst
	end
end

local function rysujMiejsca()
	for _, przycisk in wierszeMiejsc do
		przycisk:Destroy()
	end
	table.clear(wierszeMiejsc)

	local pozycje = { "" }
	for _, nazwa in dane.Miejsca do
		table.insert(pozycje, nazwa)
	end
	for indeks, nazwa in pozycje do
		local przycisk = W.przycisk({
			Text = nazwa == "" and "Bez wskazania miejsca" or ("📍  " .. nazwa),
			FontFace = W.czcionka(Enum.FontWeight.Medium),
			TextSize = 12,
			TextXAlignment = Enum.TextXAlignment.Left,
			TextTruncate = Enum.TextTruncate.AtEnd,
			Size = UDim2.new(1, -6, 0, 28),
			LayoutOrder = indeks,
			Parent = listaMiejsc,
		})
		W.zaokraglij(przycisk, 7)
		W.nowy("UIPadding", { PaddingLeft = UDim.new(0, 10), PaddingRight = UDim.new(0, 8), Parent = przycisk })
		W.najechanie(przycisk, function()
			return (stan.miejsce or "") ~= nazwa
		end)
		przycisk.Activated:Connect(function()
			stan.miejsce = nazwa ~= "" and nazwa or nil
			stylMiejsc()
			odswiezPodsumowanie()
		end)
		wierszeMiejsc[nazwa] = przycisk
	end
	if stan.miejsce and not table.find(dane.Miejsca, stan.miejsce) then
		stan.miejsce = nil
	end
	stylMiejsc()
end

-- ============================================================
--  Alarmy w toku
-- ============================================================

local function rysujAktywne()
	for _, dziecko in listaAktywnych:GetChildren() do
		if dziecko:IsA("Frame") then
			dziecko:Destroy()
		end
	end
	brakAktywnych.Visible = #dane.Aktywne == 0

	for indeks, alarm in dane.Aktywne do
		local rodzaj = W.znajdzRodzaj(Ustawienia, alarm.Rodzaj)
		local karta = W.nowy("Frame", {
			Size = UDim2.new(1, -6, 0, 66),
			BackgroundColor3 = K.Slot,
			LayoutOrder = indeks,
			Parent = listaAktywnych,
		})
		W.zaokraglij(karta, 10)
		W.obrysuj(karta, 0.92)
		local kropka = W.nowy("Frame", {
			Position = UDim2.fromOffset(10, 12),
			Size = UDim2.fromOffset(8, 8),
			BackgroundColor3 = rodzaj.Kolor,
			BorderSizePixel = 0,
			Parent = karta,
		})
		W.zaokraglij(kropka, 4)
		W.napis({
			Text = alarm.Rodzaj,
			FontFace = W.czcionka(Enum.FontWeight.Bold),
			TextSize = 12,
			TextTruncate = Enum.TextTruncate.AtEnd,
			Position = UDim2.fromOffset(24, 7),
			Size = UDim2.new(1, -100, 0, 18),
			Parent = karta,
		})
		W.napis({
			Text = W.ileTemu(alarm.Czas),
			TextSize = 10,
			TextColor3 = K.TekstSlaby,
			TextXAlignment = Enum.TextXAlignment.Right,
			AnchorPoint = Vector2.new(1, 0),
			Position = UDim2.new(1, -10, 0, 9),
			Size = UDim2.fromOffset(76, 14),
			Parent = karta,
		})
		W.napis({
			Text = "📍 " .. (alarm.Miejsce or "bez wskazania miejsca"),
			TextSize = 11,
			TextColor3 = K.TekstSlaby,
			TextTruncate = Enum.TextTruncate.AtEnd,
			Position = UDim2.fromOffset(10, 27),
			Size = UDim2.new(1, -20, 0, 14),
			Parent = karta,
		})
		W.napis({
			Text = ('<font color="#%s">Jadą: %d</font>  •  Nie: %d  •  z %d'):format(
				K.Zielony:ToHex(),
				#alarm.Jada,
				#alarm.NieJada,
				alarm.Odbiorcow
			),
			RichText = true,
			TextSize = 11,
			Position = UDim2.fromOffset(10, 45),
			Size = UDim2.new(1, -90, 0, 14),
			Parent = karta,
		})
		local zakoncz = W.przycisk({
			Text = "Zakończ",
			TextSize = 11,
			TextColor3 = K.Czerwony,
			BackgroundColor3 = K.Okno,
			AnchorPoint = Vector2.new(1, 0),
			Position = UDim2.new(1, -8, 0, 41),
			Size = UDim2.fromOffset(68, 20),
			Parent = karta,
		})
		W.zaokraglij(zakoncz, 6)
		W.najechanie(zakoncz)
		zakoncz.Activated:Connect(function()
			ctx.Zdarzenie("Zakoncz", alarm.Id)
		end)
	end
end

-- ============================================================
--  Wysyłanie
-- ============================================================

local function wyslij()
	if not moznaWyslac() then
		return
	end
	stan.wysylanie = true
	napisAlarmu.Text = "WYSYŁANIE..."
	odswiezPodsumowanie()

	local ok, tresc = ctx.Wyslij({
		Jednostki = wybraneJednostki(),
		Rodzaj = stan.rodzaj,
		Miejsce = stan.miejsce,
		Opis = poleOpisu.Text,
	})

	stan.wysylanie = false
	napisAlarmu.Text = "🚨  ALARMUJ"
	ctx.Powiadom(tresc or (ok and "Alarm wysłany." or "Nie udało się wysłać alarmu."), ok and "sukces" or "blad")
	if ok then
		stan.rodzaj = nil
		stan.miejsce = nil
		poleOpisu.Text = ""
		stylRodzajow()
		stylMiejsc()
	end
	W.animuj(wypelnienieAlarmu, { Size = UDim2.fromScale(0, 1) }, TweenInfo.new(0.2))
	odswiezPodsumowanie()
end

local function puscPrzycisk()
	if trzymanie then
		trzymanie.Tween:Cancel()
		trzymanie = nil
		W.animuj(wypelnienieAlarmu, { Size = UDim2.fromScale(0, 1) }, TweenInfo.new(0.15))
	end
end

local function wcisnijPrzycisk()
	if not moznaWyslac() then
		return
	end
	puscPrzycisk()
	wypelnienieAlarmu.Size = UDim2.fromScale(0, 1)
	local moje = {}
	moje.Tween = W.animuj(
		wypelnienieAlarmu,
		{ Size = UDim2.fromScale(1, 1) },
		TweenInfo.new(CZAS_PRZYTRZYMANIA, Enum.EasingStyle.Linear)
	)
	trzymanie = moje
	moje.Tween.Completed:Connect(function(stanTweena)
		if stanTweena == Enum.PlaybackState.Completed and trzymanie == moje then
			trzymanie = nil
			wyslij()
		end
	end)
end

-- ============================================================
--  Otwieranie i zamykanie
-- ============================================================

local function przeliczSkale()
	local kamera = workspace.CurrentCamera
	local ekran = kamera and kamera.ViewportSize or Vector2.new(1280, 720)
	local naWysokosc = UserInputService.TouchEnabled and 0.94 or 0.8
	bazowaSkala = math.clamp(math.min(ekran.X * 0.94 / SZEROKOSC, ekran.Y * naWysokosc / WYSOKOSC, 1.25), 0.4, 1.25)
	if stan.otwarte then
		skala.Scale = bazowaSkala
	end
end

local function zamknij()
	if not stan.otwarte then
		return
	end
	stan.otwarte = false
	puscPrzycisk()
	poleOpisu:ReleaseFocus()
	local chowanie = W.animuj(
		skala,
		{ Scale = bazowaSkala * 0.94 },
		TweenInfo.new(0.14, Enum.EasingStyle.Quad, Enum.EasingDirection.In)
	)
	W.animuj(tlo, { BackgroundTransparency = 1 }, TweenInfo.new(0.14))
	W.animuj(rozmycie, { Size = 0 }, TweenInfo.new(0.14))
	chowanie.Completed:Connect(function()
		if not stan.otwarte then
			okno.Visible = false
			tlo.Visible = false
		end
	end)
end

function Panel.Dane(noweDane)
	if typeof(noweDane) ~= "table" then
		return
	end
	dane.Jednostki = noweDane.Jednostki or {}
	dane.Miejsca = noweDane.Miejsca or {}
	dane.Aktywne = noweDane.Aktywne or {}
	rysujJednostki()
	rysujMiejsca()
	rysujAktywne()
	odswiezPodsumowanie()
	pierwszeOtwarcie = false
end

function Panel.Otworz(noweDane)
	Panel.Dane(noweDane)
	if stan.otwarte then
		return
	end
	stan.otwarte = true
	przeliczSkale()
	tlo.Visible = true
	okno.Visible = true
	skala.Scale = bazowaSkala * 0.94
	W.animuj(skala, { Scale = bazowaSkala }, TweenInfo.new(0.22, Enum.EasingStyle.Back, Enum.EasingDirection.Out))
	W.animuj(tlo, { BackgroundTransparency = 0.55 }, TweenInfo.new(0.2))
	W.animuj(rozmycie, { Size = 14 }, TweenInfo.new(0.2))
end

function Panel.Aktywne(lista)
	if typeof(lista) ~= "table" then
		return
	end
	dane.Aktywne = lista
	if okno then
		rysujAktywne()
	end
end

-- ============================================================
--  Budowa okna
-- ============================================================

function Panel.Inicjuj(ekran, kontekst)
	ctx = kontekst
	Ustawienia = kontekst.Ustawienia

	rozmycie = W.nowy("BlurEffect", { Name = "AlarmOSPRozmycie", Size = 0, Parent = Lighting })

	tlo = W.przycisk({
		Text = "",
		Modal = true,
		BackgroundColor3 = Color3.new(0, 0, 0),
		BackgroundTransparency = 1,
		Size = UDim2.fromScale(1, 1),
		Visible = false,
		ZIndex = 0,
		Parent = ekran,
	})

	okno = W.nowy("Frame", {
		Name = "PanelAlarmowy",
		ZIndex = 2,
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.fromScale(0.5, 0.5),
		Size = UDim2.fromOffset(SZEROKOSC, WYSOKOSC),
		BackgroundColor3 = K.Okno,
		BackgroundTransparency = 0.06,
		Visible = false,
		Parent = ekran,
	})
	W.zaokraglij(okno, 16)
	W.obrysuj(okno, 0.86)
	skala = W.nowy("UIScale", { Parent = okno })
	-- Okno pochłania kliknięcia, żeby nie zamykało się przez tło
	W.nowy("TextButton", {
		Text = "",
		BackgroundTransparency = 1,
		Size = UDim2.fromScale(1, 1),
		ZIndex = 0,
		AutoButtonColor = false,
		Parent = okno,
	})

	-- Nagłówek
	local kropka = W.nowy("Frame", {
		Position = UDim2.fromOffset(24, 25),
		Size = UDim2.fromOffset(10, 10),
		BackgroundColor3 = K.Czerwony,
		BorderSizePixel = 0,
		Parent = okno,
	})
	W.zaokraglij(kropka, 5)
	W.animuj(
		kropka,
		{ BackgroundTransparency = 0.7 },
		TweenInfo.new(0.7, Enum.EasingStyle.Sine, Enum.EasingDirection.InOut, -1, true)
	)
	W.napis({
		Text = "ALARM OSP",
		FontFace = W.czcionka(Enum.FontWeight.Heavy),
		TextSize = 22,
		Position = UDim2.fromOffset(44, 16),
		Size = UDim2.fromOffset(400, 26),
		Parent = okno,
	})
	W.napis({
		Text = "System powiadamiania jednostek",
		TextSize = 12,
		TextColor3 = K.TekstSlaby,
		Position = UDim2.fromOffset(44, 42),
		Size = UDim2.fromOffset(400, 14),
		Parent = okno,
	})
	local zamknijPrzycisk = W.przycisk({
		Text = "✕",
		TextSize = 16,
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.new(1, -18, 0, 14),
		Size = UDim2.fromOffset(34, 34),
		Parent = okno,
	})
	W.zaokraglij(zamknijPrzycisk, 9)
	W.obrysuj(zamknijPrzycisk, 0.88)
	W.najechanie(zamknijPrzycisk)
	zamknijPrzycisk.Activated:Connect(zamknij)
	W.nowy("Frame", {
		Position = UDim2.fromOffset(22, 70),
		Size = UDim2.new(1, -44, 0, 1),
		BackgroundColor3 = Color3.new(1, 1, 1),
		BackgroundTransparency = 0.92,
		BorderSizePixel = 0,
		Parent = okno,
	})

	-- Kolumna 1: jednostki
	etykieta("1. JEDNOSTKI", 22, 86, 150)
	local wszystkie = W.przycisk({
		Text = "Zaznacz wszystkie",
		TextSize = 11,
		TextColor3 = K.TekstSlaby,
		TextXAlignment = Enum.TextXAlignment.Right,
		BackgroundTransparency = 1,
		Position = UDim2.fromOffset(152, 86),
		Size = UDim2.fromOffset(120, 16),
		Parent = okno,
	})
	wszystkie.Activated:Connect(function()
		for _, jednostka in dane.Jednostki do
			stan.jednostki[jednostka.Nazwa] = true
		end
		for nazwa in wierszeJednostek do
			stylJednostki(nazwa)
		end
		odswiezPodsumowanie()
	end)
	listaJednostek = przewijanie(22, 110, 250, 390, 6)

	-- Kolumna 2: rodzaj, miejsce, opis
	etykieta("2. RODZAJ ZDARZENIA", 290, 86, 300)
	siatkaRodzajow = W.nowy("Frame", {
		BackgroundTransparency = 1,
		Position = UDim2.fromOffset(290, 110),
		Size = UDim2.fromOffset(300, 172),
		Parent = okno,
	})
	W.nowy("UIGridLayout", {
		CellSize = UDim2.fromOffset(146, 52),
		CellPadding = UDim2.fromOffset(8, 8),
		SortOrder = Enum.SortOrder.LayoutOrder,
		Parent = siatkaRodzajow,
	})

	etykieta("3. MIEJSCE ZDARZENIA", 290, 296, 300)
	listaMiejsc = przewijanie(290, 318, 300, 96, 4)

	etykieta("4. OPIS", 290, 424, 200)
	licznikOpisu = W.napis({
		Text = "0/" .. (Ustawienia.MaksDlugoscOpisu or 120),
		TextSize = 11,
		TextColor3 = K.TekstSlaby,
		TextXAlignment = Enum.TextXAlignment.Right,
		Position = UDim2.fromOffset(490, 424),
		Size = UDim2.fromOffset(100, 16),
		Parent = okno,
	})
	local ramkaOpisu = W.nowy("Frame", {
		Position = UDim2.fromOffset(290, 444),
		Size = UDim2.fromOffset(300, 56),
		BackgroundColor3 = K.Slot,
		Parent = okno,
	})
	W.zaokraglij(ramkaOpisu, 8)
	W.obrysuj(ramkaOpisu, 0.92)
	poleOpisu = W.nowy("TextBox", {
		BackgroundTransparency = 1,
		Position = UDim2.fromOffset(10, 7),
		Size = UDim2.new(1, -20, 1, -14),
		MultiLine = true,
		TextWrapped = true,
		ClearTextOnFocus = false,
		Text = "",
		PlaceholderText = "Np. pali się stodoła, w środku mogą być ludzie.",
		PlaceholderColor3 = K.TekstSlaby,
		TextColor3 = K.Tekst,
		FontFace = W.czcionka(),
		TextSize = 13,
		TextXAlignment = Enum.TextXAlignment.Left,
		TextYAlignment = Enum.TextYAlignment.Top,
		Parent = ramkaOpisu,
	})
	poleOpisu:GetPropertyChangedSignal("Text"):Connect(function()
		local maks = Ustawienia.MaksDlugoscOpisu or 120
		local tekst = poleOpisu.Text
		local dlugosc = utf8.len(tekst) or #tekst
		if dlugosc > maks then
			poleOpisu.Text = string.sub(tekst, 1, utf8.offset(tekst, maks + 1) - 1)
			return
		end
		licznikOpisu.Text = ("%d/%d"):format(dlugosc, maks)
	end)

	-- Kolumna 3: podsumowanie, przycisk, alarmy w toku
	etykieta("PODSUMOWANIE", 608, 86, 230)
	local karta = W.nowy("Frame", {
		Position = UDim2.fromOffset(608, 110),
		Size = UDim2.fromOffset(230, 128),
		BackgroundColor3 = K.Panel,
		BackgroundTransparency = 0.15,
		Parent = okno,
	})
	W.zaokraglij(karta, 12)
	W.obrysuj(karta, 0.9)
	podsTyp = W.napis({
		FontFace = W.czcionka(Enum.FontWeight.Bold),
		TextSize = 15,
		TextTruncate = Enum.TextTruncate.AtEnd,
		Position = UDim2.fromOffset(14, 12),
		Size = UDim2.new(1, -28, 0, 20),
		Parent = karta,
	})
	podsJednostki = W.napis({
		TextSize = 12,
		TextColor3 = K.TekstSlaby,
		TextWrapped = true,
		TextYAlignment = Enum.TextYAlignment.Top,
		Position = UDim2.fromOffset(14, 40),
		Size = UDim2.new(1, -28, 0, 30),
		Parent = karta,
	})
	podsMiejsce = W.napis({
		TextSize = 12,
		TextTruncate = Enum.TextTruncate.AtEnd,
		Position = UDim2.fromOffset(14, 76),
		Size = UDim2.new(1, -28, 0, 16),
		Parent = karta,
	})
	podsLudzie = W.napis({
		TextSize = 12,
		TextColor3 = K.TekstSlaby,
		Position = UDim2.fromOffset(14, 98),
		Size = UDim2.new(1, -28, 0, 16),
		Parent = karta,
	})

	przyciskAlarm = W.przycisk({
		Text = "",
		ClipsDescendants = true,
		Position = UDim2.fromOffset(608, 250),
		Size = UDim2.fromOffset(230, 56),
		Parent = okno,
	})
	W.zaokraglij(przyciskAlarm, 12)
	wypelnienieAlarmu = W.nowy("Frame", {
		Size = UDim2.fromScale(0, 1),
		BackgroundColor3 = Color3.new(1, 1, 1),
		BackgroundTransparency = 0.7,
		BorderSizePixel = 0,
		Parent = przyciskAlarm,
	})
	-- Roblox nie przycina do zaokrąglonych rogów, więc wypełnienie ma własne
	W.zaokraglij(wypelnienieAlarmu, 12)
	napisAlarmu = W.napis({
		Text = "🚨  ALARMUJ",
		FontFace = W.czcionka(Enum.FontWeight.Heavy),
		TextSize = 18,
		TextXAlignment = Enum.TextXAlignment.Center,
		Size = UDim2.fromScale(1, 1),
		ZIndex = 2,
		Parent = przyciskAlarm,
	})
	podpowiedzAlarmu = W.napis({
		TextSize = 11,
		TextColor3 = K.TekstSlaby,
		TextXAlignment = Enum.TextXAlignment.Center,
		Position = UDim2.fromOffset(608, 312),
		Size = UDim2.fromOffset(230, 14),
		Parent = okno,
	})
	przyciskAlarm.InputBegan:Connect(function(wejscie)
		local typ = wejscie.UserInputType
		if typ == Enum.UserInputType.MouseButton1 or typ == Enum.UserInputType.Touch then
			wcisnijPrzycisk()
		end
	end)
	przyciskAlarm.InputEnded:Connect(function(wejscie)
		local typ = wejscie.UserInputType
		if typ == Enum.UserInputType.MouseButton1 or typ == Enum.UserInputType.Touch then
			puscPrzycisk()
		end
	end)
	przyciskAlarm.MouseLeave:Connect(puscPrzycisk)

	etykieta("ALARMY W TOKU", 608, 340, 230)
	listaAktywnych = przewijanie(608, 362, 230, 138, 6)
	brakAktywnych = W.napis({
		Text = "Brak alarmów w toku",
		TextSize = 12,
		TextColor3 = K.TekstSlaby,
		TextXAlignment = Enum.TextXAlignment.Center,
		Position = UDim2.fromOffset(608, 362),
		Size = UDim2.fromOffset(230, 60),
		Parent = okno,
	})

	zbudujRodzaje()
	odswiezPodsumowanie()

	tlo.Activated:Connect(zamknij)
	local kamera = workspace.CurrentCamera
	if kamera then
		kamera:GetPropertyChangedSignal("ViewportSize"):Connect(przeliczSkale)
	end

	-- "x min temu" przy alarmach w toku
	task.spawn(function()
		while true do
			task.wait(20)
			if stan.otwarte then
				rysujAktywne()
			end
		end
	end)
end

return Panel
