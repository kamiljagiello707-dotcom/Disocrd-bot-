import { ImageResponse } from 'next/og';

export const alt = 'Reasel.pl — premium streetwear';
export const size = { width: 1200, height: 630 };
export const contentType = 'image/png';

/** Generowany obraz Open Graph (udostępnienia w social media). */
export default function OpengraphImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: '100%',
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          background: 'radial-gradient(circle at 70% 40%, #2a3300 0%, #0a0a0a 55%)',
          padding: 72,
          color: '#f2f1ee',
          fontFamily: 'sans-serif',
        }}
      >
        <div style={{ display: 'flex', fontSize: 28, letterSpacing: 8, color: '#a3a19d' }}>DROP 04 · FW26</div>
        <div style={{ display: 'flex', alignItems: 'flex-end', fontSize: 220, fontWeight: 800, letterSpacing: -8, lineHeight: 1 }}>
          REASEL<span style={{ color: '#c6ff00' }}>.</span>
        </div>
        <div style={{ display: 'flex', fontSize: 34, color: '#c9c7c4' }}>Premium streetwear · Made in Poland</div>
      </div>
    ),
    size,
  );
}
