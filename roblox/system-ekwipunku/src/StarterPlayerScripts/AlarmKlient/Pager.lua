--[[
	SYSTEM ALARMOWANIA OSP  |  Pager strażaka
	Karta alarmu z przyciskami JADĘ / NIE JADĘ, piszczenie, błysk ekranu
	i znacznik miejsca zdarzenia (słup światła + odległość) dla jadących.
]]

local Players = game:GetService("Players")
local RunService = game:GetService("RunService")
local SoundService = game:GetService("SoundService")

local W = require(script.Parent:WaitForChild("Wyglad"))
local K = W.KOLOR

local gracz = Players.LocalPlayer

local Pager = {}

local ctx, Ustawienia
local kontener, skala, blysk
local karty = {} -- [id] = karta
local kolejnosc = 0

-- ============================================================
--  Pomocnicze
-- ============================================================

local function korzenPostaci()
	local postac = gracz.Character
	return postac and postac:FindFirstChild("HumanoidRootPart")
end

local function blyskEkranu(kolor)
	blysk.BackgroundColor3 = kolor
	task.spawn(function()
		for _ = 1, 2 do
			blysk.BackgroundTransparency = 0.75
			W.animuj(blysk, { BackgroundTransparency = 1 }, TweenInfo.new(0.45)).Completed:Wait()
		end
	end)
end

local function piszcz(karta)
	local id = Ustawienia.DzwiekPagera
	if not id or id == "" then
		return
	end
	local dzwiek = W.nowy("Sound", {
		Name = "PagerOSP",
		SoundId = id,
		Volume = Ustawienia.GlosnoscPagera or 0.8,
		Parent = SoundService,
	})
	for _ = 1, Ustawienia.IlePiskowPagera or 6 do
		if karta.Odpowiedz or karta.Koniec then
			break
		end
		for _ = 1, 3 do
			dzwiek:Play()
			task.wait(0.2)
		end
		task.wait(2.2)
	end
	dzwiek:Destroy()
end

local function tekstOdpowiedzi(dane)
	return ('<font color="#%s">Jadą: %d</font>  •  Nie jadą: %d  •  Zaalarmowano: %d'):format(
		K.Zielony:ToHex(),
		#(dane.Jada or {}),
		#(dane.NieJada or {}),
		dane.Odbiorcow or 0
	)
end

-- ============================================================
--  Znacznik miejsca zdarzenia
-- ============================================================

local function usunZnacznik(karta)
	if karta.Znacznik then
		karta.Znacznik:Destroy()
		karta.Znacznik = nil
		karta.NapisZnacznika = nil
	end
end

local function utworzZnacznik(karta)
	local dane = karta.Dane
	if not dane.Pozycja or karta.Znacznik then
		return
	end
	local rodzaj = W.znajdzRodzaj(Ustawienia, dane.Rodzaj)

	local model = Instance.new("Model")
	model.Name = "ZnacznikAlarmuOSP"
	-- Słup światła widoczny z daleka (tylko u tego gracza)
	W.nowy("Part", {
		Name = "Slup",
		Shape = Enum.PartType.Cylinder,
		Size = Vector3.new(160, 1.2, 1.2),
		CFrame = CFrame.new(dane.Pozycja + Vector3.new(0, 80, 0)) * CFrame.Angles(0, 0, math.rad(90)),
		Anchored = true,
		CanCollide = false,
		CanQuery = false,
		CanTouch = false,
		CastShadow = false,
		Material = Enum.Material.Neon,
		Color = rodzaj.Kolor,
		Transparency = 0.55,
		Parent = model,
	})
	local punkt = W.nowy("Part", {
		Name = "Punkt",
		Size = Vector3.new(1, 1, 1),
		CFrame = CFrame.new(dane.Pozycja),
		Anchored = true,
		CanCollide = false,
		CanQuery = false,
		CanTouch = false,
		Transparency = 1,
		Parent = model,
	})
	local tablica = W.nowy("BillboardGui", {
		AlwaysOnTop = true,
		LightInfluence = 0,
		Size = UDim2.fromOffset(210, 48),
		StudsOffsetWorldSpace = Vector3.new(0, 7, 0),
		Parent = punkt,
	})
	local pigulka = W.nowy("Frame", {
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = K.Okno,
		BackgroundTransparency = 0.1,
		Parent = tablica,
	})
	W.zaokraglij(pigulka, 12)
	W.obrysuj(pigulka, 0.2, rodzaj.Kolor)
	W.napis({
		Text = "📍 " .. (dane.Miejsce or "Miejsce zdarzenia"),
		FontFace = W.czcionka(Enum.FontWeight.Bold),
		TextSize = 13,
		TextXAlignment = Enum.TextXAlignment.Center,
		TextTruncate = Enum.TextTruncate.AtEnd,
		Position = UDim2.fromOffset(8, 6),
		Size = UDim2.new(1, -16, 0, 18),
		Parent = pigulka,
	})
	karta.NapisZnacznika = W.napis({
		Text = "",
		TextSize = 12,
		TextColor3 = K.TekstSlaby,
		TextXAlignment = Enum.TextXAlignment.Center,
		Position = UDim2.fromOffset(8, 25),
		Size = UDim2.new(1, -16, 0, 16),
		Parent = pigulka,
	})
	model.Parent = workspace
	karta.Znacznik = model
end

-- ============================================================
--  Karta alarmu
-- ============================================================

local function schowaj(karta)
	local ramka = karta.Ramka
	if not ramka then
		return
	end
	karta.Ramka = nil
	if karta.Puls then
		karta.Puls:Cancel()
	end
	W.animuj(ramka, { GroupTransparency = 1 }, TweenInfo.new(0.3)).Completed:Wait()
	ramka:Destroy()
end

local function odpowiedz(karta, wartosc)
	if karta.Koniec or karta.Odpowiedz then
		return
	end
	karta.Odpowiedz = wartosc
	ctx.Zdarzenie("Odpowiedz", karta.Dane.Id, wartosc)

	if karta.Puls then
		karta.Puls:Cancel()
		karta.Puls = nil
		karta.Obrys.Transparency = 0.6
	end
	karta.Przyciski.Visible = false
	karta.Status.Visible = true

	if wartosc == "Jade" then
		if karta.Dane.Pozycja then
			karta.NapisStatusu.Text = "✓ Jedziesz. Słup światła wskazuje miejsce."
			utworzZnacznik(karta)
		else
			karta.NapisStatusu.Text = "✓ Potwierdzono wyjazd."
		end
	else
		karta.NapisStatusu.Text = "Nie jedziesz na to zdarzenie."
		task.delay(3, function()
			schowaj(karta)
		end)
	end
end

local function utworzKarte(dane)
	local rodzaj = W.znajdzRodzaj(Ustawienia, dane.Rodzaj)
	kolejnosc += 1
	local karta = { Dane = dane }

	local ramka = W.nowy("CanvasGroup", {
		Size = UDim2.fromOffset(340, 0),
		AutomaticSize = Enum.AutomaticSize.Y,
		BackgroundColor3 = K.Okno,
		BackgroundTransparency = 0.04,
		GroupTransparency = 1,
		LayoutOrder = -kolejnosc, -- najnowszy alarm na górze
		Parent = kontener,
	})
	W.zaokraglij(ramka, 14)
	karta.Obrys = W.obrysuj(ramka, 0, rodzaj.Kolor)
	karta.Obrys.Thickness = 2
	W.nowy("UIPadding", {
		PaddingTop = UDim.new(0, 14),
		PaddingBottom = UDim.new(0, 14),
		PaddingLeft = UDim.new(0, 14),
		PaddingRight = UDim.new(0, 14),
		Parent = ramka,
	})
	W.nowy("UIListLayout", {
		Padding = UDim.new(0, 6),
		SortOrder = Enum.SortOrder.LayoutOrder,
		Parent = ramka,
	})

	-- Nagłówek: ALARM + godzina
	local naglowek = W.nowy("Frame", {
		BackgroundTransparency = 1,
		Size = UDim2.new(1, 0, 0, 18),
		LayoutOrder = 1,
		Parent = ramka,
	})
	local kropka = W.nowy("Frame", {
		Position = UDim2.fromOffset(0, 5),
		Size = UDim2.fromOffset(8, 8),
		BackgroundColor3 = rodzaj.Kolor,
		BorderSizePixel = 0,
		Parent = naglowek,
	})
	W.zaokraglij(kropka, 4)
	karta.NapisAlarm = W.napis({
		Text = "ALARM",
		FontFace = W.czcionka(Enum.FontWeight.Heavy),
		TextSize = 13,
		TextColor3 = rodzaj.Kolor,
		Position = UDim2.fromOffset(14, 0),
		Size = UDim2.new(1, -80, 1, 0),
		Parent = naglowek,
	})
	W.napis({
		Text = os.date("%H:%M", dane.Czas),
		TextSize = 12,
		TextColor3 = K.TekstSlaby,
		TextXAlignment = Enum.TextXAlignment.Right,
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.fromScale(1, 0),
		Size = UDim2.new(0, 60, 1, 0),
		Parent = naglowek,
	})

	W.napis({
		Text = rodzaj.Ikona .. "  " .. dane.Rodzaj,
		FontFace = W.czcionka(Enum.FontWeight.Bold),
		TextSize = 20,
		TextTruncate = Enum.TextTruncate.AtEnd,
		Size = UDim2.new(1, 0, 0, 26),
		LayoutOrder = 2,
		Parent = ramka,
	})
	karta.NapisMiejsca = W.napis({
		Text = "📍 " .. (dane.Miejsce or ""),
		TextSize = 13,
		TextTruncate = Enum.TextTruncate.AtEnd,
		Size = UDim2.new(1, 0, 0, 18),
		LayoutOrder = 3,
		Visible = dane.Miejsce ~= nil,
		Parent = ramka,
	})
	W.napis({
		Text = dane.Opis or "",
		TextSize = 13,
		TextColor3 = K.TekstSlaby,
		TextWrapped = true,
		AutomaticSize = Enum.AutomaticSize.Y,
		Size = UDim2.new(1, 0, 0, 0),
		LayoutOrder = 4,
		Visible = (dane.Opis or "") ~= "",
		Parent = ramka,
	})
	W.napis({
		Text = table.concat(dane.Jednostki or {}, ", ") .. "  •  Dyspozytor: " .. tostring(dane.Autor),
		TextSize = 11,
		TextColor3 = K.TekstSlaby,
		TextTruncate = Enum.TextTruncate.AtEnd,
		Size = UDim2.new(1, 0, 0, 14),
		LayoutOrder = 5,
		Parent = ramka,
	})
	karta.NapisOdpowiedzi = W.napis({
		Text = tekstOdpowiedzi(dane),
		RichText = true,
		TextSize = 11,
		Size = UDim2.new(1, 0, 0, 14),
		LayoutOrder = 6,
		Parent = ramka,
	})

	-- Przyciski JADĘ / NIE JADĘ
	karta.Przyciski = W.nowy("Frame", {
		BackgroundTransparency = 1,
		Size = UDim2.new(1, 0, 0, 36),
		LayoutOrder = 7,
		Parent = ramka,
	})
	local jade = W.przycisk({
		Text = "JADĘ",
		FontFace = W.czcionka(Enum.FontWeight.Heavy),
		TextColor3 = K.TekstCiemny,
		BackgroundColor3 = K.Zielony,
		Size = UDim2.new(0.5, -4, 1, 0),
		Parent = karta.Przyciski,
	})
	W.zaokraglij(jade, 9)
	W.najechanie(jade)
	local nieJade = W.przycisk({
		Text = "NIE JADĘ",
		Position = UDim2.new(0.5, 4, 0, 0),
		Size = UDim2.new(0.5, -4, 1, 0),
		Parent = karta.Przyciski,
	})
	W.zaokraglij(nieJade, 9)
	W.obrysuj(nieJade, 0.88)
	W.najechanie(nieJade)
	jade.Activated:Connect(function()
		odpowiedz(karta, "Jade")
	end)
	nieJade.Activated:Connect(function()
		odpowiedz(karta, "NieJade")
	end)

	-- Stan po odpowiedzi
	karta.Status = W.nowy("Frame", {
		BackgroundTransparency = 1,
		Size = UDim2.new(1, 0, 0, 28),
		LayoutOrder = 8,
		Visible = false,
		Parent = ramka,
	})
	karta.NapisStatusu = W.napis({
		FontFace = W.czcionka(Enum.FontWeight.Medium),
		TextSize = 12,
		TextWrapped = true,
		Size = UDim2.new(1, -70, 1, 0),
		Parent = karta.Status,
	})
	local ukryj = W.przycisk({
		Text = "Ukryj",
		TextSize = 11,
		AnchorPoint = Vector2.new(1, 0.5),
		Position = UDim2.fromScale(1, 0.5),
		Size = UDim2.fromOffset(60, 26),
		Parent = karta.Status,
	})
	W.zaokraglij(ukryj, 7)
	W.najechanie(ukryj)
	ukryj.Activated:Connect(function()
		schowaj(karta)
	end)

	karta.Ramka = ramka
	W.animuj(ramka, { GroupTransparency = 0 }, TweenInfo.new(0.3))
	karta.Puls = W.animuj(
		karta.Obrys,
		{ Transparency = 0.85 },
		TweenInfo.new(0.5, Enum.EasingStyle.Sine, Enum.EasingDirection.InOut, -1, true)
	)
	return karta
end

-- ============================================================
--  API dla skryptu głównego
-- ============================================================

function Pager.Nowy(dane)
	if typeof(dane) ~= "table" or karty[dane.Id] then
		return
	end
	local karta = utworzKarte(dane)
	karty[dane.Id] = karta
	blyskEkranu(W.znajdzRodzaj(Ustawienia, dane.Rodzaj).Kolor)
	task.spawn(piszcz, karta)
end

function Pager.Aktualizuj(dane)
	local karta = typeof(dane) == "table" and karty[dane.Id]
	if not karta then
		return
	end
	karta.Dane = dane
	if karta.Ramka then
		karta.NapisOdpowiedzi.Text = tekstOdpowiedzi(dane)
	end
end

function Pager.Koniec(dane)
	local karta = typeof(dane) == "table" and karty[dane.Id]
	if not karta then
		return
	end
	karty[dane.Id] = nil
	karta.Koniec = true
	usunZnacznik(karta)
	if karta.Ramka then
		if karta.Puls then
			karta.Puls:Cancel()
			karta.Puls = nil
		end
		karta.Obrys.Transparency = 0.8
		karta.NapisAlarm.Text = "ALARM ZAKOŃCZONY"
		karta.NapisAlarm.TextColor3 = K.TekstSlaby
		karta.NapisOdpowiedzi.Text = tekstOdpowiedzi(dane)
		karta.Przyciski.Visible = false
		karta.Status.Visible = true
		karta.NapisStatusu.Text = "Akcja zakończona."
		task.delay(4, function()
			schowaj(karta)
		end)
	end
end

function Pager.Inicjuj(ekran, kontekst)
	ctx = kontekst
	Ustawienia = kontekst.Ustawienia

	blysk = W.nowy("Frame", {
		Size = UDim2.fromScale(1, 1),
		BackgroundTransparency = 1,
		BorderSizePixel = 0,
		ZIndex = 0,
		Parent = ekran,
	})

	kontener = W.nowy("Frame", {
		Name = "Pager",
		BackgroundTransparency = 1,
		AnchorPoint = Vector2.new(1, 0.5),
		Position = UDim2.new(1, -20, 0.5, 0),
		Size = UDim2.fromOffset(340, 620),
		ZIndex = 5,
		Parent = ekran,
	})
	W.nowy("UIListLayout", {
		Padding = UDim.new(0, 10),
		VerticalAlignment = Enum.VerticalAlignment.Center,
		SortOrder = Enum.SortOrder.LayoutOrder,
		Parent = kontener,
	})
	skala = W.nowy("UIScale", { Parent = kontener })

	local function przeliczSkale()
		local kamera = workspace.CurrentCamera
		local rozmiar = kamera and kamera.ViewportSize or Vector2.new(1280, 720)
		skala.Scale = math.clamp(math.min(rozmiar.X / 1280, rozmiar.Y / 720), 0.6, 1.15)
	end
	przeliczSkale()
	local kamera = workspace.CurrentCamera
	if kamera then
		kamera:GetPropertyChangedSignal("ViewportSize"):Connect(przeliczSkale)
	end

	-- Odległość do miejsca zdarzenia (karta i znacznik) + "jesteś na miejscu"
	local nastepnyPomiar = 0
	RunService.Heartbeat:Connect(function()
		if os.clock() < nastepnyPomiar then
			return
		end
		nastepnyPomiar = os.clock() + 0.25
		local korzen = korzenPostaci()
		if not korzen then
			return
		end
		for _, karta in karty do
			local pozycja = karta.Dane.Pozycja
			if pozycja then
				local dystans = (korzen.Position - pozycja).Magnitude
				local metry = W.metry(dystans)
				if karta.Ramka then
					karta.NapisMiejsca.Text = ("📍 %s  •  %s"):format(karta.Dane.Miejsce or "", metry)
				end
				if karta.NapisZnacznika then
					karta.NapisZnacznika.Text = metry
				end
				if karta.Znacznik and dystans <= (Ustawienia.OdlegloscNaMiejscu or 25) then
					usunZnacznik(karta)
					ctx.Powiadom("Jesteś na miejscu zdarzenia.", "sukces")
				end
			end
		end
	end)
end

return Pager
