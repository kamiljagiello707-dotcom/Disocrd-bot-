#version 150

// VescClient – tekst TTF z 16 próbkami w obrębie piksela: gładkie krawędzie glifów przy każdej skali
// (vanilla próbkuje "nearest" i odrzuca piksele z alfą < 0.1, co daje poszarpane litery).

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec2 dx = dFdx(texCoord0), dy = dFdy(texCoord0);
    float a = 0.0;
    for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
            vec2 o = (vec2(float(i), float(j)) + 0.5) / 4.0 - 0.5;
            a += texture(Sampler0, texCoord0 + dx * o.x + dy * o.y).r;
        }
    }
    a /= 16.0;
    vec4 color = vec4(1.0, 1.0, 1.0, a) * vertexColor * ColorModulator;
    if (color.a < 0.004) discard;
    if (vertexDistance > FogStart) {
        float f = vertexDistance < FogEnd ? smoothstep(FogStart, FogEnd, vertexDistance) : 1.0;
        color.rgb = mix(color.rgb, FogColor.rgb, f * FogColor.a);
    }
    fragColor = color;
}
