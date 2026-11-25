--[[
	SYSTEM WEZWAŃ 112  |  USTAWIENIA
	Czyta je serwer i gracz, więc NIE wpisuj tu nic tajnego.

	MIEJSCA WEZWAŃ
		Postaw na ziemi część i dodaj jej Tag "MiejsceWezwania"
		(Properties > Tags > +). Nazwa części = adres widoczny dla strażaków.
		Przód części (Front) = kierunek drogi (drzewo leży w poprzek, auta stoją wzdłuż).
		Atrybut "Rodzaje" (tekst, opcjonalnie) mówi, jakie wezwania mogą tu wypaść,
		np. "Drzewo, Olej, Wypadek". Bez atrybutu: wszystkie rodzaje.

	KOMENDA (admini, właściciel gry, każdy w Studio)
		/wezwanie            - losowe wezwanie
		/wezwanie Drzewo     - konkretny rodzaj (Id z listy niżej)
		/wezwanie lista      - lista rodzajów
		/wezwanie stop       - kończy wszystkie wezwania
]]

local Ustawienia = {}

-- ============================================================
--  KTO JEST STRAŻAKIEM (widzi ikony 112 i może wykonywać zadania)
-- ============================================================

Ustawienia.RangiStrazakow = { "OSP", "PSP", "Kierownik PSP" } -- atrybut Ranga (system ekwipunku)
Ustawienia.DruzynyStrazakow = { "OSP", "PSP" } -- nazwy drużyn (Team)

-- Kto może używać komendy /wezwanie (poza właścicielem gry i Studio)
Ustawienia.Admini = {} -- UserId
Ustawienia.AdminOdRangiWGrupie = 255

-- ============================================================
--  AUTOMATYCZNE WEZWANIA
-- ============================================================

Ustawienia.Automatyczne = true -- wezwania pojawiają się same
Ustawienia.PierwszeWezwaniePo = 90 -- sekund od startu serwera
Ustawienia.OdstepMin = 240 -- najkrótszy odstęp między wezwaniami (s)
Ustawienia.OdstepMax = 480 -- najdłuższy odstęp (s)
Ustawienia.MaksAktywnych = 2 -- ile wezwań naraz najwyżej
Ustawienia.MinStrazakow = 1 -- ilu strażaków musi być w grze, żeby padło wezwanie
Ustawienia.CzasNaAkcje = 900 -- po tylu sekundach niewykonane wezwanie wygasa

-- ============================================================
--  NARZĘDZIA (nazwy Tooli) potrzebne do zadań
-- ============================================================

-- false = każde zadanie da się zrobić bez narzędzia (np. gdy gra nie ma tych przedmiotów)
Ustawienia.WymagajNarzedzi = true

Ustawienia.Narzedzia = {
	Pily = { "Piła Łańcuchowa", "K12 Chainsaw" },
	Gasnicze = { "Prądownica", "W-75", "Wytwornica Pianowa", "Cobra", "Tłumica", "Gaśnica" },
	Hydraulika = { "Szczęki Życia:  Nożyce", "Szczęki Życia: Rozrzutniki", "Huligun" },
	Medyczne = { "PSP-R1", "Plecak medyczny" },
	Szyny = { "Szyny Krammera" },
	Sorbent = { "Sorbent" },
	Szczotka = { "Szczotka" },
	Bosak = { "Bosak", "Bosak (krótszy)" },
	Termowizja = { "Kamera Termowizyjna" },
	Tasma = { "Taśma Strażacka", "Pachołki" },
}

-- ============================================================
--  RODZAJE WEZWAŃ
--  Wlaczony = false wyłącza rodzaj. Alarm = rodzaj w systemie alarmowania OSP.
-- ============================================================

Ustawienia.Rodzaje = {
	Drzewo = {
		Wlaczony = true,
		Nazwa = "Powalone drzewo",
		Opis = "Drzewo przewróciło się na jezdnię i blokuje przejazd.",
		Alarm = "Miejscowe zagrożenie",
		Kolor = Color3.fromRGB(245, 165, 70),
	},
	PozarLasu = {
		Wlaczony = true,
		Nazwa = "Pożar lasu",
		Opis = "Pali się poszycie leśne, ogień szybko się rozprzestrzenia.",
		Alarm = "Pożar",
		Kolor = Color3.fromRGB(240, 80, 65),
	},
	PozarBudynku = {
		Wlaczony = true,
		Nazwa = "Pożar budynku",
		Opis = "Pożar w budynku, w środku może być osoba poszkodowana.",
		Alarm = "Pożar",
		Kolor = Color3.fromRGB(240, 80, 65),
	},
	Wypadek = {
		Wlaczony = true,
		Nazwa = "Wypadek drogowy",
		Opis = "Zderzenie samochodu, kierowca zakleszczony w pojeździe.",
		Alarm = "Wypadek drogowy",
		Kolor = Color3.fromRGB(245, 205, 80),
	},
	PozarAuta = {
		Wlaczony = true,
		Nazwa = "Pożar samochodu",
		Opis = "Samochód stoi w płomieniach na poboczu.",
		Alarm = "Pożar",
		Kolor = Color3.fromRGB(240, 80, 65),
	},
	Olej = {
		Wlaczony = true,
		Nazwa = "Plama oleju",
		Opis = "Plama oleju na jezdni, zagrożenie poślizgiem.",
		Alarm = "Miejscowe zagrożenie",
		Kolor = Color3.fromRGB(245, 165, 70),
	},
	Poszkodowany = {
		Wlaczony = true,
		Nazwa = "Osoba poszkodowana",
		Opis = "Osoba leży na chodniku, prawdopodobnie złamana noga.",
		Alarm = "Pomoc medyczna",
		Kolor = Color3.fromRGB(125, 165, 255),
	},
	Kot = {
		Wlaczony = true,
		Nazwa = "Kot na drzewie",
		Opis = "Kot nie może zejść z drzewa, właścicielka prosi o pomoc.",
		Alarm = "Miejscowe zagrożenie",
		Kolor = Color3.fromRGB(245, 165, 70),
	},
	Podtopienie = {
		Wlaczony = true,
		Nazwa = "Podtopienie",
		Opis = "Woda zalała teren przy budynku, trzeba ją wypompować.",
		Alarm = "Miejscowe zagrożenie",
		Kolor = Color3.fromRGB(80, 160, 240),
	},
	Szerszenie = {
		Wlaczony = true,
		Nazwa = "Gniazdo szerszeni",
		Opis = "Gniazdo szerszeni na drzewie przy chodniku.",
		Alarm = "Miejscowe zagrożenie",
		Kolor = Color3.fromRGB(245, 200, 75),
	},
}

-- ============================================================
--  POWIADOMIENIA, DŹWIĘKI, ANIMACJE
-- ============================================================

-- Jeśli w grze jest system alarmowania OSP, wezwanie wysyła też alarm (pager)
Ustawienia.UzyjSystemuAlarmowego = true

-- Licznik wykonanych wezwań w tabeli graczy (leaderstats)
Ustawienia.Statystyki = true
Ustawienia.NazwaStatystyki = "Wezwania"

Ustawienia.DzwiekWezwania = "rbxasset://sounds/electronicpingshort.wav"
Ustawienia.DzwiekOgnia = "" -- trzaskanie ognia (opcjonalnie)
Ustawienia.DzwiekOs = "" -- bzyczenie szerszeni (opcjonalnie)

-- Animacje podczas przytrzymywania E. Domyślnie animacja narzędzia R6 od Roblox.
-- Własne animacje musi wgrać właściciel gry (konto albo grupa gry).
Ustawienia.Animacje = {
	Ciecie = "rbxassetid://129967390",
	Praca = "rbxassetid://129967390",
	Gaszenie = "",
	Pomoc = "",
}

return Ustawienia
