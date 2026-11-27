--[[
	SYSTEM POWIADOMIEŃ  |  ładne powiadomienia w prawym górnym rogu

	NA SERWERZE (Script):
		local Powiadomienia = require(game.ReplicatedStorage.Powiadomienia)
		Powiadomienia.Wyslij(gracz, { Tytul = "Drzwi otwarte", Tekst = "Pojazd: Sedan", Typ = "akcja", Ikona = "auto" })
		Powiadomienia.WyslijWszystkim({ Tytul = "Serwer", Tekst = "Restart za 5 minut", Typ = "ostrzezenie" })
		Powiadomienia.WyslijWPoblizu(pozycja, 40, { Tekst = "Ktoś otwiera drzwi" }, pominTegoGracza)
		Powiadomienia.WyslijDruzynie(druzyna, { Tekst = "Zbiórka w remizie" })

	U GRACZA (LocalScript):
		local karta = Powiadomienia.Pokaz({ Tytul = "Przeszukiwanie", Tekst = "Szafka...", Postep = 3 })
		karta:Aktualizuj({ Tytul = "Znaleziono", Tekst = "Latarka", Typ = "sukces" })
		karta:Zamknij()

	POLA:
		Tytul, Tekst
		Typ     - "info", "sukces", "ostrzezenie", "blad", "akcja"
		Ikona   - info, ok, uwaga, blad, drzwi, auto, szukaj, klucz, skrzynka, osoba, telefon
		Czas    - ile sekund karta jest widoczna
		Postep  - sekundy paska ładowania (np. przeszukiwanie)
		Id      - kolejne powiadomienie z tym samym Id podmienia kartę zamiast dodawać nową

	BEZ KODU (atrybuty na ProximityPrompt):
		Powiadomienie          - tekst dla gracza, który użył (np. "Otworzyłeś szafkę")
		PowiadomienieWPoblizu  - tekst dla innych w pobliżu (np. "{gracz} otwiera szafkę")
		PowiadomienieTrwa      - karta z paskiem w czasie przytrzymywania E (np. "Przeszukujesz...")
		PowiadomienieTytul, PowiadomienieTyp, PowiadomienieIkona, PowiadomienieZasieg
		W tekstach: {gracz} = nazwa gracza, {obiekt} = ObjectText zapytania
]]

local Players = game:GetService("Players")
local RunService = game:GetService("RunService")
local TweenService = game:GetService("TweenService")
local SoundService = game:GetService("SoundService")
local ProximityPromptService = game:GetService("ProximityPromptService")

local Ustawienia = require(script:WaitForChild("Ustawienia"))
local Ikony = require(script:WaitForChild("Ikony"))

local Powiadomienia = {}

local NAZWA_ZDARZENIA = "Zdarzenie"
local CZCIONKA = "rbxasset://fonts/families/GothamSSm.json"
local KOLOR_OKNA = Color3.fromRGB(24, 24, 27)
local KOLOR_TEKSTU = Color3.fromRGB(236, 236, 240)
local KOLOR_SLABY = Color3.fromRGB(150, 150, 160)

local function zdarzenie()
	if RunService:IsServer() then
		local istniejace = script:FindFirstChild(NAZWA_ZDARZENIA)
		if not istniejace then
			istniejace = Instance.new("RemoteEvent")
			istniejace.Name = NAZWA_ZDARZENIA
			istniejace.Parent = script
		end
		return istniejace
	end
	return script:WaitForChild(NAZWA_ZDARZENIA)
end

-- Zostawia tylko znane pola o poprawnych typach
local function oczysc(dane)
	if typeof(dane) == "string" then
		dane = { Tekst = dane }
	end
	if typeof(dane) ~= "table" then
		return nil
	end
	local id = dane.Id
	return {
		Tytul = typeof(dane.Tytul) == "string" and dane.Tytul or nil,
		Tekst = typeof(dane.Tekst) == "string" and dane.Tekst or nil,
		Typ = (typeof(dane.Typ) == "string" and Ustawienia.Typy[dane.Typ]) and dane.Typ or "info",
		Ikona = typeof(dane.Ikona) == "string" and dane.Ikona or nil,
		Czas = typeof(dane.Czas) == "number" and math.clamp(dane.Czas, 1, 60) or nil,
		Postep = typeof(dane.Postep) == "number" and math.clamp(dane.Postep, 0.2, 60) or nil,
		Id = (typeof(id) == "string" or typeof(id) == "number") and tostring(id) or nil,
	}
end

-- ============================================================
--  API serwera
-- ============================================================

function Powiadomienia.Wyslij(gracz, dane)
	if RunService:IsClient() then
		if gracz == Players.LocalPlayer then
			return Powiadomienia.Pokaz(dane)
		end
		return nil
	end
	local czyste = oczysc(dane)
	if czyste and typeof(gracz) == "Instance" and gracz:IsA("Player") and gracz.Parent == Players then
		zdarzenie():FireClient(gracz, czyste)
	end
	return nil
end

function Powiadomienia.WyslijWszystkim(dane)
	for _, gracz in Players:GetPlayers() do
		Powiadomienia.Wyslij(gracz, dane)
	end
end

function Powiadomienia.WyslijWPoblizu(pozycja, promien, dane, pomin)
	for _, gracz in Players:GetPlayers() do
		local postac = gracz.Character
		local korzen = postac and postac:FindFirstChild("HumanoidRootPart")
		if gracz ~= pomin and korzen and (korzen.Position - pozycja).Magnitude <= promien then
			Powiadomienia.Wyslij(gracz, dane)
		end
	end
end

function Powiadomienia.WyslijDruzynie(druzyna, dane)
	for _, gracz in Players:GetPlayers() do
		if gracz.Team == druzyna then
			Powiadomienia.Wyslij(gracz, dane)
		end
	end
end

local function pozycjaZapytania(zapytanie)
	local rodzic = zapytanie.Parent
	if not rodzic then
		return nil
	end
	if rodzic:IsA("BasePart") then
		return rodzic.Position
	elseif rodzic:IsA("Attachment") then
		return rodzic.WorldPosition
	elseif rodzic:IsA("Model") then
		return rodzic:GetPivot().Position
	end
	return nil
end

local wystartowanoSerwer = false
function Powiadomienia.StartSerwer()
	if wystartowanoSerwer or not RunService:IsServer() then
		return
	end
	wystartowanoSerwer = true
	zdarzenie()

	-- Powiadomienia z atrybutów na ProximityPrompt (bez pisania kodu)
	ProximityPromptService.PromptTriggered:Connect(function(zapytanie, gracz)
		local dlaGracza = zapytanie:GetAttribute("Powiadomienie")
		local wPoblizu = zapytanie:GetAttribute("PowiadomienieWPoblizu")
		if typeof(dlaGracza) ~= "string" and typeof(wPoblizu) ~= "string" then
			return
		end
		local function podstaw(tekst)
			tekst = string.gsub(tekst, "{gracz}", (string.gsub(gracz.DisplayName, "%%", "%%%%")))
			tekst = string.gsub(tekst, "{obiekt}", (string.gsub(zapytanie.ObjectText, "%%", "%%%%")))
			return tekst
		end
		local tytul = zapytanie:GetAttribute("PowiadomienieTytul")
		if typeof(tytul) ~= "string" then
			tytul = zapytanie.ObjectText ~= "" and zapytanie.ObjectText or zapytanie.ActionText
		end
		local ikona = zapytanie:GetAttribute("PowiadomienieIkona")
		if typeof(dlaGracza) == "string" then
			Powiadomienia.Wyslij(gracz, {
				Tytul = tytul,
				Tekst = podstaw(dlaGracza),
				Typ = zapytanie:GetAttribute("PowiadomienieTyp") or "akcja",
				Ikona = ikona,
			})
		end
		local pozycja = pozycjaZapytania(zapytanie)
		if typeof(wPoblizu) == "string" and pozycja then
			local zasieg = zapytanie:GetAttribute("PowiadomienieZasieg")
			Powiadomienia.WyslijWPoblizu(pozycja, typeof(zasieg) == "number" and zasieg or Ustawienia.ZasiegWPoblizu, {
				Tytul = tytul,
				Tekst = podstaw(wPoblizu),
				Typ = "info",
				Ikona = ikona,
			}, gracz)
		end
	end)
end

-- ============================================================
--  Karty u gracza
-- ============================================================

local ekran, stos, skala
local karty = {} -- od najstarszej
local poId = {}
local numer = 0

local function nowy(klasa, wlasciwosci)
	local obiekt = Instance.new(klasa)
	local rodzic
	for klucz, wartosc in wlasciwosci do
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

local function zaokraglij(obiekt, promien)
	nowy("UICorner", { CornerRadius = UDim.new(0, promien), Parent = obiekt })
end

local function graj(typ)
	local id = Ustawienia.Dzwieki and Ustawienia.Dzwieki[typ]
	if not id or id == "" then
		return
	end
	local dzwiek = nowy("Sound", { SoundId = id, Volume = Ustawienia.Glosnosc or 0.4, Parent = SoundService })
	dzwiek:Play()
	task.delay(3, function()
		dzwiek:Destroy()
	end)
end

local function zamknijKarte(karta)
	if karta.Zamykana then
		return
	end
	karta.Zamykana = true
	karta.Token = {}
	if karta.Id and poId[karta.Id] == karta then
		poId[karta.Id] = nil
	end
	local indeks = table.find(karty, karta)
	if indeks then
		table.remove(karty, indeks)
	end
	local info = TweenInfo.new(0.25, Enum.EasingStyle.Quad, Enum.EasingDirection.In)
	TweenService:Create(karta.Karta, info, {
		Position = UDim2.fromOffset(Ustawienia.Szerokosc + 40, 0),
		GroupTransparency = 1,
	}):Play()
	task.delay(0.27, function()
		karta.Uchwyt:Destroy()
	end)
end

local function ustawWyglad(karta, dane)
	local kolor = Ustawienia.Typy[dane.Typ] or Ustawienia.Typy.info
	karta.PoleIkony.BackgroundColor3 = kolor
	for _, dziecko in karta.PoleIkony:GetChildren() do
		if dziecko.Name == "Ikona" then
			dziecko:Destroy()
		end
	end
	local ikona = dane.Ikona and Ikony.Istnieje(dane.Ikona) and dane.Ikona or (Ustawienia.IkonyTypow[dane.Typ] or "info")
	Ikony.rysuj(ikona, karta.PoleIkony, kolor, 22)
	karta.Tytul.Text = dane.Tytul or "Powiadomienie"
	karta.Tekst.Text = dane.Tekst or ""
	karta.Tekst.Visible = (dane.Tekst or "") ~= ""
	karta.Pasek.BackgroundColor3 = kolor
	karta.Obrys.Color = kolor
	karta.Obrys.Transparency = 0.3
	TweenService:Create(karta.Obrys, TweenInfo.new(0.8), { Transparency = 0.85 }):Play()
end

local function uruchomCzas(karta, dane)
	local token = {}
	karta.Token = token
	if dane.Postep then
		-- Pasek ładowania: zapełnia się, a karta czeka na wynik (albo znika sama)
		karta.Pasek.Size = UDim2.fromScale(0, 1)
		TweenService:Create(karta.Pasek, TweenInfo.new(dane.Postep, Enum.EasingStyle.Linear), { Size = UDim2.fromScale(1, 1) }):Play()
		task.delay(dane.Postep + (dane.Czas or 2.5), function()
			if karta.Token == token then
				zamknijKarte(karta)
			end
		end)
	else
		local czas = dane.Czas or Ustawienia.CzasDomyslny or 5
		karta.Pasek.Size = UDim2.fromScale(1, 1)
		TweenService:Create(karta.Pasek, TweenInfo.new(czas, Enum.EasingStyle.Linear), { Size = UDim2.fromScale(0, 1) }):Play()
		task.delay(czas, function()
			if karta.Token == token then
				zamknijKarte(karta)
			end
		end)
	end
end

local function utworzKarte(dane)
	numer += 1
	local szerokosc = Ustawienia.Szerokosc or 340
	local naDole = Ustawienia.Pozycja == "PrawyDol"
	local uchwyt = nowy("Frame", {
		Name = "Powiadomienie",
		BackgroundTransparency = 1,
		Size = UDim2.fromOffset(szerokosc, 0),
		AutomaticSize = Enum.AutomaticSize.Y,
		LayoutOrder = naDole and numer or -numer, -- najnowsze najbliżej rogu
		Parent = stos,
	})
	local karta = nowy("CanvasGroup", {
		Size = UDim2.new(1, 0, 0, 0),
		AutomaticSize = Enum.AutomaticSize.Y,
		Position = UDim2.fromOffset(szerokosc + 40, 0),
		BackgroundColor3 = KOLOR_OKNA,
		BackgroundTransparency = 0.04,
		GroupTransparency = 1,
		Parent = uchwyt,
	})
	zaokraglij(karta, 14)
	local obrys = nowy("UIStroke", { Color = Color3.new(1, 1, 1), Transparency = 0.85, Thickness = 1, Parent = karta })
	nowy("UIPadding", {
		PaddingTop = UDim.new(0, 12),
		PaddingBottom = UDim.new(0, 10),
		PaddingLeft = UDim.new(0, 12),
		PaddingRight = UDim.new(0, 14),
		Parent = karta,
	})
	nowy("UIListLayout", { Padding = UDim.new(0, 9), SortOrder = Enum.SortOrder.LayoutOrder, Parent = karta })

	local tresc = nowy("Frame", {
		BackgroundTransparency = 1,
		Size = UDim2.new(1, 0, 0, 0),
		AutomaticSize = Enum.AutomaticSize.Y,
		LayoutOrder = 1,
		Parent = karta,
	})
	local poleIkony = nowy("Frame", {
		Size = UDim2.fromOffset(38, 38),
		BackgroundTransparency = 0.84,
		Parent = tresc,
	})
	zaokraglij(poleIkony, 11)
	local tytul = nowy("TextLabel", {
		BackgroundTransparency = 1,
		Position = UDim2.fromOffset(50, 1),
		Size = UDim2.new(1, -104, 0, 18),
		FontFace = Font.new(CZCIONKA, Enum.FontWeight.Bold),
		TextSize = 14,
		TextColor3 = KOLOR_TEKSTU,
		TextXAlignment = Enum.TextXAlignment.Left,
		TextTruncate = Enum.TextTruncate.AtEnd,
		Parent = tresc,
	})
	nowy("TextLabel", {
		BackgroundTransparency = 1,
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.new(1, 0, 0, 2),
		Size = UDim2.fromOffset(50, 14),
		FontFace = Font.new(CZCIONKA, Enum.FontWeight.Medium),
		Text = os.date("%H:%M"),
		TextSize = 11,
		TextColor3 = KOLOR_SLABY,
		TextXAlignment = Enum.TextXAlignment.Right,
		Parent = tresc,
	})
	local tekst = nowy("TextLabel", {
		BackgroundTransparency = 1,
		Position = UDim2.fromOffset(50, 21),
		Size = UDim2.new(1, -50, 0, 0),
		AutomaticSize = Enum.AutomaticSize.Y,
		FontFace = Font.new(CZCIONKA, Enum.FontWeight.Regular),
		TextSize = 12,
		TextColor3 = KOLOR_SLABY,
		TextXAlignment = Enum.TextXAlignment.Left,
		TextWrapped = true,
		Parent = tresc,
	})
	local tor = nowy("Frame", {
		Size = UDim2.new(1, 0, 0, 3),
		BackgroundColor3 = Color3.new(1, 1, 1),
		BackgroundTransparency = 0.9,
		BorderSizePixel = 0,
		LayoutOrder = 2,
		Parent = karta,
	})
	zaokraglij(tor, 2)
	local pasek = nowy("Frame", { Size = UDim2.fromScale(1, 1), BorderSizePixel = 0, Parent = tor })
	zaokraglij(pasek, 2)

	-- Kliknięcie zamyka kartę
	local przycisk = nowy("TextButton", {
		Text = "",
		BackgroundTransparency = 1,
		Size = UDim2.fromScale(1, 1),
		ZIndex = 5,
		Parent = uchwyt,
	})

	local nowaKarta = {
		Uchwyt = uchwyt,
		Karta = karta,
		Obrys = obrys,
		PoleIkony = poleIkony,
		Tytul = tytul,
		Tekst = tekst,
		Pasek = pasek,
		Id = dane.Id,
	}
	przycisk.Activated:Connect(function()
		zamknijKarte(nowaKarta)
	end)
	ustawWyglad(nowaKarta, dane)
	uruchomCzas(nowaKarta, dane)

	TweenService:Create(karta, TweenInfo.new(0.35, Enum.EasingStyle.Back, Enum.EasingDirection.Out), {
		Position = UDim2.fromOffset(0, 0),
		GroupTransparency = 0,
	}):Play()
	graj(dane.Typ)

	table.insert(karty, nowaKarta)
	if dane.Id then
		poId[dane.Id] = nowaKarta
	end
	while #karty > (Ustawienia.MaksNaEkranie or 4) do
		zamknijKarte(karty[1])
	end
	return nowaKarta
end

local function aktualizujKarte(karta, dane)
	for klucz, wartosc in dane do
		karta.Dane[klucz] = wartosc
	end
	-- Nowe pola zastępują stare, a brak Postep oznacza zwykłą kartę z czasem
	karta.Dane.Postep = dane.Postep
	ustawWyglad(karta, karta.Dane)
	uruchomCzas(karta, karta.Dane)
	graj(dane.Typ)
end

local wystartowanoKlient = false
function Powiadomienia.StartKlient()
	if wystartowanoKlient or not RunService:IsClient() then
		return
	end
	wystartowanoKlient = true
	local gracz = Players.LocalPlayer
	local naDole = Ustawienia.Pozycja == "PrawyDol"

	ekran = nowy("ScreenGui", {
		Name = "Powiadomienia",
		ResetOnSpawn = false,
		IgnoreGuiInset = true,
		ZIndexBehavior = Enum.ZIndexBehavior.Sibling,
		DisplayOrder = 50,
		Parent = gracz:WaitForChild("PlayerGui"),
	})
	stos = nowy("Frame", {
		Name = "Stos",
		BackgroundTransparency = 1,
		AnchorPoint = naDole and Vector2.new(1, 1) or Vector2.new(1, 0),
		Position = naDole and UDim2.new(1, -16, 1, -16) or UDim2.new(1, -16, 0, Ustawienia.OdstepOdGory or 64),
		Size = UDim2.fromOffset(Ustawienia.Szerokosc or 340, 600),
		Parent = ekran,
	})
	nowy("UIListLayout", {
		Padding = UDim.new(0, 8),
		HorizontalAlignment = Enum.HorizontalAlignment.Right,
		VerticalAlignment = naDole and Enum.VerticalAlignment.Bottom or Enum.VerticalAlignment.Top,
		SortOrder = Enum.SortOrder.LayoutOrder,
		Parent = stos,
	})
	skala = nowy("UIScale", { Parent = stos })
	local function przeliczSkale()
		local kamera = workspace.CurrentCamera
		local rozmiar = kamera and kamera.ViewportSize or Vector2.new(1280, 720)
		skala.Scale = math.clamp(math.min(rozmiar.X / 1280, rozmiar.Y / 720), 0.75, 1.15)
	end
	przeliczSkale()
	if workspace.CurrentCamera then
		workspace.CurrentCamera:GetPropertyChangedSignal("ViewportSize"):Connect(przeliczSkale)
	end

	-- Powiadomienia z serwera (po zniknięciu ekranu wczytywania, jeśli jest)
	zdarzenie().OnClientEvent:Connect(function(dane)
		task.spawn(function()
			local poczatek = os.clock()
			while gracz:GetAttribute("EkranWczytywania") == true and os.clock() - poczatek < 90 do
				task.wait(0.25)
			end
			Powiadomienia.Pokaz(dane)
		end)
	end)

	-- Karta z paskiem w czasie przytrzymywania E (atrybut PowiadomienieTrwa)
	local trwajaca
	ProximityPromptService.PromptButtonHoldBegan:Connect(function(zapytanie, kto)
		local tekst = zapytanie:GetAttribute("PowiadomienieTrwa")
		if kto ~= gracz or typeof(tekst) ~= "string" then
			return
		end
		if trwajaca then
			trwajaca:Zamknij()
		end
		local tytul = zapytanie:GetAttribute("PowiadomienieTytul")
		trwajaca = Powiadomienia.Pokaz({
			Tytul = typeof(tytul) == "string" and tytul or (zapytanie.ObjectText ~= "" and zapytanie.ObjectText or zapytanie.ActionText),
			Tekst = tekst,
			Typ = "akcja",
			Ikona = zapytanie:GetAttribute("PowiadomienieIkona") or "szukaj",
			Postep = zapytanie.HoldDuration,
			Czas = 1,
		})
	end)
	local function koniecTrwania(_, kto)
		if kto == gracz and trwajaca then
			trwajaca:Zamknij()
			trwajaca = nil
		end
	end
	ProximityPromptService.PromptButtonHoldEnded:Connect(koniecTrwania)
	ProximityPromptService.PromptTriggered:Connect(koniecTrwania)

	-- Wsiadanie do pojazdu i wysiadanie
	local ostatniPojazd
	local function podlaczPostac(postac)
		local humanoid = postac:WaitForChild("Humanoid", 10)
		if not humanoid then
			return
		end
		humanoid.Seated:Connect(function(siedzi, siedzenie)
			if not Ustawienia.PowiadomOPojazdach then
				return
			end
			if siedzi and siedzenie and siedzenie:IsA("VehicleSeat") then
				local pojazd = siedzenie:FindFirstAncestorOfClass("Model")
				ostatniPojazd = pojazd and pojazd.Name or "Pojazd"
				Powiadomienia.Pokaz({ Tytul = "Wsiadasz do pojazdu", Tekst = ostatniPojazd, Typ = "akcja", Ikona = "auto", Czas = 3 })
			elseif not siedzi and ostatniPojazd then
				Powiadomienia.Pokaz({ Tytul = "Wysiadasz z pojazdu", Tekst = ostatniPojazd, Typ = "akcja", Ikona = "auto", Czas = 3 })
				ostatniPojazd = nil
			end
		end)
	end
	gracz.CharacterAdded:Connect(podlaczPostac)
	if gracz.Character then
		task.spawn(podlaczPostac, gracz.Character)
	end
end

-- ============================================================
--  API gracza
-- ============================================================

function Powiadomienia.Pokaz(dane)
	if RunService:IsServer() then
		error("Powiadomienia.Pokaz działa tylko u gracza. Na serwerze użyj Powiadomienia.Wyslij(gracz, dane).", 2)
	end
	Powiadomienia.StartKlient()
	local czyste = oczysc(dane)
	if not czyste then
		return nil
	end
	local karta = czyste.Id and poId[czyste.Id]
	if karta and not karta.Zamykana then
		aktualizujKarte(karta, czyste)
	else
		karta = utworzKarte(czyste)
		karta.Dane = czyste
	end
	return {
		Aktualizuj = function(_, noweDane)
			local nowe = oczysc(noweDane)
			if nowe and not karta.Zamykana then
				aktualizujKarte(karta, nowe)
			end
		end,
		Zamknij = function()
			zamknijKarte(karta)
		end,
	}
end

return Powiadomienia
