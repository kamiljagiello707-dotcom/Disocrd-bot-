#version 150

// RazerClient – analityczny (SDF) zaokrąglony prostokąt: wypełnienie, obrys, cień.
// texCoord0 = pozycja lokalna w jednostkach GUI (0,0 = lewy górny róg prostokąta).

in vec2 texCoord0;
in vec4 vertexColor;

uniform vec4 ColorModulator;
uniform vec2 Size;       // szerokość, wysokość
uniform vec4 Radius;     // lewy-górny, prawy-górny, prawy-dolny, lewy-dolny
uniform float Thickness; // 0 = wypełnienie, >0 = obrys o tej grubości (do środka)
uniform float Soft;      // 0 = ostra krawędź z antyaliasingiem, >0 = miękki cień o tym zasięgu

out vec4 fragColor;

float sdRoundBox(vec2 p, vec2 b, vec4 r) {
    float rr = p.x > 0.0 ? (p.y > 0.0 ? r.z : r.y) : (p.y > 0.0 ? r.w : r.x);
    vec2 q = abs(p) - b + rr;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rr;
}

void main() {
    vec2 hs = Size * 0.5;
    float d = sdRoundBox(texCoord0 - hs, hs, Radius);
    // szerokość jednego piksela ekranu w jednostkach lokalnych
    float px = max(length(vec2(dFdx(texCoord0.x), dFdy(texCoord0.y))) * 0.70710678, 1e-4);
    float a;
    if (Soft > 0.0) {
        float s = max(Soft, px);
        a = 1.0 - smoothstep(-s, s, d);
        a *= a;
    } else {
        a = clamp(0.5 - d / px, 0.0, 1.0);
        if (Thickness > 0.0) {
            float t = max(Thickness, px);
            a *= clamp(0.5 + (d + t) / px, 0.0, 1.0);
        }
    }
    vec4 c = vertexColor * ColorModulator;
    c.a *= a;
    if (c.a <= 0.0015) discard;
    fragColor = c;
}
