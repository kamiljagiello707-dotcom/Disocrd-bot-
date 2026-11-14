using System;
using System.Windows;

namespace Liquid.Services
{
    public sealed class TrayService : IDisposable
    {
        private readonly System.Windows.Forms.NotifyIcon _icon;
        private readonly System.Windows.Forms.ToolStripMenuItem _counter, _overlay;

        public TrayService()
        {
            _icon = new System.Windows.Forms.NotifyIcon { Text = "Liquid", Visible = true };
            try
            {
                var res = Application.GetResourceStream(new Uri("pack://application:,,,/Assets/liquid.ico"));
                if (res != null) _icon.Icon = new System.Drawing.Icon(res.Stream);
            }
            catch { }
            if (_icon.Icon == null) _icon.Icon = System.Drawing.SystemIcons.Application;

            var menu = new System.Windows.Forms.ContextMenuStrip();
            menu.Items.Add("Otwórz Liquid", null, (s, e) => AppServices.ShowMain());
            _counter = new System.Windows.Forms.ToolStripMenuItem("Licznik FPS", null, (s, e) =>
            {
                if (AppServices.Hub.CounterOn) AppServices.Hub.StopCounter(); else AppServices.Hub.StartCounter();
            });
            _overlay = new System.Windows.Forms.ToolStripMenuItem("Overlay w grze  (Ctrl+Shift+F10)", null, (s, e) => AppServices.Hub.ToggleOverlay());
            menu.Items.Add(_counter);
            menu.Items.Add(_overlay);
            menu.Items.Add(new System.Windows.Forms.ToolStripSeparator());
            menu.Items.Add("Zamknij", null, (s, e) => AppServices.Main?.Quit());
            menu.Opening += (s, e) =>
            {
                _counter.Checked = AppServices.Hub.CounterOn;
                _overlay.Checked = AppServices.Hub.OverlayOn;
            };
            _icon.ContextMenuStrip = menu;
            _icon.DoubleClick += (s, e) => AppServices.ShowMain();
        }

        public void Dispose()
        {
            _icon.Visible = false;
            _icon.Dispose();
        }
    }
}
