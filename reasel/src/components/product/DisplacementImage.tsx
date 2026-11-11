'use client';

import { useEffect, useRef, useState } from 'react';
import Image from 'next/image';
import { cn } from '@/lib/format';

const VERT = `
attribute vec2 position;
varying vec2 vUv;
void main() {
  vUv = position * 0.5 + 0.5;
  gl_Position = vec4(position, 0.0, 1.0);
}`;

const FRAG = `
precision mediump float;
uniform sampler2D uTex1;
uniform sampler2D uTex2;
uniform float uProgress;
uniform float uTime;
uniform vec2 uRes;
uniform vec2 uImg1;
uniform vec2 uImg2;
varying vec2 vUv;

// object-fit: cover w shaderze
vec2 cover(vec2 uv, vec2 res, vec2 img) {
  float rs = res.x / res.y;
  float ri = img.x / img.y;
  vec2 s = rs < ri ? vec2(rs / ri, 1.0) : vec2(1.0, ri / rs);
  return (uv - 0.5) * s + 0.5;
}

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float noise(vec2 p) {
  vec2 i = floor(p); vec2 f = fract(p);
  vec2 u = f * f * (3.0 - 2.0 * f);
  return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

void main() {
  float n = noise(vUv * 4.0 + uTime * 0.15) * 0.6 + noise(vUv * 9.0 - uTime * 0.1) * 0.4;
  float p = uProgress;
  float strength = 0.28;
  vec2 uv1 = cover(vUv, uRes, uImg1) + vec2(n - 0.5, (n - 0.5) * 0.6) * p * strength;
  vec2 uv2 = cover(vUv, uRes, uImg2) - vec2(n - 0.5, (n - 0.5) * 0.6) * (1.0 - p) * strength;
  vec4 a = texture2D(uTex1, uv1);
  vec4 b = texture2D(uTex2, uv2);
  float mask = smoothstep(0.0, 1.0, clamp(p * 1.6 - n * 0.6 + (vUv.y - 0.5) * 0.25, 0.0, 1.0));
  gl_FragColor = mix(a, b, mask);
}`;

/** Zdjęcie przez optymalizator Next.js – ta sama domena, więc brak problemów z CORS w WebGL. */
const optimized = (src: string, w = 640) => `/_next/image?url=${encodeURIComponent(src)}&w=${w}&q=75`;

function loadImage(src: string) {
  return new Promise<HTMLImageElement>((resolve, reject) => {
    const img = new window.Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => resolve(img);
    img.onerror = reject;
    img.src = src;
  });
}

function compile(gl: WebGLRenderingContext, type: number, src: string) {
  const s = gl.createShader(type)!;
  gl.shaderSource(s, src);
  gl.compileShader(s);
  return s;
}

interface Props {
  images: [{ src: string; alt: string }, { src: string; alt: string }];
  hovered: boolean;
  /** Włącza shader (desktop, brak reduced motion). W innym wypadku – crossfade CSS. */
  enableShader: boolean;
  sizes: string;
  priority?: boolean;
  className?: string;
}

/**
 * Płynne przejście między zdjęciami produktu shaderem z deformacją (displacement).
 * Kontekst WebGL tworzony jest dopiero przy najechaniu i zwalniany po powrocie,
 * więc siatka wielu kart nie wyczerpuje limitu kontekstów przeglądarki.
 */
export function DisplacementImage({ images, hovered, enableShader, sizes, priority, className }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [glVisible, setGlVisible] = useState(false);
  const [glReady, setGlReady] = useState(false);
  // Po zwolnieniu kontekstu WebGL canvas musi zostać zamontowany na nowo
  const [canvasKey, setCanvasKey] = useState(0);
  const glState = useRef<{
    gl: WebGLRenderingContext;
    uniforms: Record<string, WebGLUniformLocation | null>;
    progress: number;
    raf: number;
    target: number;
    start: number;
  } | null>(null);

  // Inicjalizacja WebGL przy pierwszym najechaniu
  useEffect(() => {
    if (!enableShader || !hovered || glState.current) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const gl = canvas.getContext('webgl', { premultipliedAlpha: false, antialias: false });
    if (!gl) return;
    let cancelled = false;

    const rect = canvas.getBoundingClientRect();
    const dpr = Math.min(window.devicePixelRatio, 2);
    canvas.width = Math.max(1, Math.round(rect.width * dpr));
    canvas.height = Math.max(1, Math.round(rect.height * dpr));

    const program = gl.createProgram()!;
    gl.attachShader(program, compile(gl, gl.VERTEX_SHADER, VERT));
    gl.attachShader(program, compile(gl, gl.FRAGMENT_SHADER, FRAG));
    gl.linkProgram(program);
    gl.useProgram(program);

    const buffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW);
    const loc = gl.getAttribLocation(program, 'position');
    gl.enableVertexAttribArray(loc);
    gl.vertexAttribPointer(loc, 2, gl.FLOAT, false, 0, 0);

    const uniforms = Object.fromEntries(
      ['uTex1', 'uTex2', 'uProgress', 'uTime', 'uRes', 'uImg1', 'uImg2'].map((n) => [n, gl.getUniformLocation(program, n)]),
    );

    Promise.all([loadImage(optimized(images[0].src)), loadImage(optimized(images[1].src))])
      .then(([a, b]) => {
        if (cancelled) return;
        [a, b].forEach((img, i) => {
          const tex = gl.createTexture();
          gl.activeTexture(i === 0 ? gl.TEXTURE0 : gl.TEXTURE1);
          gl.bindTexture(gl.TEXTURE_2D, tex);
          gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, true);
          gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, img);
          gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
          gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.MIRRORED_REPEAT);
          gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.MIRRORED_REPEAT);
        });
        gl.uniform1i(uniforms.uTex1, 0);
        gl.uniform1i(uniforms.uTex2, 1);
        gl.uniform2f(uniforms.uRes, canvas.width, canvas.height);
        gl.uniform2f(uniforms.uImg1, a.naturalWidth, a.naturalHeight);
        gl.uniform2f(uniforms.uImg2, b.naturalWidth, b.naturalHeight);
        gl.viewport(0, 0, canvas.width, canvas.height);
        glState.current = { gl, uniforms, progress: 0, raf: 0, target: 1, start: performance.now() };
        setGlReady(true);
      })
      .catch(() => {
        /* błąd ładowania – zostaje crossfade CSS */
      });

    return () => {
      cancelled = true;
    };
  }, [enableShader, hovered, images, canvasKey]);

  // Animacja postępu w stronę 0 / 1
  useEffect(() => {
    const s = glState.current;
    if (!s || !glReady) return;
    s.target = hovered ? 1 : 0;
    if (hovered) setGlVisible(true);
    cancelAnimationFrame(s.raf);

    const loop = () => {
      s.progress += (s.target - s.progress) * 0.075;
      if (Math.abs(s.target - s.progress) < 0.002) s.progress = s.target;
      const { gl, uniforms } = s;
      gl.uniform1f(uniforms.uProgress, s.progress);
      gl.uniform1f(uniforms.uTime, (performance.now() - s.start) / 1000);
      gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
      if (s.progress !== s.target) {
        s.raf = requestAnimationFrame(loop);
      } else if (s.target === 0) {
        // Powrót zakończony – zwalniamy kontekst WebGL
        setGlVisible(false);
        s.gl.getExtension('WEBGL_lose_context')?.loseContext();
        glState.current = null;
        setGlReady(false);
        setCanvasKey((k) => k + 1);
      }
    };
    s.raf = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(s.raf);
  }, [hovered, glReady]);

  const useCss = !enableShader;

  return (
    <div className={cn('relative h-full w-full overflow-hidden', className)}>
      <Image
        src={images[0].src}
        alt={images[0].alt}
        fill
        sizes={sizes}
        priority={priority}
        className={cn(
          'object-cover transition-[transform,opacity] duration-[1200ms] ease-expo',
          hovered && 'scale-[1.04]',
          useCss && hovered && 'opacity-0',
        )}
      />
      {useCss && (
        <Image
          src={images[1].src}
          alt=""
          aria-hidden
          fill
          sizes={sizes}
          className={cn(
            'object-cover opacity-0 transition-[transform,opacity] duration-700 ease-expo',
            hovered && 'scale-[1.04] opacity-100',
          )}
        />
      )}
      {enableShader && (hovered || glVisible) && (
        <canvas
          key={canvasKey}
          ref={canvasRef}
          aria-hidden
          className={cn('pointer-events-none absolute inset-0 h-full w-full', glVisible ? 'opacity-100' : 'opacity-0')}
        />
      )}
    </div>
  );
}
