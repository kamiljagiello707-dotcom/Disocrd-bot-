--[[
	MENTOR  |  Menu rozmowy
	Dymek z tekstem mentora (pisze się na żywo), lista rozdziałów,
	kroki, przyciski Wstecz / Dalej i "Pokaż drogę".
]]

local Players = game:GetService("Players")
local UserInputService = game:GetService("UserInputService")

local W = require(script.Parent:WaitForChild("Wyglad"))
local K = W.KOLOR

local gracz = Players.LocalPlayer

local Menu = {}

local SZEROKOSC, WYSOKOSC = 640, 480

local ctx, Ustawienia, AKCENT
local okno, skala, przyciemnienie
local ramkaPortretu, tekstDymka, listaRozdzialow, listaKrokow
local naglowekRozdzialu, wypelnieniePostepu, tytulRozdzialu
local przyciskDrogi, przyciskTeleportu, przyciskWstecz, przyciskDalej

local stan = { otwarte = false, rozdzial = 1, przeczytane = {} }
local wierszeRozdzialow = {}
local brakujaceCele = {}
local bazowaSkala = 1
local numerPisania = 0

-- ============================================================
--  Pomocnicze
-- ============================================================

local function podstaw(tekst)
	local nick = string.gsub(gracz.DisplayName, "%%", "%%%%")
	local serwer = string.gsub(Ustawienia.NazwaSerwera or "", "%%", "%%%%")
	tekst = string.gsub(tekst or "", "{nick}", nick)
	tekst = string.gsub(tekst, "{serwer}", serwer)
	return tekst
end

-- Tekst w dymku pojawia się litera po literze
local function napisz(tekst)
	numerPisania += 1
	local moje = numerPisania
	tekstDymka.Text = tekst
	tekstDymka.MaxVisibleGraphemes = 0
	local dlugosc = utf8.len(tekst) or #tekst
	local szybkosc = math.max(Ustawienia.SzybkoscPisania or 60, 1)
	local poczatek = os.clock()
	task.spawn(function()
		while moje == numerPisania do
			local widoczne = math.floor((os.clock() - poczatek) * szybkosc)
			if widoczne >= dlugosc then
				break
			end
			tekstDymka.MaxVisibleGraphemes = widoczne
			task.wait()
		end
		if moje == numerPisania then
			tekstDymka.MaxVisibleGraphemes = -1
		end
	end)
end

local function dokonczPisanie()
	numerPisania += 1
	tekstDymka.MaxVisibleGraphemes = -1
end

local function ustawPortret(model)
	for _, dziecko in ramkaPortretu:GetChildren() do
		if not dziecko:IsA("UICorner") then
			dziecko:Destroy()
		end
	end
	local glowa = model and model:FindFirstChild("Head")
	local kopia
	if glowa then
		local archiwalny = model.Archivable
		model.Archivable = true
		local ok, wynik = pcall(function()
			return model:Clone()
		end)
		model.Archivable = archiwalny
		kopia = ok and wynik or nil
	end
	if not kopia or not kopia:FindFirstChild("Head") then
		W.napis({
			Text = "🧑",
			TextScaled = true,
			TextXAlignment = Enum.TextXAlignment.Center,
			Size = UDim2.fromScale(1, 1),
			Parent = ramkaPortretu,
		})
		return
	end
	for _, obiekt in kopia:GetDescendants() do
		if obiekt:IsA("LuaSourceContainer") or obiekt:IsA("ProximityPrompt") or obiekt:IsA("LayerCollector") then
			obiekt:Destroy()
		end
	end
	local podglad = W.nowy("ViewportFrame", {
		BackgroundTransparency = 1,
		Size = UDim2.fromScale(1, 1),
		Ambient = Color3.fromRGB(200, 200, 205),
		LightDirection = Vector3.new(-0.3, -0.6, -1),
		Parent = ramkaPortretu,
	})
	kopia.Parent = podglad
	local kopiaGlowy = kopia:FindFirstChild("Head")
	local kamera = W.nowy("Camera", { FieldOfView = 30, Parent = podglad })
	kamera.CFrame = CFrame.lookAt(
		kopiaGlowy.Position + kopiaGlowy.CFrame.LookVector * 3.4 + Vector3.new(0, 0.2, 0),
		kopiaGlowy.Position - Vector3.new(0, 0.2, 0)
	)
	podglad.CurrentCamera = kamera
end

-- ============================================================
--  Rozdziały
-- ============================================================

local pokazRozdzial -- niżej

local function stylListy()
	for indeks, wiersz in wierszeRozdzialow do
		local aktywny = indeks == stan.rozdzial
		W.kolorPrzycisku(wiersz.Przycisk, aktywny and K.Bialy or K.Slot)
		wiersz.Tytul.TextColor3 = aktywny and K.TekstCiemny or K.Tekst
		wiersz.Znak.Visible = stan.przeczytane[indeks] == true and not aktywny
	end
end

local function zbudujListe()
	for indeks, rozdzial in Ustawienia.Rozdzialy do
		local przycisk = W.przycisk({
			Text = "",
			Size = UDim2.new(1, -6, 0, 34),
			LayoutOrder = indeks,
			Parent = listaRozdzialow,
		})
		W.zaokraglij(przycisk, 9)
		W.napis({
			Text = rozdzial.Ikona or "•",
			TextSize = 15,
			TextXAlignment = Enum.TextXAlignment.Center,
			Position = UDim2.fromOffset(8, 0),
			Size = UDim2.new(0, 24, 1, 0),
			Parent = przycisk,
		})
		local tytul = W.napis({
			Text = podstaw(rozdzial.Tytul),
			FontFace = W.czcionka(Enum.FontWeight.Bold),
			TextSize = 12,
			TextTruncate = Enum.TextTruncate.AtEnd,
			Position = UDim2.fromOffset(38, 0),
			Size = UDim2.new(1, -64, 1, 0),
			Parent = przycisk,
		})
		local znak = W.napis({
			Text = "✓",
			FontFace = W.czcionka(Enum.FontWeight.Bold),
			TextSize = 13,
			TextColor3 = K.Zielony,
			TextXAlignment = Enum.TextXAlignment.Center,
			AnchorPoint = Vector2.new(1, 0.5),
			Position = UDim2.new(1, -8, 0.5, 0),
			Size = UDim2.fromOffset(16, 16),
			Visible = false,
			Parent = przycisk,
		})
		W.najechanie(przycisk, function()
			return stan.rozdzial ~= indeks
		end)
		przycisk.Activated:Connect(function()
			pokazRozdzial(indeks)
		end)
		wierszeRozdzialow[indeks] = { Przycisk = przycisk, Tytul = tytul, Znak = znak }
	end
end

local function rysujKroki(kroki)
	for _, dziecko in listaKrokow:GetChildren() do
		if dziecko:IsA("Frame") then
			dziecko:Destroy()
		end
	end
	for indeks, krok in kroki or {} do
		local karta = W.nowy("Frame", {
			Size = UDim2.new(1, -8, 0, 0),
			AutomaticSize = Enum.AutomaticSize.Y,
			BackgroundColor3 = K.Slot,
			LayoutOrder = indeks,
			Parent = listaKrokow,
		})
		W.zaokraglij(karta, 10)
		W.obrysuj(karta, 0.93)
		W.nowy("UIPadding", {
			PaddingTop = UDim.new(0, 10),
			PaddingBottom = UDim.new(0, 10),
			PaddingLeft = UDim.new(0, 10),
			PaddingRight = UDim.new(0, 12),
			Parent = karta,
		})
		local numer = W.nowy("Frame", {
			Size = UDim2.fromOffset(24, 24),
			BackgroundColor3 = AKCENT,
			Parent = karta,
		})
		W.zaokraglij(numer, 12)
		W.napis({
			Text = tostring(indeks),
			FontFace = W.czcionka(Enum.FontWeight.Heavy),
			TextSize = 12,
			TextColor3 = Color3.new(1, 1, 1),
			TextXAlignment = Enum.TextXAlignment.Center,
			Size = UDim2.fromScale(1, 1),
			Parent = numer,
		})
		W.napis({
			Text = podstaw(krok),
			TextSize = 13,
			TextWrapped = true,
			LineHeight = 1.1,
			AutomaticSize = Enum.AutomaticSize.Y,
			Position = UDim2.fromOffset(36, 3),
			Size = UDim2.new(1, -36, 0, 0),
			Parent = karta,
		})
	end
	listaKrokow.CanvasPosition = Vector2.zero
end

pokazRozdzial = function(indeks)
	local liczba = #Ustawienia.Rozdzialy
	if liczba == 0 then
		return
	end
	indeks = math.clamp(indeks, 1, liczba)
	stan.rozdzial = indeks
	stan.przeczytane[indeks] = true
	local rozdzial = Ustawienia.Rozdzialy[indeks]

	napisz(podstaw(rozdzial.Tekst))
	naglowekRozdzialu.Text = ("ROZDZIAŁ %d / %d"):format(indeks, liczba)
	W.animuj(wypelnieniePostepu, { Size = UDim2.fromScale(indeks / liczba, 1) }, TweenInfo.new(0.25))
	tytulRozdzialu.Text = podstaw(rozdzial.Tytul)
	rysujKroki(rozdzial.Kroki)

	-- Przycisk drogi: tylko gdy cel naprawdę jest na mapie
	local cel = rozdzial.Przycisk and rozdzial.Przycisk.Cel
	local celJest = cel ~= nil and ctx.MaCel(cel)
	if cel and not celJest and not brakujaceCele[cel] then
		brakujaceCele[cel] = true
		warn(("[Mentor] Nie ma części z Tagiem MentorCel o nazwie '%s' (rozdział %d)."):format(cel, indeks))
	end
	przyciskDrogi.Visible = celJest
	przyciskTeleportu.Visible = celJest and Ustawienia.PozwolNaTeleport == true
	if celJest then
		przyciskDrogi.Text = "📍  " .. podstaw(rozdzial.Przycisk.Tekst or "Pokaż drogę")
	end

	przyciskWstecz.Visible = indeks > 1
	przyciskDalej.Text = indeks == liczba and "Zakończ" or "Dalej  ›"
	stylListy()
end

-- ============================================================
--  Otwieranie i zamykanie
-- ============================================================

local function przeliczSkale()
	local kamera = workspace.CurrentCamera
	local ekran = kamera and kamera.ViewportSize or Vector2.new(1280, 720)
	local naSzerokosc = Ustawienia.KameraNaMentora and 0.56 or 0.94
	if UserInputService.TouchEnabled then
		naSzerokosc = math.max(naSzerokosc, 0.7)
	end
	bazowaSkala = math.clamp(math.min(ekran.X * naSzerokosc / SZEROKOSC, ekran.Y * 0.86 / WYSOKOSC, 1.25), 0.4, 1.25)
	if stan.otwarte then
		skala.Scale = bazowaSkala
	end
end

function Menu.Otwarte()
	return stan.otwarte
end

function Menu.Zamknij()
	if not stan.otwarte then
		return
	end
	stan.otwarte = false
	dokonczPisanie()
	local chowanie = W.animuj(
		skala,
		{ Scale = bazowaSkala * 0.94 },
		TweenInfo.new(0.14, Enum.EasingStyle.Quad, Enum.EasingDirection.In)
	)
	W.animuj(przyciemnienie, { BackgroundTransparency = 1 }, TweenInfo.new(0.2))
	chowanie.Completed:Connect(function()
		if not stan.otwarte then
			okno.Visible = false
			przyciemnienie.Visible = false
		end
	end)
	ctx.PoZamknieciu()
end

function Menu.Otworz(model)
	if stan.otwarte or #Ustawienia.Rozdzialy == 0 then
		return
	end
	stan.otwarte = true
	ustawPortret(model)
	przeliczSkale()
	okno.Visible = true
	przyciemnienie.Visible = true
	skala.Scale = bazowaSkala * 0.94
	W.animuj(skala, { Scale = bazowaSkala }, TweenInfo.new(0.25, Enum.EasingStyle.Back, Enum.EasingDirection.Out))
	W.animuj(przyciemnienie, { BackgroundTransparency = 0 }, TweenInfo.new(0.3))
	pokazRozdzial(stan.rozdzial)
end

-- ============================================================
--  Budowa okna
-- ============================================================

function Menu.Inicjuj(ekran, kontekst)
	ctx = kontekst
	Ustawienia = kontekst.Ustawienia
	AKCENT = Ustawienia.KolorAkcentu or K.Czerwony

	-- Przyciemnienie: z kamerą na mentora ciemniej tylko po prawej (tam jest menu)
	przyciemnienie = W.nowy("Frame", {
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = Color3.new(0, 0, 0),
		BackgroundTransparency = 1,
		BorderSizePixel = 0,
		Visible = false,
		ZIndex = 0,
		Parent = ekran,
	})
	W.nowy("UIGradient", {
		Transparency = Ustawienia.KameraNaMentora and NumberSequence.new({
			NumberSequenceKeypoint.new(0, 1),
			NumberSequenceKeypoint.new(0.35, 0.85),
			NumberSequenceKeypoint.new(1, 0.35),
		}) or NumberSequence.new(0.5),
		Parent = przyciemnienie,
	})

	okno = W.nowy("Frame", {
		Name = "MenuMentora",
		ZIndex = 2,
		AnchorPoint = Ustawienia.KameraNaMentora and Vector2.new(1, 0.5) or Vector2.new(0.5, 0.5),
		Position = Ustawienia.KameraNaMentora and UDim2.new(1, -40, 0.5, 0) or UDim2.fromScale(0.5, 0.5),
		Size = UDim2.fromOffset(SZEROKOSC, WYSOKOSC),
		BackgroundColor3 = K.Okno,
		BackgroundTransparency = 0.06,
		Visible = false,
		Parent = ekran,
	})
	W.zaokraglij(okno, 16)
	W.obrysuj(okno, 0.86)
	skala = W.nowy("UIScale", { Parent = okno })
	-- Modal odblokowuje myszkę w pierwszej osobie
	W.nowy("TextButton", {
		Text = "",
		Modal = true,
		BackgroundTransparency = 1,
		Size = UDim2.fromScale(1, 1),
		ZIndex = 0,
		AutoButtonColor = false,
		Parent = okno,
	})

	-- Nagłówek: portret, imię, tytuł
	ramkaPortretu = W.nowy("Frame", {
		Position = UDim2.fromOffset(22, 16),
		Size = UDim2.fromOffset(48, 48),
		BackgroundColor3 = K.Slot,
		ClipsDescendants = true,
		Parent = okno,
	})
	W.zaokraglij(ramkaPortretu, 12)
	W.obrysuj(ramkaPortretu, 0.4, AKCENT)
	W.napis({
		Text = Ustawienia.Imie,
		FontFace = W.czcionka(Enum.FontWeight.Bold),
		TextSize = 17,
		Position = UDim2.fromOffset(82, 19),
		Size = UDim2.fromOffset(460, 20),
		Parent = okno,
	})
	W.napis({
		Text = Ustawienia.Tytul,
		TextSize = 12,
		TextColor3 = K.TekstSlaby,
		Position = UDim2.fromOffset(82, 41),
		Size = UDim2.fromOffset(460, 14),
		Parent = okno,
	})
	local zamknij = W.przycisk({
		Text = "✕",
		TextSize = 16,
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.new(1, -18, 0, 16),
		Size = UDim2.fromOffset(34, 34),
		Parent = okno,
	})
	W.zaokraglij(zamknij, 9)
	W.obrysuj(zamknij, 0.88)
	W.najechanie(zamknij)
	zamknij.Activated:Connect(Menu.Zamknij)

	-- Dymek z tekstem mentora (kliknięcie pokazuje od razu cały tekst)
	local strzalka = W.nowy("Frame", {
		Position = UDim2.fromOffset(40, 72),
		Size = UDim2.fromOffset(12, 12),
		Rotation = 45,
		BackgroundColor3 = K.Panel,
		BorderSizePixel = 0,
		Parent = okno,
	})
	strzalka.ZIndex = 1
	local dymek = W.przycisk({
		Text = "",
		Position = UDim2.fromOffset(22, 77),
		Size = UDim2.fromOffset(SZEROKOSC - 44, 72),
		BackgroundColor3 = K.Panel,
		ZIndex = 2,
		Parent = okno,
	})
	W.zaokraglij(dymek, 12)
	tekstDymka = W.napis({
		TextSize = 13,
		TextWrapped = true,
		LineHeight = 1.15,
		TextYAlignment = Enum.TextYAlignment.Center,
		Position = UDim2.fromOffset(16, 8),
		Size = UDim2.new(1, -32, 1, -16),
		Parent = dymek,
	})
	dymek.Activated:Connect(dokonczPisanie)

	-- Lista rozdziałów
	listaRozdzialow = W.nowy("ScrollingFrame", {
		BackgroundTransparency = 1,
		BorderSizePixel = 0,
		Position = UDim2.fromOffset(22, 164),
		Size = UDim2.fromOffset(196, 250),
		CanvasSize = UDim2.new(),
		AutomaticCanvasSize = Enum.AutomaticSize.Y,
		ScrollBarThickness = 3,
		ScrollBarImageColor3 = K.Bialy,
		ScrollBarImageTransparency = 0.7,
		Parent = okno,
	})
	W.nowy("UIListLayout", { Padding = UDim.new(0, 5), SortOrder = Enum.SortOrder.LayoutOrder, Parent = listaRozdzialow })

	-- Treść rozdziału
	naglowekRozdzialu = W.napis({
		FontFace = W.czcionka(Enum.FontWeight.Bold),
		TextSize = 11,
		TextColor3 = K.TekstSlaby,
		Position = UDim2.fromOffset(232, 164),
		Size = UDim2.fromOffset(386, 14),
		Parent = okno,
	})
	local torPostepu = W.nowy("Frame", {
		Position = UDim2.fromOffset(232, 182),
		Size = UDim2.fromOffset(386, 3),
		BackgroundColor3 = K.Slot,
		BorderSizePixel = 0,
		Parent = okno,
	})
	W.zaokraglij(torPostepu, 2)
	wypelnieniePostepu = W.nowy("Frame", {
		Size = UDim2.fromScale(0, 1),
		BackgroundColor3 = AKCENT,
		BorderSizePixel = 0,
		Parent = torPostepu,
	})
	W.zaokraglij(wypelnieniePostepu, 2)
	tytulRozdzialu = W.napis({
		FontFace = W.czcionka(Enum.FontWeight.Heavy),
		TextSize = 20,
		TextTruncate = Enum.TextTruncate.AtEnd,
		Position = UDim2.fromOffset(232, 192),
		Size = UDim2.fromOffset(386, 26),
		Parent = okno,
	})
	listaKrokow = W.nowy("ScrollingFrame", {
		BackgroundTransparency = 1,
		BorderSizePixel = 0,
		Position = UDim2.fromOffset(232, 226),
		Size = UDim2.fromOffset(390, 188),
		CanvasSize = UDim2.new(),
		AutomaticCanvasSize = Enum.AutomaticSize.Y,
		ScrollBarThickness = 3,
		ScrollBarImageColor3 = K.Bialy,
		ScrollBarImageTransparency = 0.7,
		Parent = okno,
	})
	W.nowy("UIListLayout", { Padding = UDim.new(0, 8), SortOrder = Enum.SortOrder.LayoutOrder, Parent = listaKrokow })

	-- Dół: droga / teleport po lewej, Wstecz / Dalej po prawej
	local akcje = W.nowy("Frame", {
		BackgroundTransparency = 1,
		Position = UDim2.fromOffset(22, 428),
		Size = UDim2.fromOffset(350, 36),
		Parent = okno,
	})
	W.nowy("UIListLayout", {
		FillDirection = Enum.FillDirection.Horizontal,
		Padding = UDim.new(0, 8),
		SortOrder = Enum.SortOrder.LayoutOrder,
		Parent = akcje,
	})
	przyciskDrogi = W.przycisk({
		Text = "📍  Pokaż drogę",
		TextSize = 13,
		TextColor3 = Color3.new(1, 1, 1),
		TextTruncate = Enum.TextTruncate.AtEnd,
		BackgroundColor3 = AKCENT,
		AutomaticSize = Enum.AutomaticSize.X,
		Size = UDim2.fromOffset(0, 36),
		LayoutOrder = 1,
		Visible = false,
		Parent = akcje,
	})
	W.zaokraglij(przyciskDrogi, 9)
	W.nowy("UIPadding", { PaddingLeft = UDim.new(0, 14), PaddingRight = UDim.new(0, 14), Parent = przyciskDrogi })
	W.nowy("UISizeConstraint", { MaxSize = Vector2.new(230, 36), Parent = przyciskDrogi })
	W.najechanie(przyciskDrogi)
	przyciskTeleportu = W.przycisk({
		Text = "Zabierz mnie tam",
		TextSize = 13,
		Size = UDim2.fromOffset(140, 36),
		LayoutOrder = 2,
		Visible = false,
		Parent = akcje,
	})
	W.zaokraglij(przyciskTeleportu, 9)
	W.obrysuj(przyciskTeleportu, 0.88)
	W.najechanie(przyciskTeleportu)

	przyciskWstecz = W.przycisk({
		Text = "‹  Wstecz",
		TextSize = 13,
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.new(1, -158, 0, 428),
		Size = UDim2.fromOffset(100, 36),
		Parent = okno,
	})
	W.zaokraglij(przyciskWstecz, 9)
	W.obrysuj(przyciskWstecz, 0.88)
	W.najechanie(przyciskWstecz)
	przyciskDalej = W.przycisk({
		Text = "Dalej  ›",
		TextSize = 13,
		TextColor3 = K.TekstCiemny,
		BackgroundColor3 = K.Bialy,
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.new(1, -22, 0, 428),
		Size = UDim2.fromOffset(128, 36),
		Parent = okno,
	})
	W.zaokraglij(przyciskDalej, 9)
	W.najechanie(przyciskDalej)

	local function aktualnyCel()
		local rozdzial = Ustawienia.Rozdzialy[stan.rozdzial]
		return rozdzial and rozdzial.Przycisk and rozdzial.Przycisk.Cel
	end
	przyciskDrogi.Activated:Connect(function()
		local cel = aktualnyCel()
		if cel then
			Menu.Zamknij()
			ctx.PokazDroge(cel)
		end
	end)
	przyciskTeleportu.Activated:Connect(function()
		local cel = aktualnyCel()
		if cel then
			Menu.Zamknij()
			ctx.Teleport(cel)
		end
	end)
	przyciskWstecz.Activated:Connect(function()
		pokazRozdzial(stan.rozdzial - 1)
	end)
	przyciskDalej.Activated:Connect(function()
		if stan.rozdzial >= #Ustawienia.Rozdzialy then
			Menu.Zamknij()
			ctx.Powiadom(podstaw("Powodzenia, {nick}! Wróć do mnie, gdy będziesz czegoś potrzebować."), "sukces")
		else
			pokazRozdzial(stan.rozdzial + 1)
		end
	end)

	-- Strzałki na klawiaturze przewijają rozdziały
	UserInputService.InputBegan:Connect(function(wejscie, przetworzone)
		if przetworzone or not stan.otwarte then
			return
		end
		if wejscie.KeyCode == Enum.KeyCode.Right then
			pokazRozdzial(stan.rozdzial + 1)
		elseif wejscie.KeyCode == Enum.KeyCode.Left then
			pokazRozdzial(stan.rozdzial - 1)
		end
	end)

	zbudujListe()
	local kamera = workspace.CurrentCamera
	if kamera then
		kamera:GetPropertyChangedSignal("ViewportSize"):Connect(przeliczSkale)
	end
end

return Menu
