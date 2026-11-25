--[[
	TABLET KIEROWNIKA PSP  |  Ekran tabletu
	Obudowa z paskiem statusu, Strona Główna z logo i kafelkami
	oraz aplikacje: Bramy, Syrena, Załoga, Pojazdy, Komunikaty, Oświetlenie.
]]

local UserInputService = game:GetService("UserInputService")
local Players = game:GetService("Players")

local W = require(script.Parent:WaitForChild("Wyglad"))
local Ikony = require(script.Parent:WaitForChild("Ikony"))
local K = W.KOLOR

local gracz = Players.LocalPlayer

local Tablet = {}

local SZEROKOSC, WYSOKOSC = 900, 600
local IKONY_APLIKACJI = {
	Bramy = "brama",
	Syrena = "syrena",
	Zaloga = "zaloga",
	Pojazdy = "pojazd",
	Komunikaty = "komunikat",
	Oswietlenie = "swiatlo",
}
local SZARY = Color3.fromRGB(70, 70, 78)
local ZOLTY = Color3.fromRGB(245, 200, 75)

local ctx, U, AKCENT
local przyciemnienie, obudowa, skala, ekranTabletu
local widokStart, widokAplikacji, tytulAplikacji, ikonaAplikacji, trescAplikacji
local napisGodziny, napisStanu
local kafelki = {}
local stan = { otwarte = false, aplikacja = nil, dane = nil, odswiez = nil, sesja = 0, petla = nil }
local bazowaSkala = 1

-- ============================================================
--  Pomocnicze
-- ============================================================

local function szklo(wlasciwosci)
	local pelne = {
		BackgroundColor3 = Color3.new(1, 1, 1),
		BackgroundTransparency = 0.955,
	}
	for klucz, wartosc in wlasciwosci do
		pelne[klucz] = wartosc
	end
	local ramka = W.nowy("Frame", pelne)
	W.zaokraglij(ramka, 16)
	W.obrysuj(ramka, 0.9)
	return ramka
end

local function ramkaIkony(rodzic, nazwa, x, y, rozmiar, kolorTla, kolorIkony, przezroczystosc)
	local pole = W.nowy("Frame", {
		Position = UDim2.fromOffset(x, y),
		Size = UDim2.fromOffset(rozmiar, rozmiar),
		BackgroundColor3 = kolorTla or AKCENT,
		BackgroundTransparency = przezroczystosc or 0.8,
		Parent = rodzic,
	})
	W.zaokraglij(pole, math.floor(rozmiar * 0.3))
	Ikony.rysuj(nazwa, pole, kolorIkony or K.Bialy, math.floor(rozmiar * 0.58))
	return pole
end

local function przycisk(tekst, glowny, wlasciwosci)
	local pelne = {
		Text = tekst,
		TextSize = 13,
		TextColor3 = glowny and Color3.new(1, 1, 1) or K.Tekst,
		BackgroundColor3 = glowny and AKCENT or Color3.fromRGB(48, 48, 54),
	}
	for klucz, wartosc in wlasciwosci do
		pelne[klucz] = wartosc
	end
	local guzik = W.przycisk(pelne)
	W.zaokraglij(guzik, 10)
	if not glowny then
		W.obrysuj(guzik, 0.9)
	end
	W.najechanie(guzik)
	return guzik
end

local function przelacznik(rodzic, wlaczony, poZmianie, pozycja)
	local tor = W.przycisk({
		Text = "",
		AnchorPoint = pozycja and Vector2.new(1, 0) or Vector2.new(1, 0.5),
		Position = pozycja or UDim2.new(1, -16, 0.5, 0),
		Size = UDim2.fromOffset(46, 26),
		BackgroundColor3 = wlaczony and K.Zielony or SZARY,
		Parent = rodzic,
	})
	W.zaokraglij(tor, 13)
	local galka = W.nowy("Frame", {
		Position = UDim2.fromOffset(wlaczony and 22 or 2, 2),
		Size = UDim2.fromOffset(22, 22),
		BackgroundColor3 = Color3.new(1, 1, 1),
		Parent = tor,
	})
	W.zaokraglij(galka, 11)
	tor.Activated:Connect(function()
		wlaczony = not wlaczony
		W.animuj(tor, { BackgroundColor3 = wlaczony and K.Zielony or SZARY })
		W.animuj(galka, { Position = UDim2.fromOffset(wlaczony and 22 or 2, 2) }, TweenInfo.new(0.15))
		poZmianie(wlaczony)
	end)
	return tor
end

local function etykieta(rodzic, tekst, x, y, szerokosc)
	return W.napis({
		Text = tekst,
		FontFace = W.czcionka(Enum.FontWeight.Bold),
		TextSize = 11,
		TextColor3 = K.TekstSlaby,
		Position = UDim2.fromOffset(x, y),
		Size = UDim2.fromOffset(szerokosc, 16),
		Parent = rodzic,
	})
end

local function przewijanie(rodzic, x, y, szerokosc, wysokosc, komorka)
	local ramka = W.nowy("ScrollingFrame", {
		BackgroundTransparency = 1,
		BorderSizePixel = 0,
		Position = UDim2.fromOffset(x, y),
		Size = UDim2.fromOffset(szerokosc, wysokosc),
		CanvasSize = UDim2.new(),
		AutomaticCanvasSize = Enum.AutomaticSize.Y,
		ScrollingDirection = Enum.ScrollingDirection.Y,
		ScrollBarThickness = 3,
		ScrollBarImageColor3 = K.Bialy,
		ScrollBarImageTransparency = 0.7,
		Parent = rodzic,
	})
	W.nowy("UIPadding", { PaddingTop = UDim.new(0, 1), PaddingLeft = UDim.new(0, 1), Parent = ramka })
	if komorka then
		W.nowy("UIGridLayout", {
			CellSize = komorka,
			CellPadding = UDim2.fromOffset(16, 16),
			SortOrder = Enum.SortOrder.LayoutOrder,
			Parent = ramka,
		})
	else
		W.nowy("UIListLayout", { Padding = UDim.new(0, 8), SortOrder = Enum.SortOrder.LayoutOrder, Parent = ramka })
	end
	return ramka
end

local function wyczysc(ramka)
	for _, dziecko in ramka:GetChildren() do
		if dziecko:IsA("GuiObject") then
			dziecko:Destroy()
		end
	end
end

local function pustyStan(rodzic, tytul, opis, y)
	local ramka = W.nowy("Frame", {
		BackgroundTransparency = 1,
		Position = UDim2.fromOffset(0, y or 120),
		Size = UDim2.new(1, 0, 0, 60),
		Visible = false,
		Parent = rodzic,
	})
	local naglowek = W.napis({
		Text = tytul,
		FontFace = W.czcionka(Enum.FontWeight.Bold),
		TextSize = 16,
		TextXAlignment = Enum.TextXAlignment.Center,
		Size = UDim2.new(1, 0, 0, 22),
		Parent = ramka,
	})
	local tresc = W.napis({
		Text = opis,
		TextSize = 13,
		TextColor3 = K.TekstSlaby,
		TextXAlignment = Enum.TextXAlignment.Center,
		TextWrapped = true,
		Position = UDim2.fromOffset(0, 26),
		Size = UDim2.new(1, 0, 0, 34),
		Parent = ramka,
	})
	return ramka, naglowek, tresc
end

local function awatar(rodzic, userId, x, y, rozmiar)
	local obraz = W.nowy("ImageLabel", {
		Position = UDim2.fromOffset(x, y),
		Size = UDim2.fromOffset(rozmiar, rozmiar),
		BackgroundColor3 = Color3.fromRGB(48, 48, 54),
		Image = ("rbxthumb://type=AvatarHeadShot&id=%d&w=48&h=48"):format(userId),
		Parent = rodzic,
	})
	W.zaokraglij(obraz, math.floor(rozmiar / 2))
	return obraz
end

local function pigulka(rodzic, tekst, kolor, x, y)
	local ramka = W.nowy("Frame", {
		Position = UDim2.fromOffset(x, y),
		Size = UDim2.fromOffset(0, 18),
		AutomaticSize = Enum.AutomaticSize.X,
		BackgroundColor3 = kolor,
		BackgroundTransparency = 0.82,
		Parent = rodzic,
	})
	W.zaokraglij(ramka, 9)
	W.nowy("UIPadding", { PaddingLeft = UDim.new(0, 8), PaddingRight = UDim.new(0, 8), Parent = ramka })
	W.napis({
		Text = tekst,
		FontFace = W.czcionka(Enum.FontWeight.Bold),
		TextSize = 10,
		TextColor3 = kolor,
		AutomaticSize = Enum.AutomaticSize.X,
		Size = UDim2.fromScale(0, 1),
		Parent = ramka,
	})
	return ramka
end

-- ============================================================
--  Dane i akcje
-- ============================================================

local function podtytulKafelka(id, dane)
	if not dane then
		return "Ładowanie..."
	end
	if id == "Bramy" then
		local otwarte = 0
		for _, brama in dane.Bramy do
			if brama.Otwarta then
				otwarte += 1
			end
		end
		return #dane.Bramy == 0 and "Brak bram" or ("Otwarte: %d z %d"):format(otwarte, #dane.Bramy)
	elseif id == "Syrena" then
		if not dane.MaSyrene then
			return "Brak syreny"
		end
		return dane.Syrena and "Syrena wyje" or "Gotowa"
	elseif id == "Zaloga" then
		return ("W służbie: %d"):format(#dane.Zaloga)
	elseif id == "Pojazdy" then
		if dane.MojPojazd then
			return "Na zewnątrz: " .. dane.MojPojazd
		end
		return ("W garażu: %d"):format(#dane.Pojazdy)
	elseif id == "Komunikaty" then
		return "Wiadomość do załogi"
	elseif id == "Oswietlenie" then
		local wlaczone = 0
		for _, strefa in dane.Strefy do
			if strefa.Wlaczone then
				wlaczone += 1
			end
		end
		return #dane.Strefy == 0 and "Brak lamp" or ("Włączone strefy: %d z %d"):format(wlaczone, #dane.Strefy)
	end
	return ""
end

local function zastosujDane(dane)
	if typeof(dane) ~= "table" then
		return
	end
	stan.dane = dane
	for id, kafelek in kafelki do
		kafelek.Podtytul.Text = podtytulKafelka(id, dane)
	end
	napisStanu.Text = ("System online  •  W służbie: %d"):format(#dane.Zaloga)
	if stan.odswiez then
		stan.odswiez(dane)
	end
end

local function akcja(nazwa, a, b)
	local ok, wiadomosc, dane = ctx.Zadanie(nazwa, a, b)
	if dane then
		zastosujDane(dane)
	end
	if wiadomosc then
		ctx.Powiadom(wiadomosc, ok and "sukces" or "blad")
	end
	return ok
end

-- ============================================================
--  Aplikacje
--  Każda buduje się raz i zwraca funkcję odświeżającą dane.
-- ============================================================

local APLIKACJE = {}

APLIKACJE.Bramy = function(tresc)
	W.napis({
		Text = "Bramy garażowe remizy",
		TextSize = 13,
		TextColor3 = K.TekstSlaby,
		Size = UDim2.fromOffset(400, 36),
		Parent = tresc,
	})
	local zamknij = przycisk("Zamknij wszystkie", false, {
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.fromScale(1, 0),
		Size = UDim2.fromOffset(170, 36),
		Parent = tresc,
	})
	local otworz = przycisk("Otwórz wszystkie", true, {
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.new(1, -180, 0, 0),
		Size = UDim2.fromOffset(170, 36),
		Parent = tresc,
	})
	otworz.Activated:Connect(function()
		akcja("WszystkieBramy", true)
	end)
	zamknij.Activated:Connect(function()
		akcja("WszystkieBramy", false)
	end)
	local lista = przewijanie(tresc, 0, 52, 824, 368, UDim2.fromOffset(402, 84))
	local pusto = pustyStan(tresc, "Brak bram", "Dodaj Tag BramaPSP do bramy garażowej w remizie.")

	return function(dane)
		wyczysc(lista)
		pusto.Visible = #dane.Bramy == 0
		for indeks, brama in dane.Bramy do
			local karta = szklo({ LayoutOrder = indeks, Parent = lista })
			ramkaIkony(karta, "brama", 16, 18, 48)
			W.napis({
				Text = brama.Nazwa,
				FontFace = W.czcionka(Enum.FontWeight.Bold),
				TextSize = 15,
				TextTruncate = Enum.TextTruncate.AtEnd,
				Position = UDim2.fromOffset(78, 21),
				Size = UDim2.new(1, -160, 0, 20),
				Parent = karta,
			})
			W.napis({
				Text = brama.Otwarta and "Otwarta" or "Zamknięta",
				TextSize = 12,
				TextColor3 = brama.Otwarta and K.Zielony or K.TekstSlaby,
				Position = UDim2.fromOffset(78, 44),
				Size = UDim2.fromOffset(200, 16),
				Parent = karta,
			})
			przelacznik(karta, brama.Otwarta, function(wlaczony)
				akcja("Brama", brama.Id, wlaczony)
			end)
		end
	end
end

APLIKACJE.Syrena = function(tresc, sesja)
	local duzy = W.przycisk({
		Text = "",
		AnchorPoint = Vector2.new(0.5, 0),
		Position = UDim2.new(0.5, 0, 0, 20),
		Size = UDim2.fromOffset(176, 176),
		BackgroundColor3 = AKCENT,
		Parent = tresc,
	})
	W.zaokraglij(duzy, 88)
	local pierscien = W.nowy("UIStroke", {
		Color = AKCENT,
		Thickness = 0,
		Transparency = 0.3,
		ApplyStrokeMode = Enum.ApplyStrokeMode.Border,
		Parent = duzy,
	})
	Ikony.rysuj("syrena", duzy, Color3.new(1, 1, 1), 72)
	local napis = W.napis({
		FontFace = W.czcionka(Enum.FontWeight.Heavy),
		TextSize = 18,
		TextXAlignment = Enum.TextXAlignment.Center,
		Position = UDim2.fromOffset(0, 214),
		Size = UDim2.new(1, 0, 0, 24),
		Parent = tresc,
	})
	local opis = W.napis({
		TextSize = 13,
		TextColor3 = K.TekstSlaby,
		TextXAlignment = Enum.TextXAlignment.Center,
		Position = UDim2.fromOffset(0, 242),
		Size = UDim2.new(1, 0, 0, 18),
		Parent = tresc,
	})
	local karta = szklo({
		AnchorPoint = Vector2.new(0.5, 0),
		Position = UDim2.new(0.5, 0, 0, 290),
		Size = UDim2.fromOffset(560, 64),
		Parent = tresc,
	})
	W.napis({
		Text = ("Uruchamia syrenę i lampy ostrzegawcze w remizie na %d s. Działa też z systemem alarmowania OSP."):format(
			U.CzasSyreny or 40
		),
		TextSize = 12,
		TextColor3 = K.TekstSlaby,
		TextWrapped = true,
		TextXAlignment = Enum.TextXAlignment.Center,
		Position = UDim2.fromOffset(20, 0),
		Size = UDim2.new(1, -40, 1, 0),
		Parent = karta,
	})

	local aktywna, koniec, puls = false, 0, nil
	duzy.Activated:Connect(function()
		akcja("Syrena", not aktywna)
	end)
	task.spawn(function()
		while stan.sesja == sesja and stan.otwarte do
			if aktywna then
				opis.Text = ("Wyje jeszcze %d s"):format(math.max(0, math.ceil(koniec - os.clock())))
			end
			task.wait(0.5)
		end
	end)

	return function(dane)
		aktywna = dane.Syrena
		koniec = os.clock() + (dane.SyrenaPozostalo or 0)
		duzy.BackgroundColor3 = aktywna and Color3.fromRGB(70, 22, 26) or AKCENT
		napis.Text = aktywna and "WYŁĄCZ SYRENĘ" or "URUCHOM SYRENĘ"
		if not dane.MaSyrene then
			opis.Text = "Brak syreny na mapie (Tag AlarmSyrena)"
		elseif not aktywna then
			opis.Text = "Syrena gotowa"
		end
		if aktywna and not puls then
			pierscien.Thickness = 0
			pierscien.Transparency = 0.2
			puls = W.animuj(
				pierscien,
				{ Thickness = 18, Transparency = 1 },
				TweenInfo.new(1.1, Enum.EasingStyle.Quad, Enum.EasingDirection.Out, -1)
			)
		elseif not aktywna and puls then
			puls:Cancel()
			puls = nil
			pierscien.Thickness = 0
		end
	end
end

APLIKACJE.Zaloga = function(tresc)
	local lewaEtykieta = etykieta(tresc, "W SŁUŻBIE", 0, 0, 400)
	local prawaEtykieta = etykieta(tresc, "W POBLIŻU (MOŻESZ PRZYJĄĆ)", 420, 0, 404)
	local zaloga = przewijanie(tresc, 0, 24, 404, 396)
	local kandydaci = przewijanie(tresc, 420, 24, 404, 396)

	local function wiersz(rodzic, osoba, indeks)
		local karta = szklo({ Size = UDim2.new(1, -6, 0, 58), LayoutOrder = indeks, Parent = rodzic })
		awatar(karta, osoba.UserId, 12, 10, 38)
		W.napis({
			Text = osoba.Nazwa,
			FontFace = W.czcionka(Enum.FontWeight.Bold),
			TextSize = 14,
			TextTruncate = Enum.TextTruncate.AtEnd,
			Position = UDim2.fromOffset(60, 9),
			Size = UDim2.new(1, -160, 0, 18),
			Parent = karta,
		})
		return karta
	end

	local function pustyWiersz(rodzic, tekst)
		W.napis({
			Text = tekst,
			TextSize = 13,
			TextColor3 = K.TekstSlaby,
			TextWrapped = true,
			Size = UDim2.new(1, -6, 0, 40),
			Parent = rodzic,
		})
	end

	return function(dane)
		wyczysc(zaloga)
		wyczysc(kandydaci)
		lewaEtykieta.Text = ("W SŁUŻBIE (%d)"):format(#dane.Zaloga)
		prawaEtykieta.Text = ("W POBLIŻU (%d)"):format(#dane.Kandydaci)

		for indeks, osoba in dane.Zaloga do
			local karta = wiersz(zaloga, osoba, indeks)
			pigulka(karta, osoba.Ranga ~= "" and osoba.Ranga or "PSP", osoba.Kierownik and ZOLTY or AKCENT, 60, 31)
			if osoba.To then
				pigulka(karta, "TY", K.TekstSlaby, 300, 20)
			elseif not osoba.Kierownik then
				local zwolnij = przycisk("Zwolnij", false, {
					TextColor3 = K.Czerwony,
					AnchorPoint = Vector2.new(1, 0.5),
					Position = UDim2.new(1, -12, 0.5, 0),
					Size = UDim2.fromOffset(84, 30),
					Parent = karta,
				})
				zwolnij.Activated:Connect(function()
					akcja("Zwolnij", osoba.UserId)
				end)
			end
		end
		if #dane.Zaloga == 0 then
			pustyWiersz(zaloga, "Nikogo z załogi nie ma teraz w grze.")
		end

		for indeks, osoba in dane.Kandydaci do
			local karta = wiersz(kandydaci, osoba, indeks)
			W.napis({
				Text = W.metry(osoba.Odleglosc) .. " od Ciebie",
				TextSize = 11,
				TextColor3 = K.TekstSlaby,
				Position = UDim2.fromOffset(60, 31),
				Size = UDim2.fromOffset(200, 16),
				Parent = karta,
			})
			local przyjmij = przycisk("Przyjmij", false, {
				TextColor3 = K.TekstCiemny,
				BackgroundColor3 = K.Zielony,
				AnchorPoint = Vector2.new(1, 0.5),
				Position = UDim2.new(1, -12, 0.5, 0),
				Size = UDim2.fromOffset(90, 30),
				Parent = karta,
			})
			przyjmij.Activated:Connect(function()
				akcja("Przyjmij", osoba.UserId)
			end)
		end
		if #dane.Kandydaci == 0 then
			pustyWiersz(kandydaci, "Podejdź do gracza, którego chcesz przyjąć do PSP.")
		end
	end
end

APLIKACJE.Pojazdy = function(tresc)
	W.napis({
		Text = "Wybierz wóz, który ma wyjechać z garażu",
		TextSize = 13,
		TextColor3 = K.TekstSlaby,
		Size = UDim2.fromOffset(500, 36),
		Parent = tresc,
	})
	local schowaj = przycisk("Schowaj mój pojazd", false, {
		TextColor3 = K.Czerwony,
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.fromScale(1, 0),
		Size = UDim2.fromOffset(190, 36),
		Visible = false,
		Parent = tresc,
	})
	schowaj.Activated:Connect(function()
		akcja("Schowaj")
	end)
	local lista = przewijanie(tresc, 0, 52, 824, 368, UDim2.fromOffset(258, 128))
	local pusto, naglowekPustego, opisPustego =
		pustyStan(tresc, "Brak pojazdów", "Wrzuć modele wozów do ServerStorage > PojazdyPSP.")

	return function(dane)
		wyczysc(lista)
		schowaj.Visible = dane.MojPojazd ~= nil
		pusto.Visible = #dane.Pojazdy == 0 or dane.MiejscPojazdow == 0
		if #dane.Pojazdy == 0 then
			naglowekPustego.Text = "Brak pojazdów"
			opisPustego.Text = "Wrzuć modele wozów do ServerStorage > PojazdyPSP."
			return
		elseif dane.MiejscPojazdow == 0 then
			naglowekPustego.Text = "Brak miejsc przed garażem"
			opisPustego.Text = "Dodaj Tag MiejscePojazduPSP do części, na której ma stanąć wóz."
		end
		for indeks, nazwa in dane.Pojazdy do
			local karta = szklo({ LayoutOrder = indeks, Parent = lista })
			ramkaIkony(karta, "pojazd", 16, 16, 48)
			W.napis({
				Text = nazwa,
				FontFace = W.czcionka(Enum.FontWeight.Bold),
				TextSize = 15,
				TextTruncate = Enum.TextTruncate.AtEnd,
				Position = UDim2.fromOffset(16, 76),
				Size = UDim2.new(1, -32, 0, 20),
				Parent = karta,
			})
			local naZewnatrz = dane.MojPojazd == nazwa
			W.napis({
				Text = naZewnatrz and "Na zewnątrz" or "W garażu",
				TextSize = 12,
				TextColor3 = naZewnatrz and K.Zielony or K.TekstSlaby,
				Position = UDim2.fromOffset(16, 98),
				Size = UDim2.fromOffset(120, 16),
				Parent = karta,
			})
			if not naZewnatrz and dane.MiejscPojazdow > 0 then
				local wyjedz = przycisk("Wyjedź", true, {
					AnchorPoint = Vector2.new(1, 0),
					Position = UDim2.new(1, -14, 0, 18),
					Size = UDim2.fromOffset(92, 32),
					Parent = karta,
				})
				wyjedz.Activated:Connect(function()
					akcja("Wyjedz", nazwa)
				end)
			end
		end
	end
end

APLIKACJE.Komunikaty = function(tresc)
	local karta = szklo({ Size = UDim2.fromOffset(824, 96), Parent = tresc })
	local maks = U.MaksDlugoscKomunikatu or 150
	local pole = W.nowy("TextBox", {
		BackgroundTransparency = 1,
		Position = UDim2.fromOffset(16, 12),
		Size = UDim2.new(1, -32, 1, -32),
		MultiLine = true,
		TextWrapped = true,
		ClearTextOnFocus = false,
		Text = "",
		PlaceholderText = "Treść komunikatu do całej załogi, np. Zbiórka w remizie o 18:00.",
		PlaceholderColor3 = K.TekstSlaby,
		TextColor3 = K.Tekst,
		FontFace = W.czcionka(),
		TextSize = 14,
		TextXAlignment = Enum.TextXAlignment.Left,
		TextYAlignment = Enum.TextYAlignment.Top,
		Parent = karta,
	})
	local licznik = W.napis({
		Text = "0/" .. maks,
		TextSize = 11,
		TextColor3 = K.TekstSlaby,
		TextXAlignment = Enum.TextXAlignment.Right,
		AnchorPoint = Vector2.new(1, 1),
		Position = UDim2.new(1, -14, 1, -8),
		Size = UDim2.fromOffset(80, 14),
		Parent = karta,
	})
	pole:GetPropertyChangedSignal("Text"):Connect(function()
		local dlugosc = utf8.len(pole.Text) or #pole.Text
		if dlugosc > maks then
			pole.Text = string.sub(pole.Text, 1, utf8.offset(pole.Text, maks + 1) - 1)
			return
		end
		licznik.Text = ("%d/%d"):format(dlugosc, maks)
	end)
	W.napis({
		Text = "Komunikat zobaczą wszyscy z załogi, którzy są w grze.",
		TextSize = 12,
		TextColor3 = K.TekstSlaby,
		Position = UDim2.fromOffset(0, 108),
		Size = UDim2.fromOffset(500, 38),
		Parent = tresc,
	})
	local wyslij = przycisk("Wyślij do załogi", true, {
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.new(1, 0, 0, 108),
		Size = UDim2.fromOffset(190, 38),
		Parent = tresc,
	})
	wyslij.Activated:Connect(function()
		if akcja("Komunikat", pole.Text) then
			pole.Text = ""
		end
	end)
	etykieta(tresc, "OSTATNIE KOMUNIKATY", 0, 164, 300)
	local lista = przewijanie(tresc, 0, 186, 824, 234)

	return function(dane)
		wyczysc(lista)
		for indeks, wpis in dane.Komunikaty do
			local wiersz = szklo({
				Size = UDim2.new(1, -6, 0, 0),
				AutomaticSize = Enum.AutomaticSize.Y,
				LayoutOrder = indeks,
				Parent = lista,
			})
			W.nowy("UIPadding", {
				PaddingTop = UDim.new(0, 10),
				PaddingBottom = UDim.new(0, 12),
				PaddingLeft = UDim.new(0, 14),
				PaddingRight = UDim.new(0, 14),
				Parent = wiersz,
			})
			W.napis({
				Text = wpis.Autor,
				FontFace = W.czcionka(Enum.FontWeight.Bold),
				TextSize = 13,
				Size = UDim2.new(1, -60, 0, 18),
				Parent = wiersz,
			})
			W.napis({
				Text = os.date("%H:%M", wpis.Czas),
				TextSize = 11,
				TextColor3 = K.TekstSlaby,
				TextXAlignment = Enum.TextXAlignment.Right,
				AnchorPoint = Vector2.new(1, 0),
				Position = UDim2.fromScale(1, 0),
				Size = UDim2.fromOffset(60, 18),
				Parent = wiersz,
			})
			W.napis({
				Text = wpis.Tekst,
				TextSize = 13,
				TextColor3 = K.TekstSlaby,
				TextWrapped = true,
				AutomaticSize = Enum.AutomaticSize.Y,
				Position = UDim2.fromOffset(0, 22),
				Size = UDim2.new(1, 0, 0, 0),
				Parent = wiersz,
			})
		end
		if #dane.Komunikaty == 0 then
			W.napis({
				Text = "Na razie nie było żadnych komunikatów.",
				TextSize = 13,
				TextColor3 = K.TekstSlaby,
				Size = UDim2.new(1, -6, 0, 30),
				Parent = lista,
			})
		end
	end
end

APLIKACJE.Oswietlenie = function(tresc)
	W.napis({
		Text = "Światła w strefach remizy",
		TextSize = 13,
		TextColor3 = K.TekstSlaby,
		Size = UDim2.fromOffset(400, 36),
		Parent = tresc,
	})
	local wylacz = przycisk("Wyłącz wszystko", false, {
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.fromScale(1, 0),
		Size = UDim2.fromOffset(170, 36),
		Parent = tresc,
	})
	local wlacz = przycisk("Włącz wszystko", true, {
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.new(1, -180, 0, 0),
		Size = UDim2.fromOffset(170, 36),
		Parent = tresc,
	})
	wlacz.Activated:Connect(function()
		akcja("Strefa", nil, true)
	end)
	wylacz.Activated:Connect(function()
		akcja("Strefa", nil, false)
	end)
	local lista = przewijanie(tresc, 0, 52, 824, 368, UDim2.fromOffset(258, 120))
	local pusto = pustyStan(tresc, "Brak lamp", "Dodaj Tag SwiatloPSP do lamp w remizie (atrybut Strefa grupuje je).")

	return function(dane)
		wyczysc(lista)
		pusto.Visible = #dane.Strefy == 0
		for indeks, strefa in dane.Strefy do
			local karta = szklo({ LayoutOrder = indeks, Parent = lista })
			ramkaIkony(
				karta,
				"swiatlo",
				16,
				16,
				48,
				strefa.Wlaczone and ZOLTY or SZARY,
				strefa.Wlaczone and ZOLTY or K.TekstSlaby,
				0.82
			)
			W.napis({
				Text = strefa.Nazwa,
				FontFace = W.czcionka(Enum.FontWeight.Bold),
				TextSize = 15,
				TextTruncate = Enum.TextTruncate.AtEnd,
				Position = UDim2.fromOffset(16, 74),
				Size = UDim2.new(1, -32, 0, 20),
				Parent = karta,
			})
			W.napis({
				Text = ("Lampy: %d"):format(strefa.Ile),
				TextSize = 12,
				TextColor3 = K.TekstSlaby,
				Position = UDim2.fromOffset(16, 95),
				Size = UDim2.fromOffset(120, 16),
				Parent = karta,
			})
			przelacznik(karta, strefa.Wlaczone, function(wlaczony)
				akcja("Strefa", strefa.Nazwa, wlaczony)
			end, UDim2.new(1, -14, 0, 18))
		end
	end
end

-- ============================================================
--  Nawigacja
-- ============================================================

local function pokazStart()
	stan.sesja += 1
	stan.aplikacja = nil
	stan.odswiez = nil
	widokAplikacji.Visible = false
	widokStart.Visible = true
end

local function nazwaAplikacji(id)
	for _, aplikacja in U.Aplikacje do
		if aplikacja.Id == id then
			return aplikacja.Nazwa or id
		end
	end
	return id
end

local function otworzAplikacje(id)
	local budowa = APLIKACJE[id]
	if not budowa then
		return
	end
	stan.sesja += 1
	stan.aplikacja = id
	wyczysc(trescAplikacji)
	for _, dziecko in ikonaAplikacji:GetChildren() do
		dziecko:Destroy()
	end
	Ikony.rysuj(IKONY_APLIKACJI[id], ikonaAplikacji, K.Bialy, 20)
	tytulAplikacji.Text = nazwaAplikacji(id)
	stan.odswiez = budowa(trescAplikacji, stan.sesja)
	if stan.dane then
		stan.odswiez(stan.dane)
	end
	widokStart.Visible = false
	widokAplikacji.Visible = true
	widokAplikacji.Position = UDim2.fromOffset(40, 30)
	W.animuj(widokAplikacji, { Position = UDim2.fromOffset(0, 30) }, TweenInfo.new(0.2, Enum.EasingStyle.Quart))
end

local function przeliczSkale()
	local kamera = workspace.CurrentCamera
	local rozmiar = kamera and kamera.ViewportSize or Vector2.new(1280, 720)
	bazowaSkala = math.clamp(math.min(rozmiar.X * 0.92 / SZEROKOSC, rozmiar.Y * 0.88 / WYSOKOSC, 1.35), 0.4, 1.35)
	skala.Scale = bazowaSkala
end

function Tablet.Otwarty()
	return stan.otwarte
end

function Tablet.Otworz()
	if stan.otwarte then
		return
	end
	stan.otwarte = true
	przeliczSkale()
	pokazStart()
	napisGodziny.Text = os.date("%H:%M")
	obudowa.Visible = true
	przyciemnienie.Visible = true
	obudowa.Position = UDim2.fromScale(0.5, 1.6)
	W.animuj(obudowa, { Position = UDim2.fromScale(0.5, 0.52) }, TweenInfo.new(0.45, Enum.EasingStyle.Quart, Enum.EasingDirection.Out))
	W.animuj(przyciemnienie, { BackgroundTransparency = 0.45 }, TweenInfo.new(0.3))

	local petla = {}
	stan.petla = petla
	task.spawn(function()
		local ok, wiadomosc, dane = ctx.Zadanie("Stan")
		if not ok then
			ctx.Powiadom(wiadomosc or "Brak dostępu do tabletu.", "blad")
			ctx.Odloz()
			return
		end
		zastosujDane(dane)
		-- Odświeżanie na żywo, dopóki tablet jest otwarty
		while stan.otwarte and stan.petla == petla do
			task.wait(3)
			if stan.otwarte and stan.petla == petla then
				local okStan, _, noweDane = ctx.Zadanie("Stan")
				if okStan then
					zastosujDane(noweDane)
				end
			end
		end
	end)
end

function Tablet.Zamknij()
	if not stan.otwarte then
		return
	end
	stan.otwarte = false
	stan.petla = nil
	stan.sesja += 1
	local pole = UserInputService:GetFocusedTextBox()
	if pole then
		pole:ReleaseFocus()
	end
	local chowanie = W.animuj(
		obudowa,
		{ Position = UDim2.fromScale(0.5, 1.6) },
		TweenInfo.new(0.3, Enum.EasingStyle.Quart, Enum.EasingDirection.In)
	)
	W.animuj(przyciemnienie, { BackgroundTransparency = 1 }, TweenInfo.new(0.3))
	chowanie.Completed:Connect(function()
		if not stan.otwarte then
			obudowa.Visible = false
			przyciemnienie.Visible = false
		end
	end)
end

-- ============================================================
--  Budowa tabletu
-- ============================================================

local function zbudujLogo(rodzic)
	local pole = W.nowy("Frame", {
		BackgroundTransparency = 1,
		AnchorPoint = Vector2.new(0.5, 0),
		Position = UDim2.new(0.5, 0, 0, 34),
		Size = UDim2.fromOffset(116, 116),
		Parent = rodzic,
	})
	if U.Logo and U.Logo ~= "" then
		W.nowy("ImageLabel", {
			BackgroundTransparency = 1,
			Size = UDim2.fromScale(1, 1),
			Image = U.Logo,
			ScaleType = Enum.ScaleType.Fit,
			Parent = pole,
		})
		return
	end
	-- Wbudowany znak: pierścień w kolorze akcentu i napis PSP
	local kolo = W.nowy("Frame", {
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = Color3.fromRGB(22, 22, 26),
		Parent = pole,
	})
	W.zaokraglij(kolo, 58)
	W.nowy("UIStroke", { Color = AKCENT, Thickness = 4, Parent = kolo })
	local wewnetrzne = W.nowy("Frame", {
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.fromScale(0.5, 0.5),
		Size = UDim2.fromOffset(94, 94),
		BackgroundTransparency = 1,
		Parent = kolo,
	})
	W.zaokraglij(wewnetrzne, 47)
	W.nowy("UIStroke", { Color = Color3.new(1, 1, 1), Thickness = 1, Transparency = 0.8, Parent = wewnetrzne })
	W.napis({
		Text = "PSP",
		FontFace = W.czcionka(Enum.FontWeight.Heavy),
		TextSize = 34,
		TextXAlignment = Enum.TextXAlignment.Center,
		Position = UDim2.fromOffset(0, 34),
		Size = UDim2.new(1, 0, 0, 36),
		Parent = kolo,
	})
	W.napis({
		Text = "STRAŻ POŻARNA",
		FontFace = W.czcionka(Enum.FontWeight.Bold),
		TextSize = 8,
		TextColor3 = K.TekstSlaby,
		TextXAlignment = Enum.TextXAlignment.Center,
		Position = UDim2.fromOffset(0, 70),
		Size = UDim2.new(1, 0, 0, 12),
		Parent = kolo,
	})
end

function Tablet.Inicjuj(ekranGui, kontekst)
	ctx = kontekst
	U = kontekst.Ustawienia
	AKCENT = U.KolorAkcentu or K.Czerwony

	przyciemnienie = W.nowy("Frame", {
		Size = UDim2.fromScale(1, 1),
		BackgroundColor3 = Color3.new(0, 0, 0),
		BackgroundTransparency = 1,
		BorderSizePixel = 0,
		Visible = false,
		ZIndex = 0,
		Parent = ekranGui,
	})

	-- Obudowa tabletu
	obudowa = W.nowy("Frame", {
		Name = "Tablet",
		ZIndex = 2,
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.fromScale(0.5, 0.52),
		Size = UDim2.fromOffset(SZEROKOSC, WYSOKOSC),
		BackgroundColor3 = Color3.fromRGB(16, 16, 18),
		Visible = false,
		Parent = ekranGui,
	})
	W.zaokraglij(obudowa, 40)
	W.nowy("UIStroke", { Color = Color3.fromRGB(78, 78, 86), Thickness = 2, Transparency = 0.25, Parent = obudowa })
	skala = W.nowy("UIScale", { Parent = obudowa })
	W.nowy("TextButton", {
		Text = "",
		Modal = true,
		BackgroundTransparency = 1,
		Size = UDim2.fromScale(1, 1),
		ZIndex = 0,
		AutoButtonColor = false,
		Parent = obudowa,
	})
	local kamera = W.nowy("Frame", {
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.new(0.5, 0, 0, 9),
		Size = UDim2.fromOffset(7, 7),
		BackgroundColor3 = Color3.fromRGB(40, 40, 46),
		Parent = obudowa,
	})
	W.zaokraglij(kamera, 4)

	-- Ekran
	ekranTabletu = W.nowy("Frame", {
		Position = UDim2.fromOffset(18, 18),
		Size = UDim2.fromOffset(SZEROKOSC - 36, WYSOKOSC - 36),
		BackgroundColor3 = Color3.new(1, 1, 1),
		ClipsDescendants = true,
		Parent = obudowa,
	})
	W.zaokraglij(ekranTabletu, 24)
	W.nowy("UIGradient", {
		Rotation = 120,
		Color = ColorSequence.new(Color3.fromRGB(30, 24, 27), Color3.fromRGB(11, 11, 13)),
		Parent = ekranTabletu,
	})
	-- Delikatna czerwona poświata za logo
	for indeks, rozmiar in { 620, 420, 260 } do
		local poswiata = W.nowy("Frame", {
			AnchorPoint = Vector2.new(0.5, 0.5),
			Position = UDim2.fromOffset(432, 120),
			Size = UDim2.fromOffset(rozmiar, rozmiar),
			BackgroundColor3 = AKCENT,
			BackgroundTransparency = 0.985 - indeks * 0.008,
			Parent = ekranTabletu,
		})
		W.zaokraglij(poswiata, rozmiar / 2)
	end

	-- Pasek statusu
	napisGodziny = W.napis({
		FontFace = W.czcionka(Enum.FontWeight.Bold),
		TextSize = 13,
		Position = UDim2.fromOffset(22, 6),
		Size = UDim2.fromOffset(80, 18),
		Parent = ekranTabletu,
	})
	W.napis({
		Text = "PSP  •  " .. (U.NazwaJednostki or ""),
		FontFace = W.czcionka(Enum.FontWeight.Medium),
		TextSize = 12,
		TextColor3 = K.TekstSlaby,
		TextXAlignment = Enum.TextXAlignment.Center,
		AnchorPoint = Vector2.new(0.5, 0),
		Position = UDim2.new(0.5, 0, 0, 6),
		Size = UDim2.fromOffset(400, 18),
		Parent = ekranTabletu,
	})
	for numer = 1, 3 do
		local slupek = W.nowy("Frame", {
			AnchorPoint = Vector2.new(0, 1),
			Position = UDim2.new(1, -112 + numer * 5, 0, 20),
			Size = UDim2.fromOffset(3, 3 + numer * 3),
			BackgroundColor3 = K.Tekst,
			BorderSizePixel = 0,
			Parent = ekranTabletu,
		})
		W.zaokraglij(slupek, 1)
	end
	W.napis({
		Text = "100%",
		FontFace = W.czcionka(Enum.FontWeight.Medium),
		TextSize = 12,
		TextXAlignment = Enum.TextXAlignment.Right,
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.new(1, -52, 0, 6),
		Size = UDim2.fromOffset(40, 18),
		Parent = ekranTabletu,
	})
	local bateria = W.nowy("Frame", {
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.new(1, -22, 0, 9),
		Size = UDim2.fromOffset(24, 12),
		BackgroundTransparency = 1,
		Parent = ekranTabletu,
	})
	W.zaokraglij(bateria, 3)
	W.nowy("UIStroke", { Color = K.Tekst, Thickness = 1, Transparency = 0.4, Parent = bateria })
	local poziom = W.nowy("Frame", {
		Position = UDim2.fromOffset(2, 2),
		Size = UDim2.new(1, -4, 1, -4),
		BackgroundColor3 = K.Zielony,
		BorderSizePixel = 0,
		Parent = bateria,
	})
	W.zaokraglij(poziom, 2)

	-- Strona Główna
	widokStart = W.nowy("Frame", {
		BackgroundTransparency = 1,
		Position = UDim2.fromOffset(0, 30),
		Size = UDim2.new(1, 0, 1, -30),
		Parent = ekranTabletu,
	})
	zbudujLogo(widokStart)
	W.napis({
		Text = U.Napis or "ZARZĄDZAJ SWOJĄ PSP",
		FontFace = W.czcionka(Enum.FontWeight.Heavy),
		TextSize = 22,
		TextColor3 = AKCENT,
		TextXAlignment = Enum.TextXAlignment.Center,
		Position = UDim2.fromOffset(0, 162),
		Size = UDim2.new(1, 0, 0, 28),
		Parent = widokStart,
	})
	W.napis({
		Text = ("Kierownik: %s  •  %s"):format(gracz.DisplayName, U.NazwaJednostki or ""),
		TextSize = 13,
		TextColor3 = K.TekstSlaby,
		TextXAlignment = Enum.TextXAlignment.Center,
		Position = UDim2.fromOffset(0, 192),
		Size = UDim2.new(1, 0, 0, 18),
		Parent = widokStart,
	})
	local siatka = W.nowy("Frame", {
		BackgroundTransparency = 1,
		AnchorPoint = Vector2.new(0.5, 0),
		Position = UDim2.new(0.5, 0, 0, 234),
		Size = UDim2.fromOffset(800, 200),
		Parent = widokStart,
	})
	W.nowy("UIGridLayout", {
		CellSize = UDim2.fromOffset(256, 92),
		CellPadding = UDim2.fromOffset(16, 16),
		HorizontalAlignment = Enum.HorizontalAlignment.Center,
		SortOrder = Enum.SortOrder.LayoutOrder,
		Parent = siatka,
	})
	for indeks, aplikacja in U.Aplikacje do
		if aplikacja.Wlaczona ~= false and APLIKACJE[aplikacja.Id] then
			local kafelek = W.przycisk({
				Text = "",
				BackgroundColor3 = Color3.new(1, 1, 1),
				BackgroundTransparency = 0.955,
				LayoutOrder = indeks,
				Parent = siatka,
			})
			W.zaokraglij(kafelek, 18)
			W.obrysuj(kafelek, 0.9)
			ramkaIkony(kafelek, IKONY_APLIKACJI[aplikacja.Id], 18, 22, 48)
			W.napis({
				Text = aplikacja.Nazwa or aplikacja.Id,
				FontFace = W.czcionka(Enum.FontWeight.Bold),
				TextSize = 15,
				Position = UDim2.fromOffset(80, 25),
				Size = UDim2.new(1, -110, 0, 20),
				Parent = kafelek,
			})
			local podtytul = W.napis({
				Text = "Ładowanie...",
				TextSize = 12,
				TextColor3 = K.TekstSlaby,
				TextTruncate = Enum.TextTruncate.AtEnd,
				Position = UDim2.fromOffset(80, 47),
				Size = UDim2.new(1, -110, 0, 16),
				Parent = kafelek,
			})
			local strzalka = W.nowy("Frame", {
				BackgroundTransparency = 1,
				AnchorPoint = Vector2.new(1, 0.5),
				Position = UDim2.new(1, -12, 0.5, 0),
				Size = UDim2.fromOffset(20, 20),
				Parent = kafelek,
			})
			Ikony.rysuj("dalej", strzalka, K.TekstSlaby, 14)
			kafelek.MouseEnter:Connect(function()
				W.animuj(kafelek, { BackgroundTransparency = 0.92 })
			end)
			kafelek.MouseLeave:Connect(function()
				W.animuj(kafelek, { BackgroundTransparency = 0.955 })
			end)
			kafelek.Activated:Connect(function()
				otworzAplikacje(aplikacja.Id)
			end)
			kafelki[aplikacja.Id] = { Podtytul = podtytul }
		end
	end
	local kropka = W.nowy("Frame", {
		AnchorPoint = Vector2.new(0, 0.5),
		Position = UDim2.new(0.5, -96, 0, 470),
		Size = UDim2.fromOffset(7, 7),
		BackgroundColor3 = K.Zielony,
		Parent = widokStart,
	})
	W.zaokraglij(kropka, 4)
	napisStanu = W.napis({
		Text = "Łączenie z systemem...",
		TextSize = 12,
		TextColor3 = K.TekstSlaby,
		AnchorPoint = Vector2.new(0, 0.5),
		Position = UDim2.new(0.5, -82, 0, 470),
		Size = UDim2.fromOffset(260, 16),
		Parent = widokStart,
	})

	-- Widok aplikacji
	widokAplikacji = W.nowy("Frame", {
		BackgroundTransparency = 1,
		Position = UDim2.fromOffset(0, 30),
		Size = UDim2.new(1, 0, 1, -30),
		Visible = false,
		Parent = ekranTabletu,
	})
	local wstecz = W.przycisk({
		Text = "‹",
		TextSize = 24,
		BackgroundColor3 = Color3.new(1, 1, 1),
		BackgroundTransparency = 0.92,
		Position = UDim2.fromOffset(20, 10),
		Size = UDim2.fromOffset(38, 38),
		Parent = widokAplikacji,
	})
	W.zaokraglij(wstecz, 19)
	wstecz.Activated:Connect(pokazStart)
	ikonaAplikacji = W.nowy("Frame", {
		Position = UDim2.fromOffset(70, 10),
		Size = UDim2.fromOffset(38, 38),
		BackgroundColor3 = AKCENT,
		BackgroundTransparency = 0.8,
		Parent = widokAplikacji,
	})
	W.zaokraglij(ikonaAplikacji, 11)
	tytulAplikacji = W.napis({
		FontFace = W.czcionka(Enum.FontWeight.Heavy),
		TextSize = 21,
		Position = UDim2.fromOffset(120, 10),
		Size = UDim2.fromOffset(400, 38),
		Parent = widokAplikacji,
	})
	W.napis({
		Text = "Aktualizacja na żywo",
		TextSize = 12,
		TextColor3 = K.TekstSlaby,
		TextXAlignment = Enum.TextXAlignment.Right,
		AnchorPoint = Vector2.new(1, 0),
		Position = UDim2.new(1, -22, 0, 10),
		Size = UDim2.fromOffset(200, 38),
		Parent = widokAplikacji,
	})
	W.nowy("Frame", {
		Position = UDim2.fromOffset(20, 60),
		Size = UDim2.new(1, -40, 0, 1),
		BackgroundColor3 = Color3.new(1, 1, 1),
		BackgroundTransparency = 0.92,
		BorderSizePixel = 0,
		Parent = widokAplikacji,
	})
	trescAplikacji = W.nowy("Frame", {
		BackgroundTransparency = 1,
		Position = UDim2.fromOffset(20, 74),
		Size = UDim2.fromOffset(824, 420),
		Parent = widokAplikacji,
	})

	-- Pasek "home" na dole: w aplikacji wraca na start, na starcie chowa tablet
	local pasekHome = W.przycisk({
		Text = "",
		BackgroundTransparency = 1,
		AnchorPoint = Vector2.new(0.5, 1),
		Position = UDim2.new(0.5, 0, 1, 0),
		Size = UDim2.fromOffset(220, 22),
		ZIndex = 5,
		Parent = ekranTabletu,
	})
	local kreska = W.nowy("Frame", {
		AnchorPoint = Vector2.new(0.5, 0.5),
		Position = UDim2.fromScale(0.5, 0.5),
		Size = UDim2.fromOffset(130, 5),
		BackgroundColor3 = Color3.new(1, 1, 1),
		BackgroundTransparency = 0.45,
		Parent = pasekHome,
	})
	W.zaokraglij(kreska, 3)
	pasekHome.Activated:Connect(function()
		if stan.aplikacja then
			pokazStart()
		else
			ctx.Odloz()
		end
	end)

	task.spawn(function()
		while true do
			if stan.otwarte then
				napisGodziny.Text = os.date("%H:%M")
			end
			task.wait(10)
		end
	end)
	local kameraGry = workspace.CurrentCamera
	if kameraGry then
		kameraGry:GetPropertyChangedSignal("ViewportSize"):Connect(function()
			if stan.otwarte then
				przeliczSkale()
			end
		end)
	end
end

return Tablet
