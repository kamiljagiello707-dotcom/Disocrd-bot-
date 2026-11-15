// loader.cpp -> Client.exe — CS2 external WH: zielona sylwetka + triggerbot
// target: cs2.exe (Source 2) | Windows x64 | external, read-only + mouse_event
// klawisze:  C = ESP on/off | ALT (trzymaj) = triggerbot | X = menu | F1 = diagnostyka | END = wyjscie
// build (Linux->Win): x86_64-w64-mingw32-g++ -std=c++17 -O2 -static -mwindows
//                     loader.cpp -o Client.exe -luser32 -lgdi32 -ldwmapi -lwininet
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <tlhelp32.h>
#include <wininet.h>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cctype>
#include <string>
#include <vector>

struct vec3 { float x, y, z; };
struct vec2 { float x, y; };

// ---- offsety: domyslne WBUDOWANE (aktualny snapshot) = dziala nawet bez neta/ini ----
struct offsets_t {
    uintptr_t dwEntityList      = 40931344;
    uintptr_t dwViewMatrix      = 39163456;
    uintptr_t dwLocalPlayerPawn = 39140664;
    uintptr_t m_hPlayerPawn     = 2348;
    uintptr_t m_iszPlayerName   = 1788;
    uintptr_t m_iHealth         = 844;
    uintptr_t m_iTeamNum        = 999;
    uintptr_t m_lifeState       = 852;
    uintptr_t m_pGameSceneNode  = 816;
    uintptr_t m_vecAbsOrigin    = 200;
    uintptr_t m_iIDEntIndex     = 14092; // encja na celowniku (triggerbot)
} offs;
static const char* g_offsrc = "WBUDOWANE";

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
    else if (k == "m_iIDEntIndex") offs.m_iIDEntIndex = v;
}

// ---- auto-dump offsetow z GitHuba (a2x/cs2-dumper, bot updatuje co patch) ----
static std::string http_get(const char* url) {
    std::string out;
    HINTERNET hi = InternetOpenA("c/1.0", INTERNET_OPEN_TYPE_PRECONFIG, nullptr, nullptr, 0);
    if (!hi) return out;
    HINTERNET hc = InternetOpenUrlA(hi, url, nullptr, 0,
        INTERNET_FLAG_RELOAD | INTERNET_FLAG_SECURE | INTERNET_FLAG_NO_CACHE_WRITE, 0);
    if (hc) {
        char buf[8192]; DWORD n = 0;
        while (InternetReadFile(hc, buf, sizeof(buf), &n) && n) out.append(buf, n);
        InternetCloseHandle(hc);
    }
    InternetCloseHandle(hi);
    return out;
}
static bool json_int(const std::string& j, const char* key, uintptr_t& out) {
    std::string a = std::string("\"") + key + "\"";
    size_t p = j.find(a);
    if (p == std::string::npos) return false;
    p += a.size();
    while (p < j.size() && !isdigit((unsigned char)j[p])) ++p;
    if (p >= j.size()) return false;
    out = (uintptr_t)strtoull(j.c_str() + p, nullptr, 10);
    return true;
}
static bool fetch_offsets() {
    std::string o = http_get("https://raw.githubusercontent.com/a2x/cs2-dumper/main/output/offsets.json");
    std::string c = http_get("https://raw.githubusercontent.com/a2x/cs2-dumper/main/output/client_dll.json");
    if (o.empty() || c.empty()) return false;
    bool ok = true;
    ok &= json_int(o, "dwEntityList",      offs.dwEntityList);
    ok &= json_int(o, "dwViewMatrix",      offs.dwViewMatrix);
    ok &= json_int(o, "dwLocalPlayerPawn", offs.dwLocalPlayerPawn);
    ok &= json_int(c, "m_hPlayerPawn",     offs.m_hPlayerPawn);
    ok &= json_int(c, "m_iszPlayerName",   offs.m_iszPlayerName);
    ok &= json_int(c, "m_iHealth",         offs.m_iHealth);
    ok &= json_int(c, "m_iTeamNum",        offs.m_iTeamNum);
    ok &= json_int(c, "m_lifeState",       offs.m_lifeState);
    ok &= json_int(c, "m_pGameSceneNode",  offs.m_pGameSceneNode);
    ok &= json_int(c, "m_vecAbsOrigin",    offs.m_vecAbsOrigin);
    json_int(c, "m_iIDEntIndex",           offs.m_iIDEntIndex); // opcjonalny dla trigera
    return ok;
}
static std::string ini_path() {
    char b[MAX_PATH]{}; GetModuleFileNameA(nullptr, b, MAX_PATH);
    std::string p(b); auto s = p.find_last_of("\\/");
    return (s == std::string::npos ? "" : p.substr(0, s+1)) + "offsets.ini";
}
static bool load_ini(const char* path) {
    FILE* f = fopen(path, "r"); if (!f) return false;
    char line[256]; int got = 0;
    while (fgets(line, sizeof(line), f)) {
        char* eq = strchr(line, '='); if (!eq || line[0]==';' || line[0]=='#') continue;
        *eq = 0; std::string k(line);
        k.erase(0, k.find_first_not_of(" \t"));
        k.erase(k.find_last_not_of(" \t\r\n") + 1);
        set_offset(k, (uintptr_t)strtoull(eq+1, nullptr, 0)); ++got;
    }
    fclose(f); return got >= 10;
}

// ---- external memory ----
struct process_t {
    HANDLE handle = nullptr; uintptr_t client = 0; DWORD pid = 0;
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

struct config_t { bool esp=true, box=true, hp=true, name=true, team_check=true, trigger=true; } cfg;
static bool g_menu_open = false, g_diag = false;
static HWND g_game = nullptr;
static int g_diag_enemies = 0, g_diag_crossid = -1; // liczniki do HUD

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
    g_diag_enemies = (int)out.size();
    return out;
}

// ---- triggerbot: ALT trzymany + celownik na wrogu -> strzal ----
static void do_trigger() {
    static DWORD last_shot = 0;
    if (!cfg.trigger) { g_diag_crossid = -1; return; }
    if (!(GetAsyncKeyState(VK_MENU) & 0x8000)) { g_diag_crossid = -2; return; } // ALT nie trzymany
    uintptr_t local = g_proc.read<uintptr_t>(g_proc.client + offs.dwLocalPlayerPawn);
    if (!local) return;
    uintptr_t elist = g_proc.read<uintptr_t>(g_proc.client + offs.dwEntityList);
    if (!elist) return;
    int id = g_proc.read<int>(local + offs.m_iIDEntIndex);
    g_diag_crossid = id;
    if (id <= 0 || id > 64) return;
    uintptr_t le = g_proc.read<uintptr_t>(elist + 0x8 * ((id & 0x7FFF) >> 9) + 16);
    if (!le) return;
    uintptr_t ent = g_proc.read<uintptr_t>(le + 120 * (id & 0x1FF));
    if (!ent || ent == local) return;
    if (g_proc.read<int>(ent + offs.m_lifeState) != 0) return;
    if (g_proc.read<int>(ent + offs.m_iHealth) <= 0) return;
    int lteam = g_proc.read<int>(local + offs.m_iTeamNum);
    if (g_proc.read<int>(ent + offs.m_iTeamNum) == lteam) return; // nie do swoich
    DWORD now = GetTickCount();
    if (now - last_shot < 45) return;                             // rate-limit
    last_shot = now;
    mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0);
    Sleep(12);
    mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0);
}

// zielona sylwetka czlowieka, symetryczna, skala wg boxa
static void draw_body(HDC dc, int cx, int top, int bot, int w, HBRUSH fill) {
    float H = (float)(bot - top), hw = w * 0.5f;
    if (H < 6.0f) return;
    HGDIOBJ ob = SelectObject(dc, fill);
    HGDIOBJ op = SelectObject(dc, GetStockObject(NULL_PEN));
    int headR = (int)(H*0.085f); if (headR < 2) headR = 2;
    int headCy = top + headR;
    int shY = top + (int)(H*0.20f), waistY = top + (int)(H*0.50f);
    int hipY = top + (int)(H*0.52f), footY = bot;
    int shHW = (int)hw, wsHW = (int)(hw*0.55f), armHW = (int)(hw*0.32f), gap = (int)(hw*0.10f);
    Ellipse(dc, cx-headR, headCy-headR, cx+headR, headCy+headR);
    POINT torso[4] = { {cx-shHW,shY},{cx+shHW,shY},{cx+wsHW,waistY},{cx-wsHW,waistY} };
    Polygon(dc, torso, 4);
    POINT armL[4] = { {cx-shHW,shY},{cx-shHW-armHW,shY+2},{cx-wsHW-armHW,hipY},{cx-wsHW,hipY} };
    POINT armR[4] = { {cx+shHW,shY},{cx+shHW+armHW,shY+2},{cx+wsHW+armHW,hipY},{cx+wsHW,hipY} };
    Polygon(dc, armL, 4); Polygon(dc, armR, 4);
    POINT legL[4] = { {cx-wsHW,hipY},{cx-gap,hipY},{cx-gap,footY},{cx-wsHW,footY} };
    POINT legR[4] = { {cx+gap,hipY},{cx+wsHW,hipY},{cx+wsHW,footY},{cx+gap,footY} };
    Polygon(dc, legL, 4); Polygon(dc, legR, 4);
    SelectObject(dc, op); SelectObject(dc, ob);
}

static void render(HDC dc, int w, int h) {
    RECT full{ 0,0,w,h };
    HBRUSH key = CreateSolidBrush(RGB(255,0,255)); FillRect(dc, &full, key); DeleteObject(key);
    HBRUSH green = CreateSolidBrush(RGB(40,230,70));
    SetBkMode(dc, TRANSPARENT);

    if (cfg.esp) {
        for (auto& t : gather(w, h)) {
            float bh = t.feet.y - t.head.y; if (bh < 4.0f) continue;
            float bw = bh * 0.42f;
            int l = (int)(t.head.x - bw*0.5f), r = (int)(t.head.x + bw*0.5f);
            int top = (int)t.head.y, bot = (int)t.feet.y;
            if (cfg.box) draw_body(dc, (l+r)/2, top, bot, r-l, green);
            if (cfg.hp) {
                int filled = (bot-top) * t.hp / 100;
                RECT bg{ l-6, top, l-2, bot };
                FillRect(dc, &bg, (HBRUSH)GetStockObject(BLACK_BRUSH));
                int g = 255*t.hp/100, rr = 255-g;
                HBRUSH b = CreateSolidBrush(RGB(rr,g,0));
                RECT fg{ l-6, bot-filled, l-2, bot }; FillRect(dc, &fg, b); DeleteObject(b);
            }
            if (cfg.name && !t.name.empty()) {
                SetTextColor(dc, RGB(255,60,60));
                TextOutA(dc, l, top-14, t.name.c_str(), (int)t.name.size());
            }
        }
    } else gather(w, h); // utrzymaj licznik diag gdy ESP off

    if (g_menu_open) {
        RECT panel{ 40,40,270,230 };
        HBRUSH bg = CreateSolidBrush(RGB(18,18,22)); FillRect(dc, &panel, bg); DeleteObject(bg);
        SetTextColor(dc, RGB(255,60,60));
        TextOutA(dc, 52, 48, "CS2 CLIENT  C=ESP ALT=trig", 26);
        struct row { const char* l; bool* v; };
        row rows[] = { {"ESP (C)",&cfg.esp},{"Sylwetka (zielona)",&cfg.box},{"HP bar",&cfg.hp},
                       {"Nazwy",&cfg.name},{"Tylko wrog",&cfg.team_check},{"Triggerbot (ALT)",&cfg.trigger} };
        for (int i = 0; i < 6; ++i) {
            int y = 80 + i*24;
            SetTextColor(dc, *rows[i].v ? RGB(80,255,120) : RGB(120,120,120));
            std::string s = std::string("[") + (*rows[i].v ? "x" : " ") + "] " + rows[i].l;
            TextOutA(dc, 56, y, s.c_str(), (int)s.size());
        }
    }

    if (g_diag) {
        SetTextColor(dc, RGB(120,220,255));
        char ln[128];
        int y = 40;
        auto put = [&](const char* s){ TextOutA(dc, w-320, y, s, (int)strlen(s)); y += 16; };
        put("=== DIAG (F1) ===");
        snprintf(ln, sizeof(ln), "proc cs2.exe pid: %lu", (unsigned long)g_proc.pid); put(ln);
        snprintf(ln, sizeof(ln), "client.dll: 0x%llX", (unsigned long long)g_proc.client); put(ln);
        snprintf(ln, sizeof(ln), "offsety: %s", g_offsrc); put(ln);
        snprintf(ln, sizeof(ln), "wrogow na ekranie: %d", g_diag_enemies); put(ln);
        snprintf(ln, sizeof(ln), "celownik ent: %d", g_diag_crossid); put(ln);
        snprintf(ln, sizeof(ln), "trigger: %s", cfg.trigger ? "ON (trzymaj ALT)" : "OFF"); put(ln);
        put(g_diag_enemies == 0 ? "0 wrogow? sprawdz: trusted mode / offsety" : "czyta OK");
    }
    DeleteObject(green);
}

static void menu_click(int mx, int my) {
    bool* v[] = { &cfg.esp,&cfg.box,&cfg.hp,&cfg.name,&cfg.team_check,&cfg.trigger };
    for (int i = 0; i < 6; ++i) { int y = 80 + i*24;
        if (mx >= 56 && mx <= 258 && my >= y && my <= y+18) { *v[i] = !*v[i]; break; } }
}
static LRESULT CALLBACK wproc(HWND hw, UINT m, WPARAM wp, LPARAM lp) {
    if (m == WM_DESTROY) { PostQuitMessage(0); return 0; }
    return DefWindowProc(hw, m, wp, lp);
}

// czeka az cs2.exe wstanie, podlacza sie, laduje client.dll (z retry)
static bool attach_game() {
    for (;;) {
        g_proc.pid = find_pid(L"cs2.exe");
        if (g_proc.pid) break;
        Sleep(1000); // gra jeszcze nie odpalona — czekaj
    }
    for (int i = 0; i < 30 && !g_proc.handle; ++i) {
        g_proc.handle = OpenProcess(PROCESS_VM_READ | PROCESS_QUERY_INFORMATION, FALSE, g_proc.pid);
        if (!g_proc.handle) Sleep(500);
    }
    if (!g_proc.handle) {
        MessageBoxA(nullptr, "OpenProcess nie przeszlo (30x).\nOdpal Client.exe JAKO ADMINISTRATOR.", "Client", MB_OK | MB_ICONERROR);
        return false;
    }
    for (int i = 0; i < 60 && !g_proc.client; ++i) {        // client.dll laduje sie chwile po starcie gry
        g_proc.client = find_module(g_proc.pid, L"client.dll");
        if (!g_proc.client) Sleep(500);
    }
    if (!g_proc.client) {
        MessageBoxA(nullptr, "client.dll nie zaladowany — wejdz do gry (nie tylko menu).", "Client", MB_OK | MB_ICONERROR);
        return false;
    }
    return true;
}

int WINAPI WinMain(HINSTANCE inst, HINSTANCE, LPSTR, int) {
    // 1. offsety: net -> ini -> wbudowane (nigdy nie pada)
    if (fetch_offsets()) g_offsrc = "NET (swieze)";
    else if (load_ini(ini_path().c_str())) g_offsrc = "INI (plik)";
    else g_offsrc = "WBUDOWANE";

    // 2. podlacz sie do gry (czeka + retry)
    if (!attach_game()) return 1;
    g_game = FindWindowA(nullptr, "Counter-Strike 2");

    // 3. overlay
    WNDCLASSA wc{}; wc.lpfnWndProc = wproc; wc.hInstance = inst; wc.lpszClassName = "cs2_cl";
    RegisterClassA(&wc);
    HWND hwnd = CreateWindowExA(
        WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_TOPMOST | WS_EX_TOOLWINDOW,
        wc.lpszClassName, "cl", WS_POPUP, 0, 0,
        GetSystemMetrics(SM_CXSCREEN), GetSystemMetrics(SM_CYSCREEN), nullptr, nullptr, inst, nullptr);
    SetLayeredWindowAttributes(hwnd, RGB(255,0,255), 0, LWA_COLORKEY);
    ShowWindow(hwnd, SW_SHOW);
    HDC sdc = GetDC(hwnd), mdc = CreateCompatibleDC(sdc);

    bool xd=false, cd=false, ced=false, fd=false; MSG msg{};
    while (true) {
        while (PeekMessage(&msg, nullptr, 0, 0, PM_REMOVE)) {
            if (msg.message == WM_QUIT) goto done;
            TranslateMessage(&msg); DispatchMessage(&msg);
        }
        if (GetAsyncKeyState(VK_END) & 0x8000) break; // END = wyjscie

        bool cen = (GetAsyncKeyState('C') & 0x8000) != 0;
        if (cen && !ced) cfg.esp = !cfg.esp;  ced = cen;
        bool fn = (GetAsyncKeyState(VK_F1) & 0x8000) != 0;
        if (fn && !fd) g_diag = !g_diag;      fd = fn;
        bool xn = (GetAsyncKeyState('X') & 0x8000) != 0;
        if (xn && !xd) {
            g_menu_open = !g_menu_open;
            LONG ex = GetWindowLong(hwnd, GWL_EXSTYLE);
            if (g_menu_open) ex &= ~WS_EX_TRANSPARENT; else ex |= WS_EX_TRANSPARENT;
            SetWindowLong(hwnd, GWL_EXSTYLE, ex);
        } xd = xn;
        bool cn = (GetAsyncKeyState(VK_LBUTTON) & 0x8000) != 0;
        if (cn && !cd && g_menu_open) { POINT p; GetCursorPos(&p); ScreenToClient(hwnd, &p); menu_click(p.x, p.y); }
        cd = cn;

        do_trigger(); // triggerbot

        // gra zamknieta? wroc do czekania
        if (!IsWindow(g_game) && !find_pid(L"cs2.exe")) {
            CloseHandle(g_proc.handle); g_proc = process_t{};
            if (!attach_game()) break;
            g_game = FindWindowA(nullptr, "Counter-Strike 2");
        }

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
        Sleep(5);
    }
done:
    DeleteDC(mdc); ReleaseDC(hwnd, sdc);
    if (g_proc.handle) CloseHandle(g_proc.handle);
    return 0;
}
