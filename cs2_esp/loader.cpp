// loader.cpp — CS2 external ESP, single-exe loader (offsety z offsets.ini obok exe)
// target: cs2.exe (Source 2) | Windows x64 | X = menu, klik = toggle
// build (Linux->Win): x86_64-w64-mingw32-g++ -std=c++17 -O2 -static -mwindows
//                     loader.cpp -o esp.exe -luser32 -lgdi32 -ldwmapi
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <tlhelp32.h>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>

struct vec3 { float x, y, z; };
struct vec2 { float x, y; };

// ---- offsety z pliku (re-dump co patch, zero rekompilacji) ----
struct offsets_t {
    uintptr_t dwEntityList = 0, dwViewMatrix = 0, dwLocalPlayerPawn = 0;
    uintptr_t m_hPlayerPawn = 0, m_iszPlayerName = 0, m_iHealth = 0;
    uintptr_t m_iTeamNum = 0, m_lifeState = 0, m_pGameSceneNode = 0, m_vecAbsOrigin = 0;
} offs;

static void set_offset(const std::string& k, uintptr_t v) {
    if (k == "dwEntityList") offs.dwEntityList = v;
    else if (k == "dwViewMatrix") offs.dwViewMatrix = v;
    else if (k == "dwLocalPlayerPawn") offs.dwLocalPlayerPawn = v;
    else if (k == "m_hPlayerPawn") offs.m_hPlayerPawn = v;
    else if (k == "m_iszPlayerName") offs.m_iszPlayerName = v;
    else if (k == "m_iHealth") offs.m_iHealth = v;
    else if (k == "m_iTeamNum") offs.m_iTeamNum = v;
    else if (k == "m_lifeState") offs.m_lifeState = v;
    else if (k == "m_pGameSceneNode") offs.m_pGameSceneNode = v;
    else if (k == "m_vecAbsOrigin") offs.m_vecAbsOrigin = v;
}

static bool load_ini(const char* path) {
    FILE* f = fopen(path, "r");
    if (!f) return false;
    char line[256];
    int got = 0;
    while (fgets(line, sizeof(line), f)) {
        char* eq = strchr(line, '=');
        if (!eq || line[0] == ';' || line[0] == '#') continue;
        *eq = 0;
        std::string key(line);
        key.erase(0, key.find_first_not_of(" \t"));
        key.erase(key.find_last_not_of(" \t\r\n") + 1);
        uintptr_t val = (uintptr_t)strtoull(eq + 1, nullptr, 0); // 0x.. lub dec
        set_offset(key, val);
        ++got;
    }
    fclose(f);
    return got >= 10;
}

// ---- external memory ----
struct process_t {
    HANDLE handle = nullptr;
    uintptr_t client = 0;
    DWORD pid = 0;
    template <typename T> T read(uintptr_t a) const {
        T v{}; ReadProcessMemory(handle, (LPCVOID)a, &v, sizeof(T), nullptr); return v;
    }
    bool read_raw(uintptr_t a, void* b, size_t n) const {
        return ReadProcessMemory(handle, (LPCVOID)a, b, n, nullptr) != 0;
    }
} g_proc;

static DWORD find_pid(const wchar_t* name) {
    HANDLE s = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (s == INVALID_HANDLE_VALUE) return 0;
    PROCESSENTRY32W e{ sizeof(e) }; DWORD pid = 0;
    for (BOOL ok = Process32FirstW(s, &e); ok; ok = Process32NextW(s, &e))
        if (_wcsicmp(e.szExeFile, name) == 0) { pid = e.th32ProcessID; break; }
    CloseHandle(s); return pid;
}
static uintptr_t find_module(DWORD pid, const wchar_t* mod) {
    HANDLE s = CreateToolhelp32Snapshot(TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32, pid);
    if (s == INVALID_HANDLE_VALUE) return 0;
    MODULEENTRY32W m{ sizeof(m) }; uintptr_t b = 0;
    for (BOOL ok = Module32FirstW(s, &m); ok; ok = Module32NextW(s, &m))
        if (_wcsicmp(m.szModule, mod) == 0) { b = (uintptr_t)m.modBaseAddr; break; }
    CloseHandle(s); return b;
}

static bool world_to_screen(const float* vm, const vec3& p, vec2& o, int w, int h) {
    float cw = vm[12]*p.x + vm[13]*p.y + vm[14]*p.z + vm[15];
    if (cw < 0.01f) return false;
    float cx = vm[0]*p.x + vm[1]*p.y + vm[2]*p.z + vm[3];
    float cy = vm[4]*p.x + vm[5]*p.y + vm[6]*p.z + vm[7];
    float inv = 1.0f / cw;
    o.x = (w*0.5f) + cx*inv*(w*0.5f);
    o.y = (h*0.5f) - cy*inv*(h*0.5f);
    return true;
}

struct config_t { bool esp=true, box=true, hp=true, name=true, team_check=true; } cfg;
static bool g_menu_open = false;
static HWND g_game = nullptr;

struct target_t { vec2 head, feet; int hp; std::string name; };

static std::vector<target_t> gather(int sw, int sh) {
    std::vector<target_t> out;
    if (!g_proc.client) return out;
    float vm[16];
    if (!g_proc.read_raw(g_proc.client + offs.dwViewMatrix, vm, sizeof(vm))) return out;
    uintptr_t elist = g_proc.read<uintptr_t>(g_proc.client + offs.dwEntityList);
    if (!elist) return out;
    uintptr_t local = g_proc.read<uintptr_t>(g_proc.client + offs.dwLocalPlayerPawn);
    int lteam = local ? g_proc.read<int>(local + offs.m_iTeamNum) : 0;

    for (int i = 1; i <= 64; ++i) {
        uintptr_t le = g_proc.read<uintptr_t>(elist + (8 * (i >> 9)) + 16);
        if (!le) continue;
        uintptr_t ctrl = g_proc.read<uintptr_t>(le + 120 * (i & 0x1FF));
        if (!ctrl) continue;
        uint32_t h = g_proc.read<uint32_t>(ctrl + offs.m_hPlayerPawn);
        if (!h || h == 0xFFFFFFFF) continue;
        uintptr_t le2 = g_proc.read<uintptr_t>(elist + 0x8 * ((h & 0x7FFF) >> 9) + 16);
        if (!le2) continue;
        uintptr_t pawn = g_proc.read<uintptr_t>(le2 + 120 * (h & 0x1FF));
        if (!pawn || pawn == local) continue;
        if (g_proc.read<int>(pawn + offs.m_lifeState) != 0) continue;
        int hp = g_proc.read<int>(pawn + offs.m_iHealth);
        if (hp <= 0 || hp > 100) continue;
        int team = g_proc.read<int>(pawn + offs.m_iTeamNum);
        if (cfg.team_check && team == lteam) continue;
        uintptr_t scene = g_proc.read<uintptr_t>(pawn + offs.m_pGameSceneNode);
        if (!scene) continue;
        vec3 origin = g_proc.read<vec3>(scene + offs.m_vecAbsOrigin);
        vec3 feet = origin, head = { origin.x, origin.y, origin.z + 72.0f };
        vec2 sf, sh2;
        if (!world_to_screen(vm, feet, sf, sw, sh)) continue;
        if (!world_to_screen(vm, head, sh2, sw, sh)) continue;
        char nm[128]{}; g_proc.read_raw(ctrl + offs.m_iszPlayerName, nm, sizeof(nm)-1);
        out.push_back({ sh2, sf, hp, std::string(nm) });
    }
    return out;
}

static void line(HDC dc, int x1, int y1, int x2, int y2, HPEN p) {
    HGDIOBJ o = SelectObject(dc, p);
    MoveToEx(dc, x1, y1, nullptr); LineTo(dc, x2, y2);
    SelectObject(dc, o);
}

static void render(HDC dc, int w, int h) {
    RECT full{ 0,0,w,h };
    HBRUSH key = CreateSolidBrush(RGB(255, 0, 255)); // magenta = dziura (color-key)
    FillRect(dc, &full, key); DeleteObject(key);
    HBRUSH black = (HBRUSH)GetStockObject(BLACK_BRUSH);
    SetBkMode(dc, TRANSPARENT);

    if (cfg.esp) {
        for (auto& t : gather(w, h)) {
            float bh = t.feet.y - t.head.y;
            if (bh < 4.0f) continue;
            float bw = bh * 0.42f;
            int l = (int)(t.head.x - bw*0.5f), r = (int)(t.head.x + bw*0.5f);
            int top = (int)t.head.y, bot = (int)t.feet.y;
            if (cfg.box) {
                RECT body{ l, top, r, bot };
                FillRect(dc, &body, black); // pelna czarna sylwetka
            }
            if (cfg.hp) {
                int filled = (bot - top) * t.hp / 100;
                RECT bg{ l-6, top, l-2, bot };
                FillRect(dc, &bg, (HBRUSH)GetStockObject(BLACK_BRUSH));
                int g = 255*t.hp/100, rr = 255 - g;
                HBRUSH b = CreateSolidBrush(RGB(rr, g, 0));
                RECT fg{ l-6, bot-filled, l-2, bot };
                FillRect(dc, &fg, b); DeleteObject(b);
            }
            if (cfg.name && !t.name.empty()) {
                SetTextColor(dc, RGB(255, 60, 60));
                TextOutA(dc, l, top - 14, t.name.c_str(), (int)t.name.size());
            }
        }
    }

    if (g_menu_open) {
        RECT panel{ 40,40,260,210 };
        HBRUSH bg = CreateSolidBrush(RGB(18,18,22));
        FillRect(dc, &panel, bg); DeleteObject(bg);
        SetTextColor(dc, RGB(255,60,60));
        TextOutA(dc, 52, 48, "CS2 WH  C=ESP  X=menu", 21);
        struct row { const char* l; bool* v; };
        row rows[] = { {"ESP (C)",&cfg.esp},{"Czarna sylwetka",&cfg.box},{"HP bar",&cfg.hp},
                       {"Nazwy",&cfg.name},{"Tylko wrog",&cfg.team_check} };
        for (int i = 0; i < 5; ++i) {
            int y = 80 + i*24;
            SetTextColor(dc, *rows[i].v ? RGB(80,255,120) : RGB(120,120,120));
            std::string s = std::string("[") + (*rows[i].v ? "x" : " ") + "] " + rows[i].l;
            TextOutA(dc, 56, y, s.c_str(), (int)s.size());
        }
    }
}

static void menu_click(int mx, int my) {
    bool* v[] = { &cfg.esp,&cfg.box,&cfg.hp,&cfg.name,&cfg.team_check };
    for (int i = 0; i < 5; ++i) {
        int y = 80 + i*24;
        if (mx >= 56 && mx <= 250 && my >= y && my <= y+18) { *v[i] = !*v[i]; break; }
    }
}

static LRESULT CALLBACK wproc(HWND hw, UINT m, WPARAM wp, LPARAM lp) {
    if (m == WM_DESTROY) { PostQuitMessage(0); return 0; }
    return DefWindowProc(hw, m, wp, lp);
}

// sciezka do offsets.ini obok exe
static std::string ini_path_next_to_exe() {
    char buf[MAX_PATH]{}; GetModuleFileNameA(nullptr, buf, MAX_PATH);
    std::string p(buf); auto s = p.find_last_of("\\/");
    return (s == std::string::npos ? std::string() : p.substr(0, s+1)) + "offsets.ini";
}

int WINAPI WinMain(HINSTANCE inst, HINSTANCE, LPSTR, int) {
    std::string ini = ini_path_next_to_exe();
    if (!load_ini(ini.c_str())) {
        MessageBoxA(nullptr, ("Brak/niepelny offsets.ini obok exe:\n" + ini +
            "\n\nWklej swiezy dump (10 wartosci).").c_str(), "ESP", MB_OK | MB_ICONERROR);
        return 1;
    }
    g_proc.pid = find_pid(L"cs2.exe");
    if (!g_proc.pid) { MessageBoxA(nullptr, "cs2.exe nie znaleziony — odpal gre.", "ESP", MB_OK); return 1; }
    g_proc.handle = OpenProcess(PROCESS_VM_READ | PROCESS_QUERY_INFORMATION, FALSE, g_proc.pid);
    if (!g_proc.handle) { MessageBoxA(nullptr, "OpenProcess failed — odpal jako admin.", "ESP", MB_OK); return 1; }
    g_proc.client = find_module(g_proc.pid, L"client.dll");
    if (!g_proc.client) { MessageBoxA(nullptr, "client.dll nie znaleziony.", "ESP", MB_OK); return 1; }
    g_game = FindWindowA(nullptr, "Counter-Strike 2");

    WNDCLASSA wc{}; wc.lpfnWndProc = wproc; wc.hInstance = inst; wc.lpszClassName = "cs2_ovl";
    RegisterClassA(&wc);
    HWND hwnd = CreateWindowExA(
        WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_TOPMOST | WS_EX_TOOLWINDOW,
        wc.lpszClassName, "ovl", WS_POPUP, 0, 0,
        GetSystemMetrics(SM_CXSCREEN), GetSystemMetrics(SM_CYSCREEN), nullptr, nullptr, inst, nullptr);
    SetLayeredWindowAttributes(hwnd, RGB(255,0,255), 0, LWA_COLORKEY);
    ShowWindow(hwnd, SW_SHOW);

    HDC sdc = GetDC(hwnd), mdc = CreateCompatibleDC(sdc);
    bool xd = false, cd = false, ced = false; MSG msg{};
    while (true) {
        while (PeekMessage(&msg, nullptr, 0, 0, PM_REMOVE)) {
            if (msg.message == WM_QUIT) goto done;
            TranslateMessage(&msg); DispatchMessage(&msg);
        }
        bool cen = (GetAsyncKeyState('C') & 0x8000) != 0; // C = ESP on/off
        if (cen && !ced) cfg.esp = !cfg.esp;
        ced = cen;
        bool xn = (GetAsyncKeyState('X') & 0x8000) != 0;
        if (xn && !xd) {
            g_menu_open = !g_menu_open;
            LONG ex = GetWindowLong(hwnd, GWL_EXSTYLE);
            if (g_menu_open) ex &= ~WS_EX_TRANSPARENT; else ex |= WS_EX_TRANSPARENT;
            SetWindowLong(hwnd, GWL_EXSTYLE, ex);
        }
        xd = xn;
        bool cn = (GetAsyncKeyState(VK_LBUTTON) & 0x8000) != 0;
        if (cn && !cd && g_menu_open) { POINT p; GetCursorPos(&p); ScreenToClient(hwnd, &p); menu_click(p.x, p.y); }
        cd = cn;

        int w = GetSystemMetrics(SM_CXSCREEN), h = GetSystemMetrics(SM_CYSCREEN);
        if (g_game && IsWindow(g_game)) {
            RECT gr; if (GetClientRect(g_game, &gr)) {
                POINT tl{ 0,0 }; ClientToScreen(g_game, &tl);
                SetWindowPos(hwnd, HWND_TOPMOST, tl.x, tl.y, gr.right, gr.bottom, SWP_NOACTIVATE);
                w = gr.right; h = gr.bottom;
            }
        }
        HBITMAP bmp = CreateCompatibleBitmap(sdc, w, h);
        HGDIOBJ old = SelectObject(mdc, bmp);
        render(mdc, w, h);
        BitBlt(sdc, 0, 0, w, h, mdc, 0, 0, SRCCOPY);
        SelectObject(mdc, old); DeleteObject(bmp);
        Sleep(6);
    }
done:
    DeleteDC(mdc); ReleaseDC(hwnd, sdc); CloseHandle(g_proc.handle);
    return 0;
}
