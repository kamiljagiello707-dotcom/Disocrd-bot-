--[[
	MENTOR  |  Serwer
	Stawia mentora na każdej części z Tagiem "Mentor", zapamiętuje,
	kto już z nim rozmawiał, i (jeśli włączone) teleportuje do celów.
	Ustawienia: ReplicatedStorage > Mentor > Ustawienia
]]

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local CollectionService = game:GetService("CollectionService")
local DataStoreService = game:GetService("DataStoreService")
local RunService = game:GetService("RunService")

local folderWspolny = ReplicatedStorage:WaitForChild("Mentor")
local Ustawienia = require(folderWspolny:WaitForChild("Ustawienia"))

local TAG_PUNKT = "Mentor"
local TAG_NPC = "MentorNPC"
local TAG_CEL = "MentorCel"
local ATRYBUT = "MentorOdwiedzony"
local CZCIONKA = "rbxasset://fonts/families/GothamSSm.json"
local ANIMACJA_STANIA = "rbxassetid://180435571" -- domyślna animacja stania R6 od Roblox

-- ============================================================
--  Komunikacja i pozycje dla graczy
--  (pozycje są w ReplicatedStorage, bo przy StreamingEnabled gracz
--  może nie mieć wczytanego mentora ani celu, gdy jest daleko)
-- ============================================================

local function nowyFolder(nazwa)
	local stary = folderWspolny:FindFirstChild(nazwa)
	if stary then
		stary:Destroy()
	end
	local folder = Instance.new("Folder")
	folder.Name = nazwa
	folder.Parent = folderWspolny
	return folder
end

local zdarzenia = nowyFolder("Zdarzenia")
local remote = Instance.new("RemoteEvent")
remote.Name = "Mentor"
remote.Parent = zdarzenia

local cele = nowyFolder("Cele")
local pozycjeMentorow = nowyFolder("Mentorzy")

-- ============================================================
--  Wygląd mentora
-- ============================================================

local zapisanyOpis

local function opisWygladu()
	if zapisanyOpis then
		return zapisanyOpis:Clone()
	end
	local userId = Ustawienia.WygladZGracza or 0
	if userId == 0 and game.CreatorType == Enum.CreatorType.User then
		userId = game.CreatorId
	end
	if userId > 0 then
		local ok, opis = pcall(function()
			return Players:GetHumanoidDescriptionFromUserId(userId)
		end)
		if ok and opis then
			zapisanyOpis = opis
			return opis:Clone()
		end
		warn("[Mentor] Nie udało się pobrać wyglądu gracza " .. userId .. ", używam zwykłego.")
	end
	local opis = Instance.new("HumanoidDescription")
	local skora = Color3.fromRGB(234, 184, 146)
	opis.HeadColor = skora
	opis.LeftArmColor = skora
	opis.RightArmColor = skora
	opis.TorsoColor = Color3.fromRGB(45, 45, 52)
	opis.LeftLegColor = Color3.fromRGB(30, 30, 36)
	opis.RightLegColor = Color3.fromRGB(30, 30, 36)
	zapisanyOpis = opis
	return opis:Clone()
end

local function plakietka(glowa)
	local tablica = Instance.new("BillboardGui")
	tablica.Name = "Plakietka"
	tablica.Size = UDim2.fromOffset(240, 80)
	tablica.StudsOffset = Vector3.new(0, 2.6, 0)
	tablica.MaxDistance = 70
	tablica.LightInfluence = 0

	-- Wykrzyknik: gracz sam go pokazuje lub chowa (zależnie od tego, czy jest nowy)
	local wykrzyknik = Instance.new("Frame")
	wykrzyknik.Name = "Wykrzyknik"
	wykrzyknik.AnchorPoint = Vector2.new(0.5, 0)
	wykrzyknik.Position = UDim2.fromScale(0.5, 0)
	wykrzyknik.Size = UDim2.fromOffset(28, 28)
	wykrzyknik.BackgroundColor3 = Ustawienia.KolorAkcentu
	wykrzyknik.Visible = false
	local zaokraglenie = Instance.new("UICorner")
	zaokraglenie.CornerRadius = UDim.new(0.5, 0)
	zaokraglenie.Parent = wykrzyknik
	local znak = Instance.new("TextLabel")
	znak.BackgroundTransparency = 1
	znak.Size = UDim2.fromScale(1, 1)
	znak.Text = "!"
	znak.TextColor3 = Color3.new(1, 1, 1)
	znak.TextSize = 20
	znak.FontFace = Font.new(CZCIONKA, Enum.FontWeight.Heavy)
	znak.Parent = wykrzyknik
	wykrzyknik.Parent = tablica

	local imie = Instance.new("TextLabel")
	imie.BackgroundTransparency = 1
	imie.Position = UDim2.fromOffset(0, 34)
	imie.Size = UDim2.new(1, 0, 0, 22)
	imie.Text = Ustawienia.Imie
	imie.TextColor3 = Color3.fromRGB(245, 245, 247)
	imie.TextStrokeTransparency = 0.6
	imie.TextSize = 18
	imie.FontFace = Font.new(CZCIONKA, Enum.FontWeight.Bold)
	imie.Parent = tablica

	local tytul = Instance.new("TextLabel")
	tytul.BackgroundTransparency = 1
	tytul.Position = UDim2.fromOffset(0, 56)
	tytul.Size = UDim2.new(1, 0, 0, 18)
	tytul.Text = Ustawienia.Tytul
	tytul.TextColor3 = Color3.fromRGB(210, 210, 218)
	tytul.TextStrokeTransparency = 0.7
	tytul.TextSize = 13
	tytul.FontFace = Font.new(CZCIONKA, Enum.FontWeight.Medium)
	tytul.Parent = tablica

	tablica.Parent = glowa
end

local function postawMentora(punkt)
	if not punkt:IsA("BasePart") or punkt:GetAttribute("MentorPostawiony") then
		return
	end
	punkt:SetAttribute("MentorPostawiony", true)

	local ok, model = pcall(function()
		return Players:CreateHumanoidModelFromDescription(opisWygladu(), Enum.HumanoidRigType.R6)
	end)
	if not ok or not model then
		warn("[Mentor] Nie udało się stworzyć mentora: " .. tostring(model))
		return
	end
	model.Name = Ustawienia.Imie

	local humanoid = model:FindFirstChildOfClass("Humanoid")
	local korzen = model:FindFirstChild("HumanoidRootPart")
	local glowa = model:FindFirstChild("Head")
	if not humanoid or not korzen or not glowa then
		warn("[Mentor] Model mentora nie ma Humanoid / HumanoidRootPart / Head.")
		model:Destroy()
		return
	end
	humanoid.DisplayDistanceType = Enum.HumanoidDisplayDistanceType.None
	humanoid.MaxHealth = math.huge
	humanoid.Health = math.huge
	korzen.Anchored = true

	-- Ustawienie w miejscu części: tylko obrót w poziomie, stopy na wysokości jej spodu
	-- (część zwykle stoi na ziemi i potem i tak robi się niewidzialna)
	local podstawa = punkt.Position - Vector3.new(0, punkt.Size.Y / 2, 0)
	local przod = punkt.CFrame.LookVector * Vector3.new(1, 0, 1)
	if przod.Magnitude < 0.01 then
		przod = Vector3.new(0, 0, -1)
	end
	model:PivotTo(CFrame.lookAt(podstawa, podstawa + przod.Unit))
	local srodek, rozmiar = model:GetBoundingBox()
	local dol = srodek.Position.Y - rozmiar.Y / 2
	model:PivotTo(model:GetPivot() + Vector3.new(0, podstawa.Y - dol, 0))

	punkt.Transparency = 1
	punkt.CanCollide = false
	punkt.CanTouch = false
	punkt.CanQuery = false

	local zapytanie = Instance.new("ProximityPrompt")
	zapytanie.Name = "Rozmowa"
	zapytanie.ActionText = "Porozmawiaj"
	zapytanie.ObjectText = Ustawienia.Imie
	zapytanie.HoldDuration = 0
	zapytanie.MaxActivationDistance = 10
	zapytanie.RequiresLineOfSight = false
	zapytanie:SetAttribute("MentorPrompt", true)
	zapytanie.Parent = korzen

	plakietka(glowa)

	CollectionService:AddTag(model, TAG_NPC)
	model.Parent = workspace

	local animator = humanoid:FindFirstChildOfClass("Animator")
	if not animator then
		animator = Instance.new("Animator")
		animator.Parent = humanoid
	end
	local animacja = Instance.new("Animation")
	animacja.AnimationId = ANIMACJA_STANIA
	pcall(function()
		local sciezka = animator:LoadAnimation(animacja)
		sciezka.Looped = true
		sciezka:Play()
	end)

	local pozycja = Instance.new("CFrameValue")
	pozycja.Name = Ustawienia.Imie
	pozycja.Value = korzen.CFrame
	pozycja.Parent = pozycjeMentorow
end

local function opublikujCel(obiekt)
	if not obiekt:IsA("PVInstance") or not obiekt:IsDescendantOf(workspace) then
		return
	end
	local wartosc = cele:FindFirstChild(obiekt.Name) or Instance.new("CFrameValue")
	wartosc.Name = obiekt.Name
	wartosc.Value = obiekt:GetPivot()
	wartosc.Parent = cele
end

-- ============================================================
--  Kto już rozmawiał z mentorem
-- ============================================================

local magazyn
if Ustawienia.ZapamietajGraczy then
	local ok, wynik = pcall(function()
		return DataStoreService:GetDataStore("MentorOdwiedzeni_v1")
	end)
	if ok then
		magazyn = wynik
	else
		warn("[Mentor] DataStore niedostępny, nowy gracz = każde wejście: " .. tostring(wynik))
	end
end

local function klucz(gracz)
	return "gracz_" .. gracz.UserId
end

local function dolaczyl(gracz)
	local odwiedzony = false
	if magazyn then
		local ok, wynik = pcall(function()
			return magazyn:GetAsync(klucz(gracz))
		end)
		if ok then
			odwiedzony = wynik == true
		elseif not RunService:IsStudio() then
			warn("[Mentor] Nie udało się wczytać " .. gracz.Name .. ": " .. tostring(wynik))
		end
	end
	if gracz.Parent == Players then
		gracz:SetAttribute(ATRYBUT, odwiedzony)
	end
end

-- ============================================================
--  Zdarzenia od graczy
-- ============================================================

local ostatniaAkcja = {}
local ostatniTeleport = {}

remote.OnServerEvent:Connect(function(gracz, akcja, nazwaCelu)
	local teraz = os.clock()
	if ostatniaAkcja[gracz] and teraz - ostatniaAkcja[gracz] < 0.5 then
		return
	end
	ostatniaAkcja[gracz] = teraz

	if akcja == "Odwiedzono" then
		if gracz:GetAttribute(ATRYBUT) ~= true then
			gracz:SetAttribute(ATRYBUT, true)
			if magazyn then
				task.spawn(function()
					pcall(function()
						magazyn:SetAsync(klucz(gracz), true, { gracz.UserId })
					end)
				end)
			end
		end
	elseif akcja == "Teleport" then
		if not Ustawienia.PozwolNaTeleport or typeof(nazwaCelu) ~= "string" then
			return
		end
		if ostatniTeleport[gracz] and teraz - ostatniTeleport[gracz] < 10 then
			return
		end
		local postac = gracz.Character
		local humanoid = postac and postac:FindFirstChildOfClass("Humanoid")
		if not humanoid or humanoid.Health <= 0 then
			return
		end
		for _, obiekt in CollectionService:GetTagged(TAG_CEL) do
			if obiekt.Name == nazwaCelu and obiekt:IsA("PVInstance") and obiekt:IsDescendantOf(workspace) then
				ostatniTeleport[gracz] = teraz
				local wysokosc = obiekt:IsA("BasePart") and obiekt.Size.Y / 2 or 0
				postac:PivotTo(obiekt:GetPivot() + Vector3.new(0, wysokosc + 3.5, 0))
				return
			end
		end
	end
end)

-- ============================================================
--  Start
-- ============================================================

for _, punkt in CollectionService:GetTagged(TAG_PUNKT) do
	task.spawn(postawMentora, punkt)
end
CollectionService:GetInstanceAddedSignal(TAG_PUNKT):Connect(postawMentora)

for _, obiekt in CollectionService:GetTagged(TAG_CEL) do
	opublikujCel(obiekt)
end
CollectionService:GetInstanceAddedSignal(TAG_CEL):Connect(opublikujCel)

Players.PlayerAdded:Connect(dolaczyl)
for _, gracz in Players:GetPlayers() do
	task.spawn(dolaczyl, gracz)
end
Players.PlayerRemoving:Connect(function(gracz)
	ostatniaAkcja[gracz] = nil
	ostatniTeleport[gracz] = nil
end)

if #CollectionService:GetTagged(TAG_PUNKT) == 0 then
	warn("[Mentor] Brak części z Tagiem 'Mentor': mentor nie stanie na mapie.")
end
