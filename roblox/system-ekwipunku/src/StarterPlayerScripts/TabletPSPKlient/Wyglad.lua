--[[
	TABLET KIEROWNIKA PSP  |  Wygląd (te same kolory i czcionka co w ekwipunku)
]]

local TweenService = game:GetService("TweenService")

local Wyglad = {}

Wyglad.KOLOR = {
	Okno = Color3.fromRGB(24, 24, 27),
	Panel = Color3.fromRGB(33, 33, 37),
	Slot = Color3.fromRGB(43, 43, 48),
	SlotNajechany = Color3.fromRGB(54, 54, 60),
	SlotWybrany = Color3.fromRGB(70, 70, 78),
	Bialy = Color3.fromRGB(245, 245, 247),
	Tekst = Color3.fromRGB(236, 236, 240),
	TekstSlaby = Color3.fromRGB(150, 150, 160),
	TekstCiemny = Color3.fromRGB(22, 22, 25),
	Zielony = Color3.fromRGB(95, 215, 135),
	Czerwony = Color3.fromRGB(240, 80, 65),
}

Wyglad.SZYBKO = TweenInfo.new(0.12, Enum.EasingStyle.Quad, Enum.EasingDirection.Out)

local CZCIONKA = "rbxasset://fonts/families/GothamSSm.json"

function Wyglad.czcionka(waga)
	return Font.new(CZCIONKA, waga or Enum.FontWeight.Regular)
end

function Wyglad.nowy(klasa, wlasciwosci)
	local obiekt = Instance.new(klasa)
	local rodzic
	for klucz, wartosc in wlasciwosci or {} do
		if klucz == "Parent" then
			rodzic = wartosc
		else
			obiekt[klucz] = wartosc
		end
	end
	if rodzic then
		obiekt.Parent = rodzic
	end
	return obiekt
end

function Wyglad.zaokraglij(rodzic, promien)
	return Wyglad.nowy("UICorner", { CornerRadius = UDim.new(0, promien), Parent = rodzic })
end

function Wyglad.obrysuj(rodzic, przezroczystosc, kolor)
	return Wyglad.nowy("UIStroke", {
		Color = kolor or Color3.new(1, 1, 1),
		Transparency = przezroczystosc or 0.9,
		Thickness = 1,
		ApplyStrokeMode = Enum.ApplyStrokeMode.Border,
		Parent = rodzic,
	})
end

function Wyglad.napis(wlasciwosci)
	local pelne = {
		BackgroundTransparency = 1,
		TextColor3 = Wyglad.KOLOR.Tekst,
		FontFace = Wyglad.czcionka(),
		TextSize = 14,
		TextXAlignment = Enum.TextXAlignment.Left,
	}
	for klucz, wartosc in wlasciwosci do
		pelne[klucz] = wartosc
	end
	return Wyglad.nowy("TextLabel", pelne)
end

function Wyglad.przycisk(wlasciwosci)
	local pelne = {
		AutoButtonColor = false,
		BorderSizePixel = 0,
		FontFace = Wyglad.czcionka(Enum.FontWeight.Bold),
		TextSize = 14,
		TextColor3 = Wyglad.KOLOR.Tekst,
		BackgroundColor3 = Wyglad.KOLOR.Slot,
	}
	for klucz, wartosc in wlasciwosci do
		pelne[klucz] = wartosc
	end
	return Wyglad.nowy("TextButton", pelne)
end

function Wyglad.animuj(obiekt, wlasciwosci, info)
	local tween = TweenService:Create(obiekt, info or Wyglad.SZYBKO, wlasciwosci)
	tween:Play()
	return tween
end

-- Podświetlenie przycisku po najechaniu myszką
function Wyglad.najechanie(guzik, czyAktywny)
	guzik.MouseEnter:Connect(function()
		if czyAktywny == nil or czyAktywny() then
			local bazowy = guzik:GetAttribute("KolorBazowy") or guzik.BackgroundColor3
			guzik:SetAttribute("KolorBazowy", bazowy)
			Wyglad.animuj(guzik, { BackgroundColor3 = bazowy:Lerp(Color3.new(0.55, 0.55, 0.55), 0.2) })
		end
	end)
	guzik.MouseLeave:Connect(function()
		local bazowy = guzik:GetAttribute("KolorBazowy")
		if bazowy then
			Wyglad.animuj(guzik, { BackgroundColor3 = bazowy })
		end
	end)
end

-- Ustawia kolor przycisku (także ten, do którego wraca po zjechaniu myszką)
function Wyglad.kolorPrzycisku(guzik, kolor)
	guzik:SetAttribute("KolorBazowy", kolor)
	Wyglad.animuj(guzik, { BackgroundColor3 = kolor })
end

-- 1 stud = 0,28 m
function Wyglad.metry(studs)
	local metry = studs * 0.28
	if metry >= 1000 then
		return ("%.1f km"):format(metry / 1000)
	end
	return ("%d m"):format(math.floor(metry + 0.5))
end

return Wyglad
