--[[
	SYSTEM WEZWAŃ 112  |  Silnik wezwań (serwer)

	Wezwanie z innego skryptu:
		local Wezwania = require(game.ServerScriptService.Wezwania112Serwer.Wezwania)
		Wezwania.Utworz("PozarLasu")                  -- losowe miejsce
		Wezwania.Utworz("Drzewo", workspace.MojePunkt) -- konkretne miejsce
		Wezwania.ZakonczoneWezwanie:Connect(function(dane) print(dane.Nazwa, dane.Udane) end)

	Rodzaj wezwania (moduł Rodzaje) dostaje obiekt wezwania i używa:
		w:Zadanie{ Id, Opis, Cele, Narzedzia, Czas, Akcja, Animacja, Zasieg, Wymaga, PoCelu, PoZakonczeniu, Typ }
		w:Ogien{ Id, Opis, Punkty, Wielkosc, NaZiemi, Wymaga, Rozprzestrzenianie = { Co, Maks, Promien } }
		w:DodajCel(zadanie, czesc), w:Postep(zadanie, 0..1), w:Wykonaj(zadanie)
		w:Pozycja(x, y, z) -> CFrame wzgledem miejsca, w:NaZiemie(Vector3) -> Vector3
]]

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local ServerScriptService = game:GetService("ServerScriptService")
local CollectionService = game:GetService("CollectionService")
local TweenService = game:GetService("TweenService")
local RunService = game:GetService("RunService")
local TextChatService = game:GetService("TextChatService")

local folderWspolny = ReplicatedStorage:WaitForChild("Wezwania112")
local Ustawienia = require(folderWspolny:WaitForChild("Ustawienia"))

local TAG_MIEJSCE = "MiejsceWezwania"

local Wezwania = {}
local Wezwanie = {}
Wezwanie.__index = Wezwanie

local sygnalKonca = Instance.new("BindableEvent")
Wezwania.ZakonczoneWezwanie = sygnalKonca.Event

local budowniczowie = {} -- [id rodzaju] = function(wezwanie)
local aktywne = {} -- [id] = wezwanie
local licznik = 0
local ostatnieUzycie = {} -- [miejsce] = os.clock()
local folderStanu, folderModeli, remote

-- ============================================================
--  Pomocnicze
-- ============================================================

local function strazak(gracz)
	local ranga = gracz:GetAttribute("Ranga")
	if ranga and table.find(Ustawienia.RangiStrazakow or {}, ranga) then
		return true
	end
	return gracz.Team ~= nil and table.find(Ustawienia.DruzynyStrazakow or {}, gracz.Team.Name) ~= nil
end
Wezwania.Strazak = strazak

local function korzen(gracz)
	local postac = gracz.Character
	return postac and postac:FindFirstChild("HumanoidRootPart")
end

local function powiadom(gracz, rodzaj, dane)
	if remote and gracz.Parent == Players then
		remote:FireClient(gracz, rodzaj, dane)
	end
end

local function powiadomStrazakow(rodzaj, dane)
	for _, gracz in Players:GetPlayers() do
		if strazak(gracz) then
			powiadom(gracz, rodzaj, dane)
		end
	end
end

local function liczbaStrazakow()
	local liczba = 0
	for _, gracz in Players:GetPlayers() do
		if strazak(gracz) then
			liczba += 1
		end
	end
	return liczba
end

local function najblizszyStrazak(pozycja, zasieg)
	local najlepszy, najmniej
	for _, gracz in Players:GetPlayers() do
		local jegoKorzen = korzen(gracz)
		if jegoKorzen and strazak(gracz) then
			local dystans = (jegoKorzen.Position - pozycja).Magnitude
			if dystans <= zasieg and (not najmniej or dystans < najmniej) then
				najlepszy, najmniej = gracz, dystans
			end
		end
	end
	return najlepszy
end

-- Zanika i usuwa obiekt (część, model albo gałąź z częściami)
local function zniknij(obiekt, czas)
	czas = czas or 0.8
	local info = TweenInfo.new(czas, Enum.EasingStyle.Quad, Enum.EasingDirection.Out)
	for _, potomek in { obiekt, table.unpack(obiekt:GetDescendants()) } do
		if (potomek:IsA("BasePart") and potomek.Transparency < 1) or potomek:IsA("Decal") then
			TweenService:Create(potomek, info, { Transparency = 1 }):Play()
		elseif
			potomek:IsA("ParticleEmitter")
			or potomek:IsA("Fire")
			or potomek:IsA("Smoke")
			or potomek:IsA("Light")
			or potomek:IsA("ProximityPrompt")
		then
			potomek.Enabled = false
		end
	end
	task.delay(czas + 0.1, function()
		obiekt:Destroy()
	end)
end
Wezwania.Zniknij = zniknij

local function nazwyNarzedzi(grupa)
	local lista = grupa and Ustawienia.Narzedzia[grupa]
	return lista and table.concat(lista, ", ") or ""
end

-- ============================================================
--  Obiekt wezwania
-- ============================================================

function Wezwanie:Pozycja(x, y, z)
	return self.CF * CFrame.new(x, y, z)
end

function Wezwanie:NaZiemie(pozycja)
	local parametry = RaycastParams.new()
	parametry.FilterType = Enum.RaycastFilterType.Exclude
	local pomijane = { folderModeli }
	for _, gracz in Players:GetPlayers() do
		if gracz.Character then
			table.insert(pomijane, gracz.Character)
		end
	end
	parametry.FilterDescendantsInstances = pomijane
	local trafienie = workspace:Raycast(pozycja + Vector3.new(0, 40, 0), Vector3.new(0, -120, 0), parametry)
	if trafienie then
		return Vector3.new(pozycja.X, trafienie.Position.Y, pozycja.Z)
	end
	return pozycja
end

function Wezwanie:_aktualizuj(zadanie)
	local folder = zadanie.Folder
	folder:SetAttribute("Opis", zadanie.Opis)
	folder:SetAttribute("Kolejnosc", zadanie.Kolejnosc)
	folder:SetAttribute("Zrobione", zadanie.Zrobione)
	folder:SetAttribute("Wszystkie", zadanie.Liczba)
	folder:SetAttribute("Aktywne", zadanie.Aktywne)
	folder:SetAttribute("Gotowe", zadanie.Gotowe)
	folder:SetAttribute("Narzedzia", Ustawienia.WymagajNarzedzi and nazwyNarzedzi(zadanie.Narzedzia) or "")
	folder:SetAttribute("Postep", zadanie.Postep)
end

function Wezwanie:_aktywuj(zadanie)
	zadanie.Aktywne = true
	for _, stan in zadanie.Cele do
		if stan.Prompt and not stan.Zrobione then
			stan.Prompt.Enabled = true
		end
	end
	self:_aktualizuj(zadanie)
	if zadanie.PoAktywacji then
		task.spawn(zadanie.PoAktywacji)
	end
end

function Wezwanie:_sprawdzKoniec()
	for _, zadanie in self.Zadania do
		if not zadanie.Gotowe then
			return
		end
	end
	Wezwania.Zakoncz(self, true)
end

function Wezwanie:Wykonaj(zadanie)
	if zadanie.Gotowe or self.Zakonczone then
		return
	end
	zadanie.Gotowe = true
	zadanie.Zrobione = math.max(zadanie.Zrobione, zadanie.Liczba)
	zadanie.Postep = 1
	self:_aktualizuj(zadanie)
	if zadanie.PoZakonczeniu then
		task.spawn(zadanie.PoZakonczeniu)
	end
	for _, inne in self.Zadania do
		if inne.Wymaga == zadanie.Id and not inne.Aktywne then
			self:_aktywuj(inne)
		end
	end
	self:_sprawdzKoniec()
end

function Wezwanie:Postep(zadanie, ulamek)
	zadanie.Postep = math.clamp(ulamek, 0, 1)
	self:_aktualizuj(zadanie)
end

-- Cel wykonany (przez gracza albo np. wodę z prądownicy)
function Wezwanie:_oznacz(zadanie, cel, gracz)
	local stan = zadanie.Cele[cel]
	if not stan or stan.Zrobione or self.Zakonczone then
		return
	end
	stan.Zrobione = true
	if stan.Prompt then
		stan.Prompt:Destroy()
		stan.Prompt = nil
	end
	if gracz then
		self.Uczestnicy[gracz.UserId] = true
	end
	zadanie.Zrobione += 1
	self:_aktualizuj(zadanie)
	if zadanie.PoCelu then
		task.spawn(zadanie.PoCelu, cel, gracz)
	else
		zniknij(cel)
	end
	if zadanie.Zrobione >= zadanie.Liczba then
		self:Wykonaj(zadanie)
	end
end

-- Próba wykonania celu przez gracza (E): sprawdza rangę, odległość i narzędzie
function Wezwanie:_proba(zadanie, cel, gracz)
	if self.Zakonczone or not zadanie.Aktywne then
		return
	end
	if not strazak(gracz) then
		powiadom(gracz, "Info", { Tekst = "To zadanie dla straży pożarnej.", Rodzaj = "blad" })
		return
	end
	local mojKorzen = korzen(gracz)
	if not mojKorzen or not cel.Parent or (mojKorzen.Position - cel.Position).Magnitude > zadanie.Zasieg + 10 then
		return
	end
	if zadanie.Narzedzia and Ustawienia.WymagajNarzedzi then
		local lista = Ustawienia.Narzedzia[zadanie.Narzedzia] or {}
		local trzymane = gracz.Character:FindFirstChildOfClass("Tool")
		if not trzymane or not table.find(lista, trzymane.Name) then
			powiadom(gracz, "Info", { Tekst = "Weź do ręki: " .. table.concat(lista, " / "), Rodzaj = "blad" })
			return
		end
	end
	self:_oznacz(zadanie, cel, gracz)
end

function Wezwanie:DodajCel(zadanie, cel)
	local stan = { Zrobione = false }
	zadanie.Cele[cel] = stan
	zadanie.Liczba += 1
	if zadanie.Typ ~= "Auto" then
		local zapytanie = Instance.new("ProximityPrompt")
		zapytanie.Name = "Wezwanie112"
		zapytanie.ActionText = zadanie.Akcja
		zapytanie.ObjectText = self.Def.Nazwa
		zapytanie.HoldDuration = zadanie.Czas
		zapytanie.MaxActivationDistance = zadanie.Zasieg
		zapytanie.RequiresLineOfSight = false
		zapytanie.Enabled = zadanie.Aktywne
		zapytanie:SetAttribute("Wezwanie112", true)
		zapytanie:SetAttribute("Zasieg", zadanie.Zasieg)
		zapytanie:SetAttribute("Animacja", zadanie.Animacja or "")
		zapytanie.Triggered:Connect(function(gracz)
			self:_proba(zadanie, cel, gracz)
		end)
		zapytanie.Parent = cel
		stan.Prompt = zapytanie
	end
	self:_aktualizuj(zadanie)
	return stan
end

function Wezwanie:Zadanie(opcje)
	local zadanie = {
		Id = opcje.Id,
		Opis = opcje.Opis,
		Typ = opcje.Typ or "Akcja",
		Narzedzia = opcje.Narzedzia,
		Czas = opcje.Czas or 3,
		Akcja = opcje.Akcja or "Wykonaj",
		Animacja = opcje.Animacja or "Praca",
		Zasieg = opcje.Zasieg or 10,
		Wymaga = opcje.Wymaga,
		PoCelu = opcje.PoCelu,
		PoZakonczeniu = opcje.PoZakonczeniu,
		PoAktywacji = opcje.PoAktywacji,
		Cele = {},
		Liczba = 0,
		Zrobione = 0,
		Postep = 0,
		Gotowe = false,
		Aktywne = opcje.Wymaga == nil,
		Kolejnosc = #self.Zadania + 1,
	}
	zadanie.Folder = Instance.new("Folder")
	zadanie.Folder.Name = "Zadanie" .. zadanie.Kolejnosc
	table.insert(self.Zadania, zadanie)
	self.ZadaniaPoId[zadanie.Id] = zadanie
	for _, cel in opcje.Cele or {} do
		self:DodajCel(zadanie, cel)
	end
	if zadanie.Typ == "Auto" then
		zadanie.Liczba = 1
	end
	self:_aktualizuj(zadanie)
	zadanie.Folder.Parent = self.Folder
	return zadanie
end

-- ============================================================
--  Ogień (gasi się wodą z prądownic albo przytrzymaniem E z gaśniczym narzędziem)
-- ============================================================

function Wezwanie:_dodajOgien(zadanie, pozycja, wielkosc)
	wielkosc = wielkosc or 1
	local czesc = Instance.new("Part")
	czesc.Name = "Ogien"
	czesc.Size = Vector3.new(6, 6, 6) * wielkosc
	czesc.CFrame = CFrame.new(pozycja)
	czesc.Anchored = true
	czesc.CanCollide = false
	czesc.CanQuery = false
	czesc.CanTouch = true -- woda z prądownic musi w nią trafić
	czesc.Transparency = 1

	local ogien = Instance.new("Fire")
	ogien.Name = "Fire"
	ogien.Size = 9 * wielkosc
	ogien.Heat = 12
	ogien.Color = Color3.fromRGB(255, 120, 40)
	ogien.SecondaryColor = Color3.fromRGB(255, 200, 60)
	ogien.Parent = czesc

	local swiatlo = Instance.new("PointLight")
	swiatlo.Name = "FireLight"
	swiatlo.Color = Color3.fromRGB(255, 140, 60)
	swiatlo.Range = 18
	swiatlo.Brightness = 2.5
	swiatlo.Parent = czesc

	if (Ustawienia.DzwiekOgnia or "") ~= "" then
		local dzwiek = Instance.new("Sound")
		dzwiek.SoundId = Ustawienia.DzwiekOgnia
		dzwiek.Looped = true
		dzwiek.Volume = 0.6
		dzwiek.RollOffMaxDistance = 80
		dzwiek.Parent = czesc
		dzwiek:Play()
	end

	-- Dym osobno: inaczej woda z części prądownic najpierw "gasiłaby" dym
	local dym = Instance.new("Part")
	dym.Name = "Dym"
	dym.Size = Vector3.new(1, 1, 1)
	dym.CFrame = CFrame.new(pozycja + Vector3.new(0, 6 * wielkosc, 0))
	dym.Anchored = true
	dym.CanCollide = false
	dym.CanQuery = false
	dym.CanTouch = false
	dym.Transparency = 1
	local efekt = Instance.new("Smoke")
	efekt.Color = Color3.fromRGB(55, 55, 58)
	efekt.Opacity = 0.35
	efekt.RiseVelocity = 7
	efekt.Size = 7 * wielkosc
	efekt.Parent = dym
	dym.Parent = self.Model
	czesc.Parent = self.Model

	local stan = self:DodajCel(zadanie, czesc)
	stan.Dym = dym
	ogien.Destroying:Connect(function()
		if not stan.Zrobione and not self.Zakonczone then
			self:_oznacz(zadanie, czesc, najblizszyStrazak(czesc.Position, 90))
		end
	end)
	return czesc
end

function Wezwanie:Ogien(opcje)
	local zadanie = self:Zadanie({
		Id = opcje.Id or "Ogien",
		Opis = opcje.Opis or "Ugaś pożar",
		Narzedzia = "Gasnicze",
		Czas = opcje.Czas or 3,
		Akcja = "Gaś ogień",
		Animacja = "Gaszenie",
		Zasieg = 14,
		Wymaga = opcje.Wymaga,
		PoZakonczeniu = opcje.PoZakonczeniu,
	})
	zadanie.Typ = "Ogien"
	-- Ugaszone ognisko: para wodna i zniknięcie
	zadanie.PoCelu = function(czesc)
		local stan = zadanie.Cele[czesc]
		local dym = stan and stan.Dym
		if dym then
			local efekt = dym:FindFirstChildOfClass("Smoke")
			if efekt then
				efekt.Color = Color3.fromRGB(230, 230, 235)
				efekt.Opacity = 0.2
			end
			task.delay(2.5, function()
				zniknij(dym, 0.5)
			end)
		end
		czesc:Destroy()
	end

	local naZiemi = opcje.NaZiemi ~= false
	for _, pozycja in opcje.Punkty do
		if naZiemi then
			pozycja = self:NaZiemie(pozycja) + Vector3.new(0, 2, 0)
		end
		self:_dodajOgien(zadanie, pozycja, opcje.Wielkosc)
	end

	local rozprzestrzenianie = opcje.Rozprzestrzenianie
	if rozprzestrzenianie then
		task.spawn(function()
			while not self.Zakonczone and not zadanie.Gotowe do
				task.wait(rozprzestrzenianie.Co or 30)
				if self.Zakonczone or zadanie.Gotowe or not zadanie.Aktywne then
					continue
				end
				local plonace = {}
				for czesc, stan in zadanie.Cele do
					if not stan.Zrobione and czesc.Parent then
						table.insert(plonace, czesc)
					end
				end
				if #plonace > 0 and zadanie.Liczba < (rozprzestrzenianie.Maks or 12) then
					local zrodlo = plonace[math.random(#plonace)]
					local kat = math.random() * math.pi * 2
					local nowa = zrodlo.Position + Vector3.new(math.cos(kat), 0, math.sin(kat)) * math.random(8, 12)
					local srodek = self.CF.Position
					local odSrodka = (nowa - srodek) * Vector3.new(1, 0, 1)
					local promien = rozprzestrzenianie.Promien or 35
					if odSrodka.Magnitude > promien then
						nowa = Vector3.new(srodek.X, nowa.Y, srodek.Z) + odSrodka.Unit * promien
					end
					if naZiemi then
						nowa = self:NaZiemie(nowa) + Vector3.new(0, 2, 0)
					end
					self:_dodajOgien(zadanie, nowa, opcje.Wielkosc)
				end
			end
		end)
	end
	return zadanie
end

-- ============================================================
--  Tworzenie i kończenie wezwań
-- ============================================================

local function modulAlarmow()
	local skrypt = ServerScriptService:FindFirstChild("AlarmSerwer")
	local modul = skrypt and skrypt:FindFirstChild("Alarmy")
	if not modul then
		return nil
	end
	local ok, wynik = pcall(require, modul)
	return ok and wynik or nil
end

local function jednostkiAlarmowe()
	local lista = {}
	local folder = ReplicatedStorage:FindFirstChild("AlarmOSP")
	local modul = folder and folder:FindFirstChild("Ustawienia")
	if modul then
		local ok, ustawieniaAlarmu = pcall(require, modul)
		if ok then
			for _, jednostka in ustawieniaAlarmu.Jednostki or {} do
				table.insert(lista, jednostka.Nazwa)
			end
		end
	end
	return lista
end

local function miejscaDla(idRodzaju)
	local lista = {}
	for _, miejsce in CollectionService:GetTagged(TAG_MIEJSCE) do
		if miejsce:IsA("BasePart") and miejsce:IsDescendantOf(workspace) then
			local rodzaje = miejsce:GetAttribute("Rodzaje")
			local pasuje = typeof(rodzaje) ~= "string" or rodzaje == ""
			if not pasuje then
				for nazwa in string.gmatch(rodzaje, "[^,%s]+") do
					if string.lower(nazwa) == string.lower(idRodzaju) then
						pasuje = true
						break
					end
				end
			end
			local zajete = false
			for _, wezwanie in aktywne do
				if wezwanie.Miejsce == miejsce then
					zajete = true
					break
				end
			end
			if pasuje and not zajete then
				table.insert(lista, miejsce)
			end
		end
	end
	return lista
end

local function wlaczoneRodzaje()
	local lista = {}
	for id, dane in Ustawienia.Rodzaje do
		if dane.Wlaczony ~= false and budowniczowie[id] then
			table.insert(lista, id)
		end
	end
	table.sort(lista)
	return lista
end
Wezwania.Rodzaje = wlaczoneRodzaje

function Wezwania.Zarejestruj(id, budowa)
	budowniczowie[id] = budowa
end

function Wezwania.Aktywne()
	return aktywne
end

function Wezwania.Zakoncz(wezwanie, udane)
	if wezwanie.Zakonczone then
		return
	end
	wezwanie.Zakonczone = true
	aktywne[wezwanie.Id] = nil
	wezwanie.Folder:SetAttribute("Zakonczone", true)
	wezwanie.Folder:SetAttribute("Udane", udane)
	for _, zadanie in wezwanie.Zadania do
		for _, stan in zadanie.Cele do
			if stan.Prompt then
				stan.Prompt:Destroy()
				stan.Prompt = nil
			end
		end
	end

	local uczestnicy = 0
	for userId in wezwanie.Uczestnicy do
		uczestnicy += 1
		local gracz = Players:GetPlayerByUserId(userId)
		if gracz and udane and Ustawienia.Statystyki then
			local tabela = gracz:FindFirstChild("leaderstats")
			if not tabela then
				tabela = Instance.new("Folder")
				tabela.Name = "leaderstats"
				tabela.Parent = gracz
			end
			local licznikWezwan = tabela:FindFirstChild(Ustawienia.NazwaStatystyki)
			if not licznikWezwan then
				licznikWezwan = Instance.new("IntValue")
				licznikWezwan.Name = Ustawienia.NazwaStatystyki
				licznikWezwan.Parent = tabela
			end
			licznikWezwan.Value += 1
		end
	end

	local dane = {
		Nazwa = wezwanie.Def.Nazwa,
		Miejsce = wezwanie.Miejsce.Name,
		Udane = udane,
		Uczestnikow = uczestnicy,
	}
	powiadomStrazakow("Koniec", dane)
	sygnalKonca:Fire(dane)

	if wezwanie.IdAlarmu then
		local alarmy = modulAlarmow()
		if alarmy and alarmy.Zakoncz then
			pcall(alarmy.Zakoncz, wezwanie.IdAlarmu)
		end
	end

	task.delay(udane and 6 or 1, function()
		wezwanie.Folder:Destroy()
		zniknij(wezwanie.Model, 2)
	end)
end

function Wezwania.Utworz(idRodzaju, miejsce)
	local def = Ustawienia.Rodzaje[idRodzaju]
	local budowa = budowniczowie[idRodzaju]
	if not def or not budowa then
		return nil, "Nie ma takiego rodzaju wezwania: " .. tostring(idRodzaju)
	end
	if not miejsce then
		local wolne = miejscaDla(idRodzaju)
		if #wolne == 0 then
			return nil, "Brak wolnego miejsca dla: " .. def.Nazwa .. " (Tag " .. TAG_MIEJSCE .. ")"
		end
		-- Najdłużej nieużywane miejsce ma pierwszeństwo
		table.sort(wolne, function(a, b)
			return (ostatnieUzycie[a] or 0) < (ostatnieUzycie[b] or 0)
		end)
		miejsce = wolne[math.random(1, math.min(#wolne, 3))]
	end

	licznik += 1
	local wezwanie = setmetatable({
		Id = licznik,
		IdRodzaju = idRodzaju,
		Def = def,
		Miejsce = miejsce,
		Zadania = {},
		ZadaniaPoId = {},
		Uczestnicy = {},
		Zakonczone = false,
	}, Wezwanie)

	local podstawa = miejsce.Position - Vector3.new(0, miejsce.Size.Y / 2, 0)
	local przod = miejsce.CFrame.LookVector * Vector3.new(1, 0, 1)
	przod = przod.Magnitude > 0.01 and przod.Unit or Vector3.new(0, 0, -1)
	wezwanie.CF = CFrame.lookAt(podstawa, podstawa + przod)

	wezwanie.Model = Instance.new("Model")
	wezwanie.Model.Name = "Wezwanie_" .. licznik
	wezwanie.Model.Parent = folderModeli

	wezwanie.Folder = Instance.new("Folder")
	wezwanie.Folder.Name = tostring(licznik)
	wezwanie.Folder:SetAttribute("Rodzaj", idRodzaju)
	wezwanie.Folder:SetAttribute("Nazwa", def.Nazwa)
	wezwanie.Folder:SetAttribute("Opis", def.Opis)
	wezwanie.Folder:SetAttribute("Miejsce", miejsce.Name)
	wezwanie.Folder:SetAttribute("Pozycja", podstawa)
	wezwanie.Folder:SetAttribute("Kolor", def.Kolor)
	wezwanie.Folder:SetAttribute("Czas", os.time())

	local ok, blad = pcall(budowa, wezwanie)
	if not ok then
		warn("[Wezwania112] Błąd przy tworzeniu " .. idRodzaju .. ": " .. tostring(blad))
		wezwanie.Model:Destroy()
		wezwanie.Folder:Destroy()
		return nil, "Błąd przy tworzeniu wezwania."
	end

	aktywne[wezwanie.Id] = wezwanie
	ostatnieUzycie[miejsce] = os.clock()
	wezwanie.Folder.Parent = folderStanu

	-- Pager z systemu alarmowania OSP (jeśli jest w grze)
	local wyslanoAlarm = false
	if Ustawienia.UzyjSystemuAlarmowego then
		local alarmy = modulAlarmow()
		if alarmy then
			local okAlarm, wynik, _, idAlarmu = pcall(alarmy.Wyslij, {
				Jednostki = jednostkiAlarmowe(),
				Rodzaj = def.Alarm,
				Miejsce = miejsce,
				NazwaMiejsca = miejsce.Name,
				Opis = def.Opis,
			}, "Centrum 112")
			if okAlarm and wynik then
				wyslanoAlarm = true
				wezwanie.IdAlarmu = idAlarmu
			end
		end
	end
	powiadomStrazakow("Nowe", {
		Id = wezwanie.Id,
		Nazwa = def.Nazwa,
		Miejsce = miejsce.Name,
		Opis = def.Opis,
		Kolor = def.Kolor,
		Pager = wyslanoAlarm,
	})

	task.delay(Ustawienia.CzasNaAkcje or 900, function()
		if not wezwanie.Zakonczone then
			Wezwania.Zakoncz(wezwanie, false)
		end
	end)
	return wezwanie
end

function Wezwania.Losuj()
	local rodzaje = wlaczoneRodzaje()
	for _ = 1, #rodzaje do
		local id = rodzaje[math.random(#rodzaje)]
		if #miejscaDla(id) > 0 then
			return Wezwania.Utworz(id)
		end
	end
	return nil, "Brak wolnych miejsc wezwań (Tag " .. TAG_MIEJSCE .. ")."
end

-- ============================================================
--  Komenda /wezwanie
-- ============================================================

local rangaWGrupie = {}

local function czyAdmin(gracz)
	if RunService:IsStudio() or table.find(Ustawienia.Admini or {}, gracz.UserId) then
		return true
	end
	if game.CreatorType == Enum.CreatorType.User then
		return gracz.UserId == game.CreatorId
	end
	if game.CreatorType == Enum.CreatorType.Group then
		if rangaWGrupie[gracz] == nil then
			local ok, wynik = pcall(function()
				return gracz:GetRankInGroup(game.CreatorId)
			end)
			rangaWGrupie[gracz] = ok and wynik >= (Ustawienia.AdminOdRangiWGrupie or 255)
		end
		return rangaWGrupie[gracz]
	end
	return false
end

local function info(gracz, tekst, rodzaj)
	powiadom(gracz, "Info", { Tekst = tekst, Rodzaj = rodzaj or "info" })
end

local function komenda(gracz, tekst)
	if not czyAdmin(gracz) then
		info(gracz, "Nie masz uprawnień do tworzenia wezwań.", "blad")
		return
	end
	local argument = string.match(tekst, "^%s*%S+%s+(%S+)")
	if argument == nil then
		local wezwanie, blad = Wezwania.Losuj()
		info(gracz, wezwanie and ("Utworzono: " .. wezwanie.Def.Nazwa .. " (" .. wezwanie.Miejsce.Name .. ")") or blad, wezwanie and "sukces" or "blad")
		return
	end
	local maly = string.lower(argument)
	if maly == "lista" then
		info(gracz, "Rodzaje: " .. table.concat(wlaczoneRodzaje(), ", "))
		return
	elseif maly == "stop" then
		for _, wezwanie in aktywne do
			Wezwania.Zakoncz(wezwanie, false)
		end
		info(gracz, "Zakończono wszystkie wezwania.", "sukces")
		return
	end
	for _, id in wlaczoneRodzaje() do
		if string.lower(id) == maly then
			local wezwanie, blad = Wezwania.Utworz(id)
			info(gracz, wezwanie and ("Utworzono: " .. wezwanie.Def.Nazwa .. " (" .. wezwanie.Miejsce.Name .. ")") or blad, wezwanie and "sukces" or "blad")
			return
		end
	end
	info(gracz, "Nie ma takiego rodzaju. Wpisz /wezwanie lista", "blad")
end

local function podlaczKomende()
	if TextChatService.ChatVersion == Enum.ChatVersion.TextChatService then
		local polecenie = Instance.new("TextChatCommand")
		polecenie.Name = "Wezwanie112"
		polecenie.PrimaryAlias = "/wezwanie"
		polecenie.Triggered:Connect(function(zrodlo, tekst)
			local gracz = Players:GetPlayerByUserId(zrodlo.UserId)
			if gracz then
				komenda(gracz, tekst)
			end
		end)
		polecenie.Parent = TextChatService:FindFirstChild("TextChatCommands") or TextChatService
	else
		local function sluchaj(gracz)
			gracz.Chatted:Connect(function(wiadomosc)
				local pierwsze = string.match(wiadomosc, "^%s*(%S+)")
				if pierwsze and string.lower(pierwsze) == "/wezwanie" then
					komenda(gracz, wiadomosc)
				end
			end)
		end
		Players.PlayerAdded:Connect(sluchaj)
		for _, gracz in Players:GetPlayers() do
			sluchaj(gracz)
		end
	end
end

-- ============================================================
--  Start
-- ============================================================

local wystartowano = false

function Wezwania.Start()
	if wystartowano then
		return
	end
	wystartowano = true

	local stary = folderWspolny:FindFirstChild("Aktywne")
	if stary then
		stary:Destroy()
	end
	folderStanu = Instance.new("Folder")
	folderStanu.Name = "Aktywne"
	folderStanu.Parent = folderWspolny

	local stareZdarzenia = folderWspolny:FindFirstChild("Zdarzenia")
	if stareZdarzenia then
		stareZdarzenia:Destroy()
	end
	local zdarzenia = Instance.new("Folder")
	zdarzenia.Name = "Zdarzenia"
	remote = Instance.new("RemoteEvent")
	remote.Name = "Powiadomienie"
	remote.Parent = zdarzenia
	zdarzenia.Parent = folderWspolny

	folderModeli = workspace:FindFirstChild("Wezwania112") or Instance.new("Folder")
	folderModeli.Name = "Wezwania112"
	folderModeli.Parent = workspace

	podlaczKomende()
	Players.PlayerRemoving:Connect(function(gracz)
		rangaWGrupie[gracz] = nil
	end)

	if #CollectionService:GetTagged(TAG_MIEJSCE) == 0 then
		warn("[Wezwania112] Brak miejsc z Tagiem " .. TAG_MIEJSCE .. ": wezwania nie będą się pojawiać.")
	end

	-- Automatyczne wezwania
	task.spawn(function()
		task.wait(Ustawienia.PierwszeWezwaniePo or 90)
		while true do
			if Ustawienia.Automatyczne then
				local ileAktywnych = 0
				for _ in aktywne do
					ileAktywnych += 1
				end
				if ileAktywnych < (Ustawienia.MaksAktywnych or 2) and liczbaStrazakow() >= (Ustawienia.MinStrazakow or 1) then
					Wezwania.Losuj()
				end
			end
			task.wait(math.random(Ustawienia.OdstepMin or 240, math.max(Ustawienia.OdstepMax or 480, Ustawienia.OdstepMin or 240)))
		end
	end)
end

return Wezwania
