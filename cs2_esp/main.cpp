// main.cpp — CS2 external ESP overlay (box + hp bar + nazwa, przez sciany)
// C++17 | MSVC | Windows 11 x64 | target: cs2.exe (Counter-Strike 2, Source 2)
// build:  cl /std:c++17 /EHsc /O2 main.cpp /link user32.lib gdi32.lib dwmapi.lib
// run:    jako administrator (OpenProcess na cs2.exe wymaga elevacji)
// menu:   X = otworz/zamknij | w menu klik myszka toggluje opcje
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <tlhelp32.h>
#include <dwmapi.h>
#include <cstdint>
#include <cmath>
#include <string>
#include <vector>
#include "offsets.hpp"
#pragma comment(lib, "user32.lib")
#pragma comment(lib, "gdi32.lib")
#pragma comment(lib, "dwmapi.lib")

struct vec3 { float x, y, z; };
struct vec2 { float x, y; };

// ---- external memory ----
struct process_t {
    HANDLE handle = nullptr;
    uintptr_t client = 0;      // baza client.dll
    DWORD pid = 0;

    template <typename T> T read(uintptr_t addr) const {
        T v{}; ReadProcessMemory(handle, (LPCVOID)addr, &v, sizeof(T), nullptr); return v;
    }
    bool read_raw(uintptr_t addr, void* buf, size_t n) const {
        return ReadProcessMemory(handle, (LPCVOID)addr, buf, n, nullptr) != 0;
    }
};

static DWORD find_pid(const wchar_t* name) {
    HANDLE snap = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (snap == INVALID_HANDLE_VALUE) return 0;
    PROCESSENTRY32W e{ sizeof(e) };
    DWORD pid = 0;
    for (BOOL ok = Process32FirstW(snap, &e); ok; ok = Process32NextW(snap, &e))
        if (_wcsicmp(e.szExeFile, name) == 0) { pid = e.th32ProcessID; break; }
    CloseHandle(snap);
    return pid;
}

static uintptr_t find_module(DWORD pid, const wchar_t* mod) {
    HANDLE snap = CreateToolhelp32Snapshot(TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32, pid);
    if (snap == INVALID_HANDLE_VALUE) return 0;
    MODULEENTRY32W m{ sizeof(m) };
    uintptr_t base = 0;
    for (BOOL ok = Module32FirstW(snap, &m); ok; ok = Module32NextW(snap, &m))
        if (_wcsicmp(m.szModule, mod) == 0) { base = (uintptr_t)m.modBaseAddr; break; }
    CloseHandle(snap);
    return base;
}

// ---- world -> screen (view matrix 4x4 row-major, float[16]) ----
static bool world_to_screen(const float* vm, const vec3& p, vec2& out, int w, int h) {
    float cw = vm[12] * p.x + vm[13] * p.y + vm[14] * p.z + vm[15];
    if (cw < 0.01f) return false;                     // za kamera / clip
    float cx = vm[0] * p.x + vm[1] * p.y + vm[2] * p.z + vm[3];
    float cy = vm[4] * p.x + vm[5] * p.y + vm[6] * p.z + vm[7];
    float inv = 1.0f / cw;
    out.x = (w * 0.5f) + (cx * inv) * (w * 0.5f);
    out.y = (h * 0.5f) - (cy * inv) * (h * 0.5f);
    return true;
}

// ---- stan / menu ----
struct config_t {
    bool esp        = true;
    bool draw_box   = true;
    bool draw_hp    = true;
    bool draw_name  = true;
    bool team_check = true;   // tylko wrogowie
} cfg;

static bool g_menu_open = false;
static process_t g_proc;
static HWND g_game = nullptr;

// jeden wrog gotowy do rysowania
struct target_t { vec2 head, feet; int hp; std::string name; bool valid; };

static std::vector<target_t> gather_targets(int scr_w, int scr_h) {
    std::vector<target_t> out;
    if (!g_proc.client) return out;

    float vm[16];
    if (!g_proc.read_raw(g_proc.client + offs::dwViewMatrix, vm, sizeof(vm))) return out;

    uintptr_t entity_list = g_proc.read<uintptr_t>(g_proc.client + offs::dwEntityList);
    if (!entity_list) return out;

    uintptr_t local_pawn = g_proc.read<uintptr_t>(g_proc.client + offs::dwLocalPlayerPawn);
    int local_team = local_pawn ? g_proc.read<int>(local_pawn + offs::m_iTeamNum) : 0;

    for (int i = 1; i <= 64; ++i) {
        uintptr_t list_entry = g_proc.read<uintptr_t>(entity_list + (8 * (i >> 9)) + 16);
        if (!list_entry) continue;
        uintptr_t controller = g_proc.read<uintptr_t>(list_entry + 120 * (i & 0x1FF));
        if (!controller) continue;

        uint32_t pawn_handle = g_proc.read<uint32_t>(controller + offs::m_hPlayerPawn);
        if (!pawn_handle || pawn_handle == 0xFFFFFFFF) continue;

        // handle -> pawn przez ten sam entity list
        uintptr_t le2 = g_proc.read<uintptr_t>(entity_list + 0x8 * ((pawn_handle & 0x7FFF) >> 9) + 16);
        if (!le2) continue;
        uintptr_t pawn = g_proc.read<uintptr_t>(le2 + 120 * (pawn_handle & 0x1FF));
        if (!pawn || pawn == local_pawn) continue;

        int life = g_proc.read<int>(pawn + offs::m_lifeState);
        if (life != 0) continue;                      // 0 = zywy
        int hp = g_proc.read<int>(pawn + offs::m_iHealth);
        if (hp <= 0 || hp > 100) continue;

        int team = g_proc.read<int>(pawn + offs::m_iTeamNum);
        if (cfg.team_check && team == local_team) continue;

        uintptr_t scene = g_proc.read<uintptr_t>(pawn + offs::m_pGameSceneNode);
        if (!scene) continue;
        vec3 origin = g_proc.read<vec3>(scene + offs::m_vecAbsOrigin);

        vec3 feet = origin;
        vec3 head = { origin.x, origin.y, origin.z + 72.0f }; // approx wysokosc stojacej postaci

        vec2 sf, sh;
        if (!world_to_screen(vm, feet, sf, scr_w, scr_h)) continue;
        if (!world_to_screen(vm, head, sh, scr_w, scr_h)) continue;

        char name[128]{};
        g_proc.read_raw(controller + offs::m_iszPlayerName, name, sizeof(name) - 1);

        out.push_back({ sh, sf, hp, std::string(name), true });
    }
    return out;
}

// ---- GDI render ----
static void draw_box_line(HDC dc, int x1, int y1, int x2, int y2, HPEN pen) {
    HGDIOBJ old = SelectObject(dc, pen);
    MoveToEx(dc, x1, y1, nullptr); LineTo(dc, x2, y2);
    SelectObject(dc, old);
}

static void render(HDC dc, int w, int h) {
    // czyszczenie na color-key (czarny = przezroczysty)
    RECT full{ 0, 0, w, h };
    FillRect(dc, &full, (HBRUSH)GetStockObject(BLACK_BRUSH));

    HPEN pen_box = CreatePen(PS_SOLID, 1, RGB(255, 40, 40));
    HPEN pen_hp  = CreatePen(PS_SOLID, 1, RGB(0, 0, 0));
    SetBkMode(dc, TRANSPARENT);

    if (cfg.esp) {
        auto targets = gather_targets(w, h);
        for (auto& t : targets) {
            float bh = t.feet.y - t.head.y;            // wysokosc boxa w px
            if (bh < 4.0f) continue;
            float bw = bh * 0.42f;
            int left = (int)(t.head.x - bw * 0.5f);
            int top  = (int)t.head.y;
            int right = (int)(t.head.x + bw * 0.5f);
            int bot  = (int)t.feet.y;

            if (cfg.draw_box) {
                draw_box_line(dc, left, top, right, top, pen_box);
                draw_box_line(dc, left, bot, right, bot, pen_box);
                draw_box_line(dc, left, top, left, bot, pen_box);
                draw_box_line(dc, right, top, right, bot, pen_box);
            }
            if (cfg.draw_hp) {
                int bar_h = bot - top;
                int filled = bar_h * t.hp / 100;
                RECT bg{ left - 6, top, left - 2, bot };
                FillRect(dc, &bg, (HBRUSH)GetStockObject(BLACK_BRUSH));
                int g = 255 * t.hp / 100, r = 255 - g;
                HBRUSH hb = CreateSolidBrush(RGB(r, g, 0));
                RECT fg{ left - 6, bot - filled, left - 2, bot };
                FillRect(dc, &fg, hb);
                DeleteObject(hb);
            }
            if (cfg.draw_name && !t.name.empty()) {
                SetTextColor(dc, RGB(230, 230, 230));
                TextOutA(dc, left, top - 14, t.name.c_str(), (int)t.name.size());
            }
        }
    }

    // --- menu ---
    if (g_menu_open) {
        RECT panel{ 40, 40, 260, 210 };
        HBRUSH bg = CreateSolidBrush(RGB(18, 18, 22));
        FillRect(dc, &panel, bg); DeleteObject(bg);
        SetTextColor(dc, RGB(255, 60, 60));
        TextOutA(dc, 52, 48, "CS2 ESP  [X zamyka]", 19);

        struct row { const char* label; bool* val; };
        row rows[] = {
            { "ESP",        &cfg.esp },
            { "Box",        &cfg.draw_box },
            { "HP bar",     &cfg.draw_hp },
            { "Nazwy",      &cfg.draw_name },
            { "Tylko wrog", &cfg.team_check },
        };
        for (int i = 0; i < 5; ++i) {
            int y = 80 + i * 24;
            SetTextColor(dc, *rows[i].val ? RGB(80, 255, 120) : RGB(120, 120, 120));
            std::string s = std::string("[") + (*rows[i].val ? "x" : " ") + "] " + rows[i].label;
            TextOutA(dc, 56, y, s.c_str(), (int)s.size());
        }
    }

    DeleteObject(pen_box);
    DeleteObject(pen_hp);
}

// klik w menu -> toggle wiersza
static void handle_menu_click(int mx, int my) {
    if (!g_menu_open) return;
    bool* vals[] = { &cfg.esp, &cfg.draw_box, &cfg.draw_hp, &cfg.draw_name, &cfg.team_check };
    for (int i = 0; i < 5; ++i) {
        int y = 80 + i * 24;
        if (mx >= 56 && mx <= 250 && my >= y && my <= y + 18) { *vals[i] = !*vals[i]; break; }
    }
}

static LRESULT CALLBACK wnd_proc(HWND hwnd, UINT msg, WPARAM wp, LPARAM lp) {
    if (msg == WM_DESTROY) { PostQuitMessage(0); return 0; }
    return DefWindowProc(hwnd, msg, wp, lp);
}

int WINAPI WinMain(HINSTANCE inst, HINSTANCE, LPSTR, int) {
    // 1. podlacz sie do gry
    g_proc.pid = find_pid(L"cs2.exe");
    if (!g_proc.pid) { MessageBoxA(nullptr, "cs2.exe nie znaleziony — odpal gre.", "ESP", MB_OK); return 1; }
    g_proc.handle = OpenProcess(PROCESS_VM_READ | PROCESS_QUERY_INFORMATION, FALSE, g_proc.pid);
    if (!g_proc.handle) { MessageBoxA(nullptr, "OpenProcess failed — odpal jako admin.", "ESP", MB_OK); return 1; }
    g_proc.client = find_module(g_proc.pid, L"client.dll");
    if (!g_proc.client) { MessageBoxA(nullptr, "client.dll nie znaleziony.", "ESP", MB_OK); return 1; }
    g_game = FindWindowA(nullptr, "Counter-Strike 2");

    // 2. przezroczyste okno topmost nad gra
    WNDCLASSA wc{};
    wc.lpfnWndProc = wnd_proc;
    wc.hInstance = inst;
    wc.lpszClassName = "cs2_esp_overlay";
    RegisterClassA(&wc);

    HWND hwnd = CreateWindowExA(
        WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_TOPMOST | WS_EX_TOOLWINDOW,
        wc.lpszClassName, "overlay", WS_POPUP,
        0, 0, GetSystemMetrics(SM_CXSCREEN), GetSystemMetrics(SM_CYSCREEN),
        nullptr, nullptr, inst, nullptr);

    SetLayeredWindowAttributes(hwnd, RGB(0, 0, 0), 0, LWA_COLORKEY); // czarny = dziura
    ShowWindow(hwnd, SW_SHOW);

    HDC screen_dc = GetDC(hwnd);
    HDC mem_dc = CreateCompatibleDC(screen_dc);

    bool x_down = false, click_down = false;
    MSG msg{};
    while (true) {
        while (PeekMessage(&msg, nullptr, 0, 0, PM_REMOVE)) {
            if (msg.message == WM_QUIT) goto done;
            TranslateMessage(&msg); DispatchMessage(&msg);
        }

        // X togluje menu (edge)
        bool x_now = (GetAsyncKeyState('X') & 0x8000) != 0;
        if (x_now && !x_down) {
            g_menu_open = !g_menu_open;
            // menu otwarte -> okno lapie klik; zamkniete -> click-through
            LONG ex = GetWindowLong(hwnd, GWL_EXSTYLE);
            if (g_menu_open) ex &= ~WS_EX_TRANSPARENT; else ex |= WS_EX_TRANSPARENT;
            SetWindowLong(hwnd, GWL_EXSTYLE, ex);
        }
        x_down = x_now;

        // klik w menu (edge)
        bool click_now = (GetAsyncKeyState(VK_LBUTTON) & 0x8000) != 0;
        if (click_now && !click_down && g_menu_open) {
            POINT p; GetCursorPos(&p); ScreenToClient(hwnd, &p);
            handle_menu_click(p.x, p.y);
        }
        click_down = click_now;

        // dopasuj overlay do okna gry (windowed/fullscreen-windowed)
        int w = GetSystemMetrics(SM_CXSCREEN), h = GetSystemMetrics(SM_CYSCREEN);
        if (g_game && IsWindow(g_game)) {
            RECT gr; if (GetClientRect(g_game, &gr)) {
                POINT tl{ 0,0 }; ClientToScreen(g_game, &tl);
                SetWindowPos(hwnd, HWND_TOPMOST, tl.x, tl.y, gr.right, gr.bottom, SWP_NOACTIVATE);
                w = gr.right; h = gr.bottom;
            }
        }

        // double buffer -> brak migotania
        HBITMAP bmp = CreateCompatibleBitmap(screen_dc, w, h);
        HGDIOBJ old = SelectObject(mem_dc, bmp);
        render(mem_dc, w, h);
        BitBlt(screen_dc, 0, 0, w, h, mem_dc, 0, 0, SRCCOPY);
        SelectObject(mem_dc, old);
        DeleteObject(bmp);

        Sleep(6); // ~160 fps overlay
    }
done:
    DeleteDC(mem_dc);
    ReleaseDC(hwnd, screen_dc);
    CloseHandle(g_proc.handle);
    return 0;
}
