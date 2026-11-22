--[[
	SYSTEM EKWIPUNKU  |  Wspólne funkcje dla serwera i gracza.
	Nie trzeba tu nic zmieniać, ustawienia są w Konfiguracja.
]]

local Konfiguracja = require(script.Parent:WaitForChild("Konfiguracja"))

local Przedmioty = {}

-- Atrybut na Toolu: z jakiej rangi pochodzi przedmiot (nil = zwykły przedmiot)
Przedmioty.ATRYBUT_RANGI = "RangaPrzedmiotu"

-- Atrybut na graczu z jego rangą
Przedmioty.ATRYBUT_GRACZA = "Ranga"

-- Zwraca opis Toola: Atrybut na Toolu > wpis w Konfiguracja > wartość domyślna
function Przedmioty.Dane(narzedzie)
	local wpis = Konfiguracja.Przedmioty[narzedzie.Name] or {}
	local domyslne = Konfiguracja.DomyslnyPrzedmiot

	local function pole(nazwa)
		local atrybut = narzedzie:GetAttribute(nazwa)
		if atrybut ~= nil then
			return atrybut
		end
		if wpis[nazwa] ~= nil then
			return wpis[nazwa]
		end
		return domyslne[nazwa]
	end

	local ikona = pole("Ikona")
	if (ikona == nil or ikona == "") and narzedzie.TextureId ~= "" then
		ikona = narzedzie.TextureId
	end

	return {
		Nazwa = pole("Nazwa") or narzedzie.Name,
		Kategoria = pole("Kategoria") or "Różne",
		Rzadkosc = pole("Rzadkosc") or "Zwykły",
		Waga = tonumber(pole("Waga")) or 0,
		Opis = pole("Opis") or "",
		Ikona = ikona,
		Ranga = narzedzie:GetAttribute(Przedmioty.ATRYBUT_RANGI),
	}
end

function Przedmioty.KolorRzadkosci(rzadkosc)
	return Konfiguracja.Rzadkosci[rzadkosc] or Color3.fromRGB(200, 200, 205)
end

-- Wszystkie Toole gracza: w plecaku i ten trzymany w ręce
function Przedmioty.Narzedzia(gracz)
	local lista = {}
	local plecak = gracz:FindFirstChildOfClass("Backpack")
	if plecak then
		for _, obiekt in plecak:GetChildren() do
			if obiekt:IsA("Tool") then
				table.insert(lista, obiekt)
			end
		end
	end
	local postac = gracz.Character
	if postac then
		for _, obiekt in postac:GetChildren() do
			if obiekt:IsA("Tool") then
				table.insert(lista, obiekt)
			end
		end
	end
	return lista
end

function Przedmioty.LacznaWaga(gracz)
	local suma = 0
	for _, narzedzie in Przedmioty.Narzedzia(gracz) do
		suma += Przedmioty.Dane(narzedzie).Waga
	end
	return suma
end

function Przedmioty.Ranga(gracz)
	return gracz:GetAttribute(Przedmioty.ATRYBUT_GRACZA) or Konfiguracja.DomyslnaRanga
end

return Przedmioty
