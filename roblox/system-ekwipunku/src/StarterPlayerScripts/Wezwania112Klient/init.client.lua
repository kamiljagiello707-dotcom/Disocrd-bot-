--[[
	SYSTEM WEZWAŃ 112  |  Strona gracza
	- ikona 112 nad każdym miejscem zdarzenia (widzą ją strażacy, także z daleka)
	- lista wezwań z odległością i postępem zadań, kliknięcie włącza nawigację
	- powiadomienia: nowe wezwanie, "Pomyślnie zakończono wezwanie"
	- animacja podczas przytrzymywania E przy zadaniu
]]

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local RunService = game:GetService("RunService")
local ProximityPromptService = game:GetService("ProximityPromptService")
local UserInputService = game:GetService("UserInputService")
local SoundService = game:GetService("SoundService")

local gracz = Players.LocalPlayer
local folderWspolny = ReplicatedStorage:WaitForChild("Wezwania112")
local U = require(folderWspolny:WaitForChild("Ustawienia"))
local aktywne = folderWspolny:WaitForChild("Aktywne")
local remote = folderWspolny:WaitForChild("Zdarzenia"):WaitForChild("Powiadomienie")

local W = require(script:WaitForChild("Wyglad"))
local K = W.KOLOR
local CZERWONY = Color3.fromRGB(225, 38, 45)
local ZASIEG_SZCZEGOLOW = 90

local ekran = W.nowy("ScreenGui", {
	Name = "Wezwania112",
	ResetOnSpawn = false,
	IgnoreGuiInset = true,
	ZIndexBehavior = Enum.ZIndexBehavior.Sibling,
	DisplayOrder = 26,
	Parent = gracz:WaitForChild("PlayerGui"),
})

local function czyStrazak()
	local ranga = gracz:GetAttribute("Ranga")
	if ranga and table.find(U.RangiStrazakow or {}, ranga) then
		return true
	end
	return gracz.Team ~= nil and table.find(U.DruzynyStrazakow or {}, gracz.Team.Name) ~= nil
end

local function korzenPostaci()
	local postac = gracz.Character
	return postac and postac:FindFirstChild("HumanoidRootPart")
end

local function czekajNaWczytanie()
	local poczatek = os.clock()
	while gracz:GetAttribute("EkranWczytywania") == true and os.clock() - poczatek < 90 do
		task.wait(0.25)
	end
end

local function odznaka112(rodzic, rozmiar, kolor)
	local kolo = W.nowy("Frame", {
		Size = UDim2.fromOffset(rozmiar, rozmiar),
		BackgroundColor3 = kolor or CZERWONY,
		Parent = rodzic,
	})
	W.zaokraglij(kolo, math.floor(rozmiar / 2))
	W.nowy("UIStroke", { Color = Color3.new(1, 1, 1), Thickness = math.max(1, math.floor(rozmiar / 22)), Transparency = 0.15, Parent = kolo })
	W.napis({
		Text = "112",
		FontFace = W.czcionka(Enum.FontWeight.Heavy),
		TextSize = math.floor(rozmiar * 0.34),
		TextColor3 = Color3.new(1, 1, 1),
		TextXAlignment = Enum.TextXAlignment.Center,
		Size = UDim2.fromScale(1, 1),
		Parent = kolo,
	})
	return kolo
end

-- ============================================================
--  Powiadomienia
-- ============================================================

local stos = W.nowy("Frame", {
	BackgroundTransparency = 1,
	AnchorPoint = Vector2.new(0.5, 0),
	Position = UDim2.new(0.5, 0, 0, 16),
	Size = UDim2.fromOffset(460, 400),
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
-- znak: "112", "ok", "x" albo nil (kropka)
local function karta(znak, kolor, tytul, tresc, czas)
	numer += 1
	local ramka = W.nowy("CanvasGroup", {
		Size = UDim2.fromOffset(440, 0),
		AutomaticSize = Enum.AutomaticSize.Y,
		BackgroundColor3 = K.Okno,
		BackgroundTransparency = 0.05,
		GroupTransparency = 1,
		LayoutOrder = numer,
		Parent = stos,
	})
	W.zaokraglij(ramka, 16)
	W.obrysuj(ramka, 0.5, kolor)
	W.nowy("UIPadding", {
		PaddingTop = UDim.new(0, 12),
		PaddingBottom = UDim.new(0, 12),
		PaddingLeft = UDim.new(0, 14),
		PaddingRight = UDim.new(0, 14),
		Parent = ramka,
	})
	if znak == "112" then
		odznaka112(ramka, 40, kolor)
	else
		local kolo = W.nowy("Frame", { Size = UDim2.fromOffset(40, 40), BackgroundColor3 = kolor, Parent = ramka })
		W.zaokraglij(kolo, 20)
		W.napis({
			Text = znak == "ok" and "✓" or (znak == "x" and "✕" or "•"),
			FontFace = W.czcionka(Enum.FontWeight.Heavy),
			TextSize = 20,
			TextColor3 = K.TekstCiemny,
			TextXAlignment = Enum.TextXAlignment.Center,
			Size = UDim2.fromScale(1, 1),
			Parent = kolo,
		})
	end
	W.napis({
		Text = tytul,
		FontFace = W.czcionka(Enum.FontWeight.Bold),
		TextSize = 15,
		Position = UDim2.fromOffset(54, 1),
		Size = UDim2.new(1, -54, 0, 19),
		Parent = ramka,
	})
	W.napis({
		Text = tresc,
		TextSize = 12,
		TextColor3 = K.TekstSlaby,
		TextWrapped = true,
		AutomaticSize = Enum.AutomaticSize.Y,
		Position = UDim2.fromOffset(54, 22),
		Size = UDim2.new(1, -54, 0, 16),
		Parent = ramka,
	})
	W.animuj(ramka, { GroupTransparency = 0 }, TweenInfo.new(0.25))
	task.delay(czas or 5, function()
		W.animuj(ramka, { GroupTransparency = 1 }, TweenInfo.new(0.4)).Completed:Wait()
		ramka:Destroy()
	end)
end

local blysk = W.nowy("Frame", {
	Size = UDim2.fromScale(1, 1),
	BackgroundColor3 = CZERWONY,
	BackgroundTransparency = 1,
	BorderSizePixel = 0,
	ZIndex = 0,
	Parent = ekran,
})

local function dzwiek()
	local id = U.DzwiekWezwania
	if not id or id == "" then
		return
	end
	local sygnal = W.nowy("Sound", { SoundId = id, Volume = 0.8, Parent = SoundService })
	task.spawn(function()
		for _ = 1, 3 do
			sygnal:Play()
			task.wait(0.25)
		end
		task.wait(1)
		sygnal:Destroy()
	end)
end

remote.OnClientEvent:Connect(function(rodzaj, dane)
	if typeof(dane) ~= "table" then
		return
	end
	task.spawn(function()
		czekajNaWczytanie()
		if rodzaj == "Nowe" then
			local kolor = dane.Kolor or CZERWONY
			karta("112", CZERWONY, "Nowe wezwanie: " .. tostring(dane.Nazwa), ("%s\n%s\nJedź do ikony 112 na mapie."):format(tostring(dane.Miejsce), tostring(dane.Opis)), 9)
			if not dane.Pager then
				dzwiek()
				blysk.BackgroundColor3 = kolor
				blysk.BackgroundTransparency = 0.8
				W.animuj(blysk, { BackgroundTransparency = 1 }, TweenInfo.new(0.6))
			end
		elseif rodzaj == "Koniec" then
			if dane.Udane then
				karta("ok", K.Zielony, "Pomyślnie zakończono wezwanie", ("%s  •  %s\nUczestników akcji: %d"):format(tostring(dane.Nazwa), tostring(dane.Miejsce), dane.Uczestnikow or 0), 7)
			else
				karta("x", K.Czerwony, "Wezwanie wygasło", ("%s  •  %s"):format(tostring(dane.Nazwa), tostring(dane.Miejsce)), 6)
			end
		elseif rodzaj == "Info" then
			local kolor = dane.Rodzaj == "blad" and K.Czerwony or (dane.Rodzaj == "sukces" and K.Zielony or CZERWONY)
			karta(nil, kolor, "Wezwania 112", tostring(dane.Tekst), 4)
		end
	end)
end)

-- ============================================================
--  Ikony 112 nad miejscami zdarzeń
-- ============================================================

local znaczniki = {} -- [folder] = { Czesc, Napis }
local nawigacja = nil -- { Folder, Slup }

local function usunZnacznik(folder)
	local znacznik = znaczniki[folder]
	if znacznik then
		znacznik.Czesc:Destroy()
		znaczniki[folder] = nil
	end
end

local function utworzZnacznik(folder)
	if znaczniki[folder] then
		return
	end
	local pozycja = folder:GetAttribute("Pozycja")
	if typeof(pozycja) ~= "Vector3" then
		return
	end
	local kolor = folder:GetAttribute("Kolor") or CZERWONY
	local czesc = W.nowy("Part", {
		Name = "Znacznik112",
		Size = Vector3.new(1, 1, 1),
		CFrame = CFrame.new(pozycja),
		Anchored = true,
		CanCollide = false,
		CanQuery = false,
		CanTouch = false,
		Transparency = 1,
	})
	local tablica = W.nowy("BillboardGui", {
		AlwaysOnTop = true,
		LightInfluence = 0,
		Size = UDim2.fromOffset(230, 90),
		StudsOffsetWorldSpace = Vector3.new(0, 12, 0),
		Parent = czesc,
	})
	-- Pulsujący pierścień za ikoną
	local pierscien = W.nowy("Frame", {
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.new(0.5, 0, 0, 24),
		Size = UDim2.fromOffset(48, 48),
		BackgroundTransparency = 1,
		Parent = tablica,
	})
	W.zaokraglij(pierscien, 40)
	local obrysPierscienia = W.nowy("UIStroke", { Color = CZERWONY, Thickness = 3, Parent = pierscien })
	W.animuj(pierscien, { Size = UDim2.fromOffset(84, 84) }, TweenInfo.new(1.3, Enum.EasingStyle.Quad, Enum.EasingDirection.Out, -1))
	W.animuj(obrysPierscienia, { Transparency = 1 }, TweenInfo.new(1.3, Enum.EasingStyle.Quad, Enum.EasingDirection.Out, -1))
	local ikona = odznaka112(tablica, 48)
	ikona.AnchorPoint = Vector2.new(0.5, 0)
	ikona.Position = UDim2.fromScale(0.5, 0)

	local pigulka = W.nowy("Frame", {
		AnchorPoint = Vector2.new(0.5, 0),
		Position = UDim2.new(0.5, 0, 0, 56),
		Size = UDim2.fromOffset(0, 26),
		AutomaticSize = Enum.AutomaticSize.X,
		BackgroundColor3 = K.Okno,
		BackgroundTransparency = 0.1,
		Parent = tablica,
	})
	W.zaokraglij(pigulka, 13)
	W.obrysuj(pigulka, 0.3, kolor)
	W.nowy("UIPadding", { PaddingLeft = UDim.new(0, 12), PaddingRight = UDim.new(0, 12), Parent = pigulka })
	local napis = W.napis({
		Text = tostring(folder:GetAttribute("Nazwa")),
		FontFace = W.czcionka(Enum.FontWeight.Bold),
		TextSize = 12,
		AutomaticSize = Enum.AutomaticSize.X,
		Size = UDim2.fromScale(0, 1),
		Parent = pigulka,
	})
	czesc.Parent = workspace
	znaczniki[folder] = { Czesc = czesc, Napis = napis }
end

local function usunNawigacje()
	if nawigacja then
		nawigacja.Slup:Destroy()
		nawigacja = nil
	end
end

local function nawiguj(folder)
	local poprzednia = nawigacja and nawigacja.Folder
	usunNawigacje()
	if poprzednia == folder then
		return -- drugie kliknięcie wyłącza nawigację
	end
	local pozycja = folder:GetAttribute("Pozycja")
	if typeof(pozycja) ~= "Vector3" then
		return
	end
	local slup = W.nowy("Part", {
		Name = "Nawigacja112",
		Shape = Enum.PartType.Cylinder,
		Size = Vector3.new(160, 1.4, 1.4),
		CFrame = CFrame.new(pozycja + Vector3.new(0, 80, 0)) * CFrame.Angles(0, 0, math.rad(90)),
		Anchored = true,
		CanCollide = false,
		CanQuery = false,
		CanTouch = false,
		CastShadow = false,
		Material = Enum.Material.Neon,
		Color = folder:GetAttribute("Kolor") or CZERWONY,
		Transparency = 0.5,
		Parent = workspace,
	})
	nawigacja = { Folder = folder, Slup = slup }
end

-- ============================================================
--  Lista wezwań (lewy dolny róg)
-- ============================================================

local panel = W.nowy("Frame", {
	BackgroundTransparency = 1,
	AnchorPoint = Vector2.new(0, 1),
	Position = UserInputService.TouchEnabled and UDim2.new(0, 16, 1, -210) or UDim2.new(0, 16, 1, -16),
	Size = UDim2.fromOffset(310, 0),
	AutomaticSize = Enum.AutomaticSize.Y,
	Visible = false,
	Parent = ekran,
})
W.nowy("UIListLayout", {
	Padding = UDim.new(0, 8),
	VerticalAlignment = Enum.VerticalAlignment.Bottom,
	SortOrder = Enum.SortOrder.LayoutOrder,
	Parent = panel,
})
local skalaPanelu = W.nowy("UIScale", { Parent = panel })

local etykietyOdleglosci = {} -- [folder] = TextLabel
local szczegolyDla = nil

local function odleglosc(folder)
	local korzen = korzenPostaci()
	local pozycja = folder:GetAttribute("Pozycja")
	if not korzen or typeof(pozycja) ~= "Vector3" then
		return math.huge
	end
	return (korzen.Position - pozycja).Magnitude
end

local function posortowaneWezwania()
	local lista = {}
	for _, folder in aktywne:GetChildren() do
		if folder:IsA("Folder") and not folder:GetAttribute("Zakonczone") then
			table.insert(lista, folder)
		end
	end
	table.sort(lista, function(a, b)
		return odleglosc(a) < odleglosc(b)
	end)
	return lista
end

local function liniaZadania(rodzic, zadanie, kolejnosc)
	local gotowe = zadanie:GetAttribute("Gotowe") == true
	local aktywneZadanie = zadanie:GetAttribute("Aktywne") == true
	local znak, kolor = "○", K.TekstSlaby
	if gotowe then
		znak, kolor = "✓", K.Zielony
	elseif aktywneZadanie then
		znak, kolor = "●", K.Tekst
	end
	local postep = ""
	if aktywneZadanie and not gotowe then
		local wszystkie = zadanie:GetAttribute("Wszystkie") or 0
		local zrobione = zadanie:GetAttribute("Zrobione") or 0
		local ulamek = zadanie:GetAttribute("Postep") or 0
		if ulamek > 0 and ulamek < 1 then
			postep = ("  %d%%"):format(math.floor(ulamek * 100))
		elseif wszystkie > 1 then
			postep = ("  %d/%d"):format(zrobione, wszystkie)
		end
	end
	local wiersz = W.nowy("Frame", {
		BackgroundTransparency = 1,
		Size = UDim2.new(1, 0, 0, 0),
		AutomaticSize = Enum.AutomaticSize.Y,
		LayoutOrder = kolejnosc,
		Parent = rodzic,
	})
	W.napis({
		Text = znak,
		FontFace = W.czcionka(Enum.FontWeight.Bold),
		TextSize = 12,
		TextColor3 = kolor,
		Size = UDim2.fromOffset(16, 16),
		Parent = wiersz,
	})
	local tekst = tostring(zadanie:GetAttribute("Opis")) .. postep
	local narzedzia = zadanie:GetAttribute("Narzedzia")
	if aktywneZadanie and not gotowe and narzedzia and narzedzia ~= "" then
		-- Drugi wiersz mniejszą czcionką: czego użyć
		tekst ..= ('\n<font size="10" color="#%s">Weź: %s</font>'):format(K.TekstSlaby:ToHex(), narzedzia)
	end
	W.napis({
		Text = tekst,
		RichText = true,
		FontFace = W.czcionka(aktywneZadanie and not gotowe and Enum.FontWeight.Medium or Enum.FontWeight.Regular),
		TextSize = 12,
		TextColor3 = kolor,
		TextTransparency = (not aktywneZadanie and not gotowe) and 0.35 or 0,
		TextWrapped = true,
		AutomaticSize = Enum.AutomaticSize.Y,
		Position = UDim2.fromOffset(18, 0),
		Size = UDim2.new(1, -18, 0, 16),
		Parent = wiersz,
	})
end

local function przebuduj()
	for _, dziecko in panel:GetChildren() do
		if dziecko:IsA("GuiObject") then
			dziecko:Destroy()
		end
	end
	table.clear(etykietyOdleglosci)

	local strazak = czyStrazak()
	local lista = strazak and posortowaneWezwania() or {}
	panel.Visible = #lista > 0
	if #lista == 0 then
		return
	end
	szczegolyDla = odleglosc(lista[1]) <= ZASIEG_SZCZEGOLOW and lista[1] or nil

	local naglowek = W.nowy("Frame", {
		Size = UDim2.new(1, 0, 0, 30),
		BackgroundColor3 = K.Okno,
		BackgroundTransparency = 0.12,
		LayoutOrder = 0,
		Parent = panel,
	})
	W.zaokraglij(naglowek, 10)
	W.obrysuj(naglowek, 0.88)
	local mala = odznaka112(naglowek, 20)
	mala.Position = UDim2.fromOffset(8, 5)
	W.napis({
		Text = ("WEZWANIA 112  •  %d"):format(#lista),
		FontFace = W.czcionka(Enum.FontWeight.Heavy),
		TextSize = 12,
		Position = UDim2.fromOffset(36, 0),
		Size = UDim2.new(1, -44, 1, 0),
		Parent = naglowek,
	})

	for indeks, folder in lista do
		local kolor = folder:GetAttribute("Kolor") or CZERWONY
		local wybrana = nawigacja and nawigacja.Folder == folder
		local kartaWezwania = W.przycisk({
			Text = "",
			Size = UDim2.new(1, 0, 0, 0),
			AutomaticSize = Enum.AutomaticSize.Y,
			BackgroundColor3 = K.Okno,
			BackgroundTransparency = 0.08,
			LayoutOrder = indeks,
			Parent = panel,
		})
		W.zaokraglij(kartaWezwania, 12)
		W.obrysuj(kartaWezwania, wybrana and 0.2 or 0.6, kolor)
		W.nowy("UIPadding", {
			PaddingTop = UDim.new(0, 10),
			PaddingBottom = UDim.new(0, 10),
			PaddingLeft = UDim.new(0, 12),
			PaddingRight = UDim.new(0, 12),
			Parent = kartaWezwania,
		})
		W.nowy("UIListLayout", { Padding = UDim.new(0, 5), SortOrder = Enum.SortOrder.LayoutOrder, Parent = kartaWezwania })

		local gora = W.nowy("Frame", { BackgroundTransparency = 1, Size = UDim2.new(1, 0, 0, 34), LayoutOrder = 1, Parent = kartaWezwania })
		local kropka = W.nowy("Frame", {
			Position = UDim2.fromOffset(0, 4),
			Size = UDim2.fromOffset(10, 10),
			BackgroundColor3 = kolor,
			Parent = gora,
		})
		W.zaokraglij(kropka, 5)
		W.napis({
			Text = tostring(folder:GetAttribute("Nazwa")),
			FontFace = W.czcionka(Enum.FontWeight.Bold),
			TextSize = 13,
			TextTruncate = Enum.TextTruncate.AtEnd,
			Position = UDim2.fromOffset(18, 0),
			Size = UDim2.new(1, -90, 0, 18),
			Parent = gora,
		})
		etykietyOdleglosci[folder] = W.napis({
			Text = "",
			FontFace = W.czcionka(Enum.FontWeight.Bold),
			TextSize = 12,
			TextColor3 = kolor,
			TextXAlignment = Enum.TextXAlignment.Right,
			AnchorPoint = Vector2.new(1, 0),
			Position = UDim2.fromScale(1, 0),
			Size = UDim2.fromOffset(70, 18),
			Parent = gora,
		})
		W.napis({
			Text = "📍 " .. tostring(folder:GetAttribute("Miejsce")) .. (wybrana and "  •  nawigacja włączona" or "  •  kliknij, aby nawigować"),
			TextSize = 11,
			TextColor3 = K.TekstSlaby,
			TextTruncate = Enum.TextTruncate.AtEnd,
			Position = UDim2.fromOffset(18, 18),
			Size = UDim2.new(1, -18, 0, 15),
			Parent = gora,
		})

		if folder == szczegolyDla then
			local zadania = {}
			for _, zadanie in folder:GetChildren() do
				if zadanie:IsA("Folder") then
					table.insert(zadania, zadanie)
				end
			end
			table.sort(zadania, function(a, b)
				return (a:GetAttribute("Kolejnosc") or 0) < (b:GetAttribute("Kolejnosc") or 0)
			end)
			W.nowy("Frame", {
				Size = UDim2.new(1, 0, 0, 1),
				BackgroundColor3 = Color3.new(1, 1, 1),
				BackgroundTransparency = 0.9,
				BorderSizePixel = 0,
				LayoutOrder = 2,
				Parent = kartaWezwania,
			})
			for numerZadania, zadanie in zadania do
				liniaZadania(kartaWezwania, zadanie, 2 + numerZadania)
			end
		end

		kartaWezwania.Activated:Connect(function()
			nawiguj(folder)
			przebuduj()
		end)
	end
end

local zaplanowane = false
local function zaplanuj()
	if zaplanowane then
		return
	end
	zaplanowane = true
	task.defer(function()
		zaplanowane = false
		przebuduj()
	end)
end

-- ============================================================
--  Zadania: zasięg E tylko dla strażaków, animacja przy przytrzymaniu
-- ============================================================

local function ustawPrompt(zapytanie)
	if zapytanie:IsA("ProximityPrompt") and zapytanie:GetAttribute("Wezwanie112") then
		zapytanie.MaxActivationDistance = czyStrazak() and (zapytanie:GetAttribute("Zasieg") or 10) or 0
	end
end

local folderModeli = workspace:WaitForChild("Wezwania112", 30)
local function wszystkiePrompty()
	if folderModeli then
		for _, obiekt in folderModeli:GetDescendants() do
			ustawPrompt(obiekt)
		end
	end
end
if folderModeli then
	folderModeli.DescendantAdded:Connect(ustawPrompt)
end

local sciezka
local function zatrzymajAnimacje()
	if sciezka then
		sciezka:Stop(0.2)
		sciezka = nil
	end
end

ProximityPromptService.PromptButtonHoldBegan:Connect(function(zapytanie, kto)
	if kto ~= gracz or not zapytanie:GetAttribute("Wezwanie112") then
		return
	end
	local id = U.Animacje and U.Animacje[zapytanie:GetAttribute("Animacja") or ""]
	local postac = gracz.Character
	local humanoid = postac and postac:FindFirstChildOfClass("Humanoid")
	local animator = humanoid and humanoid:FindFirstChildOfClass("Animator")
	if not id or id == "" or not animator then
		return
	end
	zatrzymajAnimacje()
	local animacja = Instance.new("Animation")
	animacja.AnimationId = id
	local ok, nowa = pcall(function()
		return animator:LoadAnimation(animacja)
	end)
	if ok and nowa then
		sciezka = nowa
		sciezka.Looped = true
		sciezka.Priority = Enum.AnimationPriority.Action
		sciezka:Play(0.15)
	end
end)
ProximityPromptService.PromptButtonHoldEnded:Connect(function(_, kto)
	if kto == gracz then
		zatrzymajAnimacje()
	end
end)
ProximityPromptService.PromptTriggered:Connect(function(_, kto)
	if kto == gracz then
		zatrzymajAnimacje()
	end
end)

-- ============================================================
--  Śledzenie wezwań
-- ============================================================

local function odswiezWidocznosc()
	local strazak = czyStrazak()
	for _, folder in aktywne:GetChildren() do
		if strazak and not folder:GetAttribute("Zakonczone") then
			utworzZnacznik(folder)
		else
			usunZnacznik(folder)
		end
	end
	if not strazak then
		usunNawigacje()
	end
	wszystkiePrompty()
	zaplanuj()
end

local function podlaczFolder(folder)
	folder.AttributeChanged:Connect(function()
		if folder:GetAttribute("Zakonczone") then
			usunZnacznik(folder)
			if nawigacja and nawigacja.Folder == folder then
				usunNawigacje()
			end
		end
		zaplanuj()
	end)
	local function podlaczZadanie(zadanie)
		if zadanie:IsA("Folder") then
			zadanie.AttributeChanged:Connect(zaplanuj)
		end
	end
	folder.ChildAdded:Connect(function(zadanie)
		podlaczZadanie(zadanie)
		zaplanuj()
	end)
	for _, zadanie in folder:GetChildren() do
		podlaczZadanie(zadanie)
	end
	odswiezWidocznosc()
end

aktywne.ChildAdded:Connect(podlaczFolder)
aktywne.ChildRemoved:Connect(function(folder)
	usunZnacznik(folder)
	if nawigacja and nawigacja.Folder == folder then
		usunNawigacje()
	end
	zaplanuj()
end)
for _, folder in aktywne:GetChildren() do
	podlaczFolder(folder)
end

gracz:GetAttributeChangedSignal("Ranga"):Connect(odswiezWidocznosc)
gracz:GetPropertyChangedSignal("Team"):Connect(odswiezWidocznosc)

-- Odległości na żywo, szczegóły najbliższego wezwania, koniec nawigacji na miejscu
local nastepnyPomiar = 0
RunService.Heartbeat:Connect(function()
	if os.clock() < nastepnyPomiar then
		return
	end
	nastepnyPomiar = os.clock() + 0.3
	for folder, znacznik in znaczniki do
		local dystans = odleglosc(folder)
		znacznik.Napis.Text = ("%s  •  %s"):format(
			tostring(folder:GetAttribute("Nazwa")),
			dystans == math.huge and "?" or W.metry(dystans)
		)
	end
	for folder, etykieta in etykietyOdleglosci do
		local dystans = odleglosc(folder)
		etykieta.Text = dystans == math.huge and "" or W.metry(dystans)
	end
	if nawigacja and odleglosc(nawigacja.Folder) < 25 then
		usunNawigacje()
		zaplanuj()
	end
	if panel.Visible then
		local najblizsze = posortowaneWezwania()[1]
		local nowe = najblizsze and odleglosc(najblizsze) <= ZASIEG_SZCZEGOLOW and najblizsze or nil
		if nowe ~= szczegolyDla then
			zaplanuj()
		end
	end
end)

local function przeliczSkale()
	local kamera = workspace.CurrentCamera
	local rozmiar = kamera and kamera.ViewportSize or Vector2.new(1280, 720)
	skalaPanelu.Scale = math.clamp(math.min(rozmiar.X / 1280, rozmiar.Y / 720), 0.7, 1.15)
end
przeliczSkale()
local kamera = workspace.CurrentCamera
if kamera then
	kamera:GetPropertyChangedSignal("ViewportSize"):Connect(przeliczSkale)
end
odswiezWidocznosc()
