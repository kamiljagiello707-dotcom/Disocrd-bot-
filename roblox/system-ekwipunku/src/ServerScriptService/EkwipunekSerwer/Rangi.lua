--[[
	SYSTEM EKWIPUNKU  |  Serwer: rangi i przedmioty służbowe

	Każdy gracz startuje z rangą z Konfiguracja.DomyslnaRanga (Obywatel).
	Gdy dostaje inną rangę (np. OSP), dostaje do EQ wszystkie Toole z folderu:
		ServerStorage > EkwipunekPrzedmioty > OSP
	Gdy traci rangę, te przedmioty znikają. Po śmierci wracają same.

	Jak nadać rangę (na serwerze), wybierz jeden sposób:
		1) require(game.ServerScriptService.EkwipunekSerwer.Rangi).UstawRange(gracz, "OSP")
		2) gracz:SetAttribute("Ranga", "OSP")
		3) przenieś gracza do drużyny (Team) o nazwie "OSP"
		4) komenda admina na czacie:  /ranga <nick> OSP
]]

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local ServerStorage = game:GetService("ServerStorage")
local StarterPack = game:GetService("StarterPack")
local Teams = game:GetService("Teams")
local DataStoreService = game:GetService("DataStoreService")
local RunService = game:GetService("RunService")
local TextChatService = game:GetService("TextChatService")

local folderWspolny = ReplicatedStorage:WaitForChild("Ekwipunek")
local Konfiguracja = require(folderWspolny:WaitForChild("Konfiguracja"))
local Przedmioty = require(folderWspolny:WaitForChild("Przedmioty"))

local ATRYBUT_GRACZA = Przedmioty.ATRYBUT_GRACZA
local ATRYBUT_RANGI = Przedmioty.ATRYBUT_RANGI
local NAZWA_FOLDERU = "EkwipunekPrzedmioty"
local NAZWA_ZAPISU = "EkwipunekRangi_v1"

local Rangi = {}

local sygnalZmiany = Instance.new("BindableEvent")
-- Rangi.ZmienionoRange:Connect(function(gracz, nowaRanga, staraRanga) end)
Rangi.ZmienionoRange = sygnalZmiany.Event

local nadana = {} -- [gracz] = ranga, której przedmioty gracz teraz ma
local wersja = {} -- [gracz] = licznik zmian rangi (chroni przed nakładaniem się zmian)
local gotowy = {} -- [gracz] = true, gdy ranga została wczytana
local wczytanoZapis = {} -- [gracz] = true, gdy odczyt z DataStore się udał
local zapisana = {} -- [gracz] = ranga zapisana w DataStore
local znane = {} -- [gracz] = { [Tool] = true }  przedmioty już sprawdzone pod kątem wagi
local ostatniaAkcja = {} -- [gracz] = os.clock()
local zmianaZDruzyny = {} -- [gracz] = true, gdy rangę zmieniła sama drużyna (nie ruszamy wtedy Team)

local remotePowiadomienie
local magazyn

-- ============================================================
--  Pomocnicze
-- ============================================================

local function powiadom(gracz, tresc, rodzaj)
	if remotePowiadomienie and gracz.Parent == Players then
		remotePowiadomienie:FireClient(gracz, tresc, rodzaj or "info")
	end
end

local function listaRang()
	local lista = {}
	for nazwa in Konfiguracja.Rangi do
		table.insert(lista, nazwa)
	end
	table.sort(lista)
	return table.concat(lista, ", ")
end

local function zbiorZnanych(gracz)
	local zbior = znane[gracz]
	if not zbior then
		zbior = setmetatable({}, { __mode = "k" })
		znane[gracz] = zbior
	end
	return zbior
end

local function folderPrzedmiotow(ranga)
	local glowny = ServerStorage:FindFirstChild(NAZWA_FOLDERU)
	return glowny and glowny:FindFirstChild(ranga)
end

local function maPrzedmiot(gracz, narzedzie)
	local rodzic = narzedzie.Parent
	if rodzic == nil then
		return false
	end
	return rodzic == gracz:FindFirstChildOfClass("Backpack") or rodzic == gracz.Character
end

-- ============================================================
--  API
-- ============================================================

-- Zwraca poprawną nazwę rangi (bez względu na wielkość liter) albo nil
function Rangi.ZnajdzRange(nazwa)
	if typeof(nazwa) ~= "string" then
		return nil
	end
	if Konfiguracja.Rangi[nazwa] then
		return nazwa
	end
	local mala = string.lower(nazwa)
	for klucz in Konfiguracja.Rangi do
		if string.lower(klucz) == mala then
			return klucz
		end
	end
	return nil
end

function Rangi.PobierzRange(gracz)
	return Przedmioty.Ranga(gracz)
end

-- Ustawia rangę graczowi. Zwraca true albo false + powód.
function Rangi.UstawRange(gracz, ranga)
	if typeof(gracz) ~= "Instance" or not gracz:IsA("Player") then
		return false, "To nie jest gracz"
	end
	local nazwa = Rangi.ZnajdzRange(ranga)
	if not nazwa then
		return false, "Nie ma rangi: " .. tostring(ranga)
	end
	gracz:SetAttribute(ATRYBUT_GRACZA, nazwa)
	return true
end

-- ============================================================
--  Zapis rangi (DataStore)
-- ============================================================

local function kluczZapisu(gracz)
	return "gracz_" .. gracz.UserId
end

local function przygotujMagazyn()
	if not Konfiguracja.ZapisujRange then
		return
	end
	local ok, wynik = pcall(function()
		return DataStoreService:GetDataStore(NAZWA_ZAPISU)
	end)
	if ok then
		magazyn = wynik
	else
		warn("[Ekwipunek] Zapis rang wyłączony, DataStore niedostępny: " .. tostring(wynik))
	end
end

local function wczytajRange(gracz)
	if not magazyn then
		return nil
	end
	local proby = RunService:IsStudio() and 1 or 3
	for proba = 1, proby do
		local ok, wynik = pcall(function()
			return magazyn:GetAsync(kluczZapisu(gracz))
		end)
		if ok then
			wczytanoZapis[gracz] = true
			zapisana[gracz] = wynik
			return wynik
		end
		warn(("[Ekwipunek] Nie udało się wczytać rangi %s (%d/%d): %s"):format(gracz.Name, proba, proby, tostring(wynik)))
		if proba < proby then
			task.wait(2)
		end
		if gracz.Parent ~= Players then
			return nil
		end
	end
	return nil
end

local function zapiszRange(gracz, ranga)
	-- Nie zapisujemy, gdy odczyt się nie udał: nie nadpiszemy prawdziwej rangi
	if not magazyn or not wczytanoZapis[gracz] or zapisana[gracz] == ranga then
		return
	end
	zapisana[gracz] = ranga
	task.spawn(function()
		local ok, blad = pcall(function()
			magazyn:SetAsync(kluczZapisu(gracz), ranga, { gracz.UserId })
		end)
		if not ok then
			zapisana[gracz] = nil
			warn(("[Ekwipunek] Nie udało się zapisać rangi %s: %s"):format(gracz.Name, tostring(blad)))
		end
	end)
end

-- ============================================================
--  Przedmioty służbowe
-- ============================================================

local function usunSluzbowe(gracz)
	for _, narzedzie in Przedmioty.Narzedzia(gracz) do
		if narzedzie:GetAttribute(ATRYBUT_RANGI) ~= nil then
			narzedzie:Destroy()
		end
	end
end

local function zabierzSluzbowe(gracz)
	-- Najpierw odkładamy przedmiot z ręki, żeby jego skrypty zdążyły
	-- przywrócić ręce postaci (inaczej zostałyby wykrzywione)
	local postac = gracz.Character
	local humanoid = postac and postac:FindFirstChildOfClass("Humanoid")
	local trzymany = postac and postac:FindFirstChildOfClass("Tool")
	if humanoid and trzymany and trzymany:GetAttribute(ATRYBUT_RANGI) ~= nil then
		humanoid:UnequipTools()
		task.wait(0.3)
	end
	usunSluzbowe(gracz)
end

local function dajSluzbowe(gracz, ranga)
	local folder = folderPrzedmiotow(ranga)
	local plecak = gracz:FindFirstChildOfClass("Backpack")
	if not folder or not plecak then
		return 0
	end
	local zbior = zbiorZnanych(gracz)
	local ile = 0
	for _, wzor in folder:GetChildren() do
		if wzor:IsA("Tool") then
			local kopia = wzor:Clone()
			if kopia then
				kopia:SetAttribute(ATRYBUT_RANGI, ranga)
				if not Konfiguracja.MoznaWyrzucacSluzbowe then
					kopia.CanBeDropped = false
				end
				zbior[kopia] = true
				kopia.Parent = plecak
				ile += 1
			else
				warn("[Ekwipunek] Nie da się skopiować " .. wzor:GetFullName() .. " (Archivable = false?)")
			end
		end
	end
	return ile
end

-- ============================================================
--  Wyrzucanie i limit wagi
-- ============================================================

local function wyrzucNaZiemie(gracz, narzedzie)
	local postac = gracz.Character
	local korzen = postac and postac:FindFirstChild("HumanoidRootPart")
	local uchwyt = narzedzie:FindFirstChild("Handle")
	if not korzen or not uchwyt or not uchwyt:IsA("BasePart") then
		return false
	end

	local humanoid = postac:FindFirstChildOfClass("Humanoid")
	if humanoid and narzedzie.Parent == postac then
		humanoid:UnequipTools()
	end

	zbiorZnanych(gracz)[narzedzie] = nil

	-- Przez chwilę przedmiotu nie da się podnieść, żeby nie wrócił od razu do gracza
	local moznaDotknac = uchwyt.CanTouch
	uchwyt.CanTouch = false
	narzedzie.Parent = workspace
	narzedzie:PivotTo(korzen.CFrame * CFrame.new(0, 0.5, -4))
	task.delay(2, function()
		if uchwyt.Parent then
			uchwyt.CanTouch = moznaDotknac
		end
	end)
	return true
end

local function sprawdzNowy(gracz, narzedzie)
	if not narzedzie:IsA("Tool") then
		return
	end
	local zbior = zbiorZnanych(gracz)
	if zbior[narzedzie] or not maPrzedmiot(gracz, narzedzie) then
		return
	end
	zbior[narzedzie] = true

	if not Konfiguracja.BlokujPrzeciazenie then
		return
	end
	-- Przedmioty służbowe i startowe (StarterPack) zawsze się mieszczą
	if narzedzie:GetAttribute(ATRYBUT_RANGI) ~= nil or StarterPack:FindFirstChild(narzedzie.Name) then
		return
	end

	local waga = Przedmioty.LacznaWaga(gracz)
	if waga > Konfiguracja.MaksymalnaWaga + 0.001 then
		local nazwa = Przedmioty.Dane(narzedzie).Nazwa
		if wyrzucNaZiemie(gracz, narzedzie) then
			powiadom(
				gracz,
				("Za ciężko! %s wypada Ci z rąk (%.1f / %g kg)"):format(nazwa, waga, Konfiguracja.MaksymalnaWaga),
				"blad"
			)
		end
	end
end

local function obserwujKontener(gracz, kontener)
	kontener.ChildAdded:Connect(function(obiekt)
		sprawdzNowy(gracz, obiekt)
	end)
end

local function naAkcje(gracz, akcja, narzedzie)
	local teraz = os.clock()
	if ostatniaAkcja[gracz] and teraz - ostatniaAkcja[gracz] < 0.3 then
		return
	end
	ostatniaAkcja[gracz] = teraz

	if akcja ~= "Wyrzuc" then
		return
	end
	if typeof(narzedzie) ~= "Instance" or not narzedzie:IsA("Tool") or not maPrzedmiot(gracz, narzedzie) then
		return
	end

	local dane = Przedmioty.Dane(narzedzie)
	if dane.Ranga ~= nil and not Konfiguracja.MoznaWyrzucacSluzbowe then
		powiadom(gracz, "To przedmiot służbowy, nie możesz go wyrzucić.", "blad")
		return
	end
	if narzedzie:GetAttribute("MozeWyrzucic") == false then
		powiadom(gracz, "Tego przedmiotu nie można wyrzucić.", "blad")
		return
	end

	if wyrzucNaZiemie(gracz, narzedzie) then
		powiadom(gracz, "Wyrzucono: " .. dane.Nazwa, "info")
	else
		powiadom(gracz, "Tego przedmiotu nie da się wyrzucić.", "blad")
	end
end

-- ============================================================
--  Drużyny (Teams)
-- ============================================================

local function druzynaRangi(ranga)
	local druzyna = Teams:FindFirstChild(ranga)
	if druzyna and druzyna:IsA("Team") then
		return druzyna
	end
	return nil
end

local function ustawDruzyne(gracz, ranga)
	if not Konfiguracja.PolaczZDruzynami then
		return
	end
	local druzyna = druzynaRangi(ranga)
	if druzyna then
		if gracz.Team ~= druzyna then
			gracz.Neutral = false
			gracz.Team = druzyna
		end
	elseif gracz.Team and Rangi.ZnajdzRange(gracz.Team.Name) then
		-- Gracz stoi w drużynie innej rangi, a nowa ranga nie ma drużyny
		gracz.Team = nil
		gracz.Neutral = true
	end
end

local function poZmianieDruzyny(gracz)
	if not Konfiguracja.PolaczZDruzynami or not gotowy[gracz] then
		return
	end
	local obecna = Przedmioty.Ranga(gracz)
	local druzyna = gracz.Team
	local ranga = druzyna and Rangi.ZnajdzRange(druzyna.Name)
	if not ranga then
		-- Wyszedł z drużyny swojej rangi do drużyny bez rangi: wraca do domyślnej
		if druzynaRangi(obecna) then
			ranga = Konfiguracja.DomyslnaRanga
		else
			return
		end
	end
	if ranga ~= obecna then
		zmianaZDruzyny[gracz] = true
		gracz:SetAttribute(ATRYBUT_GRACZA, ranga)
	end
end

-- ============================================================
--  Zmiana rangi
-- ============================================================

local function zastosujRange(gracz)
	if not gotowy[gracz] or gracz.Parent ~= Players then
		return
	end

	local surowa = gracz:GetAttribute(ATRYBUT_GRACZA)
	local ranga = Rangi.ZnajdzRange(surowa)
	if not ranga then
		warn(("[Ekwipunek] Nieznana ranga '%s' u %s, ustawiam domyślną"):format(tostring(surowa), gracz.Name))
		gracz:SetAttribute(ATRYBUT_GRACZA, Konfiguracja.DomyslnaRanga)
		return
	end
	if ranga ~= surowa then
		gracz:SetAttribute(ATRYBUT_GRACZA, ranga) -- poprawiamy wielkość liter
		return
	end

	local stara = nadana[gracz]
	if stara == ranga then
		return
	end
	nadana[gracz] = ranga
	wersja[gracz] = (wersja[gracz] or 0) + 1
	local mojaWersja = wersja[gracz]

	-- Gdy gracz sam zmienił drużynę (np. na taką spoza listy rang), zostawiamy go w niej
	if zmianaZDruzyny[gracz] then
		zmianaZDruzyny[gracz] = nil
	else
		ustawDruzyne(gracz, ranga)
	end
	zapiszRange(gracz, ranga)

	zabierzSluzbowe(gracz)
	if wersja[gracz] ~= mojaWersja or gracz.Parent ~= Players then
		return -- w międzyczasie przyszła nowsza zmiana
	end
	local ile = 0
	if gracz.Character then
		ile = dajSluzbowe(gracz, ranga)
	end

	local klawisz = Konfiguracja.Klawisz.Name
	if stara == nil then
		if ranga ~= Konfiguracja.DomyslnaRanga then
			powiadom(gracz, ("Twoja ranga: %s. Wyposażenie czeka w EQ [%s]."):format(ranga, klawisz), "sukces")
		end
	elseif ile > 0 then
		powiadom(gracz, ("Nowa ranga: %s. Dostajesz %d przedmiotów, otwórz EQ [%s]."):format(ranga, ile, klawisz), "sukces")
	else
		powiadom(gracz, ("Nowa ranga: %s."):format(ranga), "info")
	end

	sygnalZmiany:Fire(gracz, ranga, stara)
end

local function poOdrodzeniu(gracz, postac)
	obserwujKontener(gracz, postac)
	task.wait(0.1)
	if gracz.Character ~= postac or not gotowy[gracz] then
		return
	end
	local ranga = nadana[gracz]
	if ranga then
		-- Stare przedmioty służbowe przepadły po śmierci, dajemy nowe
		usunSluzbowe(gracz)
		dajSluzbowe(gracz, ranga)
	end
end

-- ============================================================
--  Komenda admina: /ranga <nick> <ranga>
-- ============================================================

local function czyAdmin(gracz)
	if RunService:IsStudio() then
		return true
	end
	if table.find(Konfiguracja.Admini, gracz.UserId) then
		return true
	end
	if game.CreatorType == Enum.CreatorType.User then
		return gracz.UserId == game.CreatorId
	end
	if game.CreatorType == Enum.CreatorType.Group then
		local ok, rangaWGrupie = pcall(function()
			return gracz:GetRankInGroup(game.CreatorId)
		end)
		return ok and rangaWGrupie >= (Konfiguracja.AdminOdRangiWGrupie or 255)
	end
	return false
end

local function znajdzGracza(tekst, autor)
	local maly = string.lower(tekst)
	if maly == "me" or maly == "ja" then
		return autor
	end
	for _, kandydat in Players:GetPlayers() do
		if string.lower(kandydat.Name) == maly then
			return kandydat
		end
	end
	local znaleziony
	for _, kandydat in Players:GetPlayers() do
		local nick = string.lower(kandydat.Name)
		local wyswietlany = string.lower(kandydat.DisplayName)
		if string.sub(nick, 1, #maly) == maly or string.sub(wyswietlany, 1, #maly) == maly then
			if znaleziony then
				return nil, "Pasuje kilku graczy, wpisz dłuższy nick."
			end
			znaleziony = kandydat
		end
	end
	return znaleziony
end

local function komendaRangi(autor, tekst)
	if not czyAdmin(autor) then
		powiadom(autor, "Nie masz uprawnień do nadawania rang.", "blad")
		return
	end
	local slowa = {}
	for slowo in string.gmatch(tekst, "%S+") do
		table.insert(slowa, slowo)
	end
	if #slowa < 3 then
		powiadom(autor, "Użycie: /ranga <nick> <ranga>. Rangi: " .. listaRang(), "info")
		return
	end
	local cel, blad = znajdzGracza(slowa[2], autor)
	if not cel then
		powiadom(autor, blad or ("Nie znaleziono gracza: " .. slowa[2]), "blad")
		return
	end
	local ranga = Rangi.ZnajdzRange(table.concat(slowa, " ", 3))
	if not ranga then
		powiadom(autor, "Nie ma takiej rangi. Rangi: " .. listaRang(), "blad")
		return
	end
	Rangi.UstawRange(cel, ranga)
	powiadom(autor, ("Ranga %s dla %s."):format(ranga, cel.DisplayName), "sukces")
end

local function podlaczKomende()
	if TextChatService.ChatVersion == Enum.ChatVersion.TextChatService then
		local komenda = Instance.new("TextChatCommand")
		komenda.Name = "EkwipunekRanga"
		komenda.PrimaryAlias = "/ranga"
		komenda.Triggered:Connect(function(zrodlo, tekst)
			local autor = Players:GetPlayerByUserId(zrodlo.UserId)
			if autor then
				komendaRangi(autor, tekst)
			end
		end)
		komenda.Parent = TextChatService:FindFirstChild("TextChatCommands") or TextChatService
	else
		local function sluchajCzatu(gracz)
			gracz.Chatted:Connect(function(wiadomosc)
				local pierwsze = string.match(wiadomosc, "^%s*(%S+)")
				if pierwsze and string.lower(pierwsze) == "/ranga" then
					komendaRangi(gracz, wiadomosc)
				end
			end)
		end
		Players.PlayerAdded:Connect(sluchajCzatu)
		for _, gracz in Players:GetPlayers() do
			sluchajCzatu(gracz)
		end
	end
end

-- ============================================================
--  Gracze
-- ============================================================

local function dolaczyl(gracz)
	-- Pilnowanie wagi: plecak (tworzony od nowa po śmierci) i postać
	local plecak = gracz:FindFirstChildOfClass("Backpack")
	if plecak then
		obserwujKontener(gracz, plecak)
	end
	gracz.ChildAdded:Connect(function(obiekt)
		if obiekt:IsA("Backpack") then
			obserwujKontener(gracz, obiekt)
		end
	end)
	gracz.CharacterAdded:Connect(function(postac)
		poOdrodzeniu(gracz, postac)
	end)
	if gracz.Character then
		obserwujKontener(gracz, gracz.Character)
	end

	-- Wczytanie rangi
	local zapis = wczytajRange(gracz)
	if gracz.Parent ~= Players then
		return
	end
	local ranga = Rangi.ZnajdzRange(zapis)
		or Rangi.ZnajdzRange(gracz:GetAttribute(ATRYBUT_GRACZA))
		or Konfiguracja.DomyslnaRanga

	gotowy[gracz] = true
	gracz:GetAttributeChangedSignal(ATRYBUT_GRACZA):Connect(function()
		zastosujRange(gracz)
	end)
	gracz:GetPropertyChangedSignal("Team"):Connect(function()
		poZmianieDruzyny(gracz)
	end)
	gracz:SetAttribute(ATRYBUT_GRACZA, ranga)
	zastosujRange(gracz)
end

local function wyszedl(gracz)
	nadana[gracz] = nil
	wersja[gracz] = nil
	gotowy[gracz] = nil
	wczytanoZapis[gracz] = nil
	zapisana[gracz] = nil
	znane[gracz] = nil
	ostatniaAkcja[gracz] = nil
	zmianaZDruzyny[gracz] = nil
end

-- ============================================================
--  Start
-- ============================================================

local wystartowano = false

function Rangi.Start()
	if wystartowano then
		return
	end
	wystartowano = true

	if not Konfiguracja.Rangi[Konfiguracja.DomyslnaRanga] then
		warn("[Ekwipunek] DomyslnaRanga '" .. tostring(Konfiguracja.DomyslnaRanga) .. "' nie jest na liście Rangi!")
	end
	if not ServerStorage:FindFirstChild(NAZWA_FOLDERU) then
		warn("[Ekwipunek] Brak folderu ServerStorage." .. NAZWA_FOLDERU .. ", rangi nie dadzą żadnych przedmiotów.")
	end

	local zdarzenia = folderWspolny:FindFirstChild("Zdarzenia")
	if zdarzenia then
		zdarzenia:Destroy()
	end
	zdarzenia = Instance.new("Folder")
	zdarzenia.Name = "Zdarzenia"

	local remoteAkcja = Instance.new("RemoteEvent")
	remoteAkcja.Name = "Akcja"
	remoteAkcja.Parent = zdarzenia

	remotePowiadomienie = Instance.new("RemoteEvent")
	remotePowiadomienie.Name = "Powiadomienie"
	remotePowiadomienie.Parent = zdarzenia

	zdarzenia.Parent = folderWspolny

	przygotujMagazyn()
	remoteAkcja.OnServerEvent:Connect(naAkcje)
	podlaczKomende()

	Players.PlayerAdded:Connect(dolaczyl)
	Players.PlayerRemoving:Connect(wyszedl)
	for _, gracz in Players:GetPlayers() do
		task.spawn(dolaczyl, gracz)
	end
end

return Rangi
