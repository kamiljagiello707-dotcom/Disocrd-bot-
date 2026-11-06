#version 150

// Zaokrąglony prostokąt liczony jako SDF: gładkie rogi, miękkie cienie i obrysy.
in vec4 vertexColor;

uniform vec4 ColorModulator;
uniform vec2 ScreenSize;
uniform vec4 Rect;      // x, y, szerokość, wysokość w pikselach (od lewego górnego rogu)
uniform float Radius;   // promień w pikselach
uniform float Softness; // szerokość wygładzenia / rozmycia w pikselach
uniform float Thickness;// > 0 = tylko obrys

out vec4 fragColor;

float roundedBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    vec2 frag = vec2(gl_FragCoord.x, ScreenSize.y - gl_FragCoord.y);
    vec2 halfSize = Rect.zw * 0.5;
    float d = roundedBox(frag - (Rect.xy + halfSize), halfSize, Radius);
    if (Thickness > 0.0) {
        d = abs(d + Thickness * 0.5) - Thickness * 0.5;
    }
    float alpha = 1.0 - smoothstep(-Softness, Softness, d);
    vec4 color = vertexColor * ColorModulator;
    color.a *= alpha;
    if (color.a <= 0.002) {
        discard;
    }
    fragColor = color;
}
