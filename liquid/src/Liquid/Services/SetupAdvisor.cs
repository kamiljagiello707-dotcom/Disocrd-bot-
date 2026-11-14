using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.RegularExpressions;

namespace Liquid.Services
{
    public sealed class Verdict
    {
        public int Score;               // 0-100
        public int Cpu, Gpu, Ram, Disk; // punkty cząstkowe
        public const int CpuMax = 30, GpuMax = 35, RamMax = 20, DiskMax = 15;
        public string Rating = "";
        public string Message = "";
        public List<string> Pros = new List<string>();
        public List<string> Cons = new List<string>();
    }

    /// <summary>Wiadomość od Liquid: czy setup jest dobry.</summary>
    public static class SetupAdvisor
    {
        public static Verdict Evaluate(SystemSpecs s)
        {
            var v = new Verdict();
            string cpuLabel = ShortCpu(s.CpuName);

            // ---------- CPU ----------
            int threadsPts = s.Threads >= 16 ? 30 : s.Threads >= 12 ? 26 : s.Threads >= 8 ? 21 : s.Threads >= 6 ? 15 : s.Threads >= 4 ? 9 : 4;
            int tier = CpuTier(s.CpuName, out bool old);
            int cpu = threadsPts;
            if (tier > 0) cpu = (int)Math.Round(threadsPts * 0.5 + new[] { 0, 6, 13, 21, 26, 30 }[tier] * 0.5);
            if (old) cpu -= 7;
            v.Cpu = Clamp(cpu, 2, Verdict.CpuMax);

            // ---------- GPU ----------
            var g = s.Gpus.FirstOrDefault();
            v.Gpu = g == null ? 3 : GpuScore(g);

            // ---------- RAM ----------
            v.Ram = s.RamGb >= 32 ? 20 : s.RamGb >= 16 ? 16 : s.RamGb >= 12 ? 11 : s.RamGb >= 8 ? 7 : 3;
            if (s.RamModules == 1 && s.RamGb >= 8) v.Ram -= 2;
            v.Ram = Clamp(v.Ram, 1, Verdict.RamMax);

            // ---------- Dysk ----------
            var sys = s.Disks.FirstOrDefault(d => d.IsSystem) ?? s.Disks.FirstOrDefault();
            bool anySsd = s.Disks.Any(d => d.Type == "SSD" || d.Bus == "NVMe");
            if (sys != null && sys.Bus == "NVMe") v.Disk = 15;
            else if (sys != null && sys.Type == "SSD") v.Disk = 12;
            else if (anySsd) v.Disk = 9;
            else if (sys != null && sys.Type == "HDD") v.Disk = 3;
            else v.Disk = 8;

            v.Score = Clamp(v.Cpu + v.Gpu + v.Ram + v.Disk, 0, 100);

            // ---------- Werdykt ----------
            string usage;
            if (v.Score >= 85) { v.Rating = "Topowy setup"; usage = "Odpalisz praktycznie każdą grę w wysokich ustawieniach, także w 1440p/4K, a do tego streaming i montaż bez zadyszki."; }
            else if (v.Score >= 70) { v.Rating = "Bardzo dobry setup"; usage = "Nowe gry pójdą płynnie w 1080p/1440p na wysokich ustawieniach. Świetna maszyna do grania i pracy."; }
            else if (v.Score >= 55) { v.Rating = "Dobry setup"; usage = "Większość gier pójdzie płynnie w 1080p na średnich/wysokich. W najcięższych tytułach może trzeba zejść z detalami."; }
            else if (v.Score >= 40) { v.Rating = "Przeciętny setup"; usage = "Gry e-sportowe (CS2, Valorant, LoL, Fortnite) pójdą dobrze, ale nowe AAA będą wymagały niskich ustawień."; }
            else { v.Rating = "Słaby setup"; usage = "Do internetu, nauki i lekkich gier wystarczy, ale nowsze gry mogą się mocno przycinać."; }

            // mocne strony
            if (v.Gpu >= 28) v.Pros.Add($"Mocna karta graficzna ({g?.Name}).");
            if (v.Cpu >= 24) v.Pros.Add($"Wydajny procesor — {s.Cores} rdzeni / {s.Threads} wątków.");
            if (s.RamGb >= 16) v.Pros.Add($"{s.RamGb:0} GB RAM — wystarczająco na gry i multitasking.");
            if (v.Disk >= 12) v.Pros.Add(sys?.Bus == "NVMe" ? "System na szybkim dysku NVMe — błyskawiczne wczytywanie." : "System na dysku SSD.");
            if (s.RefreshRate >= 120) v.Pros.Add($"Monitor {s.RefreshRate} Hz — płynny obraz w grach.");

            // słabe strony + porady
            if (g == null || g.Integrated) v.Cons.Add("Brak dedykowanej karty graficznej — to największe ograniczenie w grach. Dołożenie GPU da ogromny skok FPS.");
            else if (v.Gpu < 18) v.Cons.Add($"Karta graficzna ({g.Name}) jest słaba jak na dzisiejsze gry — rozważ wymianę (np. RTX 4060 / RX 7600 lub lepsza).");
            if (v.Cpu < 15) v.Cons.Add($"Procesor ({cpuLabel}) może ograniczać kartę graficzną (bottleneck) — szczególnie w grach e-sportowych z wysokim FPS.");
            if (s.RamGb < 16) v.Cons.Add($"Tylko {s.RamGb:0} GB RAM — zalecane minimum do gier to 16 GB.");
            if (s.RamModules == 1 && s.RamGb >= 8) v.Cons.Add("Jedna kość RAM = tryb Single Channel. Dwie kości (Dual Channel) dadzą nawet +10-20% FPS.");
            if (s.RamType == "DDR4" && s.RamSpeed > 0 && s.RamSpeed < 3000) v.Cons.Add($"RAM pracuje na {s.RamSpeed} MT/s — sprawdź, czy w BIOS włączony jest profil XMP/DOCP.");
            if (s.RamType == "DDR5" && s.RamSpeed > 0 && s.RamSpeed <= 4800) v.Cons.Add($"DDR5 na {s.RamSpeed} MT/s — włącz profil XMP/EXPO w BIOS, żeby odblokować pełną prędkość.");
            if (v.Disk <= 3) v.Cons.Add("System na dysku HDD — przesiadka na SSD to najtańszy sposób na szybszy komputer.");
            if (s.RefreshRate > 0 && s.RefreshRate < 75 && v.Gpu >= 22) v.Cons.Add($"Monitor {s.RefreshRate} Hz marnuje potencjał karty — sprawdź ustawienia odświeżania albo pomyśl o monitorze 144 Hz+.");

            // najsłabsze ogniwo
            var parts = new (string name, double ratio)[]
            {
                ("procesor", v.Cpu / (double)Verdict.CpuMax),
                ("karta graficzna", v.Gpu / (double)Verdict.GpuMax),
                ("pamięć RAM", v.Ram / (double)Verdict.RamMax),
                ("dysk", v.Disk / (double)Verdict.DiskMax)
            };
            var weakest = parts.OrderBy(p => p.ratio).First();
            string weakMsg = weakest.ratio < 0.7
                ? $" Najsłabszym ogniwem jest {weakest.name} — to od niego zacznij ewentualny upgrade."
                : " Podzespoły są dobrze zbalansowane — nic tu wyraźnie nie odstaje.";

            v.Message = $"{v.Rating}! {usage}{weakMsg}";
            return v;
        }

        private static int GpuScore(GpuInfo g)
        {
            if (g.Integrated) return 5;
            string n = g.Name.ToUpperInvariant();
            int score = -1;

            var m = Regex.Match(n, @"RTX\s*(\d{2})(\d{2})");
            if (m.Success)
            {
                int series = int.Parse(m.Groups[1].Value), t = int.Parse(m.Groups[2].Value);
                score = t >= 90 ? 35 : t >= 80 ? 33 : t >= 70 ? 30 : t >= 60 ? 25 : 18;
                if (n.Contains("TI") || n.Contains("SUPER")) score += 1;
                score += series >= 40 ? 0 : series == 30 ? -3 : -7;
                if (n.Contains("LAPTOP") || n.Contains("MOBILE")) score -= 3;
            }
            else if ((m = Regex.Match(n, @"GTX\s*(\d{2})(\d{2})")).Success)
            {
                int series = int.Parse(m.Groups[1].Value), t = int.Parse(m.Groups[2].Value);
                score = series == 16 ? (t >= 60 ? 16 : 13) : series == 10 ? (t >= 80 ? 17 : t >= 70 ? 15 : t >= 60 ? 12 : 8) : 6;
            }
            else if ((m = Regex.Match(n, @"RX\s*(\d)(\d)(\d)0")).Success)
            {
                int gen = int.Parse(m.Groups[1].Value), t = int.Parse(m.Groups[2].Value);
                if (gen == 9) score = t >= 7 ? 31 : 25;
                else if (gen == 7) score = t >= 9 ? 34 : t >= 8 ? 30 : t >= 7 ? 27 : 22;
                else if (gen == 6) score = t >= 9 ? 29 : t >= 8 ? 27 : t >= 7 ? 24 : t >= 6 ? 20 : 13;
                else if (gen == 5) score = t >= 7 ? 18 : 13;
                else score = 9;
                if (n.Contains("XT")) score += 1;
            }
            else if ((m = Regex.Match(n, @"RX\s*(\d{3})\b")).Success) score = 10;
            else if (Regex.IsMatch(n, @"ARC.*(A7|B5|B7)")) score = 22;
            else if (Regex.IsMatch(n, @"ARC.*A[35]")) score = 14;

            if (score < 0)
            {
                double vr = g.VramGb;
                score = vr >= 12 ? 28 : vr >= 8 ? 22 : vr >= 6 ? 16 : vr >= 4 ? 10 : 6;
            }
            return Clamp(score, 3, Verdict.GpuMax);
        }

        private static int CpuTier(string name, out bool old)
        {
            old = false;
            string n = (name ?? "").ToUpperInvariant();
            int tier = 0;
            if (Regex.IsMatch(n, @"I9|RYZEN 9|ULTRA 9")) tier = 5;
            else if (Regex.IsMatch(n, @"I7|RYZEN 7|ULTRA 7")) tier = 4;
            else if (Regex.IsMatch(n, @"I5|RYZEN 5|ULTRA 5")) tier = 3;
            else if (Regex.IsMatch(n, @"I3|RYZEN 3")) tier = 2;
            else if (Regex.IsMatch(n, @"CELERON|PENTIUM|ATHLON|ATOM")) tier = 1;

            var mi = Regex.Match(n, @"I[3579]-(\d{4,5})");
            if (mi.Success)
            {
                string num = mi.Groups[1].Value;
                int gen = num.Length == 5 ? int.Parse(num.Substring(0, 2)) : int.Parse(num.Substring(0, 1));
                if (gen < 8) old = true;
            }
            var ma = Regex.Match(n, @"RYZEN \d (\d)\d{3}");
            if (ma.Success && int.Parse(ma.Groups[1].Value) < 2) old = true;
            return tier;
        }

        private static string ShortCpu(string n) =>
            Regex.Replace(n ?? "", @"\(R\)|\(TM\)|CPU|Processor|\d+-Core|@.*$", "", RegexOptions.IgnoreCase).Replace("  ", " ").Trim();

        private static int Clamp(int v, int lo, int hi) => v < lo ? lo : v > hi ? hi : v;
    }
}
