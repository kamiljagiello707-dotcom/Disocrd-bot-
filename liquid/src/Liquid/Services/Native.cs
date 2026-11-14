using System;
using System.Runtime.InteropServices;

namespace Liquid.Services
{
    internal static class Native
    {
        public const int GWL_EXSTYLE = -20;
        public const int WS_EX_TRANSPARENT = 0x00000020;
        public const int WS_EX_TOOLWINDOW = 0x00000080;
        public const int WS_EX_LAYERED = 0x00080000;
        public const int WS_EX_NOACTIVATE = 0x08000000;
        public const int WM_HOTKEY = 0x0312;
        public const uint MOD_ALT = 0x1, MOD_CONTROL = 0x2, MOD_SHIFT = 0x4, MOD_NOREPEAT = 0x4000;
        public static readonly IntPtr HWND_TOPMOST = new IntPtr(-1);
        public const uint SWP_NOSIZE = 0x1, SWP_NOMOVE = 0x2, SWP_NOACTIVATE = 0x10, SWP_SHOWWINDOW = 0x40;

        [DllImport("user32.dll", EntryPoint = "GetWindowLongPtrW")]
        private static extern IntPtr GetWindowLongPtr(IntPtr hWnd, int nIndex);
        [DllImport("user32.dll", EntryPoint = "SetWindowLongPtrW")]
        private static extern IntPtr SetWindowLongPtr(IntPtr hWnd, int nIndex, IntPtr value);

        public static void AddExStyle(IntPtr hwnd, int flags)
        {
            long cur = GetWindowLongPtr(hwnd, GWL_EXSTYLE).ToInt64();
            SetWindowLongPtr(hwnd, GWL_EXSTYLE, new IntPtr(cur | (long)flags));
        }

        [DllImport("user32.dll")]
        public static extern bool SetWindowPos(IntPtr hWnd, IntPtr after, int x, int y, int cx, int cy, uint flags);

        [DllImport("user32.dll")]
        public static extern IntPtr GetForegroundWindow();

        [DllImport("user32.dll")]
        public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint pid);

        [DllImport("user32.dll", SetLastError = true)]
        public static extern bool RegisterHotKey(IntPtr hWnd, int id, uint modifiers, uint vk);

        [DllImport("user32.dll")]
        public static extern bool UnregisterHotKey(IntPtr hWnd, int id);

        [DllImport("dwmapi.dll")]
        private static extern int DwmSetWindowAttribute(IntPtr hwnd, int attr, ref int value, int size);

        public static void ApplyDarkRoundedFrame(IntPtr hwnd)
        {
            try
            {
                int dark = 1; DwmSetWindowAttribute(hwnd, 20, ref dark, sizeof(int));   // tryb ciemny
                int round = 2; DwmSetWindowAttribute(hwnd, 33, ref round, sizeof(int)); // zaokrąglone rogi (Win 11)
                int border = unchecked((int)0x00262626); DwmSetWindowAttribute(hwnd, 34, ref border, sizeof(int));
            }
            catch { }
        }

        public static int ForegroundProcessId()
        {
            try
            {
                var h = GetForegroundWindow();
                if (h == IntPtr.Zero) return 0;
                GetWindowThreadProcessId(h, out uint pid);
                return (int)pid;
            }
            catch { return 0; }
        }
    }
}
