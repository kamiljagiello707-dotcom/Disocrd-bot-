"""Synthesised 20 s soundtrack for the MURFY spot (no samples, pure numpy).

Warm electric-piano/pad progression at ~96 BPM, soft whooshes on the cuts
(4, 8, 12, 16 s), airy bubble "plinks" from 6 s on, and a resolving chime under the end card.
"""
import numpy as np, wave, sys

SR = 48000
DUR = 20.0
N = int(SR * DUR)
t = np.arange(N) / SR
rng = np.random.default_rng(3)
out = np.zeros((N, 2))


def note(f):
    return 440.0 * 2 ** ((f - 69) / 12)


def env_adsr(n, a, d, s, r):
    e = np.ones(n) * s
    na, nd, nr = int(a * SR), int(d * SR), int(r * SR)
    e[:na] = np.linspace(0, 1, na)
    e[na:na + nd] = np.linspace(1, s, nd)
    e[-nr:] *= np.linspace(1, 0, nr)
    return e


def add(sig, start, pan=0.0, gain=1.0):
    i = int(start * SR)
    j = min(N, i + len(sig))
    l = np.cos((pan + 1) * np.pi / 4) * gain
    r = np.sin((pan + 1) * np.pi / 4) * gain
    out[i:j, 0] += sig[: j - i] * l
    out[i:j, 1] += sig[: j - i] * r


def pad(freqs, start, dur, gain=0.06):
    n = int(dur * SR)
    tt = np.arange(n) / SR
    s = np.zeros(n)
    for f in freqs:
        for det in (-0.12, 0.0, 0.11):
            ff = f * 2 ** (det / 12)
            s += np.sin(2 * np.pi * ff * tt + rng.uniform(0, 6)) * 0.6
            s += np.sin(2 * np.pi * ff * 2 * tt) * 0.12
    s *= env_adsr(n, 1.2, 0.5, 0.85, 1.4)
    s *= 0.9 + 0.1 * np.sin(2 * np.pi * 0.3 * tt)
    add(s * gain / len(freqs), start, pan=-0.2)
    add(s * gain / len(freqs), start + 0.013, pan=0.25)


def keys(f, start, gain=0.12, dec=1.6, pan=0.0):
    n = int((dec + 0.5) * SR)
    tt = np.arange(n) / SR
    # FM electric piano
    mod = np.sin(2 * np.pi * f * 1.0 * tt) * 1.4 * np.exp(-tt * 3)
    s = np.sin(2 * np.pi * f * tt + mod) * np.exp(-tt / dec * 2.2)
    s += 0.25 * np.sin(2 * np.pi * f * 4 * tt) * np.exp(-tt * 9)
    s *= np.minimum(1, tt / 0.004)
    add(s * gain, start, pan)


def whoosh(center, dur=1.1, gain=0.18, rise=True):
    n = int(dur * SR)
    noise = rng.standard_normal(n)
    # sweeping one-pole lowpass
    tt = np.linspace(0, 1, n)
    cut = (300 + 5000 * (tt if rise else 1 - tt) ** 2) / SR
    y = np.zeros(n)
    acc = 0.0
    a = 1 - np.exp(-2 * np.pi * cut)
    for i in range(n):
        acc += a[i] * (noise[i] - acc)
        y[i] = acc
    e = np.sin(np.pi * tt) ** 2 * (tt ** 1.5 if rise else 1)
    y *= e / (np.abs(y).max() + 1e-9)
    add(y * gain, center - dur * 0.85, pan=-0.4, gain=0.9)
    add(y[::-1] * gain * 0.5, center - dur * 0.85, pan=0.4, gain=0.9)


def plink(start, f, gain=0.05, pan=0.0):
    n = int(0.35 * SR)
    tt = np.arange(n) / SR
    fs = f * (1 + 0.6 * np.exp(-tt * 40))  # tiny upward "bloop"
    ph = 2 * np.pi * np.cumsum(fs) / SR
    s = np.sin(ph) * np.exp(-tt * 14) * np.minimum(1, tt / 0.002)
    add(s * gain, start, pan)


def sub_hit(start, gain=0.35):
    n = int(1.2 * SR)
    tt = np.arange(n) / SR
    f = 55 * (1 + 1.5 * np.exp(-tt * 18))
    s = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 3.2)
    add(s * gain, start)


# --- harmony: Dmaj9 -> Bm9 -> Gmaj7#11 -> A6sus -> Dmaj9 (resolve on end card)
chords = [
    (0.0, [50, 57, 61, 64, 66]),     # D maj9
    (4.0, [47, 54, 57, 61, 62]),     # Bm9
    (8.0, [43, 50, 54, 57, 61]),     # Gmaj7#11-ish
    (12.0, [45, 52, 54, 57, 59]),    # A6sus
    (16.0, [50, 57, 61, 64, 69]),    # Dmaj9 resolve
]
for st, ch in chords:
    pad([note(m) for m in ch], st, 4.6 if st < 16 else 4.0, gain=0.07)

# gentle arpeggio, 8ths at 96 bpm starting after the fade-in
beat = 60 / 96
arps = {0: [74, 78, 81, 76], 4: [71, 74, 78, 73], 8: [74, 79, 81, 78], 12: [76, 79, 81, 83], 16: [78, 81, 85, 86]}
for st, notes in arps.items():
    k = 0
    tt0 = st + (0.6 if st == 0 else 0.0)
    while tt0 < st + 4 - 0.05 and tt0 < 19.2:
        keys(note(notes[k % 4]), tt0, gain=0.055 if st else 0.04, dec=1.2, pan=0.35 * np.sin(k))
        k += 1
        tt0 += beat / 2

# cut accents
for c in (4.0, 8.0, 12.0, 16.0):
    whoosh(c, gain=0.14 if c != 16 else 0.2)
sub_hit(16.0, 0.3)
sub_hit(0.05, 0.18)

# bubble plinks
r = np.random.default_rng(11)
for i in range(22):
    st = r.uniform(6.0, 18.5)
    plink(st, r.uniform(900, 1900), gain=r.uniform(0.02, 0.045), pan=r.uniform(-0.8, 0.8))

# end-card chime (bell partials)
for f, g in ((note(86), 0.08), (note(93), 0.05), (note(98), 0.03)):
    n = int(3.5 * SR)
    tt = np.arange(n) / SR
    s = sum(np.sin(2 * np.pi * f * m * tt) * np.exp(-tt * (1.2 + m)) / m for m in (1, 2.76, 5.4))
    add(s * g, 17.0, pan=0.15)

# simple stereo room: a few decorrelated taps
wet = np.zeros_like(out)
for d, g in ((0.031, 0.35), (0.047, 0.3), (0.071, 0.25), (0.113, 0.2), (0.167, 0.15), (0.229, 0.1)):
    k = int(d * SR)
    wet[k:, 0] += out[:-k, 1] * g
    wet[k:, 1] += out[:-k, 0] * g
out = out + wet * 0.6

# fades + normalise
fade = np.ones(N)
fade[: int(0.3 * SR)] = np.linspace(0, 1, int(0.3 * SR))
fade[-int(0.8 * SR):] = np.linspace(1, 0, int(0.8 * SR))
out *= fade[:, None]
out = np.tanh(out / np.abs(out).max() * 1.3) * 0.85

with wave.open(sys.argv[1] if len(sys.argv) > 1 else 'out/murfy.wav', 'wb') as w:
    w.setnchannels(2)
    w.setsampwidth(2)
    w.setframerate(SR)
    w.writeframes((out * 32767).astype('<i2').tobytes())
print('ok')
