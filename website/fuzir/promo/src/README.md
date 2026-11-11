# Film promocyjny Fuzir.tech

Animacja jest w `promo.html` (funkcja `render(t)` rysuje dowolny moment filmu), muzyka generowana w `music.py`.

Jak wyrenderować ponownie:

1. Uruchom serwer w folderze `website`: `python3 -m http.server 8090`
2. `node capture-assets.js ./assets` i `node capture-montes.js ./assets` (nagrywa materiał ze stron), skopiuj `../fuzir.jpg` jako `assets/photo.jpg`
3. `node render-frames.js "$PWD" 3` (klatki do `frames/`)
4. `python3 music.py music.wav`
5. `ffmpeg -framerate 30 -i frames/f%04d.jpg -i music.wav -c:v libx264 -crf 19 -pix_fmt yuv420p -c:a aac -b:a 192k -shortest -movflags +faststart fuzir-promo.mp4`

Wymaga Node z Playwright, Pythona z numpy i ffmpeg.

## Wersja 60 s

- animacja: `promo60.html`, muzyka: `music60.py` (120 BPM)
- dodatkowy materiał: `node capture2.js ./assets <ścieżka do podglądu bota exchange>`
- klatki: `node render60.js "$PWD" promo60.html frames60 3`
- `python3 music60.py music60.wav`
- `ffmpeg -framerate 30 -i frames60/f%04d.jpg -i music60.wav -c:v libx264 -crf 20 -pix_fmt yuv420p -c:a aac -b:a 192k -shortest -movflags +faststart fuzir-promo-60s.mp4`

## Wersja 2 min (pełne promo)

- animacja: `promo120.html` (19 scen, 240 bitów), muzyka: `music120.py` (tworzona z `music60.py` przez `make-music120.py`)
- ekrany Fuzir Studio i drugi zrzut BubleBota: `node capture3.js ./assets`, ikona: skopiuj `../../app/icon-512.png` jako `assets/app-icon.png`
- klatki: `node render60.js "$PWD" promo120.html frames120 3`
- `python3 music120.py music120.wav`
- `ffmpeg -framerate 30 -i frames120/f%04d.jpg -i music120.wav -c:v libx264 -preset slow -crf 20 -pix_fmt yuv420p -c:a aac -b:a 192k -shortest -movflags +faststart fuzir-promo-120s.mp4`
