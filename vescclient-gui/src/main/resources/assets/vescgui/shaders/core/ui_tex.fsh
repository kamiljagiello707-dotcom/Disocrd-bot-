#version 150

// VescClient – tekstura z jakościowym pomniejszaniem (16 próbek w obrębie piksela)
// i opcjonalnym przycięciem do zaokrąglonego prostokąta z antyaliasingiem.

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

uniform vec4 ColorModulator;
uniform vec4 UVRect;   // u0, v0, u1, v1
uniform vec2 Size;
uniform vec4 Radius;
uniform float Rounded;

out vec4 fragColor;

float sdRoundBox(vec2 p, vec2 b, vec4 r) {
    float rr = p.x > 0.0 ? (p.y > 0.0 ? r.z : r.y) : (p.y > 0.0 ? r.w : r.x);
    vec2 q = abs(p) - b + rr;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rr;
}

void main() {
    vec2 dx = dFdx(texCoord0), dy = dFdy(texCoord0);
    vec2 lo = min(UVRect.xy, UVRect.zw), hi = max(UVRect.xy, UVRect.zw);
    vec3 rgb = vec3(0.0);
    float alpha = 0.0;
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            vec2 o = (vec2(float(i), float(j)) + 0.5) / 4.0 - 0.5;
            vec4 t = texture(Sampler0, clamp(texCoord0 + dx * o.x + dy * o.y, lo, hi));
            rgb += t.rgb * t.a;
            alpha += t.a;
        }
    }
    vec4 c = vec4(alpha > 0.0 ? rgb / alpha : vec3(0.0), alpha / 16.0);
    if (Rounded > 0.5) {
        vec2 local = (texCoord0 - UVRect.xy) / (UVRect.zw - UVRect.xy) * Size;
        vec2 hs = Size * 0.5;
        float d = sdRoundBox(local - hs, hs, Radius);
        float px = max(length(vec2(dFdx(local.x), dFdy(local.y))) * 0.70710678, 1e-4);
        c.a *= clamp(0.5 - d / px, 0.0, 1.0);
    }
    c *= vertexColor * ColorModulator;
    if (c.a <= 0.0015) discard;
    fragColor = c;
}
