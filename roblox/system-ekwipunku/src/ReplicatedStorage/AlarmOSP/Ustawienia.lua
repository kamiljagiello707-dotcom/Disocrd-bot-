--[[
	SYSTEM ALARMOWANIA OSP  |  USTAWIENIA
	Czyta je serwer i gracz, więc NIE wpisuj tu nic tajnego.

	Obiekty na mapie oznacza się Tagami (Properties > Tags > +):
		AlarmKomputer     - część, przy której otwiera się panel (np. monitor w remizie)
		AlarmSyrena       - część z syreną (dźwięk z DzwiekSyreny albo Sound w środku)
		AlarmSwiatlo      - lampa, która miga podczas alarmu (np. Neon + PointLight)
		MiejsceZdarzenia  - punkt na mapie do wyboru w panelu; nazwa części = adres
	Syrena i lampy z atrybutem "Jednostka" działają tylko dla tej jednostki;
	bez atrybutu działają przy każdym alarmie.
]]

local Ustawienia = {}

-- ============================================================
--  JEDNOSTKI
--  Członek jednostki = gracz z atrybutem Ranga równym "Ranga"
--  (z systemu ekwipunku) albo w drużynie (Team) o nazwie "Druzyna".
-- ============================================================

Ustawienia.Jednostki = {
	{ Nazwa = "OSP", Ranga = "OSP", Druzyna = "OSP" },
	-- { Nazwa = "OSP Górne", Ranga = "OSP Górne", Druzyna = "OSP Górne" },
}

-- ============================================================
--  KTO MOŻE ALARMOWAĆ (otwierać panel)
--  Właściciel gry i każdy w Roblox Studio mogą zawsze.
-- ============================================================

Ustawienia.Dyspozytorzy = {
	Rangi = { "OSP" }, -- atrybut Ranga
	Druzyny = { "Dyspozytor" }, -- nazwy drużyn (Team)
	Gracze = {}, -- UserId
}
-- Gra w grupie: od której rangi w grupie można alarmować (255 = właściciel)
Ustawienia.DyspozytorOdRangiWGrupie = 255

-- Komenda na czacie, która otwiera panel bez komputera ("" = wyłączona)
Ustawienia.Komenda = "/alarm"

-- ============================================================
--  RODZAJE ZDARZEŃ
-- ============================================================

Ustawienia.Rodzaje = {
	{ Nazwa = "Pożar", Ikona = "🔥", Kolor = Color3.fromRGB(240, 80, 65) },
	{ Nazwa = "Miejscowe zagrożenie", Ikona = "⚠️", Kolor = Color3.fromRGB(245, 165, 70) },
	{ Nazwa = "Wypadek drogowy", Ikona = "🚗", Kolor = Color3.fromRGB(245, 205, 80) },
	{ Nazwa = "Pomoc medyczna", Ikona = "🩺", Kolor = Color3.fromRGB(125, 165, 255) },
	{ Nazwa = "Poszukiwania", Ikona = "🔦", Kolor = Color3.fromRGB(185, 125, 255) },
	{ Nazwa = "Ćwiczenia", Ikona = "🎓", Kolor = Color3.fromRGB(200, 200, 205) },
}

-- ============================================================
--  DŹWIĘKI  ("" = brak)
--  Syrenę wybierz w Toolbox > Audio (wpisz "fire siren"), skopiuj ID.
-- ============================================================

Ustawienia.DzwiekSyreny = "" -- np. "rbxassetid://1234567890"
Ustawienia.GlosnoscSyreny = 1
Ustawienia.ZasiegSyreny = 450 -- w studach

Ustawienia.DzwiekPagera = "rbxasset://sounds/electronicpingshort.wav"
Ustawienia.GlosnoscPagera = 0.8
Ustawienia.IlePiskowPagera = 6 -- pager piszczy, dopóki gracz nie odpowie (najwyżej tyle razy)

-- ============================================================
--  CZASY I LIMITY
-- ============================================================

Ustawienia.CzasSyreny = 40 -- sekundy wycia syreny i migania lamp
Ustawienia.CzasAlarmu = 900 -- po tylu sekundach alarm kończy się sam
Ustawienia.OdstepMiedzyAlarmami = 15 -- ile sekund musi minąć między alarmami jednego dyspozytora
Ustawienia.MaksDlugoscOpisu = 120 -- znaków
Ustawienia.OdlegloscNaMiejscu = 25 -- studów: tak blisko = "jesteś na miejscu"

return Ustawienia
