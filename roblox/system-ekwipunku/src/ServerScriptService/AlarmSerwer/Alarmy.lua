--[[
	SYSTEM ALARMOWANIA OSP  |  Serwer

	Alarm z innego skryptu na serwerze (np. automatyczny pożar):
		local Alarmy = require(game.ServerScriptService.AlarmSerwer.Alarmy)
		Alarmy.Wyslij({
			Jednostki = { "OSP" },
			Rodzaj = "Pożar",
			Miejsce = workspace.Dom,       -- część/model, Vector3 albo nil
			NazwaMiejsca = "ul. Leśna 12", -- opcjonalnie (gdy Miejsce to Vector3)
			Opis = "Pali się dom",
		})

	Zdarzenie dla innych skryptów:
		Alarmy.NowyAlarm:Connect(function(alarm) print(alarm.Rodzaj, alarm.Miejsce) end)
]]

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local CollectionService = game:GetService("CollectionService")
local TextService = game:GetService("TextService")
local RunService = game:GetService("RunService")
local TextChatService = game:GetService("TextChatService")

local folderWspolny = ReplicatedStorage:WaitForChild("AlarmOSP")
local Ustawienia = require(folderWspolny:WaitForChild("Ustawienia"))

local TAG_KOMPUTER = "AlarmKomputer"
local TAG_SYRENA = "AlarmSyrena"
local TAG_SWIATLO = "AlarmSwiatlo"
local TAG_MIEJSCE = "MiejsceZdarzenia"

local Alarmy = {}

local sygnalNowego = Instance.new("BindableEvent")
Alarmy.NowyAlarm = sygnalNowego.Event

local aktywne = {} -- [id] = alarm
local licznikAlarmow = 0
local ostatniAlarm = {} -- [gracz] = os.clock()
local ostatniaAkcja = {} -- [gracz] = os.clock()
local rangaWGrupie = {} -- [gracz] = bool
local koniecSyren = {} -- [syrena] = os.clock() końca
local koniecSwiatel = {} -- [lampa] = os.clock() końca

local remoteZdarzenie
local remoteWyslij

-- ============================================================
--  Pomocnicze
-- ============================================================

local function znajdzRodzaj(nazwa)
	for _, rodzaj in Ustawienia.Rodzaje do
		if rodzaj.Nazwa == nazwa then
			return rodzaj
		end
	end
	return nil
end

local function znajdzJednostke(nazwa)
	for _, jednostka in Ustawienia.Jednostki do
		if jednostka.Nazwa == nazwa then
			return jednostka
		end
	end
	return nil
end

local function czlonek(gracz, jednostka)
	if jednostka.Ranga and gracz:GetAttribute("Ranga") == jednostka.Ranga then
		return true
	end
	if jednostka.Druzyna and gracz.Team and gracz.Team.Name == jednostka.Druzyna then
		return true
	end
	return false
end

function Alarmy.CzyDyspozytor(gracz)
	if RunService:IsStudio() then
		return true
	end
	local dyspozytorzy = Ustawienia.Dyspozytorzy or {}
	if table.find(dyspozytorzy.Gracze or {}, gracz.UserId) then
		return true
	end
	local ranga = gracz:GetAttribute("Ranga")
	if ranga and table.find(dyspozytorzy.Rangi or {}, ranga) then
		return true
	end
	if gracz.Team and table.find(dyspozytorzy.Druzyny or {}, gracz.Team.Name) then
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
			rangaWGrupie[gracz] = ok and wynik >= (Ustawienia.DyspozytorOdRangiWGrupie or 255)
		end
		return rangaWGrupie[gracz]
	end
	return false
end

local function komunikat(gracz, tresc, rodzaj)
	if remoteZdarzenie and gracz.Parent == Players then
		remoteZdarzenie:FireClient(gracz, "Komunikat", tresc, rodzaj or "info")
	end
end

local function pozycjaObiektu(obiekt)
	if obiekt:IsA("PVInstance") then
		return obiekt:GetPivot().Position
	end
	return nil
end

local function miejscaNaMapie()
	local lista = {}
	for _, obiekt in CollectionService:GetTagged(TAG_MIEJSCE) do
		if obiekt:IsA("PVInstance") and obiekt:IsDescendantOf(workspace) then
			table.insert(lista, obiekt)
		end
	end
	table.sort(lista, function(a, b)
		return a.Name < b.Name
	end)
	return lista
end

-- Ucina tekst do max znaków, nie psując polskich liter (UTF-8)
local function przytnij(tekst, maks)
	tekst = string.gsub(tekst, "[%c]", " ")
	tekst = string.gsub(tekst, "^%s+", "")
	tekst = string.gsub(tekst, "%s+$", "")
	local dlugosc = utf8.len(tekst)
	if dlugosc == nil then
		return ""
	end
	if dlugosc <= maks then
		return tekst
	end
	return string.sub(tekst, 1, utf8.offset(tekst, maks + 1) - 1)
end

-- Tekst od gracza widoczny dla innych musi przejść filtr Roblox
local function filtruj(tekst, autor)
	if tekst == "" then
		return ""
	end
	local ok, wynik = pcall(function()
		local filtr = TextService:FilterStringAsync(tekst, autor.UserId, Enum.TextFilterContext.PublicChat)
		return filtr:GetNonChatStringForBroadcastAsync()
	end)
	if ok then
		return wynik
	end
	warn("[AlarmOSP] Filtr tekstu nie zadziałał: " .. tostring(wynik))
	return nil
end

local function podsumowanie(alarm)
	local jada, nieJada = {}, {}
	for userId, odpowiedz in alarm.Odpowiedzi do
		local nazwa = alarm.Nazwy[userId] or "?"
		if odpowiedz == "Jade" then
			table.insert(jada, nazwa)
		else
			table.insert(nieJada, nazwa)
		end
	end
	table.sort(jada)
	table.sort(nieJada)
	return {
		Id = alarm.Id,
		Rodzaj = alarm.Rodzaj,
		Jednostki = alarm.Jednostki,
		Miejsce = alarm.Miejsce,
		Pozycja = alarm.Pozycja,
		Opis = alarm.Opis,
		Autor = alarm.Autor,
		Czas = alarm.Czas,
		Odbiorcow = alarm.LiczbaOdbiorcow,
		Jada = jada,
		NieJada = nieJada,
	}
end

local function listaAktywnych()
	local lista = {}
	for _, alarm in aktywne do
		table.insert(lista, podsumowanie(alarm))
	end
	table.sort(lista, function(a, b)
		return a.Id > b.Id
	end)
	return lista
end

-- Dyspozytorzy dostają aktualną listę alarmów (panel odświeża się sam)
local function wyslijAktywneDyspozytorom()
	local lista = listaAktywnych()
	for _, gracz in Players:GetPlayers() do
		if Alarmy.CzyDyspozytor(gracz) then
			remoteZdarzenie:FireClient(gracz, "Aktywne", lista)
		end
	end
end

local function wyslijOdbiorcom(alarm, akcja)
	local dane = podsumowanie(alarm)
	for userId in alarm.Odbiorcy do
		local gracz = Players:GetPlayerByUserId(userId)
		if gracz then
			remoteZdarzenie:FireClient(gracz, akcja, dane)
		end
	end
end

-- ============================================================
--  Syrena i lampy w remizie
-- ============================================================

local function dotyczy(obiekt, jednostki)
	local jednostka = obiekt:GetAttribute("Jednostka")
	return jednostka == nil or table.find(jednostki, jednostka) ~= nil
end

local function dzwiekSyreny(syrena)
	local dzwiek = syrena:FindFirstChildWhichIsA("Sound", true)
	if dzwiek or Ustawienia.DzwiekSyreny == "" then
		return dzwiek
	end
	-- Dźwięk 3D musi być w części, żeby było go słychać z okolicy
	local nosnik = syrena:IsA("BasePart") and syrena or syrena:FindFirstChildWhichIsA("BasePart", true)
	dzwiek = Instance.new("Sound")
	dzwiek.Name = "DzwiekSyreny"
	dzwiek.SoundId = Ustawienia.DzwiekSyreny
	dzwiek.Volume = Ustawienia.GlosnoscSyreny or 1
	dzwiek.RollOffMaxDistance = Ustawienia.ZasiegSyreny or 450
	dzwiek.Looped = true
	dzwiek.Parent = nosnik or syrena
	return dzwiek
end

local function migaj(lampa)
	local czesci = {}
	local swiatla = {}
	local function dodaj(obiekt)
		if obiekt:IsA("BasePart") then
			table.insert(czesci, { obiekt, obiekt.Material })
		elseif obiekt:IsA("Light") then
			table.insert(swiatla, { obiekt, obiekt.Enabled })
		end
	end
	dodaj(lampa)
	for _, obiekt in lampa:GetDescendants() do
		dodaj(obiekt)
	end

	local zapalona = false
	while koniecSwiatel[lampa] and os.clock() < koniecSwiatel[lampa] and lampa.Parent do
		zapalona = not zapalona
		for _, czesc in czesci do
			czesc[1].Material = zapalona and Enum.Material.Neon or czesc[2]
		end
		for _, swiatlo in swiatla do
			swiatlo[1].Enabled = zapalona
		end
		task.wait(0.35)
	end
	koniecSwiatel[lampa] = nil
	for _, czesc in czesci do
		czesc[1].Material = czesc[2]
	end
	for _, swiatlo in swiatla do
		swiatlo[1].Enabled = swiatlo[2]
	end
end

local function wlaczSyreny(jednostki)
	local koniec = os.clock() + (Ustawienia.CzasSyreny or 40)
	for _, syrena in CollectionService:GetTagged(TAG_SYRENA) do
		if syrena:IsDescendantOf(workspace) and dotyczy(syrena, jednostki) then
			local dzwiek = dzwiekSyreny(syrena)
			if dzwiek then
				dzwiek.Looped = true
				dzwiek.TimePosition = 0
				dzwiek:Play()
			end
			koniecSyren[syrena] = koniec
			task.delay((Ustawienia.CzasSyreny or 40) + 0.1, function()
				if koniecSyren[syrena] and os.clock() >= koniecSyren[syrena] then
					koniecSyren[syrena] = nil
					if dzwiek then
						dzwiek:Stop()
					end
				end
			end)
		end
	end
	for _, lampa in CollectionService:GetTagged(TAG_SWIATLO) do
		if lampa:IsDescendantOf(workspace) and dotyczy(lampa, jednostki) then
			local juzMiga = koniecSwiatel[lampa] ~= nil
			koniecSwiatel[lampa] = koniec
			if not juzMiga then
				task.spawn(migaj, lampa)
			end
		end
	end
end

local function wylaczSyreny()
	for syrena in koniecSyren do
		local dzwiek = syrena:FindFirstChildWhichIsA("Sound", true)
		if dzwiek then
			dzwiek:Stop()
		end
	end
	table.clear(koniecSyren)
	table.clear(koniecSwiatel) -- pętle migania same przywrócą lampy
end

-- ============================================================
--  Alarmy
-- ============================================================

local function zakoncz(id, kto)
	local alarm = aktywne[id]
	if not alarm then
		return
	end
	aktywne[id] = nil
	alarm.Zakonczyl = kto
	wyslijOdbiorcom(alarm, "Koniec")
	wyslijAktywneDyspozytorom()
	if next(aktywne) == nil then
		wylaczSyreny()
	end
end

-- Wysyła alarm. autor: gracz (z panelu) albo nil / tekst (z innego skryptu).
-- Zwraca true/false, komunikat i (gdy się udało) numer alarmu.
function Alarmy.Wyslij(dane, autor)
	local odGracza = typeof(autor) == "Instance" and autor:IsA("Player")

	local rodzaj = znajdzRodzaj(dane.Rodzaj)
	if not rodzaj then
		return false, "Wybierz rodzaj zdarzenia."
	end

	local jednostki = {}
	for _, nazwa in (typeof(dane.Jednostki) == "table" and dane.Jednostki or {}) do
		if typeof(nazwa) == "string" and znajdzJednostke(nazwa) and not table.find(jednostki, nazwa) then
			table.insert(jednostki, nazwa)
		end
	end
	if #jednostki == 0 then
		return false, "Wybierz co najmniej jedną jednostkę."
	end

	-- Miejsce: z panelu tylko punkty z tagiem MiejsceZdarzenia (po nazwie),
	-- ze skryptu dowolna część/model albo Vector3
	local miejsce, pozycja
	local wskazane = dane.Miejsce
	if typeof(wskazane) == "string" and wskazane ~= "" then
		for _, punkt in miejscaNaMapie() do
			if punkt.Name == wskazane then
				miejsce, pozycja = punkt.Name, pozycjaObiektu(punkt)
				break
			end
		end
		if not miejsce then
			return false, "Nie ma takiego miejsca na mapie."
		end
	elseif not odGracza and typeof(wskazane) == "Instance" then
		miejsce = dane.NazwaMiejsca or wskazane.Name
		pozycja = pozycjaObiektu(wskazane)
	elseif not odGracza and typeof(wskazane) == "Vector3" then
		miejsce = dane.NazwaMiejsca or "Wskazane miejsce"
		pozycja = wskazane
	end

	local opis = przytnij(typeof(dane.Opis) == "string" and dane.Opis or "", Ustawienia.MaksDlugoscOpisu or 120)
	if odGracza then
		opis = filtruj(opis, autor)
		if opis == nil then
			return false, "Nie udało się sprawdzić opisu. Spróbuj jeszcze raz."
		end
	end

	licznikAlarmow += 1
	local alarm = {
		Id = licznikAlarmow,
		Rodzaj = rodzaj.Nazwa,
		Jednostki = jednostki,
		Miejsce = miejsce,
		Pozycja = pozycja,
		Opis = opis,
		Autor = odGracza and autor.DisplayName or (typeof(autor) == "string" and autor or "System"),
		Czas = os.time(),
		Odbiorcy = {},
		Nazwy = {},
		Odpowiedzi = {},
		LiczbaOdbiorcow = 0,
	}

	for _, gracz in Players:GetPlayers() do
		for _, nazwa in jednostki do
			if czlonek(gracz, znajdzJednostke(nazwa)) then
				alarm.Odbiorcy[gracz.UserId] = true
				alarm.Nazwy[gracz.UserId] = gracz.DisplayName
				alarm.LiczbaOdbiorcow += 1
				break
			end
		end
	end

	aktywne[alarm.Id] = alarm
	wyslijOdbiorcom(alarm, "Nowy")
	wlaczSyreny(jednostki)
	wyslijAktywneDyspozytorom()
	sygnalNowego:Fire(podsumowanie(alarm))

	task.delay(Ustawienia.CzasAlarmu or 900, function()
		zakoncz(alarm.Id, "czas")
	end)

	if alarm.LiczbaOdbiorcow == 0 then
		return true, "Alarm wysłany, ale nikogo z tych jednostek nie ma teraz w grze.", alarm.Id
	end
	return true, ("Zaalarmowano strażaków: %d."):format(alarm.LiczbaOdbiorcow), alarm.Id
end

function Alarmy.Zakoncz(id)
	zakoncz(id, "skrypt")
end

function Alarmy.Aktywne()
	return listaAktywnych()
end

-- ============================================================
--  Panel dyspozytora
-- ============================================================

local function danePanelu()
	local jednostki = {}
	for _, jednostka in Ustawienia.Jednostki do
		local wGrze = 0
		for _, gracz in Players:GetPlayers() do
			if czlonek(gracz, jednostka) then
				wGrze += 1
			end
		end
		table.insert(jednostki, { Nazwa = jednostka.Nazwa, WGrze = wGrze })
	end
	local miejsca = {}
	for _, punkt in miejscaNaMapie() do
		table.insert(miejsca, punkt.Name)
	end
	return { Jednostki = jednostki, Miejsca = miejsca, Aktywne = listaAktywnych() }
end

local function otworzPanel(gracz)
	if not Alarmy.CzyDyspozytor(gracz) then
		komunikat(gracz, "Panel alarmowy jest tylko dla strażaków i dyspozytorów.", "blad")
		return
	end
	remoteZdarzenie:FireClient(gracz, "Panel", danePanelu())
end

local function przygotujKomputer(obiekt)
	if not (obiekt:IsA("BasePart") or obiekt:IsA("Attachment") or obiekt:IsA("Model")) then
		return
	end
	if obiekt:FindFirstChild("PanelAlarmowy") then
		return
	end
	local zapytanie = Instance.new("ProximityPrompt")
	zapytanie.Name = "PanelAlarmowy"
	zapytanie.ActionText = "Otwórz panel"
	zapytanie.ObjectText = "System alarmowy OSP"
	zapytanie.HoldDuration = 0.3
	zapytanie.MaxActivationDistance = 8
	zapytanie.RequiresLineOfSight = false
	zapytanie.Triggered:Connect(otworzPanel)
	zapytanie.Parent = obiekt
end

local function podlaczKomende()
	local komenda = Ustawienia.Komenda
	if typeof(komenda) ~= "string" or komenda == "" then
		return
	end
	if TextChatService.ChatVersion == Enum.ChatVersion.TextChatService then
		local polecenie = Instance.new("TextChatCommand")
		polecenie.Name = "AlarmOSPPanel"
		polecenie.PrimaryAlias = komenda
		polecenie.Triggered:Connect(function(zrodlo)
			local gracz = Players:GetPlayerByUserId(zrodlo.UserId)
			if gracz then
				otworzPanel(gracz)
			end
		end)
		polecenie.Parent = TextChatService:FindFirstChild("TextChatCommands") or TextChatService
	else
		local function sluchaj(gracz)
			gracz.Chatted:Connect(function(wiadomosc)
				local pierwsze = string.match(wiadomosc, "^%s*(%S+)")
				if pierwsze and string.lower(pierwsze) == string.lower(komenda) then
					otworzPanel(gracz)
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
--  Komunikacja z graczami
-- ============================================================

local function naWyslij(gracz, dane)
	if typeof(dane) ~= "table" then
		return false, "Błędne dane."
	end
	if not Alarmy.CzyDyspozytor(gracz) then
		return false, "Nie masz uprawnień do alarmowania."
	end
	local teraz = os.clock()
	local odstep = Ustawienia.OdstepMiedzyAlarmami or 15
	if ostatniAlarm[gracz] and teraz - ostatniAlarm[gracz] < odstep then
		return false, ("Poczekaj %d s przed kolejnym alarmem."):format(math.ceil(odstep - (teraz - ostatniAlarm[gracz])))
	end

	local jednostki = {}
	if typeof(dane.Jednostki) == "table" then
		for indeks, nazwa in dane.Jednostki do
			if indeks > 20 then
				break
			end
			if typeof(nazwa) == "string" then
				table.insert(jednostki, nazwa)
			end
		end
	end

	local ok, wynik, tresc = pcall(Alarmy.Wyslij, {
		Jednostki = jednostki,
		Rodzaj = typeof(dane.Rodzaj) == "string" and dane.Rodzaj or nil,
		Miejsce = typeof(dane.Miejsce) == "string" and dane.Miejsce or nil,
		Opis = typeof(dane.Opis) == "string" and dane.Opis or "",
	}, gracz)
	if not ok then
		warn("[AlarmOSP] " .. tostring(wynik))
		return false, "Błąd serwera, alarm nie został wysłany."
	end
	if wynik then
		ostatniAlarm[gracz] = teraz
	end
	return wynik, tresc
end

local function naZdarzenie(gracz, akcja, id, wartosc)
	local teraz = os.clock()
	if ostatniaAkcja[gracz] and teraz - ostatniaAkcja[gracz] < 0.2 then
		return
	end
	ostatniaAkcja[gracz] = teraz

	if akcja == "Odpowiedz" then
		local alarm = typeof(id) == "number" and aktywne[id]
		if not alarm or not alarm.Odbiorcy[gracz.UserId] then
			return
		end
		if wartosc ~= "Jade" and wartosc ~= "NieJade" then
			return
		end
		alarm.Odpowiedzi[gracz.UserId] = wartosc
		wyslijOdbiorcom(alarm, "Aktualizacja")
		wyslijAktywneDyspozytorom()
	elseif akcja == "Zakoncz" then
		if typeof(id) == "number" and aktywne[id] and Alarmy.CzyDyspozytor(gracz) then
			zakoncz(id, gracz.DisplayName)
		end
	elseif akcja == "Odswiez" then
		if Alarmy.CzyDyspozytor(gracz) then
			remoteZdarzenie:FireClient(gracz, "Dane", danePanelu())
		end
	end
end

-- ============================================================
--  Start
-- ============================================================

local wystartowano = false

function Alarmy.Start()
	if wystartowano then
		return
	end
	wystartowano = true

	local zdarzenia = folderWspolny:FindFirstChild("Zdarzenia")
	if zdarzenia then
		zdarzenia:Destroy()
	end
	zdarzenia = Instance.new("Folder")
	zdarzenia.Name = "Zdarzenia"
	remoteZdarzenie = Instance.new("RemoteEvent")
	remoteZdarzenie.Name = "Zdarzenie"
	remoteZdarzenie.Parent = zdarzenia
	remoteWyslij = Instance.new("RemoteFunction")
	remoteWyslij.Name = "Wyslij"
	remoteWyslij.Parent = zdarzenia
	zdarzenia.Parent = folderWspolny

	remoteWyslij.OnServerInvoke = naWyslij
	remoteZdarzenie.OnServerEvent:Connect(naZdarzenie)

	for _, obiekt in CollectionService:GetTagged(TAG_KOMPUTER) do
		przygotujKomputer(obiekt)
	end
	CollectionService:GetInstanceAddedSignal(TAG_KOMPUTER):Connect(przygotujKomputer)
	podlaczKomende()

	Players.PlayerRemoving:Connect(function(gracz)
		ostatniAlarm[gracz] = nil
		ostatniaAkcja[gracz] = nil
		rangaWGrupie[gracz] = nil
	end)

	if #CollectionService:GetTagged(TAG_KOMPUTER) == 0 then
		warn("[AlarmOSP] Brak komputera z tagiem " .. TAG_KOMPUTER .. ". Panel otworzysz komendą " .. tostring(Ustawienia.Komenda))
	end
end

return Alarmy
