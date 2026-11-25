--[[
	SYSTEM EKWIPUNKU POD RP  |  KONFIGURACJA

	Tutaj zmieniasz wszystko: klawisz, rangi, udźwig i opisy przedmiotów.
	Ten plik czyta i serwer, i gracz, więc NIE wpisuj tu nic tajnego.
]]

local Konfiguracja = {}

-- ============================================================
--  OGÓLNE
-- ============================================================

-- Klawisz otwierania i zamykania ekwipunku
Konfiguracja.Klawisz = Enum.KeyCode.X

-- Ranga, z którą startuje każdy nowy gracz
Konfiguracja.DomyslnaRanga = "Obywatel"

-- Maksymalny udźwig w kg. Przedmioty służbowe (z rangi) zawsze się mieszczą.
Konfiguracja.MaksymalnaWaga = 100

-- true = przedmiot podniesiony ponad limit wagi wypada graczowi na ziemię
Konfiguracja.BlokujPrzeciazenie = true

-- true = chowa domyślny pasek Roblox (1-9); przedmioty bierze się tylko z EQ
Konfiguracja.UkryjDomyslnyPlecak = true

-- true = po kliknięciu "Weź do ręki" ekwipunek sam się zamyka
Konfiguracja.ZamykajPoWzieciu = true

-- true = gracz może wyrzucić przedmiot służbowy (dostany z rangi)
Konfiguracja.MoznaWyrzucacSluzbowe = false

-- true = ranga zapisuje się na stałe (DataStore). Gracz przyjęty do OSP
-- będzie w OSP także po ponownym wejściu do gry.
-- W Studio zapis działa tylko po włączeniu: Game Settings > Security >
-- Enable Studio Access to API Services.
Konfiguracja.ZapisujRange = true

-- true = połączenie z drużynami (Teams). Jeśli w grze jest drużyna o nazwie
-- rangi (np. "OSP"), to wejście do niej daje rangę, a zmiana rangi
-- przenosi gracza do tej drużyny.
Konfiguracja.PolaczZDruzynami = true

-- ============================================================
--  ADMINI  (komenda na czacie:  /ranga <nick> <ranga>   np.  /ranga Kamil OSP)
-- ============================================================

-- UserId osób, które mogą nadawać rangi
Konfiguracja.Admini = {
	-- 123456789,
}

-- Jeśli gra należy do grupy: od której rangi w grupie można nadawać rangi
-- (255 = tylko właściciel grupy). Właściciel gry i każdy w Roblox Studio
-- mają dostęp automatycznie.
Konfiguracja.AdminOdRangiWGrupie = 255

-- ============================================================
--  RANGI
--  Przedmioty rangi leżą w:  ServerStorage > EkwipunekPrzedmioty > <nazwa rangi>
--  Nowa ranga = nowy wpis tutaj + nowy folder z Toolami w ServerStorage.
-- ============================================================

Konfiguracja.Rangi = {
	["Obywatel"] = {
		Nazwa = "Obywatel",
		Kolor = Color3.fromRGB(200, 200, 205),
	},
	["OSP"] = {
		Nazwa = "Ochotnicza Straż Pożarna",
		Kolor = Color3.fromRGB(235, 75, 65),
	},
	["PSP"] = {
		Nazwa = "Państwowa Straż Pożarna",
		Kolor = Color3.fromRGB(225, 38, 45),
	},
	["Kierownik PSP"] = {
		Nazwa = "Kierownik PSP",
		Kolor = Color3.fromRGB(245, 200, 75),
	},
}

-- ============================================================
--  WYGLĄD
-- ============================================================

-- Zakładki w ekwipunku (w tej kolejności). "Wszystko" dodaje się samo.
Konfiguracja.Kategorie = { "Gaśnicze", "Ratownicze", "Medyczne", "Sprzęt", "Różne" }

-- Ikona zastępcza, gdy przedmiot nie ma modelu ani własnej ikony
Konfiguracja.IkonyKategorii = {
	["Gaśnicze"] = "🧯",
	["Ratownicze"] = "🪓",
	["Medyczne"] = "🩺",
	["Sprzęt"] = "📻",
	["Różne"] = "📦",
}

-- Rzadkości: kolor paska pod przedmiotem
Konfiguracja.Rzadkosci = {
	["Zwykły"] = Color3.fromRGB(200, 200, 205),
	["Rzadki"] = Color3.fromRGB(125, 165, 255),
	["Epicki"] = Color3.fromRGB(185, 125, 255),
	["Legendarny"] = Color3.fromRGB(245, 200, 75),
}

-- ============================================================
--  PRZEDMIOTY
--  Klucz = DOKŁADNA nazwa Toola (wielkość liter i spacje mają znaczenie).
--    Nazwa     - nazwa wyświetlana w EQ (opcjonalnie)
--    Kategoria - jedna z listy Kategorie
--    Rzadkosc  - jedna z listy Rzadkosci
--    Waga      - w kg
--    Opis      - krótki opis
--    Ikona     - "rbxassetid://..." (opcjonalnie; bez tego EQ pokazuje model 3D)
--  Te same pola można też ustawić jako Attributes na samym Toolu.
-- ============================================================

-- Dane dla przedmiotów, których nie ma na liście
Konfiguracja.DomyslnyPrzedmiot = {
	Kategoria = "Różne",
	Rzadkosc = "Zwykły",
	Waga = 1,
	Opis = "",
}

Konfiguracja.Przedmioty = {
	-- Gaśnicze
	["Prądownica"] = {
		Kategoria = "Gaśnicze", Rzadkosc = "Zwykły", Waga = 2,
		Opis = "Prądownica wodna. Przytrzymaj LPM, aby podawać wodę.",
	},
	["W-75"] = {
		Kategoria = "Gaśnicze", Rzadkosc = "Zwykły", Waga = 3,
		Opis = "Linia gaśnicza W-75 z prądownicą. Przytrzymaj LPM, aby gasić.",
	},
	["Wytwornica Pianowa"] = {
		Kategoria = "Gaśnicze", Rzadkosc = "Rzadki", Waga = 3.5,
		Opis = "Wytwarza pianę gaśniczą. Przytrzymaj LPM, aby gasić.",
	},
	["Cobra"] = {
		Kategoria = "Gaśnicze", Rzadkosc = "Legendarny", Waga = 6,
		Opis = "System cięcia i gaszenia Cobra. Przytrzymaj LPM, aby podawać wodę.",
	},
	["Tłumica"] = {
		Kategoria = "Gaśnicze", Rzadkosc = "Zwykły", Waga = 1.5,
		Opis = "Do gaszenia pożarów traw i ściółki.",
	},

	-- Ratownicze
	["Bosak"] = {
		Kategoria = "Ratownicze", Rzadkosc = "Zwykły", Waga = 3,
		Opis = "Długi bosak do rozbiórki i przeszukiwania pogorzeliska.",
	},
	["Bosak (krótszy)"] = {
		Kategoria = "Ratownicze", Rzadkosc = "Zwykły", Waga = 2,
		Opis = "Krótki bosak, poręczny w ciasnych pomieszczeniach.",
	},
	["Huligun"] = {
		Kategoria = "Ratownicze", Rzadkosc = "Rzadki", Waga = 4,
		Opis = "Narzędzie do wyważania drzwi i okien.",
	},
	["K12 Chainsaw"] = {
		Nazwa = "Przecinarka K12",
		Kategoria = "Ratownicze", Rzadkosc = "Rzadki", Waga = 7,
		Opis = "Przecinarka do betonu i stali.",
	},
	["Piła Łańcuchowa"] = {
		Kategoria = "Ratownicze", Rzadkosc = "Rzadki", Waga = 6,
		Opis = "Piła do cięcia drzew i konstrukcji drewnianych.",
	},
	["Szczęki Życia:  Nożyce"] = {
		Nazwa = "Szczęki Życia: Nożyce",
		Kategoria = "Ratownicze", Rzadkosc = "Epicki", Waga = 8,
		Opis = "Hydrauliczne nożyce do cięcia karoserii przy wypadkach.",
	},
	["Szczęki Życia: Rozrzutniki"] = {
		Kategoria = "Ratownicze", Rzadkosc = "Epicki", Waga = 8.5,
		Opis = "Hydrauliczny rozpieracz do otwierania drzwi i blach.",
	},

	-- Medyczne
	["PSP-R1"] = {
		Kategoria = "Medyczne", Rzadkosc = "Rzadki", Waga = 4,
		Opis = "Torba ratownicza PSP R1 z zestawem pierwszej pomocy.",
	},
	["Plecak medyczny"] = {
		Kategoria = "Medyczne", Rzadkosc = "Rzadki", Waga = 5,
		Opis = "Plecak z wyposażeniem medycznym.",
	},
	["Szyny Krammera"] = {
		Kategoria = "Medyczne", Rzadkosc = "Zwykły", Waga = 1,
		Opis = "Szyny do unieruchamiania złamań.",
	},

	-- Sprzęt
	["Kamera Termowizyjna"] = {
		Kategoria = "Sprzęt", Rzadkosc = "Epicki", Waga = 1.5,
		Opis = "Pomaga znaleźć poszkodowanych i źródło ognia w dymie.",
	},
	["Radio"] = {
		Kategoria = "Sprzęt", Rzadkosc = "Zwykły", Waga = 0.5,
		Opis = "Radiotelefon do łączności z jednostką.",
	},
	["Dron"] = {
		Kategoria = "Sprzęt", Rzadkosc = "Legendarny", Waga = 3,
		Opis = "Dron do rozpoznania terenu z powietrza.",
	},
	["Lizak Drogowy"] = {
		Kategoria = "Sprzęt", Rzadkosc = "Zwykły", Waga = 0.5,
		Opis = "Tarcza do kierowania ruchem. Wskazuje tam, gdzie celujesz myszką.",
	},
	["Pachołki"] = {
		Kategoria = "Sprzęt", Rzadkosc = "Zwykły", Waga = 3,
		Opis = "Do zabezpieczenia miejsca zdarzenia. Naciśnij T, aby postawić.",
	},
	["Taśma Strażacka"] = {
		Kategoria = "Sprzęt", Rzadkosc = "Zwykły", Waga = 0.5,
		Opis = "Taśma do wygradzania terenu. Klikaj, aby rozciągać; R cofa.",
	},

	-- Różne
	["Citation Book"] = {
		Nazwa = "Bloczek mandatowy",
		Kategoria = "Różne", Rzadkosc = "Zwykły", Waga = 0.5,
		Opis = "Do wystawiania mandatów.",
	},
	["MRE"] = {
		Kategoria = "Różne", Rzadkosc = "Zwykły", Waga = 0.8,
		Opis = "Racja żywnościowa. Kliknij, aby zjeść.",
	},
	["Sorbent"] = {
		Kategoria = "Różne", Rzadkosc = "Zwykły", Waga = 2,
		Opis = "Do zbierania plam oleju i paliwa z jezdni.",
	},
	["Szczotka"] = {
		Kategoria = "Różne", Rzadkosc = "Zwykły", Waga = 1.5,
		Opis = "Do sprzątania jezdni po zdarzeniu.",
	},

	-- PSP
	["Tablet PSP"] = {
		Kategoria = "Sprzęt", Rzadkosc = "Epicki", Waga = 0.8,
		Opis = "Tablet kierownika: bramy, syrena, załoga, pojazdy i komunikaty.",
	},
}

return Konfiguracja
