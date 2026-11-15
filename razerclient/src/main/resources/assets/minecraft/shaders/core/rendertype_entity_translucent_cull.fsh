#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord1;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    vec4 color = tex * vertexColor * ColorModulator;
    // RazerClient ToolColor: alfa koloru w zakresie 200..254 oznacza koloryzację (siła = (a-200)/54)
    float a = vertexColor.a * 255.0;
    if (a > 199.5 && a < 254.5) {
        float k = clamp((a - 200.0) / 54.0, 0.0, 1.0);
        float l = dot(tex.rgb, vec3(0.299, 0.587, 0.114));
        float light = max(vertexColor.r, max(vertexColor.g, vertexColor.b));
        vec3 colorized = vec3(pow(l, 0.7) * 1.6) * vertexColor.rgb;
        vec3 orig = tex.rgb * light;
        color = vec4(mix(orig, colorized, k), tex.a) * ColorModulator;
    }
    if (color.a < 0.1) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
