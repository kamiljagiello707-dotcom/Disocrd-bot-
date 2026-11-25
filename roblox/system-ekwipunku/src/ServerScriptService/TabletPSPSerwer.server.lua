--[[
	TABLET KIEROWNIKA PSP  |  Serwer
	Bramy, syrena, oświetlenie, załoga, pojazdy i komunikaty.
	Każda akcja sprawdza, czy gracz jest kierownikiem.
	Ustawienia: ReplicatedStorage > TabletPSP > Ustawienia
]]

local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local ServerStorage = game:GetService("ServerStorage")
local CollectionService = game:GetService("CollectionService")
local TextService = game:GetService("TextService")
local TweenService = game:GetService("TweenService")
local RunService = game:GetService("RunService")
local Teams = game:GetService("Teams")

local folderWspolny = ReplicatedStorage:WaitForChild("TabletPSP")
local Ustawienia = require(folderWspolny:WaitForChild("Ustawienia"))

local TAG_BRAMA = "BramaPSP"
local TAG_SWIATLO = "SwiatloPSP"
local TAG_SYRENA = "AlarmSyrena"
local TAG_LAMPA = "AlarmSwiatlo"
local TAG_MIEJSCE = "MiejscePojazduPSP"
local TAG_POJAZD = "PojazdPSP"
local FOLDER_POJAZDOW = "PojazdyPSP"

-- ============================================================
--  Komunikacja
-- ============================================================

local stare = folderWspolny:FindFirstChild("Zdarzenia")
if stare then
	stare:Destroy()
end
local zdarzenia = Instance.new("Folder")
zdarzenia.Name = "Zdarzenia"
local remoteZadanie = Instance.new("RemoteFunction")
remoteZadanie.Name = "Zadanie"
remoteZadanie.Parent = zdarzenia
local remotePowiadomienie = Instance.new("RemoteEvent")
remotePowiadomienie.Name = "Powiadomienie"
remotePowiadomienie.Parent = zdarzenia
zdarzenia.Parent = folderWspolny

local function powiadom(gracz, rodzaj, dane)
	if gracz.Parent == Players then
		remotePowiadomienie:FireClient(gracz, rodzaj, dane)
	end
end

-- ============================================================
--  Uprawnienia
-- ============================================================

local rangaWGrupie = {}

-- Kierownik "z rangi" (bez wyjątków dla Studio i właściciela)
local function kierownikZRangi(gracz)
	local kierownicy = Ustawienia.Kierownicy or {}
	if table.find(kierownicy.Gracze or {}, gracz.UserId) then
		return true
	end
	local ranga = gracz:GetAttribute("Ranga")
	if ranga and table.find(kierownicy.Rangi or {}, ranga) then
		return true
	end
	return gracz.Team ~= nil and table.find(kierownicy.Druzyny or {}, gracz.Team.Name) ~= nil
end

local function maDostep(gracz)
	if RunService:IsStudio() or kierownikZRangi(gracz) then
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
			rangaWGrupie[gracz] = ok and wynik >= (Ustawienia.KierownikOdRangiWGrupie or 255)
		end
		return rangaWGrupie[gracz]
	end
	return false
end

local function wZalodze(gracz)
	local ranga = gracz:GetAttribute("Ranga")
	if ranga and table.find(Ustawienia.RangiZalogi or {}, ranga) then
		return true
	end
	return gracz.Team ~= nil and table.find(Ustawienia.DruzynyZalogi or {}, gracz.Team.Name) ~= nil
end

local function korzen(gracz)
	local postac = gracz.Character
	return postac and postac:FindFirstChild("HumanoidRootPart")
end

-- ============================================================
--  Bramy
-- ============================================================

local bramy = {} -- [obiekt] = { Id, Zamknieta, Otwarta, Wartosc, Tween }
local kolejneId = 0

local function przygotujBrame(obiekt)
	if bramy[obiekt] or not obiekt:IsA("PVInstance") then
		return
	end
	kolejneId += 1
	bramy[obiekt] = { Id = kolejneId, Zamknieta = obiekt:GetPivot(), Otwarta = false }
	obiekt:SetAttribute("Otwarta", false)
end

local function przesuniecieBramy(obiekt)
	local wlasne = obiekt:GetAttribute("Przesuniecie")
	if typeof(wlasne) == "Vector3" then
		return wlasne
	end
	local wysokosc = obiekt:IsA("BasePart") and obiekt.Size.Y or obiekt:GetExtentsSize().Y
	return Vector3.new(0, wysokosc * 0.92, 0)
end

local function ustawBrame(obiekt, otworz)
	local dane = bramy[obiekt]
	if not dane or dane.Otwarta == otworz or not obiekt:IsDescendantOf(workspace) then
		return
	end
	dane.Otwarta = otworz
	obiekt:SetAttribute("Otwarta", otworz)

	if not dane.Wartosc then
		dane.Wartosc = Instance.new("CFrameValue")
		dane.Wartosc.Changed:Connect(function(polozenie)
			if obiekt.Parent then
				obiekt:PivotTo(polozenie)
			end
		end)
	end
	if dane.Tween then
		dane.Tween:Cancel()
	end
	dane.Wartosc.Value = obiekt:GetPivot()
	local cel = otworz and (dane.Zamknieta + przesuniecieBramy(obiekt)) or dane.Zamknieta
	local czas = obiekt:GetAttribute("CzasOtwierania") or Ustawienia.CzasOtwieraniaBramy or 3
	dane.Tween = TweenService:Create(
		dane.Wartosc,
		TweenInfo.new(czas, Enum.EasingStyle.Sine, Enum.EasingDirection.InOut),
		{ Value = cel }
	)
	dane.Tween:Play()

	local dzwiek = obiekt:FindFirstChildWhichIsA("Sound", true)
	if not dzwiek and (Ustawienia.DzwiekBramy or "") ~= "" then
		local nosnik = obiekt:IsA("BasePart") and obiekt or obiekt:FindFirstChildWhichIsA("BasePart", true)
		if nosnik then
			dzwiek = Instance.new("Sound")
			dzwiek.Name = "DzwiekBramy"
			dzwiek.SoundId = Ustawienia.DzwiekBramy
			dzwiek.Parent = nosnik
		end
	end
	if dzwiek then
		dzwiek:Play()
	end
end

local function listaBram()
	local lista = {}
	for obiekt, dane in bramy do
		if obiekt:IsDescendantOf(workspace) then
			table.insert(lista, { Id = dane.Id, Nazwa = obiekt.Name, Otwarta = dane.Otwarta })
		end
	end
	table.sort(lista, function(a, b)
		return a.Nazwa < b.Nazwa
	end)
	return lista
end

local function bramaPoId(id)
	for obiekt, dane in bramy do
		if dane.Id == id then
			return obiekt
		end
	end
	return nil
end

-- ============================================================
--  Syrena i lampy ostrzegawcze
-- ============================================================

local syrenaDo = 0

local function syrenaAktywna()
	return os.clock() < syrenaDo
end

local function dzwiekSyreny(syrena)
	local dzwiek = syrena:FindFirstChildWhichIsA("Sound", true)
	if dzwiek or (Ustawienia.DzwiekSyreny or "") == "" then
		return dzwiek
	end
	local nosnik = syrena:IsA("BasePart") and syrena or syrena:FindFirstChildWhichIsA("BasePart", true)
	dzwiek = Instance.new("Sound")
	dzwiek.Name = "DzwiekSyreny"
	dzwiek.SoundId = Ustawienia.DzwiekSyreny
	dzwiek.RollOffMaxDistance = 450
	dzwiek.Looped = true
	dzwiek.Parent = nosnik or syrena
	return dzwiek
end

local function migajLampami()
	local czesci, swiatla = {}, {}
	for _, lampa in CollectionService:GetTagged(TAG_LAMPA) do
		if lampa:IsDescendantOf(workspace) then
			for _, obiekt in { lampa, table.unpack(lampa:GetDescendants()) } do
				if obiekt:IsA("BasePart") then
					table.insert(czesci, { obiekt, obiekt.Material })
				elseif obiekt:IsA("Light") then
					table.insert(swiatla, { obiekt, obiekt.Enabled })
				end
			end
		end
	end
	local zapalona = false
	while syrenaAktywna() do
		zapalona = not zapalona
		for _, czesc in czesci do
			czesc[1].Material = zapalona and Enum.Material.Neon or czesc[2]
		end
		for _, swiatlo in swiatla do
			swiatlo[1].Enabled = zapalona
		end
		task.wait(0.35)
	end
	for _, czesc in czesci do
		czesc[1].Material = czesc[2]
	end
	for _, swiatlo in swiatla do
		swiatlo[1].Enabled = swiatlo[2]
	end
end

local function ustawSyrene(wlacz)
	local czyGrala = syrenaAktywna()
	for _, syrena in CollectionService:GetTagged(TAG_SYRENA) do
		if syrena:IsDescendantOf(workspace) then
			local dzwiek = dzwiekSyreny(syrena)
			if dzwiek then
				if wlacz then
					dzwiek.Looped = true
					dzwiek.TimePosition = 0
					dzwiek:Play()
				else
					dzwiek:Stop()
				end
			end
		end
	end
	if not wlacz then
		syrenaDo = 0
		return
	end
	local czas = Ustawienia.CzasSyreny or 40
	syrenaDo = os.clock() + czas
	if not czyGrala then
		task.spawn(migajLampami)
	end
	local moje = syrenaDo
	task.delay(czas + 0.1, function()
		if syrenaDo == moje then
			ustawSyrene(false)
		end
	end)
end

-- ============================================================
--  Oświetlenie remizy
-- ============================================================

local materialWylaczony = {} -- [część] = materiał, gdy lampa nie świeci

local function nazwaStrefy(lampa)
	local strefa = lampa:GetAttribute("Strefa")
	return typeof(strefa) == "string" and strefa ~= "" and strefa or "Remiza"
end

local function czyLampaSwieci(lampa)
	local wlaczona = lampa:GetAttribute("Wlaczone")
	if wlaczona ~= nil then
		return wlaczona == true
	end
	local swiatlo = lampa:FindFirstChildWhichIsA("Light", true)
	return swiatlo == nil or swiatlo.Enabled
end

local function ustawLampe(lampa, wlacz)
	lampa:SetAttribute("Wlaczone", wlacz)
	for _, obiekt in { lampa, table.unpack(lampa:GetDescendants()) } do
		if obiekt:IsA("BasePart") then
			if materialWylaczony[obiekt] == nil then
				materialWylaczony[obiekt] = obiekt.Material == Enum.Material.Neon and Enum.Material.SmoothPlastic
					or obiekt.Material
			end
			obiekt.Material = wlacz and Enum.Material.Neon or materialWylaczony[obiekt]
		elseif obiekt:IsA("Light") then
			obiekt.Enabled = wlacz
		end
	end
end

local function listaStref()
	local strefy, kolejnosc = {}, {}
	for _, lampa in CollectionService:GetTagged(TAG_SWIATLO) do
		if lampa:IsDescendantOf(workspace) then
			local nazwa = nazwaStrefy(lampa)
			local strefa = strefy[nazwa]
			if not strefa then
				strefa = { Nazwa = nazwa, Ile = 0, Wlaczone = false }
				strefy[nazwa] = strefa
				table.insert(kolejnosc, strefa)
			end
			strefa.Ile += 1
			strefa.Wlaczone = strefa.Wlaczone or czyLampaSwieci(lampa)
		end
	end
	table.sort(kolejnosc, function(a, b)
		return a.Nazwa < b.Nazwa
	end)
	return kolejnosc
end

local function ustawStrefe(nazwa, wlacz)
	for _, lampa in CollectionService:GetTagged(TAG_SWIATLO) do
		if lampa:IsDescendantOf(workspace) and (nazwa == nil or nazwaStrefy(lampa) == nazwa) then
			ustawLampe(lampa, wlacz)
		end
	end
end

-- ============================================================
--  Załoga
-- ============================================================

local function ustawRange(gracz, ranga)
	gracz:SetAttribute("Ranga", ranga)
	local druzyna = Teams:FindFirstChild(ranga)
	if druzyna and druzyna:IsA("Team") then
		gracz.Neutral = false
		gracz.Team = druzyna
	end
end

local function stanZalogi(pytajacy)
	local zaloga, kandydaci = {}, {}
	local mojKorzen = korzen(pytajacy)
	for _, gracz in Players:GetPlayers() do
		if wZalodze(gracz) then
			table.insert(zaloga, {
				UserId = gracz.UserId,
				Nazwa = gracz.DisplayName,
				Ranga = gracz:GetAttribute("Ranga") or (gracz.Team and gracz.Team.Name) or "",
				Kierownik = kierownikZRangi(gracz),
				To = gracz == pytajacy,
			})
		elseif gracz ~= pytajacy and mojKorzen then
			local jegoKorzen = korzen(gracz)
			local dystans = jegoKorzen and (jegoKorzen.Position - mojKorzen.Position).Magnitude
			if dystans and dystans <= (Ustawienia.ZasiegPrzyjecia or 40) then
				table.insert(kandydaci, { UserId = gracz.UserId, Nazwa = gracz.DisplayName, Odleglosc = dystans })
			end
		end
	end
	table.sort(zaloga, function(a, b)
		if a.Kierownik ~= b.Kierownik then
			return a.Kierownik
		end
		return a.Nazwa < b.Nazwa
	end)
	table.sort(kandydaci, function(a, b)
		return a.Odleglosc < b.Odleglosc
	end)
	return zaloga, kandydaci
end

local function przyjmij(kierownik, userId)
	local cel = typeof(userId) == "number" and Players:GetPlayerByUserId(userId)
	if not cel or cel == kierownik then
		return false, "Nie ma takiego gracza."
	end
	if wZalodze(cel) then
		return false, cel.DisplayName .. " już jest w załodze."
	end
	local mojKorzen, jegoKorzen = korzen(kierownik), korzen(cel)
	if not mojKorzen or not jegoKorzen
		or (mojKorzen.Position - jegoKorzen.Position).Magnitude > (Ustawienia.ZasiegPrzyjecia or 40) + 5 then
		return false, cel.DisplayName .. " jest za daleko."
	end
	ustawRange(cel, Ustawienia.RangaPoPrzyjeciu)
	powiadom(cel, "Info", ("%s przyjął Cię do %s."):format(kierownik.DisplayName, Ustawienia.NazwaJednostki))
	return true, ("Przyjęto: %s."):format(cel.DisplayName)
end

local function zwolnij(kierownik, userId)
	local cel = typeof(userId) == "number" and Players:GetPlayerByUserId(userId)
	if not cel or not wZalodze(cel) then
		return false, "Tego gracza nie ma w załodze."
	end
	if cel == kierownik then
		return false, "Nie możesz zwolnić samego siebie."
	end
	if kierownikZRangi(cel) then
		return false, "Kierownika nie można zwolnić z tabletu."
	end
	ustawRange(cel, Ustawienia.RangaPoZwolnieniu)
	powiadom(cel, "Info", ("%s zwolnił Cię z %s."):format(kierownik.DisplayName, Ustawienia.NazwaJednostki))
	return true, ("Zwolniono: %s."):format(cel.DisplayName)
end

-- ============================================================
--  Pojazdy
-- ============================================================

local pojazdyGraczy = {} -- [gracz] = model
local ostatniWyjazd = {}

local function folderPojazdow()
	return ServerStorage:FindFirstChild(FOLDER_POJAZDOW)
end

local function listaPojazdow()
	local lista = {}
	local folder = folderPojazdow()
	if folder then
		for _, model in folder:GetChildren() do
			if model:IsA("Model") then
				table.insert(lista, model.Name)
			end
		end
	end
	table.sort(lista)
	return lista
end

local function wolneMiejsce()
	for _, miejsce in CollectionService:GetTagged(TAG_MIEJSCE) do
		if miejsce:IsA("BasePart") and miejsce:IsDescendantOf(workspace) then
			local zajete = false
			for _, pojazd in CollectionService:GetTagged(TAG_POJAZD) do
				if pojazd:IsDescendantOf(workspace)
					and (pojazd:GetPivot().Position - miejsce.Position).Magnitude < 12 then
					zajete = true
					break
				end
			end
			if not zajete then
				return miejsce
			end
		end
	end
	return nil
end

local function schowajPojazd(gracz)
	local pojazd = pojazdyGraczy[gracz]
	pojazdyGraczy[gracz] = nil
	if pojazd then
		pojazd:Destroy()
		return true
	end
	return false
end

local function wyjedz(gracz, nazwa)
	local teraz = os.clock()
	if ostatniWyjazd[gracz] and teraz - ostatniWyjazd[gracz] < (Ustawienia.OdstepPojazdow or 10) then
		return false, "Poczekaj chwilę przed kolejnym wyjazdem."
	end
	local folder = folderPojazdow()
	local wzor = folder and typeof(nazwa) == "string" and folder:FindFirstChild(nazwa)
	if not wzor or not wzor:IsA("Model") then
		return false, "Nie ma takiego pojazdu."
	end
	schowajPojazd(gracz)
	local miejsce = wolneMiejsce()
	if not miejsce then
		return false, "Wszystkie miejsca przed garażem są zajęte."
	end
	local kopia = wzor:Clone()
	local przod = miejsce.CFrame.LookVector * Vector3.new(1, 0, 1)
	przod = przod.Magnitude > 0.01 and przod.Unit or Vector3.new(0, 0, -1)
	local gora = miejsce.Position + Vector3.new(0, miejsce.Size.Y / 2, 0)
	kopia:PivotTo(CFrame.lookAt(gora, gora + przod))
	local srodek, rozmiar = kopia:GetBoundingBox()
	kopia:PivotTo(kopia:GetPivot() + Vector3.new(0, gora.Y - (srodek.Position.Y - rozmiar.Y / 2) + 0.1, 0))
	kopia:SetAttribute("Wlasciciel", gracz.UserId)
	CollectionService:AddTag(kopia, TAG_POJAZD)
	kopia.Parent = workspace
	pojazdyGraczy[gracz] = kopia
	ostatniWyjazd[gracz] = teraz
	return true, ("%s wyjeżdża z garażu."):format(nazwa)
end

-- ============================================================
--  Komunikaty do załogi
-- ============================================================

local komunikaty = {} -- ostatnie 5
local ostatniKomunikat = {}

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

local function wyslijKomunikat(gracz, tekst)
	if typeof(tekst) ~= "string" then
		return false, "Pusty komunikat."
	end
	local teraz = os.clock()
	local odstep = Ustawienia.OdstepKomunikatow or 20
	if ostatniKomunikat[gracz] and teraz - ostatniKomunikat[gracz] < odstep then
		return false, ("Poczekaj %d s."):format(math.ceil(odstep - (teraz - ostatniKomunikat[gracz])))
	end
	tekst = przytnij(tekst, Ustawienia.MaksDlugoscKomunikatu or 150)
	if tekst == "" then
		return false, "Pusty komunikat."
	end
	-- Tekst od gracza widoczny dla innych musi przejść filtr Roblox
	local ok, przefiltrowany = pcall(function()
		local filtr = TextService:FilterStringAsync(tekst, gracz.UserId, Enum.TextFilterContext.PublicChat)
		return filtr:GetNonChatStringForBroadcastAsync()
	end)
	if not ok then
		return false, "Nie udało się sprawdzić tekstu. Spróbuj jeszcze raz."
	end
	ostatniKomunikat[gracz] = teraz
	local wpis = { Autor = gracz.DisplayName, Tekst = przefiltrowany, Czas = os.time() }
	table.insert(komunikaty, 1, wpis)
	if #komunikaty > 5 then
		table.remove(komunikaty)
	end
	local ile = 0
	for _, odbiorca in Players:GetPlayers() do
		if wZalodze(odbiorca) or odbiorca == gracz then
			powiadom(odbiorca, "Komunikat", wpis)
			ile += 1
		end
	end
	return true, ("Wysłano do %d os."):format(ile)
end

-- ============================================================
--  Stan i akcje
-- ============================================================

local function stan(gracz)
	local zaloga, kandydaci = stanZalogi(gracz)
	local mojPojazd = pojazdyGraczy[gracz]
	if mojPojazd and not mojPojazd.Parent then
		pojazdyGraczy[gracz] = nil
		mojPojazd = nil
	end
	return {
		Bramy = listaBram(),
		Syrena = syrenaAktywna(),
		SyrenaPozostalo = math.max(0, syrenaDo - os.clock()),
		MaSyrene = #CollectionService:GetTagged(TAG_SYRENA) > 0,
		Strefy = listaStref(),
		Zaloga = zaloga,
		Kandydaci = kandydaci,
		Pojazdy = listaPojazdow(),
		MojPojazd = mojPojazd and mojPojazd.Name or nil,
		MiejscPojazdow = #CollectionService:GetTagged(TAG_MIEJSCE),
		Komunikaty = komunikaty,
	}
end

local ostatniaAkcja = {}

remoteZadanie.OnServerInvoke = function(gracz, akcja, a, b)
	if not maDostep(gracz) then
		return false, "Tablet jest tylko dla kierownika PSP.", nil
	end
	local teraz = os.clock()
	if akcja ~= "Stan" then
		if ostatniaAkcja[gracz] and teraz - ostatniaAkcja[gracz] < 0.25 then
			return false, nil, stan(gracz)
		end
		ostatniaAkcja[gracz] = teraz
	end

	local ok, wiadomosc = true, nil
	if akcja == "Brama" then
		local brama = typeof(a) == "number" and bramaPoId(a)
		if brama then
			ustawBrame(brama, b == true)
		end
	elseif akcja == "WszystkieBramy" then
		for brama in bramy do
			ustawBrame(brama, a == true)
		end
		wiadomosc = a == true and "Otwieram wszystkie bramy." or "Zamykam wszystkie bramy."
	elseif akcja == "Syrena" then
		ustawSyrene(a == true)
	elseif akcja == "Strefa" then
		ustawStrefe(typeof(a) == "string" and a or nil, b == true)
	elseif akcja == "Przyjmij" then
		ok, wiadomosc = przyjmij(gracz, a)
	elseif akcja == "Zwolnij" then
		ok, wiadomosc = zwolnij(gracz, a)
	elseif akcja == "Wyjedz" then
		ok, wiadomosc = wyjedz(gracz, a)
	elseif akcja == "Schowaj" then
		ok = schowajPojazd(gracz)
		wiadomosc = ok and "Pojazd wrócił do garażu." or "Nie masz wyprowadzonego pojazdu."
	elseif akcja == "Komunikat" then
		ok, wiadomosc = wyslijKomunikat(gracz, a)
	elseif akcja ~= "Stan" then
		return false, "Nieznana akcja.", stan(gracz)
	end
	return ok, wiadomosc, stan(gracz)
end

-- ============================================================
--  Start
-- ============================================================

for _, obiekt in CollectionService:GetTagged(TAG_BRAMA) do
	przygotujBrame(obiekt)
end
CollectionService:GetInstanceAddedSignal(TAG_BRAMA):Connect(przygotujBrame)

Players.PlayerRemoving:Connect(function(gracz)
	schowajPojazd(gracz)
	ostatniWyjazd[gracz] = nil
	ostatniKomunikat[gracz] = nil
	ostatniaAkcja[gracz] = nil
	rangaWGrupie[gracz] = nil
end)
