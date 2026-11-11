#version 150

// Zaokrąglony prostokąt liczony funkcją odległości (SDF):
// gładkie krawędzie, obramowanie i miękki cień w jednym shaderze.
uniform vec4 Shape; // szerokość i wysokość quada, połowa szerokości i wysokości kształtu (piksele)
uniform vec4 Style; // promień, miękkość krawędzi, grubość obramowania (0 = wypełnienie)

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float roundedBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + vec2(r);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    vec2 p = (texCoord0 - 0.5) * Shape.xy;
    float r = min(Style.x, min(Shape.z, Shape.w));
    float d = roundedBox(p, Shape.zw, r);
    float soft = max(Style.y, 0.55);
    float a = 1.0 - smoothstep(-soft, soft, d);
    if (Style.z > 0.0) {
        a *= smoothstep(-Style.z - 0.55, -Style.z + 0.55, d);
    }
    a *= vertexColor.a;
    if (a <= 0.002) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb, a);
}
