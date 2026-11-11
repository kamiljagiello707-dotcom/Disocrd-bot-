# tworzy music120.py z music60.py: 120 BPM, 240 bitów, struktura pod film 2 min
import os
d = os.path.dirname(os.path.abspath(__file__)) + '/'
s = open(d + 'music60.py').read()
def sub(a, b):
    global s
    assert a in s, a[:60]
    s = s.replace(a, b)
sub('TOTAL = BEAT * 120', 'TOTAL = BEAT * 240')
sub('for c in range(15):', 'for c in range(30):')
sub('vol = 0.075 if c in (0, 10) else 0.05', 'vol = 0.075 if c in (0, 1, 20, 27, 28, 29) else 0.05')
sub('DROP1, BREAK, DROP2, END = 8, 80, 88, 110', 'DROP1, BREAK, DROP2, END = 12, 162, 174, 216')
sub('for b in range(120):', 'for b in range(240):')
sub('half = 32 <= b < 40', 'half = 52 <= b < 64')
sub('if not (b in (31, 87)):', 'if not (b in (51, 123)):')
sub('if b >= DROP2 or 48 <= b < 72:', 'if b >= DROP2 or 124 <= b < 162:')
sub('if 16 <= b < END:', 'if 20 <= b < 236:')
sub('for cut in (8, 88, 110):\n    add(L, cut * BEAT - BEAT * 4', 'for cut in (12, 124, 174, 216):\n    add(L, cut * BEAT - BEAT * 4')
sub('for cut in (8, 88, 110):\n    add(L, cut * BEAT, IM', 'for cut in (12, 124, 174, 216):\n    add(L, cut * BEAT, IM')
sub('for cut in (16, 32, 40, 48, 56, 64, 72, 80, 96, 104):', 'for cut in (20, 28, 52, 64, 76, 86, 96, 106, 116, 134, 184, 196, 206):')
sub('t = np.arange(idx(BEAT * 8)) / SR', 't = np.arange(idx(BEAT * 12)) / SR')
sub('fade_out = np.clip((TOTAL - np.arange(N) / SR) / 2.2, 0, 1)', 'fade_out = np.clip((TOTAL - np.arange(N) / SR) / 3.0, 0, 1)')
s = s.replace('60 s (120 BPM, 120 bitów)', '2 min (120 BPM, 240 bitów)')
open(d + 'music120.py', 'w').write(s)
print('ok')
