// offsets.hpp — CS2 external ESP offsets | Windows 11 x64 | target: cs2.exe
// per-build: KAZDY offset nizej dryfuje przy kazdym patchu CS2. re-dump przed gra:
//   git clone https://github.com/a2x/cs2-dumper
//   cd cs2-dumper && cargo run --release
//   przepisz wartosci z output/offsets.hpp (client_dll) i output/client_dll.hpp (schema)
// wartosci ponizej to REFERENCJA (stan przykladowy) — potraktuj jak // FILL, nadpisz swoim dumpem.
#pragma once
#include <cstdint>

namespace offs {
    // client.dll relative  (output/offsets.hpp -> namespace client_dll)
    constexpr uintptr_t dwEntityList      = 0x1A1D4D8; // FILL per-build
    constexpr uintptr_t dwViewMatrix      = 0x1A32D00; // FILL per-build
    constexpr uintptr_t dwLocalPlayerPawn = 0x1A3D500; // FILL per-build

    // schema  (output/client_dll.hpp)
    constexpr uintptr_t m_hPlayerPawn    = 0x80C; // CCSPlayerController   FILL
    constexpr uintptr_t m_iszPlayerName  = 0x640; // CCSPlayerController   FILL
    constexpr uintptr_t m_iHealth        = 0x344; // C_BaseEntity          FILL
    constexpr uintptr_t m_iTeamNum       = 0x3E3; // C_BaseEntity          FILL
    constexpr uintptr_t m_lifeState      = 0x348; // C_BaseEntity (0=alive) FILL
    constexpr uintptr_t m_pGameSceneNode = 0x330; // C_BaseEntity          FILL
    constexpr uintptr_t m_vecAbsOrigin   = 0xD0;  // CGameSceneNode        FILL
}
