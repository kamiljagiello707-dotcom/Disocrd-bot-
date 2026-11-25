--[[
	MENTOR  |  USTAWIENIA
	Tu zmieniasz wszystko, co mówi mentor. Kodu nie trzeba ruszać.

	GDZIE STOI MENTOR
		Postaw na ziemi dowolną część i dodaj jej Tag "Mentor"
		(Properties > Tags > +). Mentor stanie w jej miejscu przodem tam,
		gdzie jest przód części (Front). Część sama zrobi się niewidzialna.

	PRZYCISK "POKAŻ DROGĘ"
		Postaw część w miejscu docelowym (np. przed remizą) i dodaj jej Tag "MentorCel".
		Nazwa tej części = to, co wpisujesz w Cel (np. "Remiza OSP").

	W TEKSTACH MOŻESZ UŻYĆ
		{nick}    - nazwa gracza
		{serwer}  - NazwaSerwera (niżej)
]]

local Ustawienia = {}

-- ============================================================
--  MENTOR
-- ============================================================

Ustawienia.Imie = "Mentor Janusz"
Ustawienia.Tytul = "Pomoc dla nowych graczy"
Ustawienia.NazwaSerwera = "Miasto RP"

-- Wygląd mentora: UserId gracza, którego awatar ma mieć mentor.
-- 0 = właściciel gry (działa po opublikowaniu gry), w Studio zwykły wygląd.
Ustawienia.WygladZGracza = 0

-- Kolor dodatków (numery kroków, przycisk drogi, wykrzyknik)
Ustawienia.KolorAkcentu = Color3.fromRGB(235, 75, 65)

-- ============================================================
--  ZACHOWANIE
-- ============================================================

Ustawienia.KameraNaMentora = true -- kamera pokazuje mentora, gdy menu jest otwarte
Ustawienia.ZablokujRuch = true -- postać stoi w miejscu, gdy menu jest otwarte
Ustawienia.ProwadzNowych = true -- nowi gracze widzą słup światła nad mentorem i podpowiedź
Ustawienia.ZapamietajGraczy = true -- DataStore: kto już rozmawiał z mentorem (bez tego "nowy" = każde wejście)
Ustawienia.PozwolNaTeleport = false -- true = obok "Pokaż drogę" jest też "Zabierz mnie tam"
Ustawienia.SzybkoscPisania = 60 -- liter na sekundę w dymku mentora

-- ============================================================
--  ROZDZIAŁY
--  Każdy rozdział to:
--    Ikona    - emoji przy tytule
--    Tytul    - nazwa rozdziału
--    Tekst    - co mentor mówi w dymku
--    Kroki    - lista punktów (dowolnie wiele)
--    Przycisk - (opcjonalnie) { Tekst = "...", Cel = "nazwa części z Tagiem MentorCel" }
--  Kolejność na liście = kolejność w menu. Rozdziały można dodawać i usuwać.
-- ============================================================

Ustawienia.Rozdzialy = {
	{
		Ikona = "👋",
		Tytul = "Witaj w {serwer}",
		Tekst = "Cześć, {nick}! Jestem tu, żeby pomóc Ci zacząć. Przejdź ze mną przez kilka krótkich rozdziałów, a będziesz wiedzieć wszystko, co trzeba.",
		Kroki = {
			"{serwer} to serwer Roleplay: grasz postacią i zachowujesz się tak, jak ona w prawdziwym życiu.",
			"Na start jesteś Obywatelem. Możesz zwiedzać miasto, poznawać ludzi i wybrać pracę.",
			"Klikaj Dalej, żeby przejść do kolejnego rozdziału. Do mnie możesz wrócić w każdej chwili.",
		},
	},
	{
		Ikona = "📜",
		Tytul = "Zasady RP",
		Tekst = "Zanim zaczniesz, kilka zasad. Dzięki nim gra jest fajna dla wszystkich.",
		Kroki = {
			"Nie wychodź z postaci. Sprawy spoza gry załatwiaj na czacie OOC albo na Discordzie.",
			"Nie atakuj innych bez powodu w RP (RDM) i nie rozjeżdżaj ich autem (VDM).",
			"Dbaj o życie postaci (FearRP): gdy ktoś Ci grozi, reaguj realistycznie.",
			"Nie korzystaj z wiedzy spoza gry (metagaming), np. ze streamów innych graczy.",
			"Pełne zasady znajdziesz na Discordzie serwera.",
		},
	},
	{
		Ikona = "💼",
		Tytul = "Jak zacząć pracę",
		Tekst = "Praca daje Ci zajęcie, sprzęt i ludzi do wspólnej gry. Oto jak ją zdobyć.",
		Kroki = {
			"Wybierz służbę albo pracę, która Ci się podoba.",
			"Zgłoś się na miejsce pracy, np. do remizy OSP.",
			"Po przyjęciu dostaniesz rangę, a sprzęt pojawi się w Twoim ekwipunku.",
			"Słuchaj starszych stopniem i ucz się od nich.",
		},
	},
	{
		Ikona = "🚒",
		Tytul = "Ochotnicza Straż Pożarna",
		Tekst = "OSP gasi pożary, pomaga przy wypadkach i ratuje ludzi. Zobacz, jak wygląda służba.",
		Kroki = {
			"Do OSP dołączysz w remizie. Kliknij niżej, a pokażę Ci drogę.",
			"Po dołączeniu otwórz ekwipunek klawiszem X: znajdziesz tam cały sprzęt strażaka.",
			"Gdy przyjdzie alarm, na ekranie pojawi się pager. Kliknij JADĘ, a słup światła pokaże miejsce zdarzenia.",
			"Dyspozytor wysyła alarmy z komputera w remizie albo komendą /alarm.",
		},
		Przycisk = { Tekst = "Pokaż drogę do remizy", Cel = "Remiza OSP" },
	},
	{
		Ikona = "🎮",
		Tytul = "Sterowanie",
		Tekst = "Najważniejsze klawisze, które przydadzą Ci się od razu.",
		Kroki = {
			"X otwiera ekwipunek. Podwójne kliknięcie przedmiotu bierze go do ręki.",
			"E służy do rozmowy i interakcji, np. ze mną albo z komputerem w remizie.",
			"Na telefonie ekwipunek otwiera przycisk z plecakiem po lewej stronie ekranu.",
		},
	},
	{
		Ikona = "❓",
		Tytul = "Pomoc",
		Tekst = "To już wszystko na start. Gdybyś czegoś nie wiedział, wiesz, gdzie mnie znaleźć.",
		Kroki = {
			"Pytaj innych graczy na czacie, większość chętnie pomoże.",
			"Problem z innym graczem zgłoś administracji na Discordzie.",
			"Wróć do mnie w każdej chwili, jeśli chcesz coś sobie przypomnieć.",
			"Powodzenia, {nick}!",
		},
	},
}

return Ustawienia
