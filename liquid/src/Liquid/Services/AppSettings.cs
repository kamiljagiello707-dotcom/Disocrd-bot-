using System;
using System.IO;
using System.Text.Json;

namespace Liquid.Services
{
    public sealed class AppSettings
    {
        public bool Notifications { get; set; } = true;
        public bool MinimizeToTray { get; set; } = true;
        public bool StartWithWindows { get; set; } = false;
        public bool TempWarnings { get; set; } = true;
        public int CpuWarnTemp { get; set; } = 90;
        public int GpuWarnTemp { get; set; } = 85;

        public string OverlayCorner { get; set; } = "TopLeft";
        public bool OvFrametime { get; set; } = true;
        public bool OvLows { get; set; } = true;
        public bool OvCpu { get; set; } = true;
        public bool OvGpu { get; set; } = true;
        public bool OvRam { get; set; } = false;
        public bool OvClock { get; set; } = false;
        public double OverlayScale { get; set; } = 1.0;
        public double OverlayOpacity { get; set; } = 0.85;
        public string TargetProcess { get; set; } = "";
        public bool TrayHintShown { get; set; } = false;

        public static string Folder =>
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Liquid");
        private static string FilePath => Path.Combine(Folder, "settings.json");

        public static AppSettings Load()
        {
            try
            {
                if (File.Exists(FilePath))
                    return JsonSerializer.Deserialize<AppSettings>(File.ReadAllText(FilePath)) ?? new AppSettings();
            }
            catch { }
            return new AppSettings();
        }

        public void Save()
        {
            try
            {
                Directory.CreateDirectory(Folder);
                File.WriteAllText(FilePath, JsonSerializer.Serialize(this, new JsonSerializerOptions { WriteIndented = true }));
            }
            catch { }
        }
    }
}
