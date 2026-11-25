--[[
	SYSTEM WEZWAŃ 112  |  Rodzaje wezwań
	Każdy rodzaj buduje rekwizyty i listę zadań. Nazwy, opisy i włączanie
	rodzajów są w ReplicatedStorage > Wezwania112 > Ustawienia.

	Narzedzia = nazwa grupy z Ustawienia.Narzedzia (nil = bez narzędzia).
	Wymaga = Id zadania, które musi być skończone wcześniej.
]]

local Rodzaje = {}

function Rodzaje.Zarejestruj(Wezwania, Rekwizyty)
	local zniknij = Wezwania.Zniknij

	local function wokol(w, ile, promienMin, promienMax, wysokoscMin, wysokoscMax)
		local punkty = {}
		for indeks = 1, ile do
			local kat = (indeks / ile) * math.pi * 2 + math.random() * 0.6
			local promien = math.random(promienMin * 10, promienMax * 10) / 10
			local wysokosc = math.random(wysokoscMin * 10, wysokoscMax * 10) / 10
			table.insert(punkty, w:Pozycja(math.cos(kat) * promien, wysokosc, math.sin(kat) * promien).Position)
		end
		return punkty
	end

	-- 1. Powalone drzewo na drodze
	Wezwania.Zarejestruj("Drzewo", function(w)
		local segmenty, galezie = Rekwizyty.PowaloneDrzewo(w)
		w:Zadanie({
			Id = "Ciecie",
			Opis = "Przetnij pień piłą",
			Cele = segmenty,
			Narzedzia = "Pily",
			Czas = 4,
			Akcja = "Przetnij pień",
			Animacja = "Ciecie",
		})
		w:Zadanie({
			Id = "Galezie",
			Opis = "Usuń gałęzie z jezdni",
			Cele = galezie,
			Czas = 2,
			Akcja = "Usuń gałęzie",
			Zasieg = 12,
		})
	end)

	-- 2. Pożar lasu (ogień się rozprzestrzenia)
	Wezwania.Zarejestruj("PozarLasu", function(w)
		local punkty = wokol(w, 5, 4, 16, 0, 0)
		local kolumna = Rekwizyty.KolumnaDymu(w.Model, w:Pozycja(0, 12, 0))
		w:Ogien({
			Id = "Ogien",
			Opis = "Ugaś pożar lasu",
			Punkty = punkty,
			Wielkosc = 1.1,
			Rozprzestrzenianie = { Co = 25, Maks = 14, Promien = 32 },
			PoZakonczeniu = function()
				zniknij(kolumna, 3)
			end,
		})
		local zar = w:Zadanie({
			Id = "Zar",
			Opis = "Dogaś tlące się pogorzelisko",
			Narzedzia = "Gasnicze",
			Wymaga = "Ogien",
			Czas = 3,
			Akcja = "Dogaś",
			Animacja = "Gaszenie",
			Zasieg = 12,
		})
		zar.PoAktywacji = function()
			for indeks = 1, 3 do
				w:DodajCel(zar, Rekwizyty.Zar(w.Model, w:NaZiemie(punkty[indeks])))
			end
		end
	end)

	-- 3. Pożar budynku z poszkodowanym
	Wezwania.Zarejestruj("PozarBudynku", function(w)
		w:Ogien({
			Id = "Ogien",
			Opis = "Ugaś pożar budynku",
			Punkty = wokol(w, 6, 3, 10, 2, 9),
			NaZiemi = false,
			Rozprzestrzenianie = { Co = 35, Maks = 9, Promien = 14 },
		})
		local osoba, korzen = Rekwizyty.Osoba(w.Model, w:Pozycja(0, 0, 0), "lezy", "Poszkodowany")
		w:Zadanie({
			Id = "Ewakuacja",
			Opis = "Wynieś poszkodowanego z budynku",
			Cele = { korzen },
			Czas = 4,
			Akcja = "Wynieś poszkodowanego",
			PoCelu = function()
				osoba:PivotTo(w:Pozycja(0, 0.6, -20) * CFrame.Angles(math.rad(-90), 0, 0))
			end,
		})
		w:Zadanie({
			Id = "Pomoc",
			Opis = "Udziel pierwszej pomocy",
			Cele = { korzen },
			Narzedzia = "Medyczne",
			Czas = 5,
			Akcja = "Udziel pomocy",
			Animacja = "Pomoc",
			Wymaga = "Ewakuacja",
			PoCelu = function()
				task.wait(1.5)
				zniknij(osoba)
			end,
		})
	end)

	-- 4. Wypadek drogowy z zakleszczonym kierowcą
	Wezwania.Zarejestruj("Wypadek", function(w)
		local auto = Rekwizyty.Samochod(w.Model, w:Pozycja(0, 0, 0) * CFrame.Angles(0, math.rad(18), 0), true)
		local plama = Rekwizyty.Plama(w.Model, w:Pozycja(1, 0, -8), 5)
		w:Zadanie({
			Id = "Akumulator",
			Opis = "Odłącz akumulator",
			Cele = { auto.Maska },
			Czas = 2.5,
			Akcja = "Odłącz akumulator",
			PoCelu = function()
				local dym = auto.Maska:FindFirstChildOfClass("Smoke")
				if dym then
					dym.Enabled = false
				end
			end,
		})
		w:Zadanie({
			Id = "Drzwi",
			Opis = "Wytnij drzwi narzędziem hydraulicznym",
			Cele = { auto.Drzwi },
			Narzedzia = "Hydraulika",
			Czas = 6,
			Akcja = "Wytnij drzwi",
			Animacja = "Ciecie",
			Wymaga = "Akumulator",
		})
		local pomoc = w:Zadanie({
			Id = "Pomoc",
			Opis = "Wyciągnij kierowcę i udziel pomocy",
			Narzedzia = "Medyczne",
			Czas = 5,
			Akcja = "Udziel pomocy",
			Animacja = "Pomoc",
			Wymaga = "Drzwi",
		})
		pomoc.PoAktywacji = function()
			local osoba, korzen = Rekwizyty.Osoba(w.Model, w:Pozycja(-7, 0, 1) * CFrame.Angles(0, math.rad(90), 0), "lezy", "Kierowca")
			pomoc.PoCelu = function()
				task.wait(1.5)
				zniknij(osoba)
			end
			w:DodajCel(pomoc, korzen)
		end
		w:Zadanie({
			Id = "Plama",
			Opis = "Posyp wyciek sorbentem",
			Cele = { plama },
			Narzedzia = "Sorbent",
			Czas = 3,
			Akcja = "Posyp sorbentem",
		})
	end)

	-- 5. Pożar samochodu
	Wezwania.Zarejestruj("PozarAuta", function(w)
		local auto = Rekwizyty.Samochod(w.Model, w:Pozycja(0, 0, 0), false)
		w:Ogien({
			Id = "Ogien",
			Opis = "Ugaś płonący samochód",
			Punkty = {
				auto.Maska.Position + Vector3.new(0, 1, 0),
				auto.Kabina.Position + Vector3.new(0, 1, 0),
				(auto.Nadwozie.CFrame * CFrame.new(0, 1.5, 4.5)).Position,
			},
			NaZiemi = false,
			Wielkosc = 0.8,
			PoZakonczeniu = function()
				Rekwizyty.Zwegl(auto.Model)
			end,
		})
		w:Zadanie({
			Id = "Kontrola",
			Opis = "Sprawdź pojazd kamerą termowizyjną",
			Cele = { auto.Nadwozie },
			Narzedzia = "Termowizja",
			Czas = 3,
			Akcja = "Sprawdź temperaturę",
			Wymaga = "Ogien",
			Zasieg = 12,
			PoCelu = function() end,
		})
	end)

	-- 6. Plama oleju: sorbent, potem szczotka
	Wezwania.Zarejestruj("Olej", function(w)
		local plamy = {}
		for _, dane in { { -4, 0, 7 }, { 3, 3, 6 }, { 0, -5, 5 } } do
			table.insert(plamy, Rekwizyty.Plama(w.Model, w:Pozycja(dane[1], 0, dane[2]), dane[3]))
		end
		w:Zadanie({
			Id = "Sorbent",
			Opis = "Posyp plamy sorbentem",
			Cele = plamy,
			Narzedzia = "Sorbent",
			Czas = 3,
			Akcja = "Posyp sorbentem",
			Zasieg = 9,
			PoCelu = function(plama)
				plama.Color = Color3.fromRGB(200, 180, 140)
				plama.Material = Enum.Material.Sand
				plama.Reflectance = 0
			end,
		})
		w:Zadanie({
			Id = "Sprzatanie",
			Opis = "Zamieć sorbent z jezdni",
			Cele = plamy,
			Narzedzia = "Szczotka",
			Czas = 3,
			Akcja = "Zamieć",
			Wymaga = "Sorbent",
			Zasieg = 9,
		})
	end)

	-- 7. Osoba poszkodowana
	Wezwania.Zarejestruj("Poszkodowany", function(w)
		local osoba, korzen = Rekwizyty.Osoba(w.Model, w:Pozycja(0, 0, 0) * CFrame.Angles(0, math.rad(70), 0), "lezy", "Poszkodowany")
		Rekwizyty.Osoba(w.Model, w:Pozycja(3, 0, -3) * CFrame.Angles(0, math.rad(200), 0), "stoi", "Świadek")
		w:Zadanie({
			Id = "Pomoc",
			Opis = "Oceń stan i udziel pierwszej pomocy",
			Cele = { korzen },
			Narzedzia = "Medyczne",
			Czas = 5,
			Akcja = "Udziel pomocy",
			Animacja = "Pomoc",
			PoCelu = function() end,
		})
		w:Zadanie({
			Id = "Szyny",
			Opis = "Unieruchom złamaną nogę",
			Cele = { korzen },
			Narzedzia = "Szyny",
			Czas = 4,
			Akcja = "Załóż szyny",
			Animacja = "Pomoc",
			Wymaga = "Pomoc",
			PoCelu = function()
				task.wait(1.5)
				zniknij(osoba)
			end,
		})
	end)

	-- 8. Kot na drzewie
	Wezwania.Zarejestruj("Kot", function(w)
		local drzewo = Rekwizyty.DrzewoStojace(w.Model, w:Pozycja(0, 0, 0))
		local kot = Rekwizyty.Kot(w.Model, drzewo.Galaz)
		local _, korzenWlascicielki = Rekwizyty.Osoba(w.Model, w:Pozycja(0, 0, -9) * CFrame.Angles(0, math.pi, 0), "stoi", "Właścicielka")
		w:Zadanie({
			Id = "Zdejmij",
			Opis = "Zdejmij kota z drzewa",
			Cele = { drzewo.Podstawa },
			Czas = 5,
			Akcja = "Wejdź po kota",
			Zasieg = 9,
			PoCelu = function()
				Rekwizyty.Przesun(kot, w:Pozycja(1.5, 0.5, -6.5), 1.5)
			end,
		})
		w:Zadanie({
			Id = "Oddaj",
			Opis = "Oddaj kota właścicielce",
			Cele = { korzenWlascicielki },
			Czas = 1.5,
			Akcja = "Oddaj kota",
			Wymaga = "Zdejmij",
			PoCelu = function()
				zniknij(kot)
			end,
		})
	end)

	-- 9. Podtopienie: prąd, pompa, woda opada sama
	Wezwania.Zarejestruj("Podtopienie", function(w)
		local woda, pompa, skrzynka = Rekwizyty.Podtopienie(w)
		w:Zadanie({
			Id = "Prad",
			Opis = "Odłącz zasilanie budynku",
			Cele = { skrzynka },
			Czas = 2.5,
			Akcja = "Odłącz prąd",
			PoCelu = function()
				skrzynka.Color = Color3.fromRGB(80, 80, 85)
			end,
		})
		w:Zadanie({
			Id = "Pompa",
			Opis = "Uruchom pompę",
			Cele = { pompa },
			Czas = 3,
			Akcja = "Uruchom pompę",
			Wymaga = "Prad",
			PoCelu = function() end,
		})
		local wypompowanie = w:Zadanie({ Id = "Woda", Opis = "Wypompuj wodę", Typ = "Auto", Wymaga = "Pompa" })
		wypompowanie.PoAktywacji = function()
			local poczatek, czas = os.clock(), 25
			local rozmiar, polozenie = woda.Size, woda.CFrame
			while not w.Zakonczone do
				local postep = math.clamp((os.clock() - poczatek) / czas, 0, 1)
				local wysokosc = math.max(0.05, rozmiar.Y * (1 - postep))
				woda.Size = Vector3.new(rozmiar.X, wysokosc, rozmiar.Z)
				woda.CFrame = polozenie * CFrame.new(0, -(rozmiar.Y - wysokosc) / 2, 0)
				woda.Transparency = 0.35 + 0.6 * postep
				w:Postep(wypompowanie, postep)
				if postep >= 1 then
					break
				end
				task.wait(0.5)
			end
			if not w.Zakonczone then
				woda:Destroy()
				w:Wykonaj(wypompowanie)
			end
		end
	end)

	-- 10. Gniazdo szerszeni
	Wezwania.Zarejestruj("Szerszenie", function(w)
		local drzewo = Rekwizyty.DrzewoStojace(w.Model, w:Pozycja(0, 0, 0))
		local gniazdo = Rekwizyty.Gniazdo(w.Model, drzewo.Galaz * CFrame.new(0, -1.8, 0))
		local slupki = Rekwizyty.Slupki(w, { { -6, -7 }, { 6, -7 } })
		w:Zadanie({
			Id = "Tasma",
			Opis = "Odgrodź teren taśmą",
			Cele = slupki,
			Narzedzia = "Tasma",
			Czas = 2,
			Akcja = "Rozciągnij taśmę",
			PoCelu = function(slupek)
				slupek.Color = Color3.fromRGB(220, 40, 40)
			end,
			PoZakonczeniu = function()
				Rekwizyty.Czesc(w.Model, "Tasma", Vector3.new(12, 0.15, 0.05), w:Pozycja(0, 3, -7), Color3.fromRGB(220, 40, 40), Enum.Material.Neon, {
					CanCollide = false,
				})
			end,
		})
		w:Zadanie({
			Id = "Gniazdo",
			Opis = "Zdejmij gniazdo bosakiem",
			Cele = { gniazdo },
			Narzedzia = "Bosak",
			Czas = 5,
			Akcja = "Zdejmij gniazdo",
			Animacja = "Ciecie",
			Zasieg = 16,
			PoCelu = function()
				local osy = gniazdo:FindFirstChildOfClass("ParticleEmitter")
				if osy then
					osy.Enabled = false
				end
				Rekwizyty.Przesun(gniazdo, gniazdo.CFrame - Vector3.new(0, gniazdo.Position.Y - w.CF.Position.Y - 1.3, 0), 0.6)
				task.wait(1.5)
				zniknij(gniazdo)
			end,
		})
	end)
end

return Rodzaje
