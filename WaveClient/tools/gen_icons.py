"""Generates WaveClient's icon atlas (white anti-aliased icons on transparency).
Usage: python3 tools/gen_icons.py  ->  src/main/resources/assets/waveclient/textures/icons.png
Order of ICONS must match dev.wave.util.Icons."""
import math, os
from PIL import Image, ImageDraw

CELL = 48          # final cell size
S = 4              # supersampling
C = CELL * S       # drawing cell size
COLS = 8
W = (255, 255, 255, 255)
T = 15             # stroke width (at supersampled scale)

def P(v): return v * C / 100.0  # percent -> px

def line(d, pts, w=T): 
    d.line([(P(x), P(y)) for x, y in pts], fill=W, width=w, joint="curve")
    for x, y in (pts[0], pts[-1]): dot(d, x, y, w / 2 / C * 100)

def dot(d, x, y, r): d.ellipse([P(x) - P(r), P(y) - P(r), P(x) + P(r), P(y) + P(r)], fill=W)
def circ(d, x, y, r, w=T): d.ellipse([P(x - r), P(y - r), P(x + r), P(y + r)], outline=W, width=w)
def rrect(d, x0, y0, x1, y1, r, w=T, fill=False):
    if fill: d.rounded_rectangle([P(x0), P(y0), P(x1), P(y1)], radius=P(r), fill=W)
    else: d.rounded_rectangle([P(x0), P(y0), P(x1), P(y1)], radius=P(r), outline=W, width=w)
def poly(d, pts, fill=True, w=T):
    if fill: d.polygon([(P(x), P(y)) for x, y in pts], fill=W)
    else: line(d, pts + [pts[0]], w)
def arc(d, x, y, r, a0, a1, w=T):
    d.arc([P(x - r), P(y - r), P(x + r), P(y + r)], a0, a1, fill=W, width=w)
def erase(d, box):  # clear region
    d.rectangle([P(box[0]), P(box[1]), P(box[2]), P(box[3])], fill=(0, 0, 0, 0))

def eye(d):
    pts = [(10 + 80 * i / 40, 50 - 26 * math.sin(math.pi * i / 40)) for i in range(41)]
    pts2 = [(10 + 80 * i / 40, 50 + 26 * math.sin(math.pi * i / 40)) for i in range(41)]
    line(d, pts); line(d, pts2); circ(d, 50, 50, 12); dot(d, 50, 50, 5)
def movement(d):
    poly(d, [(40, 18), (82, 50), (40, 82), (40, 64), (58, 50), (40, 36)])
    line(d, [(14, 38), (30, 38)]); line(d, [(8, 50), (28, 50)]); line(d, [(14, 62), (30, 62)])
def player(d): circ(d, 50, 32, 16); arc(d, 50, 88, 34, 200, 340)
def hud(d): rrect(d, 12, 18, 88, 82, 10); line(d, [(12, 38), (88, 38)]); line(d, [(40, 38), (40, 82)])
def gear(d):
    for i in range(8):
        a = i * math.pi / 4
        poly(d, [(50 + 42 * math.cos(a - 0.22), 50 + 42 * math.sin(a - 0.22)), (50 + 42 * math.cos(a + 0.22), 50 + 42 * math.sin(a + 0.22)),
                 (50 + 28 * math.cos(a + 0.35), 50 + 28 * math.sin(a + 0.35)), (50 + 28 * math.cos(a - 0.35), 50 + 28 * math.sin(a - 0.35))])
    d.ellipse([P(18), P(18), P(82), P(82)], fill=W); d.ellipse([P(37), P(37), P(63), P(63)], fill=(0, 0, 0, 0))
def sun(d):
    d.ellipse([P(33), P(33), P(67), P(67)], fill=W)
    for i in range(8):
        a = i * math.pi / 4
        line(d, [(50 + 26 * math.cos(a), 50 + 26 * math.sin(a)), (50 + 40 * math.cos(a), 50 + 40 * math.sin(a))])
def cube(d):
    line(d, [(50, 12), (86, 30), (86, 70), (50, 88), (14, 70), (14, 30), (50, 12)])
    line(d, [(14, 30), (50, 48), (86, 30)]); line(d, [(50, 48), (50, 88)])
def tracers(d):
    for x, y in [(18, 20), (50, 12), (84, 26)]: line(d, [(50, 86), (x, y)], 11); dot(d, x, y, 9)
def chest(d):
    rrect(d, 12, 24, 88, 82, 8); line(d, [(12, 46), (88, 46)]); rrect(d, 42, 38, 58, 58, 4, fill=True)
def zoom(d):
    circ(d, 42, 42, 26); line(d, [(62, 62), (86, 86)], 18); line(d, [(42, 30), (42, 54)]); line(d, [(30, 42), (54, 42)])
def crosshair(d):
    circ(d, 50, 50, 28)
    for a, b in [((50, 6), (50, 30)), ((50, 70), (50, 94)), ((6, 50), (30, 50)), ((70, 50), (94, 50))]: line(d, [a, b])
    dot(d, 50, 50, 6)
def camera(d):
    rrect(d, 10, 30, 90, 80, 10); poly(d, [(34, 30), (40, 18), (60, 18), (66, 30)]); circ(d, 50, 55, 14)
def noflame(d):
    pts = []
    for i in range(41):
        t = i / 40 * 2 * math.pi
        r = 30 * (1 - math.sin(t)) / 2 + 6
        pts.append((50 + 0.9 * 30 * math.sin(t) * (0.55 + 0.45 * math.sin(t / 2 + 1)), 58 - 34 * math.cos(t) * 0.9))
    poly(d, [(50, 10), (70, 40), (74, 60), (64, 80), (50, 86), (36, 80), (26, 60), (32, 42), (42, 52)], fill=False)
    line(d, [(14, 14), (86, 86)])
def moon(d):
    d.ellipse([P(14), P(14), P(78), P(78)], fill=W); d.ellipse([P(32), P(6), P(92), P(66)], fill=(0, 0, 0, 0))
    poly(d, [(76, 58), (80, 70), (92, 74), (80, 78), (76, 90), (72, 78), (60, 74), (72, 70)])
def bolt(d): poly(d, [(56, 6), (20, 56), (46, 56), (40, 94), (80, 40), (54, 40)])
def feet(d):
    d.ellipse([P(18), P(14), P(42), P(50)], fill=W); d.ellipse([P(22), P(54), P(38), P(66)], fill=W)
    d.ellipse([P(58), P(34), P(82), P(70)], fill=W); d.ellipse([P(62), P(74), P(78), P(86)], fill=W)
def jump(d):
    arc(d, 50, 74, 34, 190, 350); poly(d, [(84, 74), (74, 56), (94, 58)]); line(d, [(8, 86), (92, 86)])
def eagle(d):
    line(d, [(10, 64), (58, 64), (58, 90)]); line(d, [(72, 14), (72, 48)]); poly(d, [(60, 40), (84, 40), (72, 56)])
def respawn(d):
    arc(d, 50, 50, 32, 40, 330); poly(d, [(78, 22), (86, 50), (60, 42)])
def clock(d): circ(d, 50, 50, 36); line(d, [(50, 28), (50, 50), (66, 60)])
def wave(d):
    for yo in (-14, 6, 26):
        pts = [(10 + 80 * i / 40, 40 + yo + 9 * math.sin(i / 40 * 2 * math.pi * 1.5)) for i in range(41)]
        line(d, pts, 12)
def listicon(d):
    for y in (26, 50, 74): dot(d, 18, y, 7); line(d, [(34, y), (86, y)])
def info(d): circ(d, 50, 50, 38); dot(d, 50, 30, 7); line(d, [(50, 46), (50, 72)], 16)
def pin(d):
    pts = [(50, 90)]
    for i in range(0, 37):
        a = math.radians(150 - i * 300 / 36 + 90)
    poly(d, [(50, 92), (24, 52), (76, 52)]); d.ellipse([P(22), P(10), P(78), P(66)], fill=W)
    d.ellipse([P(39), P(27), P(61), P(49)], fill=(0, 0, 0, 0))
def shield(d): poly(d, [(50, 8), (86, 22), (82, 58), (50, 92), (18, 58), (14, 22)], fill=False)
def flask(d):
    line(d, [(38, 10), (62, 10)]); line(d, [(42, 12), (42, 40), (16, 82), (22, 90), (78, 90), (84, 82), (58, 40), (58, 12)])
    poly(d, [(28, 70), (72, 70), (80, 84), (20, 84)])
def keyboard(d):
    rrect(d, 6, 24, 94, 78, 10)
    for x in (22, 38, 54, 70): dot(d, x, 42, 5)
    for x in (30, 46, 62): dot(d, x, 56, 5)
    line(d, [(32, 66), (68, 66)], 10); dot(d, 78, 56, 5)
def grid(d):
    for i in range(3):
        for j in range(3): rrect(d, 12 + i * 27, 12 + j * 27, 34 + i * 27, 34 + j * 27, 5, fill=True)
def cursor(d): poly(d, [(24, 8), (24, 82), (42, 66), (54, 92), (66, 86), (54, 62), (78, 60)])
def bell(d):
    poly(d, [(50, 12), (70, 24), (74, 60), (86, 74), (14, 74), (26, 60), (30, 24)])
    d.ellipse([P(40), P(78), P(60), P(94)], fill=W)
def move(d):
    line(d, [(50, 10), (50, 90)]); line(d, [(10, 50), (90, 50)])
    for a, b, c in [((50, 6), (38, 20), (62, 20)), ((50, 94), (38, 80), (62, 80)), ((6, 50), (20, 38), (20, 62)), ((94, 50), (80, 38), (80, 62))]: poly(d, [a, b, c])
def search(d): circ(d, 42, 42, 28); line(d, [(64, 64), (88, 88)], 18)
def back(d): line(d, [(60, 18), (28, 50), (60, 82)], 18)
def power(d): arc(d, 50, 54, 34, 300, 240 + 360); line(d, [(50, 10), (50, 48)], 16)
def check(d): line(d, [(16, 52), (40, 76), (86, 26)], 18)
def close(d): line(d, [(20, 20), (80, 80)], 16); line(d, [(80, 20), (20, 80)], 16)
def arrow_right(d): line(d, [(40, 18), (72, 50), (40, 82)], 16)
def toggle_icon(d): rrect(d, 8, 26, 92, 74, 24); d.ellipse([P(58), P(36), P(84), P(64)], fill=W)

ICONS = [("visuals", eye), ("movement", movement), ("player", player), ("hud", hud), ("client", gear),
         ("fullbright", sun), ("esp", cube), ("tracers", tracers), ("storageesp", chest), ("zoom", zoom),
         ("crosshair", crosshair), ("nohurtcam", camera), ("nooverlay", noflame), ("ambience", moon),
         ("sprint", bolt), ("autowalk", feet), ("parkour", jump), ("eagle", eagle), ("autorespawn", respawn),
         ("antiafk", clock), ("watermark", wave), ("arraylist", listicon), ("info", info), ("coordinates", pin),
         ("armor", shield), ("potions", flask), ("keystrokes", keyboard), ("inventory", grid), ("clickgui", cursor),
         ("notifications", bell), ("hudeditor", move), ("search", search), ("back", back), ("power", power),
         ("check", check), ("close", close), ("arrow", arrow_right), ("toggle", toggle_icon)]

rows = (len(ICONS) + COLS - 1) // COLS
atlas = Image.new("RGBA", (COLS * CELL, rows * CELL), (0, 0, 0, 0))
for i, (name, fn) in enumerate(ICONS):
    img = Image.new("RGBA", (C, C), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    fn(d)
    img = img.resize((CELL, CELL), Image.LANCZOS)
    atlas.paste(img, ((i % COLS) * CELL, (i // COLS) * CELL))
out = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/waveclient/textures/icons.png")
atlas.save(out)
print("ok", atlas.size, ",".join(n for n, _ in ICONS))
