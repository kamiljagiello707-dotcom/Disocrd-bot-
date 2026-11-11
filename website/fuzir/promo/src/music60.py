# własny podkład do filmu 60 s (120 BPM, 120 bitów), bez cudzych sampli
import numpy as np, wave, sys

SR = 44100
BPM = 120
BEAT = 60 / BPM
TOTAL = BEAT * 120
N = int(SR * (TOTAL + 0.5))
rng = np.random.default_rng(7)
L = np.zeros(N); R = np.zeros(N)

def idx(t): return int(t * SR)
def note(n): return 440.0 * 2 ** ((n - 69) / 12)

def add(buf, t, sig, gain=1.0):
    i = idx(t)
    if i >= N: return
    sig = sig[: N - i]
    buf[i:i + len(sig)] += sig * gain

def env_adsr(n, a, d, s, r, sustain_len):
    a, d, r = int(a * SR), int(d * SR), int(r * SR)
    sl = max(0, int(sustain_len * SR) - a - d)
    e = np.concatenate([np.linspace(0, 1, a, False), np.linspace(1, s, d, False), np.full(sl, s), np.linspace(s, 0, r)])
    return e[:n] if len(e) >= n else np.pad(e, (0, n - len(e)))

def lowpass(x, cutoff):
    # prosty filtr jednobiegunowy, kilka przejść
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = x.copy()
    for _ in range(2):
        out = np.empty_like(y); acc = 0.0
        for i in range(len(y)):
            acc = (1 - a) * y[i] + a * acc
            out[i] = acc
        y = out
    return y

def saw(freq, dur, detune=0.0, harmonics=10):
    t = np.arange(int(dur * SR)) / SR
    s = np.zeros_like(t)
    for k in range(1, harmonics + 1):
        s += np.sin(2 * np.pi * freq * (1 + detune) * k * t + k) / k
    return s * 0.6

# akordy: Am - F - C - G (po 2 takty = 8 bitów)
chords = [[57, 60, 64], [53, 57, 60], [48, 52, 55], [55, 59, 62]]
roots = [45, 41, 48, 43]

def chord_at(beat):
    return int(beat // 8) % 4

# pad przez cały utwór
for c in range(15):
    t0 = c * 8 * BEAT
    dur = 8 * BEAT + 0.6
    ch = chords[c % 4]
    pad_l = np.zeros(int(dur * SR)); pad_r = np.zeros(int(dur * SR))
    for n in ch + [ch[0] + 12]:
        pad_l += saw(note(n), dur, -0.004, 8)
        pad_r += saw(note(n), dur, 0.004, 8)
    e = env_adsr(len(pad_l), 0.8, 0.5, 0.8, 1.0, dur - 1.0)
    vol = 0.075 if c in (0, 10) else 0.05
    add(L, t0, pad_l * e, vol); add(R, t0, pad_r * e, vol)

PAD_LP = True

# stopa
def kick():
    d = 0.42; t = np.arange(int(d * SR)) / SR
    f = 45 + 95 * np.exp(-t * 28)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) * np.exp(-t * 7.5) + 0.25 * np.sin(ph * 2) * np.exp(-t * 30)

def hat(open_=False):
    d = 0.25 if open_ else 0.06
    n = rng.standard_normal(int(d * SR))
    n = np.diff(np.concatenate([[0], n]))
    n = lowpass(n, 11000)
    return n * np.exp(-np.arange(len(n)) / SR * (14 if open_ else 70))

def clap():
    d = 0.25
    n = rng.standard_normal(int(d * SR))
    n = lowpass(n - lowpass(n, 900), 5000)
    e = np.exp(-np.arange(len(n)) / SR * 16)
    for k in (0.0, 0.011, 0.022):
        e[int(k * SR):int(k * SR) + 60] += 0.6
    return n * e * 0.6

def bass(n, dur):
    t = np.arange(int(dur * SR)) / SR
    s = np.sin(2 * np.pi * note(n) * t) + 0.35 * np.sin(2 * np.pi * note(n) * 2 * t)
    s = np.tanh(s * 1.6)
    return s * env_adsr(len(s), 0.005, 0.08, 0.7, 0.05, dur - 0.05)

def pluck(n, dur=0.3):
    t = np.arange(int(dur * SR)) / SR
    s = saw(note(n), dur, 0, 6)
    return s * np.exp(-t * 11)

K, H, HO, C = kick(), hat(), hat(True), clap()
DROP1, BREAK, DROP2, END = 8, 80, 88, 110

def lead(n, dur):
    t = np.arange(int(dur * SR)) / SR
    vib = 1 + 0.004 * np.sin(2 * np.pi * 5.5 * t)
    s_ = saw(note(n), dur, 0, 7) * 0.6 + np.sin(2 * np.pi * note(n) * vib * t) * 0.5
    return lowpass(s_, 3200) * env_adsr(len(t), 0.02, 0.15, 0.6, 0.12, dur - 0.1)

# motyw melodii (w skali a-moll), po 8 bitów na akord
MOTIF = [[69, 72, 76, 74], [69, 72, 77, 76], [67, 72, 76, 79], [67, 71, 74, 72]]

for b in range(120):
    t = b * BEAT
    ci = chord_at(b)
    groove_on = DROP1 <= b < BREAK or DROP2 <= b < END
    if groove_on:
        half = 32 <= b < 40  # scena z kodem: lżej
        if not half or b % 2 == 0:
            if not (b in (31, 87)):
                add(L, t, K, 0.55); add(R, t, K, 0.55)
        if b % 2 == 1 and not half:
            add(L, t, C, 0.18); add(R, t, C, 0.18)
        add(L, t + BEAT / 2, H, 0.07); add(R, t + BEAT / 2, H, 0.09)
        if b >= DROP2 or 48 <= b < 72:
            add(L, t + BEAT / 4, H, 0.03); add(R, t + 3 * BEAT / 4, H, 0.03)
        if b % 4 == 3:
            add(L, t + BEAT / 2, HO, 0.05); add(R, t + BEAT / 2, HO, 0.05)
        for k in range(2):
            bt = t + k * BEAT / 2
            bs = bass(roots[ci] - 12 + (12 if k == 1 and b % 2 else 0), BEAT / 2 * 0.92)
            add(L, bt, bs, 0.16); add(R, bt, bs, 0.16)
    if 16 <= b < END:
        ch = chords[ci]
        seq = [ch[0] + 12, ch[1] + 12, ch[2] + 12, ch[1] + 24]
        steps = 4 if b < BREAK or b >= DROP2 else 2
        for k in range(steps):
            pt = t + k * BEAT / steps
            p = pluck(seq[(b * 4 + k) % 4])
            pan = 0.5 + 0.35 * np.sin(b + k)
            g_ = 0.035 if b < BREAK or b >= DROP2 else 0.05
            add(L, pt, p, g_ * (1 - pan) * 2); add(R, pt, p, g_ * pan * 2)
    if DROP2 <= b < END and b % 2 == 0:
        m = MOTIF[ci][(b // 2) % 4]
        ld = lead(m, BEAT * 1.8)
        add(L, t, ld, 0.05); add(R, t + 0.012, ld, 0.05)
    if b == END:
        add(L, t, K, 0.6); add(R, t, K, 0.6)

# narastania i uderzenia
def riser(dur):
    n = rng.standard_normal(int(dur * SR))
    t = np.arange(len(n)) / SR
    hp = lowpass(n - lowpass(n, 1500), 7000)
    return hp * (t / dur) ** 2.2 * 1.6

def impact():
    d = 2.2; t = np.arange(int(d * SR)) / SR
    boom = np.sin(2 * np.pi * (38 + 30 * np.exp(-t * 6)) * t) * np.exp(-t * 2.2)
    n = rng.standard_normal(len(t)) * np.exp(-t * 5)
    return boom * 0.9 + lowpass(n, 2500) * 0.5

RS = riser(BEAT * 4)
for cut in (8, 88, 110):
    add(L, cut * BEAT - BEAT * 4, RS, 0.16); add(R, cut * BEAT - BEAT * 4, RS, 0.16)
IM = impact()
for cut in (8, 88, 110):
    add(L, cut * BEAT, IM, 0.45); add(R, cut * BEAT, IM, 0.45)
RS2 = riser(BEAT * 2)
for cut in (16, 32, 40, 48, 56, 64, 72, 80, 96, 104):
    add(L, cut * BEAT - BEAT * 2, RS2, 0.06); add(R, cut * BEAT - BEAT * 2, RS2, 0.06)

# intro: szum fal
t = np.arange(idx(BEAT * 8)) / SR
waves = lowpass(rng.standard_normal(len(t)), 500) * (0.5 + 0.5 * np.sin(t * 1.3)) * np.minimum(1, t / 1.5)
add(L, 0, waves, 0.35); add(R, 0, waves[::-1].copy(), 0.35)

mix = np.stack([L, R], 1)
mix = np.tanh(mix * 1.4) / np.tanh(1.4)
fade_in = np.minimum(1, np.arange(N) / (SR * 0.4))
fade_out = np.clip((TOTAL - np.arange(N) / SR) / 2.2, 0, 1)
mix *= (fade_in * fade_out)[:, None]
mix /= np.max(np.abs(mix)) / 0.89
out = (mix * 32767).astype(np.int16)
with wave.open(sys.argv[1], 'wb') as w:
    w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes(out.tobytes())
print('zapisano', sys.argv[1], f'{TOTAL:.2f}s')
