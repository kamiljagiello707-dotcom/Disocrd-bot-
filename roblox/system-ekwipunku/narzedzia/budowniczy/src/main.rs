//! Składa System Ekwipunku w pliki dla Roblox Studio.
//!
//!   budowniczy zbuduj <Items.rbxm> <katalog projektu>
//!       -> przedmioty/OSP.rbxm, build/SystemEkwipunku.rbxm, build/TestEkwipunku.rbxl
//!   budowniczy czytaj <plik.rbxm|.rbxl>     (wypisuje drzewo, do sprawdzania)
//!   budowniczy sprawdz <Klasa>...           (klasy bazowe i właściwości z bazy API)

use std::fs::{self, File};
use std::io::{BufReader, BufWriter};
use std::path::Path;

use rbx_dom_weak::types::{
    BrickColor, CFrame, Color3, Color3uint8, Enum, Matrix3, Ref, UDim, UDim2, Variant, Vector3,
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

    // 3. Gotowe miejsce testowe: wszystko na swoich miejscach
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
            .with_child(zrodla.stanowisko()?),
    );
    let _ = workspace;
    miejsce.insert(dm, InstanceBuilder::new("Lighting").with_name("Lighting"));
    miejsce.insert(
        dm,
        InstanceBuilder::new("ReplicatedStorage")
            .with_name("ReplicatedStorage")
            .with_child(zrodla.wspolne()?),
    );
    miejsce.insert(
        dm,
        InstanceBuilder::new("ServerScriptService")
            .with_name("ServerScriptService")
            .with_child(zrodla.serwer()?),
    );
    let magazyn_miejsca = miejsce.insert(
        dm,
        InstanceBuilder::new("ServerStorage")
            .with_name("ServerStorage")
            .with_child(folder("EkwipunekPrzedmioty").with_child(folder("Obywatel"))),
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
                .with_child(zrodla.klient()?),
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
