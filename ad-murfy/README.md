# MURFY — 20-sekundowy spot reklamowy (w całości z kodu)

Bez kamery i bez zdjęć. Każda klatka to render fizycznie opartej sceny 3D (three.js):
przezroczysta butelka z PET z bursztynowym szamponem (transmisja i absorpcja światła w objętości),
matowa nakrętka soft-touch, szczotkowane złoto, papierowa etykieta z błyszczącą farbą i złotą folią,
trawertynowe postumenty, iryzujące bańki mydlane (interferencja cienkiej warstwy),
słońce przez liście (gobo), świetlówki studyjne (area lights), głębia ostrości, ziarno i winieta.

## Scenopis (30 kl./s, 1920×1080)
| czas | ujęcie |
|---|---|
| 0–4 s | Makro nakrętki: wyjście z czerni, wąskie światło przesuwa się po złotym pierścieniu. |
| 4–8 s | Perłowa wstęga żelu owija się spiralnie wokół butelki i odsłania etykietę. |
| 8–12 s | Plan ogólny: butelka na trawertynie, kontra słońca przez liście, wznoszą się bańki. |
| 12–16 s | Bohaterski obrót kamery wokół produktu, przesuwające się refleksy. |
| 16–20 s | Packshot i plansza: **MURFY**, „Szampon do włosów”, „Lekkość, którą czuć.” |

Dźwięk (`audio.py`) jest syntetyzowany: ciepły pad Dmaj9→Bm9→Gmaj7→A6sus→Dmaj9, arpeggio,
„whoosh” na cięciach, plumknięcia baniek i dzwonek pod planszą.

## Jak wygenerować
```bash
npm install
node render.mjs --stills 2,6,10,14,18.5   # podgląd klatek do out/
node render.mjs                           # pełny render -> out/murfy-raw.mp4
python3 audio.py out/murfy.wav
ffmpeg -i out/murfy-raw.mp4 -i out/murfy.wav -c:v copy -c:a aac -b:a 192k -shortest dist/murfy-20s.mp4
```
Gotowy plik: `dist/murfy-20s.mp4`.
