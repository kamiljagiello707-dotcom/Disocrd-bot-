--[[
	TABLET KIEROWNIKA PSP  |  USTAWIENIA
	Czyta je serwer i gracz, więc NIE wpisuj tu nic tajnego.

	TABLET
		To zwykły Tool o nazwie z NazwaTabletu (niżej). Po wzięciu do ręki
		otwiera się tablet. Daj go kierownikowi przez ekwipunek (folder rangi
		"Kierownik PSP" w ServerStorage > EkwipunekPrzedmioty) albo StarterPack.

	OBIEKTY NA MAPIE (Properties > Tags > +)
		BramaPSP           - brama garażowa (Part albo Model, Anchored). Otwiera się do góry.
		                     Atrybuty (opcjonalne): Przesuniecie (Vector3), CzasOtwierania (liczba)
		SwiatloPSP         - lampa w remizie. Atrybut "Strefa" (np. "Garaż") grupuje lampy.
		AlarmSyrena        - syrena (ten sam Tag co w systemie alarmowania OSP)
		AlarmSwiatlo       - lampa ostrzegawcza, miga razem z syreną
		MiejscePojazduPSP  - miejsce, na którym staje wóz wyjeżdżający z garażu
	POJAZDY
		Modele wozów wrzuć do ServerStorage > PojazdyPSP.
]]

local Ustawienia = {}

-- ============================================================
--  WYGLĄD
-- ============================================================

Ustawienia.NazwaTabletu = "Tablet PSP" -- nazwa Toola
Ustawienia.NazwaJednostki = "JRG 1 Miasto"
Ustawienia.Napis = "ZARZĄDZAJ SWOJĄ PSP"

-- Logo na Stronie Głównej: "rbxassetid://..." albo "" (wbudowany znak PSP)
Ustawienia.Logo = ""

Ustawienia.KolorAkcentu = Color3.fromRGB(225, 38, 45)

-- ============================================================
--  KTO MOŻE UŻYWAĆ TABLETU
--  Właściciel gry i każdy w Roblox Studio mogą zawsze.
-- ============================================================

Ustawienia.Kierownicy = {
	Rangi = { "Kierownik PSP" }, -- atrybut Ranga (z systemu ekwipunku)
	Druzyny = {}, -- nazwy drużyn (Team)
	Gracze = {}, -- UserId
}
Ustawienia.KierownikOdRangiWGrupie = 255

-- ============================================================
--  ZAŁOGA
-- ============================================================

-- Kto należy do załogi PSP (ranga albo drużyna)
Ustawienia.RangiZalogi = { "PSP", "Kierownik PSP" }
Ustawienia.DruzynyZalogi = { "PSP" }

Ustawienia.RangaPoPrzyjeciu = "PSP" -- jaką rangę dostaje przyjęty gracz
Ustawienia.RangaPoZwolnieniu = "Obywatel" -- jaką rangę dostaje zwolniony gracz
Ustawienia.ZasiegPrzyjecia = 40 -- w studach: kogo kierownik widzi "w pobliżu"

-- ============================================================
--  BRAMY, SYRENA, POJAZDY, KOMUNIKATY
-- ============================================================

Ustawienia.CzasOtwieraniaBramy = 3 -- sekundy
Ustawienia.DzwiekBramy = "" -- "rbxassetid://..." (opcjonalnie)

Ustawienia.DzwiekSyreny = "" -- "rbxassetid://..." albo Sound w części z Tagiem AlarmSyrena
Ustawienia.CzasSyreny = 40 -- sekundy

Ustawienia.OdstepPojazdow = 10 -- ile sekund między wyjazdami jednego gracza

Ustawienia.MaksDlugoscKomunikatu = 150 -- znaków
Ustawienia.OdstepKomunikatow = 20 -- sekundy

-- ============================================================
--  APLIKACJE NA TABLECIE
--  Kolejność = kolejność kafelków. Wlaczona = false chowa aplikację.
--  Id musi zostać takie jak jest, Nazwa i Opis możesz zmieniać.
-- ============================================================

Ustawienia.Aplikacje = {
	{ Id = "Bramy", Nazwa = "Bramy", Wlaczona = true },
	{ Id = "Syrena", Nazwa = "Syrena", Wlaczona = true },
	{ Id = "Zaloga", Nazwa = "Załoga", Wlaczona = true },
	{ Id = "Pojazdy", Nazwa = "Pojazdy", Wlaczona = true },
	{ Id = "Komunikaty", Nazwa = "Komunikaty", Wlaczona = true },
	{ Id = "Oswietlenie", Nazwa = "Oświetlenie", Wlaczona = true },
}

return Ustawienia
