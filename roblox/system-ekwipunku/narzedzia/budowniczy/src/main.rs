//! Składa System Ekwipunku w pliki dla Roblox Studio.
//!
//!   budowniczy zbuduj <Items.rbxm> <katalog projektu>
//!       -> przedmioty/OSP.rbxm, build/SystemEkwipunku.rbxm,
//!          build/EkranWczytywania.rbxm, build/SystemAlarmowyOSP.rbxm,
//!          build/SystemMentora.rbxm, build/TabletKierownikaPSP.rbxm,
//!          build/SystemWezwan112.rbxm, build/SystemPowiadomien.rbxm,
//!          build/TestEkwipunku.rbxl
//!   budowniczy czytaj <plik.rbxm|.rbxl>     (wypisuje drzewo, do sprawdzania)
//!   budowniczy sprawdz <Klasa>...           (klasy bazowe i właściwości z bazy API)

use std::fs::{self, File};
use std::io::{BufReader, BufWriter};
use std::path::Path;

use rbx_dom_weak::types::{
    Attributes, BrickColor, CFrame, Color3, Color3uint8, Enum, Matrix3, Ref, Tags, UDim, UDim2,
    Variant, Vector3,
};
use rbx_dom_weak::{ustr, InstanceBuilder, WeakDom};

type Wynik<T> = Result<T, Box<dyn std::error::Error>>;

fn main() {
    let argumenty: Vec<String> = std::env::args().collect();
    let wynik = match argumenty.get(1).map(String::as_str) {
        Some("zbuduj") if argumenty.len() == 4 => {
            zbuduj(Path::new(&argumenty[2]), Path::new(&argumenty[3]))
        }
        Some("czytaj") if argumenty.len() == 3 => czytaj(Path::new(&argumenty[2])),
        Some("sprawdz") => sprawdz(&argumenty[2..]),
        _ => {
            eprintln!("użycie: budowniczy zbuduj <Items.rbxm> <katalog> | czytaj <plik> | sprawdz <Klasa>...");
            std::process::exit(2);
        }
    };
    if let Err(blad) = wynik {
        eprintln!("BŁĄD: {blad}");
        std::process::exit(1);
    }
}

// ------------------------------------------------------------------
//  Pomocnicze
// ------------------------------------------------------------------

fn wczytaj(sciezka: &Path) -> Wynik<WeakDom> {
    let plik = BufReader::new(File::open(sciezka)?);
    Ok(rbx_binary::from_reader(plik)?)
}

fn zapisz(sciezka: &Path, dom: &WeakDom, korzenie: &[Ref]) -> Wynik<()> {
    if let Some(folder) = sciezka.parent() {
        fs::create_dir_all(folder)?;
    }
    rbx_binary::to_writer(BufWriter::new(File::create(sciezka)?), dom, korzenie)?;
    // Sprawdzenie: plik musi dać się wczytać z powrotem
    let ponownie = wczytaj(sciezka)?;
    println!(
        "zapisano {} ({} obiektów)",
        sciezka.display(),
        ponownie.descendants().count() - 1
    );
    Ok(())
}

fn folder(nazwa: &str) -> InstanceBuilder {
    InstanceBuilder::new("Folder").with_name(nazwa)
}

fn skrypt(klasa: &str, nazwa: &str, plik: &Path) -> Wynik<InstanceBuilder> {
    let zrodlo = fs::read_to_string(plik).map_err(|e| format!("{}: {e}", plik.display()))?;
    Ok(InstanceBuilder::new(klasa)
        .with_name(nazwa)
        .with_property("Source", Variant::String(zrodlo)))
}

fn pozycja(x: f32, y: f32, z: f32) -> Variant {
    Variant::CFrame(CFrame::new(Vector3::new(x, y, z), Matrix3::identity()))
}

fn kolor(r: u8, g: u8, b: u8) -> Variant {
    Variant::Color3uint8(Color3uint8::new(r, g, b))
}

fn czesc(nazwa: &str, rozmiar: Vector3, cf: Variant, k: Variant) -> InstanceBuilder {
    InstanceBuilder::new("Part")
        .with_name(nazwa)
        .with_property("Size", rozmiar)
        .with_property("CFrame", cf)
        .with_property("Color", k)
        .with_property("Anchored", true)
        .with_property("TopSurface", Enum::from_u32(0))
        .with_property("BottomSurface", Enum::from_u32(0))
}

fn z_tagiem(budowa: InstanceBuilder, tag: &str) -> InstanceBuilder {
    budowa.with_property("Tags", Tags::from(vec![tag.to_string()]))
}

fn material(numer: u32) -> Enum {
    // Enum.Material: Plastic 256, SmoothPlastic 272, Wood 512, Metal 1088, Neon 288
    Enum::from_u32(numer)
}

/// Obrót wokół osi Y tak, żeby przód (LookVector) patrzył w stronę (dx, dz)
fn obrot_w_strone(dx: f32, dz: f32) -> Matrix3 {
    let dlugosc = (dx * dx + dz * dz).sqrt().max(0.001);
    let (sin, cos) = (-dx / dlugosc, -dz / dlugosc);
    Matrix3::new(
        Vector3::new(cos, 0.0, sin),
        Vector3::new(0.0, 1.0, 0.0),
        Vector3::new(-sin, 0.0, cos),
    )
}

fn znajdz_po_nazwie(dom: &WeakDom, nazwa: &str) -> Vec<Ref> {
    dom.descendants()
        .filter(|i| i.name == nazwa)
        .map(|i| i.referent())
        .collect()
}

// ------------------------------------------------------------------
//  Przedmioty klienta
// ------------------------------------------------------------------

const BEZPIECZNA_SZCZOTKA: &str = r#"--HeroThePianist
-- Poprawione: wcześniej każdy (także exploiter) mógł tym wyłączyć dowolną część mapy.
-- Teraz działa tylko na części o nazwie "Door" blisko szczotki trzymanej w ręce.
local tool = script.Parent

tool.RemoteEvent.OnServerEvent:Connect(function(plr, event, arg1)
	if tool.Parent ~= plr.Character then
		return
	end
	if event == "BreakdownDoor" then
		if typeof(arg1) == "Instance" and arg1:IsA("BasePart") and arg1.Name == "Door"
			and (arg1.Position - tool.Handle.Position).Magnitude < 15 then
			arg1.Transparency = .5
			arg1.CanCollide = false
		end
	elseif event == "PlayHitSound" then
		tool.Handle.Sound:Play()
	end
end)
"#;

fn wyczysc_przedmioty(dom: &mut WeakDom) -> Wynik<()> {
    // 1. Zaciemniony (obfuscated) skrypt w Radiu: wyłączony, ale to typowe miejsce na backdoora
    for r in znajdz_po_nazwie(dom, "Script - obfuscated - dont working") {
        println!("usuwam: {}", dom.full_path_of(r, "/"));
        dom.destroy(r);
    }

    // 2. Dziura w Szczotce: RemoteEvent przyjmował dowolną część z mapy
    let mut poprawiono = false;
    for r in znajdz_po_nazwie(dom, "EventScript") {
        let sciezka = dom.full_path_of(r, "/");
        if !sciezka.starts_with("Szczotka/") {
            continue;
        }
        let obiekt = dom.get_by_ref_mut(r).ok_or("brak EventScript")?;
        obiekt
            .properties
            .insert(ustr("Source"), Variant::String(BEZPIECZNA_SZCZOTKA.to_string()));
        println!("poprawiono: {sciezka}");
        poprawiono = true;
    }
    if !poprawiono {
        println!("uwaga: nie znaleziono Szczotka/EventScript");
    }
    Ok(())
}

// ------------------------------------------------------------------
//  Części systemu
// ------------------------------------------------------------------

struct Zrodla<'a> {
    src: &'a Path,
}

impl Zrodla<'_> {
    fn wspolne(&self) -> Wynik<InstanceBuilder> {
        let k = self.src.join("ReplicatedStorage/Ekwipunek");
        Ok(folder("Ekwipunek")
            .with_child(skrypt("ModuleScript", "Konfiguracja", &k.join("Konfiguracja.lua"))?)
            .with_child(skrypt("ModuleScript", "Przedmioty", &k.join("Przedmioty.lua"))?))
    }

    fn serwer(&self) -> Wynik<InstanceBuilder> {
        let k = self.src.join("ServerScriptService/EkwipunekSerwer");
        Ok(skrypt("Script", "EkwipunekSerwer", &k.join("init.server.lua"))?
            .with_child(skrypt("ModuleScript", "Rangi", &k.join("Rangi.lua"))?))
    }

    fn klient(&self) -> Wynik<InstanceBuilder> {
        skrypt(
            "LocalScript",
            "EkwipunekKlient",
            &self.src.join("StarterPlayerScripts/EkwipunekKlient.client.lua"),
        )
    }

    fn ekran_wczytywania(&self) -> Wynik<InstanceBuilder> {
        let k = self.src.join("ReplicatedFirst/EkranWczytywania");
        Ok(skrypt("LocalScript", "EkranWczytywania", &k.join("init.client.lua"))?
            .with_child(skrypt("ModuleScript", "Ustawienia", &k.join("Ustawienia.lua"))?))
    }

    fn alarm_wspolne(&self) -> Wynik<InstanceBuilder> {
        let k = self.src.join("ReplicatedStorage/AlarmOSP");
        Ok(folder("AlarmOSP").with_child(skrypt("ModuleScript", "Ustawienia", &k.join("Ustawienia.lua"))?))
    }

    fn alarm_serwer(&self) -> Wynik<InstanceBuilder> {
        let k = self.src.join("ServerScriptService/AlarmSerwer");
        Ok(skrypt("Script", "AlarmSerwer", &k.join("init.server.lua"))?
            .with_child(skrypt("ModuleScript", "Alarmy", &k.join("Alarmy.lua"))?))
    }

    fn alarm_klient(&self) -> Wynik<InstanceBuilder> {
        let k = self.src.join("StarterPlayerScripts/AlarmKlient");
        Ok(skrypt("LocalScript", "AlarmKlient", &k.join("init.client.lua"))?
            .with_child(skrypt("ModuleScript", "Wyglad", &k.join("Wyglad.lua"))?)
            .with_child(skrypt("ModuleScript", "Panel", &k.join("Panel.lua"))?)
            .with_child(skrypt("ModuleScript", "Pager", &k.join("Pager.lua"))?))
    }

    /// Przykładowa remiza: komputer, syrena na maszcie i dwie lampy alarmowe
    fn remiza(&self, ox: f32, oz: f32) -> InstanceBuilder {
        let ciemny = || kolor(30, 30, 34);
        let metal = || kolor(150, 150, 155);
        let lampa = |nazwa: &str, x: f32| {
            z_tagiem(
                czesc(nazwa, Vector3::new(1.4, 0.8, 1.4), pozycja(x, 6.4, oz), kolor(220, 40, 35))
                    .with_property("Material", material(272))
                    .with_child(
                        InstanceBuilder::new("PointLight")
                            .with_name("Swiatlo")
                            .with_property("Color", Color3::new(1.0, 0.2, 0.15))
                            .with_property("Range", 18.0f32)
                            .with_property("Brightness", 4.0f32)
                            .with_property("Enabled", false),
                    ),
                "AlarmSwiatlo",
            )
        };
        let napis_monitora = InstanceBuilder::new("SurfaceGui")
            .with_name("Ekran")
            .with_property("Face", Enum::from_u32(5))
            .with_property("SizingMode", Enum::from_u32(1))
            .with_property("PixelsPerStud", 60.0f32)
            .with_child(
                InstanceBuilder::new("TextLabel")
                    .with_name("Napis")
                    .with_property("Size", UDim2::new(UDim::new(1.0, 0), UDim::new(1.0, 0)))
                    .with_property("BackgroundColor3", Color3::new(0.09, 0.09, 0.1))
                    .with_property("Text", "ALARM OSP")
                    .with_property("TextScaled", true)
                    .with_property("TextColor3", Color3::new(0.94, 0.31, 0.25)),
            );
        InstanceBuilder::new("Model")
            .with_name("RemizaAlarm")
            .with_child(
                czesc("Biurko", Vector3::new(6.0, 3.0, 3.0), pozycja(ox, 1.5, oz), kolor(105, 70, 45))
                    .with_property("Material", material(512)),
            )
            .with_child(z_tagiem(
                czesc("Monitor", Vector3::new(3.0, 2.0, 0.3), pozycja(ox, 4.0, oz), ciemny())
                    .with_child(napis_monitora),
                "AlarmKomputer",
            ))
            .with_child(
                czesc("Maszt", Vector3::new(0.6, 14.0, 0.6), pozycja(ox + 12.0, 7.0, oz), metal())
                    .with_property("Material", material(1088)),
            )
            .with_child(z_tagiem(
                czesc("Syrena", Vector3::new(2.4, 2.0, 2.4), pozycja(ox + 12.0, 15.0, oz), metal())
                    .with_property("Material", material(1088)),
                "AlarmSyrena",
            ))
            .with_child(czesc("SlupekLewy", Vector3::new(0.4, 6.0, 0.4), pozycja(ox - 6.0, 3.0, oz), metal()))
            .with_child(czesc("SlupekPrawy", Vector3::new(0.4, 6.0, 0.4), pozycja(ox + 6.0, 3.0, oz), metal()))
            .with_child(lampa("LampaLewa", ox - 6.0))
            .with_child(lampa("LampaPrawa", ox + 6.0))
    }

    /// Punkty do wyboru w panelu (nazwa części = adres)
    fn miejsca_zdarzen(&self, widoczne: bool) -> InstanceBuilder {
        let punkt = |nazwa: &str, x: f32, z: f32| {
            z_tagiem(
                czesc(nazwa, Vector3::new(6.0, 0.4, 6.0), pozycja(x, 0.2, z), kolor(245, 165, 70))
                    .with_property("CanCollide", false)
                    .with_property("Transparency", if widoczne { 0.5f32 } else { 1.0f32 }),
                "MiejsceZdarzenia",
            )
        };
        folder("MiejscaZdarzen")
            .with_child(punkt("ul. Leśna 12", -120.0, -150.0))
            .with_child(punkt("Las przy jeziorze", 180.0, 120.0))
            .with_child(punkt("Skrzyżowanie DK7", -60.0, 200.0))
    }

    fn mentor_wspolne(&self) -> Wynik<InstanceBuilder> {
        let k = self.src.join("ReplicatedStorage/Mentor");
        Ok(folder("Mentor").with_child(skrypt("ModuleScript", "Ustawienia", &k.join("Ustawienia.lua"))?))
    }

    fn mentor_serwer(&self) -> Wynik<InstanceBuilder> {
        skrypt(
            "Script",
            "MentorSerwer",
            &self.src.join("ServerScriptService/MentorSerwer.server.lua"),
        )
    }

    fn mentor_klient(&self) -> Wynik<InstanceBuilder> {
        let k = self.src.join("StarterPlayerScripts/MentorKlient");
        Ok(skrypt("LocalScript", "MentorKlient", &k.join("init.client.lua"))?
            .with_child(skrypt("ModuleScript", "Wyglad", &k.join("Wyglad.lua"))?)
            .with_child(skrypt("ModuleScript", "Menu", &k.join("Menu.lua"))?))
    }

    /// Miejsce mentora (Tag "Mentor") zwrócone przodem w stronę (dx, dz)
    /// i cel "Remiza OSP" (Tag "MentorCel") przed tablicą OSP
    fn mentor_punkty(&self, x: f32, z: f32, dx: f32, dz: f32) -> InstanceBuilder {
        let dlugosc = (dx * dx + dz * dz).sqrt().max(0.001);
        let (sin, cos) = (-dx / dlugosc, -dz / dlugosc);
        let obrot = Matrix3::new(
            Vector3::new(cos, 0.0, sin),
            Vector3::new(0.0, 1.0, 0.0),
            Vector3::new(-sin, 0.0, cos),
        );
        let punkt = z_tagiem(
            czesc("MentorPunkt", Vector3::new(2.0, 1.0, 2.0), pozycja(x, 0.5, z), kolor(245, 245, 247))
                .with_property("CFrame", Variant::CFrame(CFrame::new(Vector3::new(x, 0.5, z), obrot))),
            "Mentor",
        );
        let cel = z_tagiem(
            czesc("Remiza OSP", Vector3::new(4.0, 1.0, 4.0), pozycja(0.0, 0.5, -10.0), kolor(235, 75, 65))
                .with_property("Transparency", 1.0f32)
                .with_property("CanCollide", false),
            "MentorCel",
        );
        folder("Mentor (miejsca)").with_child(punkt).with_child(cel)
    }

    fn tablet_wspolne(&self) -> Wynik<InstanceBuilder> {
        let k = self.src.join("ReplicatedStorage/TabletPSP");
        Ok(folder("TabletPSP").with_child(skrypt("ModuleScript", "Ustawienia", &k.join("Ustawienia.lua"))?))
    }

    fn tablet_serwer(&self) -> Wynik<InstanceBuilder> {
        skrypt(
            "Script",
            "TabletPSPSerwer",
            &self.src.join("ServerScriptService/TabletPSPSerwer.server.lua"),
        )
    }

    fn tablet_klient(&self) -> Wynik<InstanceBuilder> {
        let k = self.src.join("StarterPlayerScripts/TabletPSPKlient");
        Ok(skrypt("LocalScript", "TabletPSPKlient", &k.join("init.client.lua"))?
            .with_child(skrypt("ModuleScript", "Wyglad", &k.join("Wyglad.lua"))?)
            .with_child(skrypt("ModuleScript", "Ikony", &k.join("Ikony.lua"))?)
            .with_child(skrypt("ModuleScript", "Tablet", &k.join("Tablet.lua"))?))
    }

    /// Tool "Tablet PSP": czarna płytka z ekranem z napisem PSP
    fn tablet_narzedzie(&self) -> InstanceBuilder {
        let ekran = |nazwa: &str, sciana: u32| {
            InstanceBuilder::new("SurfaceGui")
                .with_name(nazwa)
                .with_property("Face", Enum::from_u32(sciana))
                .with_property("SizingMode", Enum::from_u32(1))
                .with_property("PixelsPerStud", 100.0f32)
                .with_property("LightInfluence", 0.0f32)
                .with_child(
                    InstanceBuilder::new("TextLabel")
                        .with_name("Logo")
                        .with_property("Size", UDim2::new(UDim::new(1.0, 0), UDim::new(1.0, 0)))
                        .with_property("BackgroundColor3", Color3::new(0.06, 0.06, 0.07))
                        .with_property("Text", "PSP")
                        .with_property("TextScaled", true)
                        .with_property("TextColor3", Color3::new(0.88, 0.15, 0.18)),
                )
        };
        InstanceBuilder::new("Tool")
            .with_name("Tablet PSP")
            .with_property("CanBeDropped", false)
            .with_property("RequiresHandle", true)
            .with_property("ToolTip", "Tablet kierownika PSP")
            .with_child(
                InstanceBuilder::new("Part")
                    .with_name("Handle")
                    .with_property("Size", Vector3::new(1.6, 1.1, 0.12))
                    .with_property("Color", kolor(22, 22, 25))
                    .with_property("Material", material(272))
                    .with_property("CanCollide", false)
                    .with_child(ekran("EkranPrzod", 5))
                    .with_child(ekran("EkranTyl", 2)),
            )
    }

    /// Przykładowa remiza PSP: 2 bramy chowane do góry za nadprożem,
    /// oświetlenie w strefach i miejsce na wóz przed bramą
    fn remiza_psp(&self, ox: f32, oz: f32) -> InstanceBuilder {
        let sciana = || kolor(205, 200, 190);
        let brama = |nazwa: &str, x: f32| {
            z_tagiem(
                czesc(nazwa, Vector3::new(14.0, 12.0, 0.6), pozycja(ox + x, 6.0, oz - 1.2), kolor(200, 40, 42))
                    .with_property("Material", material(1056)),
                "BramaPSP",
            )
        };
        let lampa = |nazwa: &str, x: f32, z: f32, strefa: &str| {
            z_tagiem(
                czesc(nazwa, Vector3::new(3.0, 0.3, 3.0), pozycja(ox + x, 25.8, oz + z), kolor(250, 250, 245))
                    .with_property("Material", material(288))
                    .with_property("Attributes", Attributes::new().with("Strefa", strefa))
                    .with_child(
                        InstanceBuilder::new("PointLight")
                            .with_name("Swiatlo")
                            .with_property("Range", 28.0f32)
                            .with_property("Brightness", 1.5f32),
                    ),
                "SwiatloPSP",
            )
        };
        let napis = InstanceBuilder::new("SurfaceGui")
            .with_name("Napis")
            .with_property("Face", Enum::from_u32(5))
            .with_property("SizingMode", Enum::from_u32(1))
            .with_property("PixelsPerStud", 20.0f32)
            .with_child(
                InstanceBuilder::new("TextLabel")
                    .with_name("Tekst")
                    .with_property("Size", UDim2::new(UDim::new(1.0, 0), UDim::new(0.6, 0)))
                    .with_property("Position", UDim2::new(UDim::new(0.0, 0), UDim::new(0.2, 0)))
                    .with_property("BackgroundTransparency", 1.0f32)
                    .with_property("Text", "PAŃSTWOWA STRAŻ POŻARNA")
                    .with_property("TextScaled", true)
                    .with_property("TextColor3", Color3::new(0.85, 0.12, 0.15)),
            );
        let obrot = Matrix3::new(
            Vector3::new(-1.0, 0.0, 0.0),
            Vector3::new(0.0, 1.0, 0.0),
            Vector3::new(0.0, 0.0, -1.0),
        );
        let miejsce = z_tagiem(
            czesc("MiejscePojazdu", Vector3::new(12.0, 0.2, 24.0), pozycja(ox - 11.0, 0.1, oz + 16.0), kolor(120, 120, 128))
                .with_property(
                    "CFrame",
                    Variant::CFrame(CFrame::new(Vector3::new(ox - 11.0, 0.1, oz + 16.0), obrot)),
                ),
            "MiejscePojazduPSP",
        );
        InstanceBuilder::new("Model")
            .with_name("RemizaPSP")
            .with_child(czesc("TylnaSciana", Vector3::new(40.0, 26.0, 1.0), pozycja(ox, 13.0, oz - 14.0), sciana()))
            .with_child(czesc("LewaSciana", Vector3::new(1.0, 26.0, 14.0), pozycja(ox - 20.0, 13.0, oz - 7.0), sciana()))
            .with_child(czesc("PrawaSciana", Vector3::new(1.0, 26.0, 14.0), pozycja(ox + 20.0, 13.0, oz - 7.0), sciana()))
            .with_child(
                czesc("Nadproze", Vector3::new(40.0, 14.0, 1.0), pozycja(ox, 19.0, oz), sciana()).with_child(napis),
            )
            .with_child(czesc("FilarLewy", Vector3::new(2.0, 12.0, 1.0), pozycja(ox - 19.0, 6.0, oz), sciana()))
            .with_child(czesc("FilarSrodek", Vector3::new(8.0, 12.0, 1.0), pozycja(ox, 6.0, oz), sciana()))
            .with_child(czesc("FilarPrawy", Vector3::new(2.0, 12.0, 1.0), pozycja(ox + 19.0, 6.0, oz), sciana()))
            .with_child(czesc("Dach", Vector3::new(42.0, 1.0, 16.0), pozycja(ox, 26.5, oz - 7.0), kolor(60, 60, 66)))
            .with_child(brama("Brama 1", -11.0))
            .with_child(brama("Brama 2", 11.0))
            .with_child(lampa("LampaGaraz1", -11.0, -7.0, "Garaż"))
            .with_child(lampa("LampaGaraz2", 11.0, -7.0, "Garaż"))
            .with_child(lampa("LampaPlac", 0.0, 3.0, "Plac"))
            .with_child(miejsce)
    }

    /// Przykładowy wóz (nieruchomy model do pokazania wyjazdu; podmień na swój)
    fn pojazdy_psp(&self) -> InstanceBuilder {
        let czerwony = || kolor(200, 30, 35);
        let kolo = |x: f32, z: f32| {
            InstanceBuilder::new("Part")
                .with_name("Kolo")
                .with_property("Shape", Enum::from_u32(2))
                .with_property("Size", Vector3::new(1.2, 3.0, 3.0))
                .with_property("CFrame", pozycja(x, 1.5, z))
                .with_property("Color", kolor(25, 25, 28))
                .with_property("Anchored", true)
        };
        folder("PojazdyPSP").with_child(
            InstanceBuilder::new("Model")
                .with_name("GBA (przyklad)")
                .with_child(czesc("Skrzynia", Vector3::new(7.0, 6.0, 14.0), pozycja(0.0, 5.0, 3.0), czerwony()))
                .with_child(czesc("Kabina", Vector3::new(7.0, 5.0, 5.0), pozycja(0.0, 4.5, -7.0), czerwony()))
                .with_child(
                    czesc("Szyba", Vector3::new(6.4, 2.0, 0.2), pozycja(0.0, 5.5, -9.55), kolor(30, 34, 40))
                        .with_property("Material", material(1568)),
                )
                .with_child(
                    czesc("Belka", Vector3::new(5.0, 0.6, 1.2), pozycja(0.0, 7.3, -7.5), kolor(40, 90, 255))
                        .with_property("Material", material(288)),
                )
                .with_child(czesc("Pas", Vector3::new(7.1, 0.6, 19.0), pozycja(0.0, 3.2, 0.5), kolor(240, 240, 240)))
                .with_child(kolo(-3.6, -6.5))
                .with_child(kolo(3.6, -6.5))
                .with_child(kolo(-3.6, 6.5))
                .with_child(kolo(3.6, 6.5)),
        )
    }

    fn stanowisko_kierownika(&self) -> Wynik<InstanceBuilder> {
        let prompt = InstanceBuilder::new("ProximityPrompt")
            .with_name("ProximityPrompt")
            .with_property("ActionText", "Zostań kierownikiem PSP (test)")
            .with_property("ObjectText", "Komenda PSP")
            .with_property("HoldDuration", 0.5f32)
            .with_property("RequiresLineOfSight", false);
        Ok(czesc(
            "StanowiskoKierownika",
            Vector3::new(4.0, 4.0, 1.0),
            pozycja(-46.0, 2.0, -24.0),
            kolor(245, 200, 75),
        )
        .with_child(prompt)
        .with_child(skrypt(
            "Script",
            "StanowiskoKierownika",
            &self.src.join("Przyklady/StanowiskoKierownika.server.lua"),
        )?))
    }

    fn wezwania_wspolne(&self) -> Wynik<InstanceBuilder> {
        let k = self.src.join("ReplicatedStorage/Wezwania112");
        Ok(folder("Wezwania112").with_child(skrypt("ModuleScript", "Ustawienia", &k.join("Ustawienia.lua"))?))
    }

    fn wezwania_serwer(&self) -> Wynik<InstanceBuilder> {
        let k = self.src.join("ServerScriptService/Wezwania112Serwer");
        Ok(skrypt("Script", "Wezwania112Serwer", &k.join("init.server.lua"))?
            .with_child(skrypt("ModuleScript", "Wezwania", &k.join("Wezwania.lua"))?)
            .with_child(skrypt("ModuleScript", "Rekwizyty", &k.join("Rekwizyty.lua"))?)
            .with_child(skrypt("ModuleScript", "Rodzaje", &k.join("Rodzaje.lua"))?))
    }

    fn wezwania_klient(&self) -> Wynik<InstanceBuilder> {
        let k = self.src.join("StarterPlayerScripts/Wezwania112Klient");
        Ok(skrypt("LocalScript", "Wezwania112Klient", &k.join("init.client.lua"))?
            .with_child(skrypt("ModuleScript", "Wyglad", &k.join("Wyglad.lua"))?))
    }

    /// Miejsca wezwań (Tag MiejsceWezwania, atrybut Rodzaje); nazwa = adres
    fn miejsca_wezwan(&self, widoczne: bool) -> InstanceBuilder {
        let punkt = |nazwa: &str, x: f32, z: f32, dx: f32, dz: f32, rodzaje: &str| {
            z_tagiem(
                czesc(nazwa, Vector3::new(4.0, 1.0, 4.0), pozycja(x, 0.5, z), kolor(60, 130, 230))
                    .with_property("CFrame", Variant::CFrame(CFrame::new(Vector3::new(x, 0.5, z), obrot_w_strone(dx, dz))))
                    .with_property("Transparency", if widoczne { 0.7f32 } else { 1.0f32 })
                    .with_property("CanCollide", false)
                    .with_property("Attributes", Attributes::new().with("Rodzaje", rodzaje)),
                "MiejsceWezwania",
            )
        };
        let drogowe = "Drzewo, Olej, Wypadek, PozarAuta, Poszkodowany";
        folder("MiejscaWezwan")
            .with_child(punkt("ul. Strażacka 4", -90.0, 70.0, 1.0, 0.0, drogowe))
            .with_child(punkt("ul. Polna", 0.0, 70.0, 1.0, 0.0, drogowe))
            .with_child(punkt("Trasa DK7", 90.0, 70.0, 1.0, 0.0, drogowe))
            .with_child(punkt("Las Miejski", 130.0, -90.0, 0.0, 1.0, "PozarLasu"))
            .with_child(punkt("ul. Leśna 7", -130.0, -60.0, 0.0, 1.0, "PozarBudynku"))
            .with_child(punkt("Osiedle Słoneczne", -150.0, 140.0, 0.0, -1.0, "Podtopienie"))
            .with_child(punkt("Park Miejski", 80.0, 160.0, 0.0, -1.0, "Kot, Szerszenie"))
    }

    /// Scena do testów: droga, dom do pożaru budynku i kilka drzew w lesie
    fn scena_wezwan(&self) -> InstanceBuilder {
        let sciana = || kolor(220, 210, 190);
        let (hx, hz) = (-130.0f32, -60.0f32);
        let drzewo = |nazwa: &str, x: f32, z: f32| {
            InstanceBuilder::new("Model")
                .with_name(nazwa)
                .with_child(
                    // Walec ma oś wzdłuż X, więc obracamy go do pionu
                    czesc("Pien", Vector3::new(14.0, 2.0, 2.0), pozycja(x, 7.0, z), kolor(96, 68, 45))
                        .with_property("Shape", Enum::from_u32(2))
                        .with_property("CFrame", Variant::CFrame(CFrame::new(
                            Vector3::new(x, 7.0, z),
                            Matrix3::new(
                                Vector3::new(0.0, -1.0, 0.0),
                                Vector3::new(1.0, 0.0, 0.0),
                                Vector3::new(0.0, 0.0, 1.0),
                            ),
                        ))),
                )
                .with_child(
                    czesc("Korona", Vector3::new(9.0, 9.0, 9.0), pozycja(x, 16.0, z), kolor(62, 112, 52))
                        .with_property("Shape", Enum::from_u32(0))
                        .with_property("Material", material(1280)),
                )
        };
        folder("ScenaWezwan")
            .with_child(czesc("Droga", Vector3::new(320.0, 0.2, 26.0), pozycja(0.0, 0.1, 70.0), kolor(55, 55, 60)).with_property("Material", material(816)))
            .with_child(czesc("Pas", Vector3::new(320.0, 0.22, 0.6), pozycja(0.0, 0.11, 70.0), kolor(235, 235, 235)))
            .with_child(czesc("Podloga", Vector3::new(22.0, 0.4, 18.0), pozycja(hx, 0.2, hz), kolor(150, 140, 130)))
            .with_child(czesc("ScianaTyl", Vector3::new(22.0, 12.0, 1.0), pozycja(hx, 6.0, hz + 9.0), sciana()))
            .with_child(czesc("ScianaL", Vector3::new(1.0, 12.0, 18.0), pozycja(hx - 11.0, 6.0, hz), sciana()))
            .with_child(czesc("ScianaP", Vector3::new(1.0, 12.0, 18.0), pozycja(hx + 11.0, 6.0, hz), sciana()))
            .with_child(czesc("ScianaPrzodL", Vector3::new(8.0, 12.0, 1.0), pozycja(hx - 7.0, 6.0, hz - 9.0), sciana()))
            .with_child(czesc("ScianaPrzodP", Vector3::new(8.0, 12.0, 1.0), pozycja(hx + 7.0, 6.0, hz - 9.0), sciana()))
            .with_child(czesc("NadDrzwiami", Vector3::new(6.0, 4.0, 1.0), pozycja(hx, 10.0, hz - 9.0), sciana()))
            .with_child(czesc("Dach", Vector3::new(24.0, 1.0, 20.0), pozycja(hx, 12.5, hz), kolor(140, 60, 45)))
            .with_child(drzewo("Drzewo1", 115.0, -100.0))
            .with_child(drzewo("Drzewo2", 145.0, -78.0))
            .with_child(drzewo("Drzewo3", 120.0, -70.0))
            .with_child(drzewo("Drzewo4", 150.0, -105.0))
            .with_child(drzewo("Drzewo5", 135.0, -115.0))
    }

    fn powiadomienia_wspolne(&self) -> Wynik<InstanceBuilder> {
        let k = self.src.join("ReplicatedStorage/Powiadomienia");
        Ok(skrypt("ModuleScript", "Powiadomienia", &k.join("init.lua"))?
            .with_child(skrypt("ModuleScript", "Ustawienia", &k.join("Ustawienia.lua"))?)
            .with_child(skrypt("ModuleScript", "Ikony", &k.join("Ikony.lua"))?))
    }

    fn powiadomienia_serwer(&self) -> Wynik<InstanceBuilder> {
        skrypt(
            "Script",
            "PowiadomieniaSerwer",
            &self.src.join("ServerScriptService/PowiadomieniaSerwer.server.lua"),
        )
    }

    fn powiadomienia_klient(&self) -> Wynik<InstanceBuilder> {
        skrypt(
            "LocalScript",
            "PowiadomieniaKlient",
            &self.src.join("StarterPlayerScripts/PowiadomieniaKlient.client.lua"),
        )
    }

    /// Przykłady: sedan z drzwiami (API), śmietnik (pasek + wynik), szafka (same atrybuty)
    fn przyklady_powiadomien(&self) -> Wynik<InstanceBuilder> {
        let (x, z) = (24.0f32, 32.0f32);
        let lakier = || kolor(40, 60, 110);
        let szklo = || kolor(35, 40, 48);
        let drzwi = |nazwa: &str, dx: f32, kat: f64| -> Wynik<InstanceBuilder> {
            Ok(czesc(nazwa, Vector3::new(0.3, 1.9, 3.2), pozycja(x + dx, 2.4, z - 0.4), kolor(34, 52, 96))
                .with_property("Attributes", Attributes::new().with("KatOtwarcia", kat))
                .with_child(
                    InstanceBuilder::new("ProximityPrompt")
                        .with_name("ProximityPrompt")
                        .with_property("ActionText", "Otwórz drzwi")
                        .with_property("ObjectText", "Sedan")
                        .with_property("HoldDuration", 0.0f32)
                        .with_property("MaxActivationDistance", 8.0f32)
                        .with_property("RequiresLineOfSight", false),
                )
                .with_child(skrypt("Script", "Drzwi", &self.src.join("Przyklady/PrzykladDrzwi.server.lua"))?))
        };
        let mut sedan = InstanceBuilder::new("Model")
            .with_name("Sedan")
            .with_child(czesc("Nadwozie", Vector3::new(6.0, 2.0, 12.0), pozycja(x, 1.9, z), lakier()))
            .with_child(czesc("Kabina", Vector3::new(5.6, 1.9, 6.0), pozycja(x, 3.85, z + 0.6), lakier()))
            .with_child(czesc("SzybaPrzod", Vector3::new(5.2, 1.6, 0.2), pozycja(x, 3.85, z - 2.45), szklo()).with_property("Material", material(1568)))
            .with_child(czesc("SzybaTyl", Vector3::new(5.2, 1.6, 0.2), pozycja(x, 3.85, z + 3.65), szklo()).with_property("Material", material(1568)))
            .with_child(drzwi("DrzwiLewe", -3.15, -70.0)?)
            .with_child(drzwi("DrzwiPrawe", 3.15, 70.0)?);
        for (dx, dz) in [(-3.0f32, -4.0f32), (3.0, -4.0), (-3.0, 4.0), (3.0, 4.0)] {
            sedan = sedan.with_child(
                czesc("Kolo", Vector3::new(1.0, 2.4, 2.4), pozycja(x + dx, 1.2, z + dz), kolor(25, 25, 28))
                    .with_property("Shape", Enum::from_u32(2)),
            );
        }
        let smietnik = czesc("Smietnik", Vector3::new(2.4, 3.2, 2.4), pozycja(12.0, 1.6, 28.0), kolor(40, 90, 50))
            .with_child(
                InstanceBuilder::new("ProximityPrompt")
                    .with_name("ProximityPrompt")
                    .with_property("ActionText", "Przeszukaj")
                    .with_property("ObjectText", "Śmietnik")
                    .with_property("HoldDuration", 3.0f32)
                    .with_property("RequiresLineOfSight", false)
                    .with_property(
                        "Attributes",
                        Attributes::new()
                            .with("PowiadomienieTrwa", "Przeszukujesz śmietnik...")
                            .with("PowiadomienieIkona", "szukaj"),
                    ),
            )
            .with_child(skrypt("Script", "Przeszukiwanie", &self.src.join("Przyklady/PrzykladSmietnik.server.lua"))?);
        let szafka = czesc("Szafka", Vector3::new(3.0, 6.0, 1.6), pozycja(6.0, 3.0, 28.0), kolor(120, 125, 135))
            .with_property("Material", material(1088))
            .with_child(
                InstanceBuilder::new("ProximityPrompt")
                    .with_name("ProximityPrompt")
                    .with_property("ActionText", "Otwórz szafkę")
                    .with_property("ObjectText", "Szafka")
                    .with_property("HoldDuration", 0.3f32)
                    .with_property("RequiresLineOfSight", false)
                    .with_property(
                        "Attributes",
                        Attributes::new()
                            .with("Powiadomienie", "Otworzyłeś szafkę. W środku wisi mundur.")
                            .with("PowiadomienieWPoblizu", "{gracz} otwiera szafkę.")
                            .with("PowiadomienieIkona", "skrzynka"),
                    ),
            );
        Ok(folder("PrzykladyPowiadomien").with_child(sedan).with_child(smietnik).with_child(szafka))
    }

    fn stanowisko(&self) -> Wynik<InstanceBuilder> {
        let napis = InstanceBuilder::new("TextLabel")
            .with_name("Napis")
            .with_property("Size", UDim2::new(UDim::new(1.0, 0), UDim::new(1.0, 0)))
            .with_property("BackgroundTransparency", 1.0f32)
            .with_property("Text", "OSP\nDołącz / odejdź")
            .with_property("TextScaled", true)
            .with_property("TextColor3", Color3::new(1.0, 1.0, 1.0));
        let ekran = InstanceBuilder::new("SurfaceGui")
            .with_name("Tablica")
            .with_property("Face", Enum::from_u32(5)) // Front
            .with_property("SizingMode", Enum::from_u32(1)) // PixelsPerStud
            .with_property("PixelsPerStud", 50.0f32)
            .with_child(napis);
        let prompt = InstanceBuilder::new("ProximityPrompt")
            .with_name("ProximityPrompt")
            .with_property("ActionText", "Dołącz / odejdź z OSP")
            .with_property("ObjectText", "Remiza OSP")
            .with_property("HoldDuration", 0.5f32)
            .with_property("RequiresLineOfSight", false);
        let tablica = czesc(
            "StanowiskoOSP",
            Vector3::new(6.0, 4.0, 1.0),
            pozycja(0.0, 3.0, -14.0),
            kolor(200, 45, 40),
        )
        .with_child(prompt)
        .with_child(ekran)
        .with_child(skrypt(
            "Script",
            "StanowiskoOSP",
            &self.src.join("Przyklady/StanowiskoOSP.server.lua"),
        )?);
        Ok(tablica)
    }
}

fn instrukcja_powiadomien() -> InstanceBuilder {
    let tekst = "SYSTEM POWIADOMIEŃ - INSTALACJA\n\
        Przeciągnij ZAWARTOŚĆ każdego folderu do usługi o tej samej nazwie:\n\
        - Do ReplicatedStorage  -> Powiadomienia  do  ReplicatedStorage\n\
        - Do ServerScriptService -> PowiadomieniaSerwer  do  ServerScriptService\n\
        - Do StarterPlayerScripts -> PowiadomieniaKlient  do  StarterPlayer > StarterPlayerScripts\n\
        - Do Workspace (przyklad) -> PrzykladyPowiadomien (opcjonalnie)\n\
        Z kodu: require(game.ReplicatedStorage.Powiadomienia).Wyslij(gracz, { Tytul, Tekst, Typ, Ikona }).\n\
        Bez kodu: atrybuty Powiadomienie / PowiadomienieWPoblizu / PowiadomienieTrwa na ProximityPrompt.";
    InstanceBuilder::new("StringValue")
        .with_name("CZYTAJ_MNIE")
        .with_property("Value", tekst)
}

fn instrukcja_wezwan() -> InstanceBuilder {
    let tekst = "SYSTEM WEZWAŃ 112 - INSTALACJA\n\
        Przeciągnij ZAWARTOŚĆ każdego folderu do usługi o tej samej nazwie:\n\
        - Do ReplicatedStorage  -> Wezwania112  do  ReplicatedStorage\n\
        - Do ServerScriptService -> Wezwania112Serwer  do  ServerScriptService\n\
        - Do StarterPlayerScripts -> Wezwania112Klient  do  StarterPlayer > StarterPlayerScripts\n\
        - Do Workspace (przyklad) -> MiejscaWezwan  do  Workspace i ustaw punkty na swojej mapie\n\
        Punkt = część z Tagiem MiejsceWezwania; nazwa = adres; atrybut Rodzaje = jakie wezwania tu wypadają.\n\
        Komenda: /wezwanie [rodzaj | lista | stop]. Ustawienia: ReplicatedStorage > Wezwania112 > Ustawienia.";
    InstanceBuilder::new("StringValue")
        .with_name("CZYTAJ_MNIE")
        .with_property("Value", tekst)
}

fn instrukcja_tabletu() -> InstanceBuilder {
    let tekst = "TABLET KIEROWNIKA PSP - INSTALACJA\n\
        Przeciągnij ZAWARTOŚĆ każdego folderu do usługi o tej samej nazwie:\n\
        - Do ReplicatedStorage  -> TabletPSP  do  ReplicatedStorage\n\
        - Do ServerScriptService -> TabletPSPSerwer  do  ServerScriptService\n\
        - Do StarterPlayerScripts -> TabletPSPKlient  do  StarterPlayer > StarterPlayerScripts\n\
        - Do ServerStorage -> folder 'Kierownik PSP' (z tabletem) do EkwipunekPrzedmioty, PojazdyPSP do ServerStorage\n\
        - Do Workspace (przyklad) -> RemizaPSP  do  Workspace\n\
        Bez systemu ekwipunku: wrzuć Tool 'Tablet PSP' do StarterPack.\n\
        Tagi: BramaPSP, SwiatloPSP (atrybut Strefa), AlarmSyrena, AlarmSwiatlo, MiejscePojazduPSP.\n\
        Ustawienia: ReplicatedStorage > TabletPSP > Ustawienia.";
    InstanceBuilder::new("StringValue")
        .with_name("CZYTAJ_MNIE")
        .with_property("Value", tekst)
}

fn instrukcja_mentora() -> InstanceBuilder {
    let tekst = "MENTOR - INSTALACJA\n\
        Przeciągnij ZAWARTOŚĆ każdego folderu do usługi o tej samej nazwie:\n\
        - Do ReplicatedStorage  -> Mentor  do  ReplicatedStorage\n\
        - Do ServerScriptService -> MentorSerwer  do  ServerScriptService\n\
        - Do StarterPlayerScripts -> MentorKlient  do  StarterPlayer > StarterPlayerScripts\n\
        - Do Workspace (przyklad) -> MentorPunkt (Tag Mentor) i Remiza OSP (Tag MentorCel)  do  Workspace\n\
        Mentor stanie na MentorPunkt przodem tam, gdzie przód części.\n\
        Teksty i rozdziały: ReplicatedStorage > Mentor > Ustawienia.";
    InstanceBuilder::new("StringValue")
        .with_name("CZYTAJ_MNIE")
        .with_property("Value", tekst)
}

fn instrukcja_alarmu() -> InstanceBuilder {
    let tekst = "SYSTEM ALARMOWANIA OSP - INSTALACJA\n\
        Przeciągnij ZAWARTOŚĆ każdego folderu do usługi o tej samej nazwie:\n\
        - Do ReplicatedStorage  -> AlarmOSP  do  ReplicatedStorage\n\
        - Do ServerScriptService -> AlarmSerwer  do  ServerScriptService\n\
        - Do StarterPlayerScripts -> AlarmKlient  do  StarterPlayer > StarterPlayerScripts\n\
        - Do Workspace (przyklad) -> RemizaAlarm i MiejscaZdarzen  do  Workspace\n\
        Obiekty na mapie mają Tagi: AlarmKomputer, AlarmSyrena, AlarmSwiatlo, MiejsceZdarzenia.\n\
        Ustawienia (jednostki, rodzaje, dźwięk syreny): ReplicatedStorage > AlarmOSP > Ustawienia.";
    InstanceBuilder::new("StringValue")
        .with_name("CZYTAJ_MNIE")
        .with_property("Value", tekst)
}

fn instrukcja() -> InstanceBuilder {
    let tekst = "SYSTEM EKWIPUNKU - INSTALACJA\n\
        Przeciągnij ZAWARTOŚĆ każdego folderu do usługi o tej samej nazwie:\n\
        - Do ReplicatedStorage  -> Ekwipunek  do  ReplicatedStorage\n\
        - Do ServerScriptService -> EkwipunekSerwer  do  ServerScriptService\n\
        - Do StarterPlayerScripts -> EkwipunekKlient  do  StarterPlayer > StarterPlayerScripts\n\
        - Do ServerStorage -> EkwipunekPrzedmioty  do  ServerStorage\n\
        - Do Workspace (przyklad) -> StanowiskoOSP  do  Workspace (opcjonalnie, do testów)\n\
        Potem: Game Settings > Avatar > R6 (przedmioty OSP są pod R6).\n\
        Ustawienia: ReplicatedStorage > Ekwipunek > Konfiguracja.";
    InstanceBuilder::new("StringValue")
        .with_name("CZYTAJ_MNIE")
        .with_property("Value", tekst)
}

// ------------------------------------------------------------------
//  Budowanie
// ------------------------------------------------------------------

fn zbuduj(plik_przedmiotow: &Path, projekt: &Path) -> Wynik<()> {
    let zrodla = Zrodla { src: &projekt.join("src") };
    let build = projekt.join("build");

    // 1. Przedmioty klienta: czyszczenie i zapakowanie w folder "OSP"
    let mut przedmioty = wczytaj(plik_przedmiotow)?;
    wyczysc_przedmioty(&mut przedmioty)?;
    let mut osp = WeakDom::new(folder("Korzen"));
    let osp_ref = osp.insert(osp.root_ref(), folder("OSP"));
    let narzedzia: Vec<Ref> = przedmioty.root().children().to_vec();
    for r in narzedzia {
        przedmioty.transfer(r, &mut osp, osp_ref);
    }
    let liczba = osp.get_by_ref(osp_ref).unwrap().children().len();
    println!("przedmioty OSP: {liczba}");
    zapisz(&projekt.join("przedmioty/OSP.rbxm"), &osp, &[osp_ref])?;

    // 2. Paczka do wklejenia do gry
    let mut paczka = WeakDom::new(folder("Korzen"));
    let korzen = paczka.root_ref();
    let glowny = paczka.insert(korzen, folder("SystemEkwipunku").with_child(instrukcja()));
    paczka.insert(glowny, folder("Do ReplicatedStorage").with_child(zrodla.wspolne()?));
    paczka.insert(glowny, folder("Do ServerScriptService").with_child(zrodla.serwer()?));
    paczka.insert(glowny, folder("Do StarterPlayerScripts").with_child(zrodla.klient()?));
    let ss = paczka.insert(glowny, folder("Do ServerStorage"));
    let magazyn = paczka.insert(ss, folder("EkwipunekPrzedmioty").with_child(folder("Obywatel")));
    let kopia = osp.clone_into_external(osp_ref, &mut paczka);
    paczka.transfer_within(kopia, magazyn);
    paczka.insert(glowny, folder("Do Workspace (przyklad)").with_child(zrodla.stanowisko()?));
    zapisz(&build.join("SystemEkwipunku.rbxm"), &paczka, &[glowny])?;

    // 3. Ekran wczytywania: jeden LocalScript do wrzucenia do ReplicatedFirst
    let mut ekran = WeakDom::new(folder("Korzen"));
    let ekran_ref = ekran.insert(ekran.root_ref(), zrodla.ekran_wczytywania()?);
    zapisz(&build.join("EkranWczytywania.rbxm"), &ekran, &[ekran_ref])?;

    // 3b. System alarmowania OSP
    let mut alarm = WeakDom::new(folder("Korzen"));
    let alarm_ref = alarm.insert(
        alarm.root_ref(),
        folder("SystemAlarmowyOSP")
            .with_child(instrukcja_alarmu())
            .with_child(folder("Do ReplicatedStorage").with_child(zrodla.alarm_wspolne()?))
            .with_child(folder("Do ServerScriptService").with_child(zrodla.alarm_serwer()?))
            .with_child(folder("Do StarterPlayerScripts").with_child(zrodla.alarm_klient()?))
            .with_child(
                folder("Do Workspace (przyklad)")
                    .with_child(zrodla.remiza(30.0, -10.0))
                    .with_child(zrodla.miejsca_zdarzen(false)),
            ),
    );
    zapisz(&build.join("SystemAlarmowyOSP.rbxm"), &alarm, &[alarm_ref])?;

    // 3c. Mentor
    let mut mentor = WeakDom::new(folder("Korzen"));
    let mentor_ref = mentor.insert(
        mentor.root_ref(),
        folder("SystemMentora")
            .with_child(instrukcja_mentora())
            .with_child(folder("Do ReplicatedStorage").with_child(zrodla.mentor_wspolne()?))
            .with_child(folder("Do ServerScriptService").with_child(zrodla.mentor_serwer()?))
            .with_child(folder("Do StarterPlayerScripts").with_child(zrodla.mentor_klient()?))
            .with_child(folder("Do Workspace (przyklad)").with_child(zrodla.mentor_punkty(-12.0, 6.0, 12.0, -6.0))),
    );
    zapisz(&build.join("SystemMentora.rbxm"), &mentor, &[mentor_ref])?;

    // 3d. Tablet kierownika PSP
    let mut tablet = WeakDom::new(folder("Korzen"));
    let tablet_ref = tablet.insert(
        tablet.root_ref(),
        folder("TabletKierownikaPSP")
            .with_child(instrukcja_tabletu())
            .with_child(folder("Do ReplicatedStorage").with_child(zrodla.tablet_wspolne()?))
            .with_child(folder("Do ServerScriptService").with_child(zrodla.tablet_serwer()?))
            .with_child(folder("Do StarterPlayerScripts").with_child(zrodla.tablet_klient()?))
            .with_child(
                folder("Do ServerStorage")
                    .with_child(
                        folder("EkwipunekPrzedmioty")
                            .with_child(folder("Kierownik PSP").with_child(zrodla.tablet_narzedzie())),
                    )
                    .with_child(zrodla.pojazdy_psp()),
            )
            .with_child(folder("Do Workspace (przyklad)").with_child(zrodla.remiza_psp(-70.0, -40.0))),
    );
    zapisz(&build.join("TabletKierownikaPSP.rbxm"), &tablet, &[tablet_ref])?;

    // 3e. System wezwań 112
    let mut wezwania = WeakDom::new(folder("Korzen"));
    let wezwania_ref = wezwania.insert(
        wezwania.root_ref(),
        folder("SystemWezwan112")
            .with_child(instrukcja_wezwan())
            .with_child(folder("Do ReplicatedStorage").with_child(zrodla.wezwania_wspolne()?))
            .with_child(folder("Do ServerScriptService").with_child(zrodla.wezwania_serwer()?))
            .with_child(folder("Do StarterPlayerScripts").with_child(zrodla.wezwania_klient()?))
            .with_child(folder("Do Workspace (przyklad)").with_child(zrodla.miejsca_wezwan(false))),
    );
    zapisz(&build.join("SystemWezwan112.rbxm"), &wezwania, &[wezwania_ref])?;

    // 3f. System powiadomień
    let mut powiadomienia = WeakDom::new(folder("Korzen"));
    let powiadomienia_ref = powiadomienia.insert(
        powiadomienia.root_ref(),
        folder("SystemPowiadomien")
            .with_child(instrukcja_powiadomien())
            .with_child(folder("Do ReplicatedStorage").with_child(zrodla.powiadomienia_wspolne()?))
            .with_child(folder("Do ServerScriptService").with_child(zrodla.powiadomienia_serwer()?))
            .with_child(folder("Do StarterPlayerScripts").with_child(zrodla.powiadomienia_klient()?))
            .with_child(folder("Do Workspace (przyklad)").with_child(zrodla.przyklady_powiadomien()?)),
    );
    zapisz(&build.join("SystemPowiadomien.rbxm"), &powiadomienia, &[powiadomienia_ref])?;

    // 4. Gotowe miejsce testowe: wszystko na swoich miejscach
    let mut miejsce = WeakDom::new(InstanceBuilder::new("DataModel"));
    let dm = miejsce.root_ref();
    let workspace = miejsce.insert(
        dm,
        InstanceBuilder::new("Workspace")
            .with_name("Workspace")
            .with_child(
                czesc(
                    "Baseplate",
                    Vector3::new(512.0, 20.0, 512.0),
                    pozycja(0.0, -10.0, 0.0),
                    kolor(91, 93, 105),
                )
                .with_property("Locked", true),
            )
            .with_child(
                InstanceBuilder::new("SpawnLocation")
                    .with_name("SpawnLocation")
                    .with_property("Size", Vector3::new(12.0, 1.0, 12.0))
                    .with_property("CFrame", pozycja(0.0, 0.5, 0.0))
                    .with_property("Color", kolor(163, 162, 165))
                    .with_property("Anchored", true)
                    .with_property("Duration", 0i32),
            )
            .with_child(zrodla.stanowisko()?)
            .with_child(zrodla.remiza(30.0, -10.0))
            .with_child(zrodla.miejsca_zdarzen(true))
            .with_child(zrodla.mentor_punkty(-12.0, 6.0, 12.0, -6.0))
            .with_child(zrodla.remiza_psp(-70.0, -40.0))
            .with_child(zrodla.stanowisko_kierownika()?)
            .with_child(zrodla.scena_wezwan())
            .with_child(zrodla.miejsca_wezwan(true))
            .with_child(zrodla.przyklady_powiadomien()?),
    );
    let _ = workspace;
    miejsce.insert(dm, InstanceBuilder::new("Lighting").with_name("Lighting"));
    miejsce.insert(
        dm,
        InstanceBuilder::new("ReplicatedFirst")
            .with_name("ReplicatedFirst")
            .with_child(zrodla.ekran_wczytywania()?),
    );
    miejsce.insert(
        dm,
        InstanceBuilder::new("ReplicatedStorage")
            .with_name("ReplicatedStorage")
            .with_child(zrodla.wspolne()?)
            .with_child(zrodla.alarm_wspolne()?)
            .with_child(zrodla.mentor_wspolne()?)
            .with_child(zrodla.tablet_wspolne()?)
            .with_child(zrodla.wezwania_wspolne()?)
            .with_child(zrodla.powiadomienia_wspolne()?),
    );
    miejsce.insert(
        dm,
        InstanceBuilder::new("ServerScriptService")
            .with_name("ServerScriptService")
            .with_child(zrodla.serwer()?)
            .with_child(zrodla.alarm_serwer()?)
            .with_child(zrodla.mentor_serwer()?)
            .with_child(zrodla.tablet_serwer()?)
            .with_child(zrodla.wezwania_serwer()?)
            .with_child(zrodla.powiadomienia_serwer()?),
    );
    let magazyn_miejsca = miejsce.insert(
        dm,
        InstanceBuilder::new("ServerStorage")
            .with_name("ServerStorage")
            .with_child(
                folder("EkwipunekPrzedmioty")
                    .with_child(folder("Obywatel"))
                    .with_child(folder("Kierownik PSP").with_child(zrodla.tablet_narzedzie())),
            )
            .with_child(zrodla.pojazdy_psp()),
    );
    let magazyn_przedmiotow = miejsce.get_by_ref(magazyn_miejsca).unwrap().children()[0];
    let kopia = osp.clone_into_external(osp_ref, &mut miejsce);
    miejsce.transfer_within(kopia, magazyn_przedmiotow);
    miejsce.insert(
        dm,
        InstanceBuilder::new("StarterPlayer")
            .with_name("StarterPlayer")
            .with_property("GameSettingsAvatar", Enum::from_u32(0)) // R6
            .with_child(
            InstanceBuilder::new("StarterPlayerScripts")
                .with_name("StarterPlayerScripts")
                .with_child(zrodla.klient()?)
                .with_child(zrodla.alarm_klient()?)
                .with_child(zrodla.mentor_klient()?)
                .with_child(zrodla.tablet_klient()?)
                .with_child(zrodla.wezwania_klient()?)
                .with_child(zrodla.powiadomienia_klient()?),
        ),
    );
    miejsce.insert(
        dm,
        InstanceBuilder::new("Teams")
            .with_name("Teams")
            .with_child(
                InstanceBuilder::new("Team")
                    .with_name("Obywatel")
                    .with_property("TeamColor", BrickColor::InstitutionalWhite)
                    .with_property("AutoAssignable", true),
            )
            .with_child(
                InstanceBuilder::new("Team")
                    .with_name("OSP")
                    .with_property("TeamColor", BrickColor::BrightRed)
                    .with_property("AutoAssignable", false),
            ),
    );
    let uslugi: Vec<Ref> = miejsce.root().children().to_vec();
    zapisz(&build.join("TestEkwipunku.rbxl"), &miejsce, &uslugi)?;

    Ok(())
}

// ------------------------------------------------------------------
//  Podgląd plików i bazy API
// ------------------------------------------------------------------

fn czytaj(sciezka: &Path) -> Wynik<()> {
    let dom = wczytaj(sciezka)?;
    fn wypisz(dom: &WeakDom, r: Ref, glebokosc: usize) {
        let obiekt = dom.get_by_ref(r).unwrap();
        let zrodlo = match obiekt.properties.get(&ustr("Source")) {
            Some(Variant::String(s)) => format!(" [{} B]", s.len()),
            _ => String::new(),
        };
        if glebokosc <= 4 || obiekt.class != "Part" {
            println!("{}{} '{}'{}", "  ".repeat(glebokosc), obiekt.class, obiekt.name, zrodlo);
        }
        if glebokosc < 4 {
            for &dziecko in obiekt.children() {
                wypisz(dom, dziecko, glebokosc + 1);
            }
        }
    }
    for &r in dom.root().children() {
        wypisz(&dom, r, 0);
    }
    println!("obiektów razem: {}", dom.descendants().count() - 1);
    Ok(())
}

fn sprawdz(klasy: &[String]) -> Wynik<()> {
    let baza = rbx_reflection_database::get()?;
    for nazwa in klasy {
        if let Some(wyliczenie) = nazwa.strip_prefix("Enum.") {
            match baza.enums.get(wyliczenie) {
                Some(e) => {
                    let mut pozycje: Vec<String> = e.items.keys().map(|k| k.to_string()).collect();
                    pozycje.sort();
                    println!("{nazwa}: {}", pozycje.join(", "));
                }
                None => println!("{nazwa}: brak w bazie"),
            }
            continue;
        }
        let Some(klasa) = baza.classes.get(nazwa.as_str()) else {
            println!("{nazwa}: brak w bazie");
            continue;
        };
        let lancuch: Vec<String> = baza
            .superclasses(klasa)
            .unwrap_or_default()
            .iter()
            .map(|k| k.name.to_string())
            .collect();
        println!("{nazwa}: {}", lancuch.join(" -> "));
        let mut wlasciwosci: Vec<String> = baza
            .superclasses(klasa)
            .unwrap_or_default()
            .iter()
            .flat_map(|k| k.properties.keys().map(|p| p.to_string()))
            .collect();
        wlasciwosci.sort();
        println!("  {}", wlasciwosci.join(", "));
    }
    Ok(())
}
