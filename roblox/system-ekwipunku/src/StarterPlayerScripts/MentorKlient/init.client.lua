--[[
	MENTOR  |  Strona gracza
	Rozmowa z mentorem (E), kamera na mentora, droga do celów
	i prowadzenie nowych graczy do mentora.
]]

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local ProximityPromptService = game:GetService("ProximityPromptService")
local CollectionService = game:GetService("CollectionService")
local RunService = game:GetService("RunService")
local TweenService = game:GetService("TweenService")

local gracz = Players.LocalPlayer
local folderWspolny = ReplicatedStorage:WaitForChild("Mentor")
local Ustawienia = require(folderWspolny:WaitForChild("Ustawienia"))
local remote = folderWspolny:WaitForChild("Zdarzenia"):WaitForChild("Mentor")
local cele = folderWspolny:WaitForChild("Cele")
local mentorzy = folderWspolny:WaitForChild("Mentorzy")

local W = require(script:WaitForChild("Wyglad"))
local Menu = require(script:WaitForChild("Menu"))
local K = W.KOLOR

local ATRYBUT = "MentorOdwiedzony"
local AKCENT = Ustawienia.KolorAkcentu or K.Czerwony
local ANIMACJA_MACHANIA = "rbxassetid://128777973" -- machanie ręką R6 od Roblox

local ekran = W.nowy("ScreenGui", {
	Name = "Mentor",
	ResetOnSpawn = false,
	IgnoreGuiInset = true,
	ZIndexBehavior = Enum.ZIndexBehavior.Sibling,
	DisplayOrder = 25,
	Parent = gracz:WaitForChild("PlayerGui"),
})

-- ============================================================
--  Powiadomienia
-- ============================================================

local stos = W.nowy("Frame", {
	BackgroundTransparency = 1,
	AnchorPoint = Vector2.new(0.5, 0),
	Position = UDim2.new(0.5, 0, 0, 64),
	Size = UDim2.fromOffset(700, 200),
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
local function powiadom(tresc, rodzaj)
	numer += 1
	local kolor = rodzaj == "sukces" and K.Zielony or (rodzaj == "blad" and K.Czerwony or K.Bialy)
	local dymek = W.nowy("CanvasGroup", {
		AutomaticSize = Enum.AutomaticSize.X,
		Size = UDim2.fromOffset(0, 38),
		BackgroundColor3 = K.Okno,
		BackgroundTransparency = 0.08,
		GroupTransparency = 1,
		LayoutOrder = numer,
		Parent = stos,
	})
	W.zaokraglij(dymek, 10)
	W.obrysuj(dymek, 0.86)
	W.nowy("UIPadding", { PaddingLeft = UDim.new(0, 14), PaddingRight = UDim.new(0, 16), Parent = dymek })
	W.nowy("UIListLayout", {
		FillDirection = Enum.FillDirection.Horizontal,
		VerticalAlignment = Enum.VerticalAlignment.Center,
		Padding = UDim.new(0, 10),
		SortOrder = Enum.SortOrder.LayoutOrder,
		Parent = dymek,
	})
	local kropka = W.nowy("Frame", {
		Size = UDim2.fromOffset(8, 8),
		BackgroundColor3 = kolor,
		BorderSizePixel = 0,
		LayoutOrder = 1,
		Parent = dymek,
	})
	W.zaokraglij(kropka, 4)
	W.napis({
		Text = tresc,
		FontFace = W.czcionka(Enum.FontWeight.Medium),
		TextSize = 13,
		AutomaticSize = Enum.AutomaticSize.X,
		Size = UDim2.fromOffset(0, 38),
		LayoutOrder = 2,
		Parent = dymek,
	})
	W.animuj(dymek, { GroupTransparency = 0 }, TweenInfo.new(0.25))
	task.delay(5, function()
		W.animuj(dymek, { GroupTransparency = 1 }, TweenInfo.new(0.4)).Completed:Wait()
		dymek:Destroy()
	end)
end

local function czekajNaWczytanie()
	local poczatek = os.clock()
	while gracz:GetAttribute("EkranWczytywania") == true and os.clock() - poczatek < 90 do
		task.wait(0.25)
	end
end

-- ============================================================
--  Droga: słup światła + odległość (widzi tylko ten gracz)
-- ============================================================

local droga -- { Model, Napis, Pozycja, NaMiejscu, DoMentora }

local function usunDroge()
	if droga then
		droga.Model:Destroy()
		droga = nil
	end
end

local function pokazDroge(pozycja, nazwa, naMiejscu, doMentora)
	usunDroge()
	local model = Instance.new("Model")
	model.Name = "DrogaMentora"
	W.nowy("Part", {
		Name = "Slup",
		Shape = Enum.PartType.Cylinder,
		Size = Vector3.new(160, 1.2, 1.2),
		CFrame = CFrame.new(pozycja + Vector3.new(0, 80, 0)) * CFrame.Angles(0, 0, math.rad(90)),
		Anchored = true,
		CanCollide = false,
		CanQuery = false,
		CanTouch = false,
		CastShadow = false,
		Material = Enum.Material.Neon,
		Color = AKCENT,
		Transparency = 0.55,
		Parent = model,
	})
	local punkt = W.nowy("Part", {
		Name = "Punkt",
		Size = Vector3.new(1, 1, 1),
		CFrame = CFrame.new(pozycja),
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
		StudsOffsetWorldSpace = Vector3.new(0, 8, 0),
		Parent = punkt,
	})
	local pigulka = W.nowy("Frame", {
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = K.Okno,
		BackgroundTransparency = 0.1,
		Parent = tablica,
	})
	W.zaokraglij(pigulka, 12)
	W.obrysuj(pigulka, 0.2, AKCENT)
	W.napis({
		Text = "📍 " .. nazwa,
		FontFace = W.czcionka(Enum.FontWeight.Bold),
		TextSize = 13,
		TextXAlignment = Enum.TextXAlignment.Center,
		TextTruncate = Enum.TextTruncate.AtEnd,
		Position = UDim2.fromOffset(8, 6),
		Size = UDim2.new(1, -16, 0, 18),
		Parent = pigulka,
	})
	local odleglosc = W.napis({
		Text = "",
		TextSize = 12,
		TextColor3 = K.TekstSlaby,
		TextXAlignment = Enum.TextXAlignment.Center,
		Position = UDim2.fromOffset(8, 25),
		Size = UDim2.new(1, -16, 0, 16),
		Parent = pigulka,
	})
	model.Parent = workspace
	droga = { Model = model, Napis = odleglosc, Pozycja = pozycja, NaMiejscu = naMiejscu, DoMentora = doMentora }
end

local nastepnyPomiar = 0
RunService.Heartbeat:Connect(function()
	if not droga or os.clock() < nastepnyPomiar then
		return
	end
	nastepnyPomiar = os.clock() + 0.25
	local postac = gracz.Character
	local korzen = postac and postac:FindFirstChild("HumanoidRootPart")
	if not korzen then
		return
	end
	local dystans = (korzen.Position - droga.Pozycja).Magnitude
	droga.Napis.Text = W.metry(dystans)
	if dystans <= 14 then
		local wiadomosc = droga.NaMiejscu
		usunDroge()
		if wiadomosc then
			powiadom(wiadomosc, "sukces")
		end
	end
end)

-- ============================================================
--  Kamera i ruch
-- ============================================================

local zapisanyTypKamery
local sterowanie

local function kameraNaMentora(model)
	if not Ustawienia.KameraNaMentora then
		return
	end
	local kamera = workspace.CurrentCamera
	local glowa = model and model:FindFirstChild("Head")
	if not kamera or not glowa then
		return
	end
	if zapisanyTypKamery == nil then
		zapisanyTypKamery = kamera.CameraType
	end
	local przod = glowa.CFrame.LookVector * Vector3.new(1, 0, 1)
	przod = przod.Magnitude > 0.01 and przod.Unit or Vector3.new(0, 0, -1)
	local prawoMentora = przod:Cross(Vector3.yAxis)
	-- Mentor ma być po lewej stronie ekranu, bo menu jest po prawej
	local oko = glowa.Position + przod * 7.5 - prawoMentora * 1.2 + Vector3.new(0, 0.3, 0)
	local cel = glowa.Position - prawoMentora * 4.2 - Vector3.new(0, 0.8, 0)
	kamera.CameraType = Enum.CameraType.Scriptable
	TweenService:Create(kamera, TweenInfo.new(0.9, Enum.EasingStyle.Quart, Enum.EasingDirection.Out), {
		CFrame = CFrame.lookAt(oko, cel),
	}):Play()
end

local function przywrocKamere()
	local kamera = workspace.CurrentCamera
	if kamera and zapisanyTypKamery ~= nil then
		kamera.CameraType = zapisanyTypKamery
	end
	zapisanyTypKamery = nil
end

local function zablokujRuch(tak)
	if not Ustawienia.ZablokujRuch then
		return
	end
	if sterowanie == nil then
		local ok, modul = pcall(function()
			return require(gracz:WaitForChild("PlayerScripts"):WaitForChild("PlayerModule", 5))
		end)
		sterowanie = ok and modul and modul.GetControls and modul:GetControls() or false
	end
	if sterowanie then
		if tak then
			sterowanie:Disable()
		else
			sterowanie:Enable()
		end
	end
end

local function machnij(model)
	local humanoid = model and model:FindFirstChildOfClass("Humanoid")
	local animator = humanoid and humanoid:FindFirstChildOfClass("Animator")
	if not animator then
		return
	end
	pcall(function()
		local animacja = Instance.new("Animation")
		animacja.AnimationId = ANIMACJA_MACHANIA
		animator:LoadAnimation(animacja):Play()
	end)
end

-- ============================================================
--  Wykrzyknik nad mentorem (dla tych, którzy jeszcze nie rozmawiali)
-- ============================================================

local function odswiezWykrzykniki()
	local nowy = gracz:GetAttribute(ATRYBUT) == false
	for _, model in CollectionService:GetTagged("MentorNPC") do
		local wykrzyknik = model:FindFirstChild("Wykrzyknik", true)
		if wykrzyknik then
			wykrzyknik.Visible = nowy
			if nowy and not wykrzyknik:GetAttribute("Podskakuje") then
				wykrzyknik:SetAttribute("Podskakuje", true)
				TweenService:Create(
					wykrzyknik,
					TweenInfo.new(0.6, Enum.EasingStyle.Sine, Enum.EasingDirection.InOut, -1, true),
					{ Position = UDim2.new(0.5, 0, 0, -6) }
				):Play()
			end
		end
	end
end

CollectionService:GetInstanceAddedSignal("MentorNPC"):Connect(function()
	task.defer(odswiezWykrzykniki)
end)
gracz:GetAttributeChangedSignal(ATRYBUT):Connect(odswiezWykrzykniki)
odswiezWykrzykniki()

-- ============================================================
--  Rozmowa
-- ============================================================

local function otworz(model)
	if Menu.Otwarte() or gracz:GetAttribute("EkranWczytywania") == true then
		return
	end
	if droga and droga.DoMentora then
		usunDroge()
	end
	Menu.Otworz(model)
	if not Menu.Otwarte() then
		return
	end
	kameraNaMentora(model)
	zablokujRuch(true)
	machnij(model)
	if gracz:GetAttribute(ATRYBUT) == false then
		remote:FireServer("Odwiedzono")
	end
end

ProximityPromptService.PromptTriggered:Connect(function(zapytanie, kto)
	if kto == gracz and zapytanie:GetAttribute("MentorPrompt") then
		otworz(zapytanie:FindFirstAncestorOfClass("Model"))
	end
end)

Menu.Inicjuj(ekran, {
	Ustawienia = Ustawienia,
	Powiadom = powiadom,
	MaCel = function(nazwa)
		return cele:FindFirstChild(nazwa) ~= nil
	end,
	PokazDroge = function(nazwa)
		local wartosc = cele:FindFirstChild(nazwa)
		if wartosc then
			pokazDroge(wartosc.Value.Position, nazwa, "Jesteś na miejscu: " .. nazwa)
			powiadom("Idź za słupem światła: " .. nazwa, "info")
		end
	end,
	Teleport = function(nazwa)
		remote:FireServer("Teleport", nazwa)
	end,
	PoZamknieciu = function()
		przywrocKamere()
		zablokujRuch(false)
	end,
})

-- Po śmierci menu się zamyka (kamera i sterowanie wracają do normy)
gracz.CharacterAdded:Connect(function()
	if Menu.Otwarte() then
		Menu.Zamknij()
	end
end)

-- ============================================================
--  Nowy gracz: podpowiedź i droga do mentora
-- ============================================================

task.spawn(function()
	if not Ustawienia.ProwadzNowych then
		return
	end
	local poczatek = os.clock()
	while gracz:GetAttribute(ATRYBUT) == nil and os.clock() - poczatek < 20 do
		task.wait(0.5)
	end
	if gracz:GetAttribute(ATRYBUT) ~= false then
		return
	end
	czekajNaWczytanie()
	task.wait(2)
	if gracz:GetAttribute(ATRYBUT) ~= false or Menu.Otwarte() then
		return
	end

	local postac = gracz.Character or gracz.CharacterAdded:Wait()
	local korzen = postac:WaitForChild("HumanoidRootPart", 10)
	local najblizszy, najmniej
	for _, wartosc in mentorzy:GetChildren() do
		if wartosc:IsA("CFrameValue") then
			local dystans = korzen and (korzen.Position - wartosc.Value.Position).Magnitude or 0
			if not najmniej or dystans < najmniej then
				najblizszy, najmniej = wartosc, dystans
			end
		end
	end
	if not najblizszy then
		return
	end
	pokazDroge(najblizszy.Value.Position, Ustawienia.Imie, "Naciśnij E, aby porozmawiać z mentorem.", true)
	powiadom(("Nowy na serwerze? Porozmawiaj z: %s (idź za słupem światła)."):format(Ustawienia.Imie), "info")
end)
