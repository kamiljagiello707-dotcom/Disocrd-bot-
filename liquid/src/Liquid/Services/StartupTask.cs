using System;
using System.Diagnostics;

namespace Liquid.Services
{
    /// <summary>Autostart z uprawnieniami administratora (Harmonogram zadań).</summary>
    public static class StartupTask
    {
        public static bool Set(bool enable)
        {
            try
            {
                string exe = Environment.ProcessPath ?? "";
                string args = enable
                    ? $"/Create /F /RL HIGHEST /SC ONLOGON /TN \"Liquid\" /TR \"\\\"{exe}\\\" --minimized\""
                    : "/Delete /F /TN \"Liquid\"";
                var psi = new ProcessStartInfo("schtasks.exe", args) { CreateNoWindow = true, UseShellExecute = false };
                using var p = Process.Start(psi);
                p?.WaitForExit(8000);
                return p != null && p.ExitCode == 0;
            }
            catch { return false; }
        }
    }
}
