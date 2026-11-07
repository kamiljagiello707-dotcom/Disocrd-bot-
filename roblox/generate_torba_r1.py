#!/usr/bin/env python3
"""Generuje realistyczna torbe ratownicza R1 jako Tool dla Roblox Studio (.rbxmx).

Uruchom:  python3 roblox/generate_torba_r1.py
Wynik:    roblox/TorbaR1.rbxmx  (+ TorbaR1.preview.json do podgladu)
"""
import json
import math
import os
from xml.sax.saxutils import escape

OUT_DIR = os.path.dirname(os.path.abspath(__file__))

# ------------------------------------------------------------------ matematyka
I3 = ((1, 0, 0), (0, 1, 0), (0, 0, 1))


def matmul(a, b):
    return tuple(tuple(sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)) for i in range(3))


def rx(d):
    c, s = math.cos(math.radians(d)), math.sin(math.radians(d))
    return ((1, 0, 0), (0, c, -s), (0, s, c))


def ry(d):
    c, s = math.cos(math.radians(d)), math.sin(math.radians(d))
    return ((c, 0, s), (0, 1, 0), (-s, 0, c))


def rz(d):
    c, s = math.cos(math.radians(d)), math.sin(math.radians(d))
    return ((c, -s, 0), (s, c, 0), (0, 0, 1))


AX_Y = rz(90)   # os cylindra (lokalne X) -> pionowo
AX_Z = ry(-90)  # os cylindra (lokalne X) -> wzdluz torby (Z)


def sub(a, b):
    return tuple(x - y for x, y in zip(a, b))


def add(a, b):
    return tuple(x + y for x, y in zip(a, b))


def mul(a, k):
    return tuple(x * k for x in a)


def dot(a, b):
    return sum(x * y for x, y in zip(a, b))


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def norm(a):
    l = math.sqrt(dot(a, a))
    return tuple(x / l for x in a)


def transpose(m):
    return tuple(tuple(m[j][i] for j in range(3)) for i in range(3))


def apply(m, v):
    return tuple(dot(m[i], v) for i in range(3))


# ------------------------------------------------------------------ kolory
RED = (191, 28, 30)          # czerwien sygnalowa (Cordura)
RED_DARK = (163, 22, 25)     # kieszenie / szwy
BLACK = (28, 28, 30)
WEBBING = (36, 36, 38)       # tasma nosna
ZIP = (22, 22, 24)
WHITE = (242, 242, 242)
REFLECT = (196, 199, 204)    # tasma odblaskowa
METAL = (168, 170, 176)
RUBBER = (40, 40, 42)

MAT = {"Plastic": 256, "SmoothPlastic": 272, "Metal": 1088, "Fabric": 1312, "Foil": 1072}
SHAPE = {"Ball": 0, "Block": 1, "Cylinder": 2}

# ------------------------------------------------------------------ model
parts = []


def part(name, size, pos, rot=I3, color=RED, mat="Fabric", shape="Block",
         collide=False, transparency=0.0, refl=0.0, guis=None):
    p = dict(name=name, size=tuple(size), pos=tuple(pos), rot=rot, color=color, mat=mat,
             shape=shape, collide=collide, transparency=transparency, refl=refl, guis=guis or [])
    parts.append(p)
    return p


def rbox(name, c, s, r, color, mat="Fabric", collide=False):
    """Prostopadloscian z zaokraglonymi krawedziami (3 bloki + 12 walcow + 8 kul)."""
    cx, cy, cz = c
    sx, sy, sz = s
    d = 2 * r
    part(name, (sx, sy - d, sz - d), c, color=color, mat=mat, collide=collide)
    part(name + "_Y", (sx - d, sy, sz - d), c, color=color, mat=mat, collide=collide)
    part(name + "_Z", (sx - d, sy - d, sz), c, color=color, mat=mat, collide=collide)
    ex, ey, ez = sx / 2 - r, sy / 2 - r, sz / 2 - r
    for a in (-1, 1):
        for b in (-1, 1):
            part(name + "_KrX", (sx - d, d, d), (cx, cy + a * ey, cz + b * ez), I3, color, mat, "Cylinder")
            part(name + "_KrY", (sy - d, d, d), (cx + a * ex, cy, cz + b * ez), AX_Y, color, mat, "Cylinder")
            part(name + "_KrZ", (sz - d, d, d), (cx + a * ex, cy + b * ey, cz), AX_Z, color, mat, "Cylinder")
            for e in (-1, 1):
                part(name + "_Rog", (d, d, d), (cx + a * ex, cy + b * ey, cz + e * ez), I3, color, mat, "Ball")


def ring(name, y, h, t, color, mat, refl=0.0):
    """Pas dookola korpusu (zamek / tasma odblaskowa) z zaokraglonymi naroznikami."""
    hx, hz = W / 2, L / 2
    o = t / 2 - 0.004
    for s in (-1, 1):
        part(name, (t, h, L - 2 * R), (s * (hx + o), y, BODY_Z), color=color, mat=mat, refl=refl)
        part(name, (W - 2 * R, h, t), (0, y, BODY_Z + s * (hz + o)), color=color, mat=mat, refl=refl)
        for e in (-1, 1):
            dd = 2 * (R + t - 0.004)
            part(name + "_Rog", (h, dd, dd), (s * (hx - R), y, BODY_Z + e * (hz - R)), AX_Y,
                 color, mat, "Cylinder", refl=refl)


def seg(name, a, b, thick, width, hint, color, mat="Fabric"):
    """Plaska tasma od punktu a do b; szerokosc w kierunku `hint`."""
    v = sub(b, a)
    ln = math.sqrt(dot(v, v))
    yv = norm(v)
    zv = norm(sub(hint, mul(yv, dot(hint, yv))))
    xv = cross(yv, zv)
    rot = tuple(tuple((xv, yv, zv)[j][i] for j in range(3)) for i in range(3))
    part(name, (thick, ln, width), mul(add(a, b), 0.5), rot, color, mat)


def text_gui(face, labels):
    return {"face": face, "labels": labels}


# wymiary (1 stud ~ 28 cm): torba ~50 x 28 x 30 cm
L = 1.8        # dlugosc (os Z)
W = 1.0        # szerokosc (os X)
H = 1.05       # wysokosc
R = 0.09       # promien zaokraglenia korpusu
BODY_Z = 0.0
TOP = -0.30                  # gora klapy (uchwyt jest w (0,0,0))
YB = TOP - H                 # spod torby
YC = TOP - H / 2

FACES = {"Right": 0, "Top": 1, "Back": 2, "Left": 3, "Bottom": 4, "Front": 5}

# --- uchwyt (Handle) - rzep spinajacy dwa uchwyty, tu trzyma reka
handle = part("Handle", (0.2, 0.17, 0.42), (0, 0, 0), color=BLACK, mat="Fabric", collide=True)
for z in (-0.12, 0.12):
    part("RzepSzew", (0.205, 0.175, 0.012), (0, 0, z), color=(55, 55, 58), mat="Fabric")

# --- korpus
rbox("Korpus", (0, YC, BODY_Z), (W, H, L), R, RED, collide=True)
# wzmocnione czarne dno
rbox("Dno", (0, YB + 0.07, BODY_Z), (W + 0.03, 0.14, L + 0.03), 0.07, BLACK, collide=True)
# zamek klapy glownej
ring("ZamekKlapy", YB + 0.92, 0.035, 0.022, ZIP, "Plastic")
# lamowka pod zamkiem
ring("Lamowka", YB + 0.895, 0.012, 0.018, BLACK, "Fabric")
# tasma odblaskowa dookola
ring("Odblask", YB + 0.215, 0.09, 0.02, REFLECT, "SmoothPlastic", refl=0.3)
ring("OdblaskSzew", YB + 0.262, 0.008, 0.024, (150, 152, 156), "Fabric")
ring("OdblaskSzew", YB + 0.168, 0.008, 0.024, (150, 152, 156), "Fabric")

# suwaki klapy (dwa, spotykaja sie na srodku boku)
for z in (-0.07, 0.07):
    zy = YB + 0.92
    part("SuwakKlapy", (0.018, 0.05, 0.04), (W / 2 + 0.025, zy - 0.005, z), color=METAL, mat="Metal")
    part("SuwakKlapy_Jezyk", (0.012, 0.075, 0.028), (W / 2 + 0.035, zy - 0.055, z), color=METAL, mat="Metal")
    part("SuwakKlapy_Linka", (0.09, 0.022, 0.022), (W / 2 + 0.038, zy - 0.13, z), AX_Y, BLACK, "Fabric", "Cylinder")
    part("SuwakKlapy_Koncowka", (0.035, 0.035, 0.035), (W / 2 + 0.038, zy - 0.18, z), color=RUBBER, mat="Plastic", shape="Ball")

# --- kieszenie boczne (dlugie boki +X i -X) z bialym krzyzem
for s in (-1, 1):
    px = s * (W / 2 + 0.045)
    py = YB + 0.575
    rbox("KieszenBoczna", (px, py, 0), (0.11, 0.55, 1.2), 0.045, RED_DARK)
    fx = s * (W / 2 + 0.1)
    zy = py + 0.275 - 0.06
    part("KieszenBoczna_Zamek", (0.02, 0.03, 1.06), (fx, zy, 0), color=ZIP, mat="Plastic")
    part("KieszenBoczna_Lamowka", (0.016, 0.012, 1.08), (fx, zy - 0.026, 0), color=BLACK)
    pz = s * 0.42
    part("KieszenBoczna_Suwak", (0.03, 0.045, 0.035), (fx + s * 0.008, zy - 0.005, pz), color=METAL, mat="Metal")
    part("KieszenBoczna_Jezyk", (0.012, 0.07, 0.026), (fx + s * 0.018, zy - 0.05, pz), color=METAL, mat="Metal")
    part("KieszenBoczna_Linka", (0.08, 0.02, 0.02), (fx + s * 0.02, zy - 0.12, pz), AX_Y, BLACK, "Fabric", "Cylinder")
    part("KieszenBoczna_Koncowka", (0.032, 0.032, 0.032), (fx + s * 0.02, zy - 0.165, pz), color=RUBBER, mat="Plastic", shape="Ball")
    # bialy krzyz (nadruk)
    cy = py - 0.04
    cx = fx + s * 0.003
    part("Krzyz", (0.008, 0.34, 0.11), (cx, cy, 0), color=WHITE, mat="SmoothPlastic")
    part("Krzyz", (0.008, 0.11, 0.34), (cx, cy, 0), color=WHITE, mat="SmoothPlastic")
    # szwy obwodowe kieszeni
    part("KieszenBoczna_Szew", (0.006, 0.006, 1.1), (fx + s * 0.001, py - 0.245, 0), color=(120, 18, 20))

# --- kieszenie czolowe (krotkie boki +Z i -Z) z naszywka R1
for s in (-1, 1):
    pz = BODY_Z + s * (L / 2 + 0.04)
    py = YB + 0.52
    rbox("KieszenCzolowa", (0, py, pz), (0.62, 0.44, 0.1), 0.04, RED_DARK)
    fz = BODY_Z + s * (L / 2 + 0.09)
    part("KieszenCzolowa_Guma", (0.6, 0.045, 0.014), (0, py + 0.22 - 0.035, fz), color=BLACK)
    face = "Back" if s > 0 else "Front"
    part("NaszywkaR1", (0.36, 0.2, 0.01), (0, py - 0.02, fz + s * 0.003), color=WHITE, mat="Fabric",
         guis=[text_gui(face, [dict(text="R1", color=(0.75, 0.1, 0.11), pos=(0, 0.05), size=(1, 0.9), font=20)])])
    # D-ringi na pasek naramienny
    for x in (-0.32, 0.32):
        dy = YB + 0.86
        part("TasmaDRing", (0.13, 0.1, 0.02), (x, dy, BODY_Z + s * (L / 2 + 0.006)), color=WEBBING)
        part("DRing", (0.13, 0.026, 0.026), (x, dy - 0.01, BODY_Z + s * (L / 2 + 0.085)), I3, METAL, "Metal", "Cylinder")
        for e in (-1, 1):
            part("DRing", (0.08, 0.024, 0.024), (x + e * 0.065, dy - 0.01, BODY_Z + s * (L / 2 + 0.047)),
                 AX_Z, METAL, "Metal", "Cylinder")

# --- uchwyty nosne (tasma) - po jednym z kazdego dlugiego boku, spiete rzepem
for s in (-1, 1):
    for z in (-1, 1):
        a = (s * 0.36, TOP + 0.005, z * 0.33)
        b = (s * 0.05, -0.03, z * 0.27)
        seg("Uchwyt", a, b, 0.028, 0.13, (0, 0, 1), WEBBING)
        # przeszycie mocujace (box-X)
        part("UchwytMocowanie", (0.2, 0.02, 0.16), (s * 0.36, TOP + 0.008, z * 0.33), color=WEBBING)
        part("UchwytMocowanie_Szew", (0.16, 0.022, 0.012), (s * 0.36, TOP + 0.009, z * 0.33), ry(40), (70, 70, 74))
        part("UchwytMocowanie_Szew", (0.16, 0.022, 0.012), (s * 0.36, TOP + 0.009, z * 0.33), ry(-40), (70, 70, 74))
    part("Uchwyt", (0.028, 0.13, 0.58), (s * 0.05, 0, 0), color=WEBBING)

# --- napis na klapie
part("NapisKlapa", (1.3, 0.006, 0.62), (0, TOP + 0.003, BODY_Z), ry(90), RED, "Fabric", transparency=1,
     guis=[text_gui("Top", [
         dict(text="R1", color=(1, 1, 1), pos=(0, 0.0), size=(1, 0.55), font=20),
         dict(text="ZESTAW RATOWNICZY", color=(1, 1, 1), pos=(0, 0.62), size=(1, 0.3), font=19),
     ])])

# --- gumowe nozki
for x in (-1, 1):
    for z in (-1, 1):
        part("Nozka", (0.035, 0.12, 0.12), (x * (W / 2 - 0.15), YB - 0.012, BODY_Z + z * (L / 2 - 0.2)),
             AX_Y, RUBBER, "Plastic", "Cylinder", collide=True)


# ------------------------------------------------------------------ zapis XML
WORLD_OFFSET = (0, 2.0, 0)  # gdzie pojawi sie w Workspace
ref_counter = [0]


def ref():
    ref_counter[0] += 1
    return "RBX%08X" % ref_counter[0]


def f(x):
    return ("%.5f" % x).rstrip("0").rstrip(".") if abs(x) > 1e-9 else "0"


def cf_xml(name, pos, rot):
    out = ['<CoordinateFrame name="%s">' % name]
    for k, v in zip("XYZ", pos):
        out.append("<%s>%s</%s>" % (k, f(v), k))
    for i in range(3):
        for j in range(3):
            out.append("<R%d%d>%s</R%d%d>" % (i, j, f(rot[i][j]), i, j))
    out.append("</CoordinateFrame>")
    return "".join(out)


def color_u8(c):
    return (0xFF << 24) | (c[0] << 16) | (c[1] << 8) | c[2]


def gui_xml(g):
    lines = ['<Item class="SurfaceGui" referent="%s"><Properties>' % ref(),
             '<string name="Name">Napis</string>',
             '<token name="Face">%d</token>' % FACES[g["face"]],
             '<token name="SizingMode">1</token>',
             '<float name="PixelsPerStud">200</float>',
             '<float name="LightInfluence">1</float>',
             '<bool name="ClipsDescendants">true</bool>',
             '</Properties>']
    for lb in g["labels"]:
        r, gg, b = lb["color"]
        lines.append(
            '<Item class="TextLabel" referent="%s"><Properties>'
            '<string name="Name">Tekst</string>'
            '<string name="Text">%s</string>'
            '<float name="BackgroundTransparency">1</float>'
            '<int name="BorderSizePixel">0</int>'
            '<UDim2 name="Position"><XS>%s</XS><XO>0</XO><YS>%s</YS><YO>0</YO></UDim2>'
            '<UDim2 name="Size"><XS>%s</XS><XO>0</XO><YS>%s</YS><YO>0</YO></UDim2>'
            '<Color3 name="TextColor3"><R>%s</R><G>%s</G><B>%s</B></Color3>'
            '<bool name="TextScaled">true</bool>'
            '<token name="Font">%d</token>'
            '</Properties></Item>' % (ref(), escape(lb["text"]), f(lb["pos"][0]), f(lb["pos"][1]),
                                       f(lb["size"][0]), f(lb["size"][1]), f(r), f(gg), f(b), lb["font"]))
    lines.append("</Item>")
    return "".join(lines)


for p in parts:
    p["ref"] = ref()
    p["wpos"] = add(p["pos"], WORLD_OFFSET)

handle_ref = parts[0]["ref"]
h_rot_t = transpose(handle["rot"])


def part_xml(p, is_handle):
    props = [
        '<string name="Name">%s</string>' % escape(p["name"]),
        '<bool name="Anchored">false</bool>',
        '<bool name="CanCollide">%s</bool>' % ("true" if p["collide"] else "false"),
        '<bool name="CanTouch">%s</bool>' % ("true" if is_handle or p["collide"] else "false"),
        '<bool name="CanQuery">true</bool>',
        '<bool name="Massless">%s</bool>' % ("false" if is_handle else "true"),
        '<bool name="CastShadow">%s</bool>' % ("false" if p["transparency"] >= 1 else "true"),
        cf_xml("CFrame", p["wpos"], p["rot"]),
        '<Color3uint8 name="Color3uint8">%d</Color3uint8>' % color_u8(p["color"]),
        '<token name="Material">%d</token>' % MAT[p["mat"]],
        '<float name="Reflectance">%s</float>' % f(p["refl"]),
        '<float name="Transparency">%s</float>' % f(p["transparency"]),
        '<Vector3 name="size"><X>%s</X><Y>%s</Y><Z>%s</Z></Vector3>' % tuple(f(v) for v in p["size"]),
        '<token name="shape">%d</token>' % SHAPE[p["shape"]],
    ]
    for s in ("Top", "Bottom", "Front", "Back", "Left", "Right"):
        props.append('<token name="%sSurface">0</token>' % s)
    out = ['<Item class="Part" referent="%s"><Properties>%s</Properties>' % (p["ref"], "".join(props))]
    for g in p["guis"]:
        out.append(gui_xml(g))
    if not is_handle:
        # Weld: Part0 = Handle, C0 = Handle^-1 * part, C1 = identyczność
        c0_rot = matmul(h_rot_t, p["rot"])
        c0_pos = apply(h_rot_t, sub(p["wpos"], parts[0]["wpos"]))
        out.append('<Item class="Weld" referent="%s"><Properties>'
                   '<string name="Name">Spaw</string>'
                   '<Ref name="Part0">%s</Ref><Ref name="Part1">%s</Ref>%s%s'
                   '</Properties></Item>' % (ref(), handle_ref, p["ref"],
                                             cf_xml("C0", c0_pos, c0_rot), cf_xml("C1", (0, 0, 0), I3)))
    out.append("</Item>")
    return "".join(out)


xml = ['<roblox xmlns:xmime="http://www.w3.org/2005/05/xmlmime" '
       'xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" '
       'xsi:noNamespaceSchemaLocation="http://www.roblox.com/roblox.xsd" version="4">',
       '<Item class="Tool" referent="%s"><Properties>' % ref(),
       '<string name="Name">Torba R1</string>',
       '<string name="ToolTip">Torba ratownicza R1</string>',
       '<bool name="RequiresHandle">true</bool>',
       '<bool name="CanBeDropped">true</bool>',
       '<bool name="ManualActivationOnly">false</bool>',
       cf_xml("Grip", (0, 0, 0), I3),
       '</Properties>']
for i, p in enumerate(parts):
    xml.append(part_xml(p, i == 0))
xml.append("</Item></roblox>")

with open(os.path.join(OUT_DIR, "TorbaR1.rbxmx"), "w", encoding="utf-8") as fh:
    fh.write("\n".join(xml))

with open(os.path.join(OUT_DIR, "TorbaR1.preview.json"), "w", encoding="utf-8") as fh:
    json.dump([dict(shape=p["shape"], size=p["size"], pos=p["wpos"], rot=p["rot"], color=p["color"],
                    t=p["transparency"], mat=p["mat"]) for p in parts], fh)

print("Czesci:", len(parts))
