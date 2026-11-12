// MURFY — 20 s product spot, rendered entirely in code (three.js, physically based).
// Every visual is a pure function of time t (seconds) so frames render deterministically.
import * as THREE from 'three';
import { EffectComposer } from 'three/addons/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/addons/postprocessing/RenderPass.js';
import { BokehPass } from 'three/addons/postprocessing/BokehPass.js';
import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js';
import { OutputPass } from 'three/addons/postprocessing/OutputPass.js';
import { ShaderPass } from 'three/addons/postprocessing/ShaderPass.js';
import { RectAreaLightUniformsLib } from 'three/addons/lights/RectAreaLightUniformsLib.js';

const W = 1920, H = 1080;
const PLINTH_H = 0.5;
const OVAL = 0.62; // depth / width ratio of the bottle cross-section

// ---------- helpers ----------
const clamp01 = (x) => Math.min(1, Math.max(0, x));
const smooth = (a, b, x) => { const t = clamp01((x - a) / (b - a)); return t * t * (3 - 2 * t); };
const easeInOut = (x) => { x = clamp01(x); return x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2; };
const easeOut = (x) => 1 - Math.pow(1 - clamp01(x), 3);
const lerp = (a, b, t) => a + (b - a) * t;
const V = (x, y, z) => new THREE.Vector3(x, y, z);
const vlerp = (a, b, t) => a.clone().lerp(b, t);
function rng(seed) { return () => { seed |= 0; seed = seed + 0x6D2B79F5 | 0; let t = Math.imul(seed ^ seed >>> 15, 1 | seed); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; }; }

function valueNoise(seed) {
  const r = rng(seed), N = 256, tab = new Float32Array(N * N);
  for (let i = 0; i < tab.length; i++) tab[i] = r();
  const at = (x, y) => tab[((y & (N - 1)) * N) + (x & (N - 1))];
  return (x, y) => {
    const xi = Math.floor(x), yi = Math.floor(y), xf = x - xi, yf = y - yi;
    const u = xf * xf * (3 - 2 * xf), v = yf * yf * (3 - 2 * yf);
    return lerp(lerp(at(xi, yi), at(xi + 1, yi), u), lerp(at(xi, yi + 1), at(xi + 1, yi + 1), u), v);
  };
}

// ---------- renderer ----------
const renderer = new THREE.WebGLRenderer({ antialias: false, preserveDrawingBuffer: true, powerPreference: 'high-performance' });
renderer.setPixelRatio(1);
renderer.setSize(W, H);
renderer.toneMapping = THREE.NeutralToneMapping;
renderer.toneMappingExposure = 1.0;
renderer.shadowMap.enabled = true;
renderer.shadowMap.type = THREE.PCFSoftShadowMap;
document.getElementById('stage').prepend(renderer.domElement);
RectAreaLightUniformsLib.init();

const scene = new THREE.Scene();
scene.background = new THREE.Color(0xe6dacb);
const camera = new THREE.PerspectiveCamera(30, W / H, 0.05, 60);

// ---------- studio environment (HDR softboxes baked into a PMREM) ----------
function buildEnvironment() {
  const env = new THREE.Scene();
  const room = new THREE.Mesh(new THREE.BoxGeometry(24, 18, 24), new THREE.MeshBasicMaterial({ color: 0x3b332b, side: THREE.BackSide }));
  room.position.y = 6; env.add(room);
  const box = (w, h, pos, look, k, col = 0xffffff) => {
    const m = new THREE.Mesh(new THREE.PlaneGeometry(w, h), new THREE.MeshBasicMaterial({ color: new THREE.Color(col).multiplyScalar(k), side: THREE.DoubleSide }));
    m.position.copy(pos); m.lookAt(look); env.add(m);
  };
  const c = V(0, 1.5, 0);
  box(7, 7, V(0, 12, 0), c, 3.2, 0xfff3e4);           // overhead
  box(1.4, 10, V(-8, 4, -2.5), c, 9.0, 0xfff1e0);     // left strip
  box(1.4, 10, V(8, 4, -1.5), c, 9.0, 0xffffff);      // right strip
  box(7, 6, V(-4, 3, 9), c, 3.2, 0xffe7cc);           // warm front fill
  box(9, 4, V(0, 3, -11), c, 1.2, 0xf5e9da);          // back bounce
  box(4, 3, V(9, 2, 8), c, 1.0, 0xffffff);            // right front card
  const pmrem = new THREE.PMREMGenerator(renderer);
  const tex = pmrem.fromScene(env, 0.02).texture;
  pmrem.dispose();
  return tex;
}
scene.environment = buildEnvironment();
scene.environmentIntensity = 0.62;

// ---------- cyclorama (seamless paper sweep) ----------
function cyclorama() {
  const prof = []; // (y, z) along the sweep
  const R = 4.5, zc = -5;
  for (let z = 14; z > zc; z -= 0.5) prof.push([0, z]);
  for (let i = 0; i <= 40; i++) { const a = (i / 40) * Math.PI / 2; prof.push([R - R * Math.cos(a), zc - R * Math.sin(a)]); }
  for (let y = R + 0.5; y <= 16; y += 0.5) prof.push([y, zc - R]);
  const xs = 2, segX = 2; const pos = [], uv = [], idx = [];
  for (let j = 0; j < prof.length; j++) for (let i = 0; i <= segX; i++) {
    const x = -20 + 40 * i / segX; pos.push(x, prof[j][0], prof[j][1]); uv.push(i / segX * xs, j / prof.length * 6);
  }
  for (let j = 0; j < prof.length - 1; j++) for (let i = 0; i < segX; i++) {
    const a = j * (segX + 1) + i, b = a + 1, c = a + segX + 1, d = c + 1; idx.push(a, c, b, b, c, d);
  }
  const g = new THREE.BufferGeometry();
  g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
  g.setAttribute('uv', new THREE.Float32BufferAttribute(uv, 2));
  const col = [];
  for (let j = 0; j < prof.length; j++) { const k = lerp(0.8, 1.0, smooth(0.0, 3.2, prof[j][0])) * lerp(0.94, 1.0, smooth(-1, 6, -prof[j][1] + 2)); for (let i = 0; i <= segX; i++) col.push(k, k * 0.985, k * 0.965); }
  g.setAttribute('color', new THREE.Float32BufferAttribute(col, 3));
  g.setIndex(idx); g.computeVertexNormals();
  return g;
}
const paperTex = (() => {
  const c = document.createElement('canvas'); c.width = c.height = 512; const x = c.getContext('2d');
  const img = x.createImageData(512, 512), n = valueNoise(7);
  for (let j = 0; j < 512; j++) for (let i = 0; i < 512; i++) {
    const v = 0.5 * n(i / 3, j / 3) + 0.3 * n(i / 11, j / 11) + 0.2 * n(i / 40, j / 40);
    const k = (i + j * 512) * 4, g = 200 + v * 40; img.data[k] = img.data[k + 1] = img.data[k + 2] = g; img.data[k + 3] = 255;
  }
  x.putImageData(img, 0, 0);
  const t = new THREE.CanvasTexture(c); t.wrapS = t.wrapT = THREE.RepeatWrapping; t.repeat.set(8, 8); return t;
})();
const cyc = new THREE.Mesh(cyclorama(), new THREE.MeshPhysicalMaterial({ color: 0xece1d3, vertexColors: true, roughness: 0.93, bumpMap: paperTex, bumpScale: 0.6, sheen: 0.2, sheenColor: 0xfff4e8 }));
cyc.receiveShadow = true; scene.add(cyc);

// ---------- travertine plinths ----------
const travertine = (() => {
  const w = 2048, h = 512;
  const col = document.createElement('canvas'); col.width = w; col.height = h;
  const bmp = document.createElement('canvas'); bmp.width = w; bmp.height = h;
  const cx = col.getContext('2d'), bx = bmp.getContext('2d');
  const ci = cx.createImageData(w, h), bi = bx.createImageData(w, h);
  const n1 = valueNoise(11), n2 = valueNoise(23), n3 = valueNoise(37);
  for (let j = 0; j < h; j++) for (let i = 0; i < w; i++) {
    const warp = n2(i / 160, j / 60) * 14;
    const band = 0.5 + 0.5 * Math.sin((j + warp * 2.5) * 0.045 + n1(i / 200, j / 20) * 5.0);
    const fine = n1(i / 2.5, j / 1.6) * 0.5 + n3(i / 9, j / 3) * 0.5;
    const pitN = n3(i / 6, j / 2.2);
    const pit = smooth(0.74, 0.86, pitN) * smooth(0.3, 0.6, n2(i / 40, j / 12));
    const k = (i + j * w) * 4;
    const base = 0.84 + band * 0.05 + fine * 0.06 - pit * 0.3;
    ci.data[k] = 232 * base; ci.data[k + 1] = 222 * base; ci.data[k + 2] = 206 * base; ci.data[k + 3] = 255;
    const b = 200 + fine * 40 - pit * 150; bi.data[k] = bi.data[k + 1] = bi.data[k + 2] = b; bi.data[k + 3] = 255;
  }
  cx.putImageData(ci, 0, 0); bx.putImageData(bi, 0, 0);
  const map = new THREE.CanvasTexture(col); map.colorSpace = THREE.SRGBColorSpace; map.anisotropy = 8; map.wrapS = THREE.RepeatWrapping;
  const bump = new THREE.CanvasTexture(bmp); bump.wrapS = THREE.RepeatWrapping;
  return new THREE.MeshPhysicalMaterial({ map, bumpMap: bump, bumpScale: 1.2, roughness: 0.62, clearcoat: 0.15, clearcoatRoughness: 0.5 });
})();
function plinth(r, h, x, z) {
  const m = new THREE.Mesh(new THREE.CylinderGeometry(r, r, h, 128, 1), travertine);
  m.position.set(x, h / 2, z); m.castShadow = m.receiveShadow = true; scene.add(m); return m;
}
plinth(1.15, PLINTH_H, 0, 0);
plinth(0.7, 1.15, -2.5, -2.4);
plinth(0.95, 0.24, 2.3, -1.3);


// ---------- the bottle ----------
const bottle = new THREE.Group();
bottle.position.y = PLINTH_H;
scene.add(bottle);

function latheFromCurve(pts, samples, seg, phiStart = 0, phiLength = Math.PI * 2) {
  const curve = new THREE.CatmullRomCurve3(pts.map(([r, y]) => V(r, y, 0)), false, 'catmullrom', 0.5);
  const p = curve.getSpacedPoints(samples).map((v) => new THREE.Vector2(Math.max(0, v.x), v.y));
  return new THREE.LatheGeometry(p, seg, phiStart, phiLength);
}
const bodyProfile = [[0.0, 0.0], [0.28, 0.0], [0.38, 0.012], [0.43, 0.05], [0.452, 0.14], [0.455, 0.4], [0.455, 1.3],
  [0.45, 1.46], [0.425, 1.6], [0.37, 1.71], [0.29, 1.79], [0.21, 1.85], [0.17, 1.9]];
const bodyMat = new THREE.MeshPhysicalMaterial({
  color: 0xffffff, transmission: 1, roughness: 0.1, ior: 1.42, thickness: 0.6,
  attenuationColor: new THREE.Color(0xd07a1c), attenuationDistance: 0.42,
  clearcoat: 1, clearcoatRoughness: 0.04, specularIntensity: 1, envMapIntensity: 1.2,
});
const body = new THREE.Mesh(latheFromCurve(bodyProfile, 220, 160), bodyMat);
body.scale.z = OVAL; body.castShadow = true; bottle.add(body);

// label (screen-printed paper with glossy ink and gold foil)
async function labelMaps() {
  await document.fonts.load('600 100px Cormorant');
  await document.fonts.load('500 40px Cormorant');
  await document.fonts.load('500 30px Inter');
  const w = 1024, h = 1180;
  const mk = () => { const c = document.createElement('canvas'); c.width = w; c.height = h; return c; };
  const col = mk(), orm = mk(); const cc = col.getContext('2d'), oc = orm.getContext('2d');
  // paper
  cc.fillStyle = '#f3ebdf'; cc.fillRect(0, 0, w, h);
  const n = valueNoise(5); const img = cc.getImageData(0, 0, w, h);
  for (let j = 0; j < h; j++) for (let i = 0; i < w; i++) { const k = (i + j * w) * 4, v = (n(i / 1.3, j / 1.3) - 0.5) * 10; img.data[k] += v; img.data[k + 1] += v; img.data[k + 2] += v; }
  cc.putImageData(img, 0, 0);
  oc.fillStyle = 'rgb(0,170,0)'; oc.fillRect(0, 0, w, h); // G = roughness 0.67, B = metalness 0
  const both = (fn) => { fn(cc, 'ink'); fn(oc, 'orm'); };
  const ink = (ctx, kind, c, gloss = 85) => { ctx.fillStyle = kind === 'ink' ? c : `rgb(0,${gloss},0)`; ctx.strokeStyle = ctx.fillStyle; };
  const foil = (ctx, kind) => { ctx.fillStyle = kind === 'ink' ? '#c79a58' : 'rgb(0,55,255)'; ctx.strokeStyle = ctx.fillStyle; };
  const spaced = (ctx, text, x, y, sp) => { // centered letter-spaced text
    const chars = [...text]; const ws = chars.map((ch) => ctx.measureText(ch).width);
    const total = ws.reduce((a, b) => a + b, 0) + sp * (chars.length - 1); let cx = x - total / 2;
    chars.forEach((ch, i) => { ctx.fillText(ch, cx, y); cx += ws[i] + sp; });
  };
  both((ctx, kind) => {
    ctx.textBaseline = 'alphabetic';
    foil(ctx, kind); ctx.lineWidth = 3; ctx.strokeRect(46, 46, w - 92, h - 92); ctx.lineWidth = 1.2; ctx.strokeRect(60, 60, w - 120, h - 120);
    ink(ctx, kind, '#5c4a38', 120); ctx.font = '500 30px Inter'; spaced(ctx, 'Nº 01 · PIELĘGNACJA CODZIENNA', w / 2, 190, 6);
    ink(ctx, kind, '#1d1814'); ctx.font = '600 215px Cormorant'; spaced(ctx, 'MURFY', w / 2, 505, 14);
    foil(ctx, kind); ctx.fillRect(w / 2 - 150, 600, 300, 4);
    ctx.beginPath(); ctx.arc(w / 2, 602, 9, 0, Math.PI * 2); ctx.fill();
    ink(ctx, kind, '#2b231c', 100); ctx.font = '500 40px Inter'; spaced(ctx, 'SZAMPON DO WŁOSÓW', w / 2, 715, 14);
    ink(ctx, kind, '#4a3c2f', 110); ctx.font = '500 52px Cormorant'; spaced(ctx, 'lekkość · połysk · objętość', w / 2, 810, 2);
    ink(ctx, kind, '#4a3c2f', 110); ctx.font = '500 36px Cormorant'; spaced(ctx, 'z proteinami ryżu i olejkiem arganowym', w / 2, 880, 1);
    ink(ctx, kind, '#2b231c', 100); ctx.font = '500 34px Inter'; spaced(ctx, '250 ml', w / 2, 1060, 6);
  });
  const map = new THREE.CanvasTexture(col); map.colorSpace = THREE.SRGBColorSpace; map.anisotropy = 16;
  const ormT = new THREE.CanvasTexture(orm); ormT.anisotropy = 16;
  return { map, ormT };
}
const labelInfo = await labelMaps();
const labelProfile = []; for (let y = 0.4; y <= 1.33; y += 0.93 / 20) labelProfile.push(new THREE.Vector2(0.4585, y));
const label = new THREE.Mesh(new THREE.LatheGeometry(labelProfile, 96, -0.98, 1.96),
  new THREE.MeshPhysicalMaterial({ map: labelInfo.map, roughnessMap: labelInfo.ormT, metalnessMap: labelInfo.ormT, roughness: 1, metalness: 1, clearcoat: 0.2, clearcoatRoughness: 0.3, transparent: true }));
label.scale.z = OVAL; label.castShadow = true; bottle.add(label);

// cap: matte soft-touch black flip-top with brushed gold ring
const capProfile = [[0.0, 1.835], [0.25, 1.835], [0.302, 1.84], [0.307, 1.86], [0.305, 2.0], [0.298, 2.2], [0.285, 2.27], [0.255, 2.302], [0.18, 2.312], [0.0, 2.314]];
const capMat = new THREE.MeshPhysicalMaterial({ color: 0x1a1613, roughness: 0.34, sheen: 0.5, sheenRoughness: 0.6, sheenColor: 0x6a5f55, clearcoat: 0.08, clearcoatRoughness: 0.6 });
const cap = new THREE.Mesh(latheFromCurve(capProfile, 160, 160), capMat);
cap.scale.z = OVAL; cap.castShadow = true; bottle.add(cap);
const goldMat = new THREE.MeshPhysicalMaterial({ color: 0xd6ad78, metalness: 1, roughness: 0.3, anisotropy: 0.6, clearcoat: 0.3 });
const ring = new THREE.Mesh(new THREE.TorusGeometry(0.309, 0.011, 24, 200), goldMat);
ring.rotation.x = Math.PI / 2; ring.position.y = 1.862; ring.scale.y = OVAL; ring.castShadow = true; bottle.add(ring);
const seam = new THREE.Mesh(new THREE.TorusGeometry(0.2995, 0.0035, 12, 200), new THREE.MeshStandardMaterial({ color: 0x050403, roughness: 0.8 }));
seam.rotation.x = Math.PI / 2; seam.position.y = 2.205; seam.scale.y = OVAL; bottle.add(seam);
// embossed monogram on cap top
const mono = new THREE.Mesh(new THREE.TorusGeometry(0.11, 0.006, 12, 120), goldMat);
mono.rotation.x = Math.PI / 2; mono.position.y = 2.314; mono.scale.y = OVAL; bottle.add(mono);

// amber caustic glow where the key light passes through the bottle
const caustic = (() => {
  const c = document.createElement('canvas'); c.width = c.height = 256; const x = c.getContext('2d');
  const g = x.createRadialGradient(128, 128, 0, 128, 128, 128);
  g.addColorStop(0, 'rgba(255,190,110,1)'); g.addColorStop(0.35, 'rgba(255,150,60,0.55)'); g.addColorStop(1, 'rgba(255,120,30,0)');
  x.fillStyle = g; x.fillRect(0, 0, 256, 256);
  const m = new THREE.Mesh(new THREE.PlaneGeometry(1, 1), new THREE.MeshBasicMaterial({ map: new THREE.CanvasTexture(c), transparent: true, blending: THREE.AdditiveBlending, depthWrite: false, opacity: 0.5, toneMapped: true }));
  m.rotation.x = -Math.PI / 2; m.position.y = PLINTH_H + 0.002; scene.add(m); return m;
})();

// contact shadows: soft AO decals where objects meet a surface
const aoTex = (() => {
  const c = document.createElement('canvas'); c.width = c.height = 256; const x = c.getContext('2d');
  const g = x.createRadialGradient(128, 128, 0, 128, 128, 128);
  g.addColorStop(0, 'rgba(0,0,0,0.85)'); g.addColorStop(0.45, 'rgba(0,0,0,0.55)'); g.addColorStop(0.75, 'rgba(0,0,0,0.15)'); g.addColorStop(1, 'rgba(0,0,0,0)');
  x.fillStyle = g; x.fillRect(0, 0, 256, 256); return new THREE.CanvasTexture(c);
})();
function ao(x, y, z, sx, sz, op) {
  const m = new THREE.Mesh(new THREE.PlaneGeometry(1, 1), new THREE.MeshBasicMaterial({ map: aoTex, transparent: true, depthWrite: false, opacity: op, color: 0x2a1d10 }));
  m.rotation.x = -Math.PI / 2; m.position.set(x, y, z); m.scale.set(sx, sz, 1); m.renderOrder = 1; scene.add(m); return m;
}
ao(0, PLINTH_H + 0.003, 0, 1.15, 0.78, 0.75);
ao(0, 0.003, 0, 3.0, 3.0, 0.8);
ao(-2.5, 0.003, -2.4, 2.1, 2.1, 0.8);
ao(2.3, 0.003, -1.3, 2.6, 2.6, 0.8);

// ---------- shampoo gel ribbon (pearlescent) ----------
class Helix extends THREE.Curve {
  getPoint(s, out = new THREE.Vector3()) {
    const a = -1.4 + s * Math.PI * 2 * 1.55;
    const rr = 0.78 + 0.08 * Math.sin(s * 9.0);
    return out.set(Math.sin(a) * rr, PLINTH_H + 0.12 + s * 2.05 + 0.05 * Math.sin(s * 13), Math.cos(a) * rr * 0.78);
  }
}
const helix = new Helix();
const RIB_SEG = 600, RIB_RAD = 24;
const ribbonGeo = new THREE.TubeGeometry(helix, RIB_SEG, 1, RIB_RAD, false);
{ // flatten into a ribbon and taper the tail
  const p = ribbonGeo.attributes.position, frames = helix.computeFrenetFrames(RIB_SEG, false);
  for (let i = 0; i <= RIB_SEG; i++) {
    const s = i / RIB_SEG, c = helix.getPointAt(s), N = frames.normals[i], B = frames.binormals[i];
    const taper = smooth(0, 0.12, s) * (0.85 + 0.15 * Math.sin(s * 20));
    for (let j = 0; j <= RIB_RAD; j++) {
      const v = (j / RIB_RAD) * Math.PI * 2, cs = -Math.cos(v), sn = Math.sin(v);
      const k = i * (RIB_RAD + 1) + j;
      const a = 0.052 * taper, b = 0.024 * taper + 0.004;
      p.setXYZ(k, c.x + a * cs * N.x + b * sn * B.x, c.y + a * cs * N.y + b * sn * B.y, c.z + a * cs * N.z + b * sn * B.z);
    }
  }
  ribbonGeo.computeVertexNormals();
}
const gelMat = new THREE.MeshPhysicalMaterial({ color: 0xeed2a8, roughness: 0.12, clearcoat: 1, clearcoatRoughness: 0.02, iridescence: 0.55, iridescenceIOR: 1.35, iridescenceThicknessRange: [180, 520], sheen: 0.6, sheenColor: 0xffe1b8, specularIntensity: 1 });
const ribbon = new THREE.Mesh(ribbonGeo, gelMat); ribbon.castShadow = true; scene.add(ribbon);
const ribbonHead = new THREE.Mesh(new THREE.SphereGeometry(1, 48, 32), gelMat); ribbonHead.castShadow = true; scene.add(ribbonHead);

// ---------- soap bubbles (thin-film interference, reflection only) ----------
const bubbleMat = new THREE.MeshPhysicalMaterial({ color: 0x000000, roughness: 0.02, metalness: 0, ior: 1.33, iridescence: 1, iridescenceIOR: 1.33, iridescenceThicknessRange: [260, 820], specularIntensity: 1, envMapIntensity: 2.6, transparent: true, blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide });
const bubbleGeo = new THREE.SphereGeometry(1, 64, 40);
const bubbles = [];
{
  const r = rng(42);
  for (let i = 0; i < 34; i++) {
    let x, z; do { x = lerp(-2.6, 1.0, r()); z = lerp(-2.4, 1.6, r()); } while (Math.hypot(x / 0.95, z / 0.75) < 1 || (Math.abs(x) < 1.15 && z > -1.4));
    const b = new THREE.Mesh(bubbleGeo, bubbleMat);
    b.userData = { x, z, y0: lerp(0.15, 1.4, r()), birth: lerp(5.0, 18.5, r()) - 1.5, life: lerp(4.5, 8, r()), rad: lerp(0.035, 0.15, r()) ** 1, sp: lerp(0.12, 0.26, r()), ph: r() * 6.28, wob: lerp(0.04, 0.12, r()) };
    b.renderOrder = 2; scene.add(b); bubbles.push(b);
  }
}

// ---------- lights ----------
scene.add(new THREE.HemisphereLight(0xfff4e6, 0x8a7560, 0.35));
const goboCanvas = document.createElement('canvas'); goboCanvas.width = goboCanvas.height = 768;
const goboSharp = document.createElement('canvas'); goboSharp.width = goboSharp.height = 768;
const goboTex = new THREE.CanvasTexture(goboCanvas); goboTex.colorSpace = THREE.SRGBColorSpace;
const leaves = (() => { const r = rng(9); const L = []; for (let i = 0; i < 70; i++) L.push({ x: r(), y: r(), a: r() * 6.28, s: lerp(0.35, 1, r()), ph: r() * 6.28 }); return L; })();
function drawGobo(t, strength) {
  let g = goboCanvas.getContext('2d'); const S = goboCanvas.width;
  const out = g; g = goboSharp.getContext('2d');
  g.fillStyle = '#fff'; g.fillRect(0, 0, S, S);
  const sh = Math.round(255 * (1 - 0.9 * strength));
  g.fillStyle = `rgb(${sh},${sh},${Math.min(255, sh + 6)})`;
  for (const l of leaves) {
    const sway = Math.sin(t * 0.9 + l.ph) * 0.02 + Math.sin(t * 2.3 + l.ph * 2) * 0.006;
    const x = (l.x + sway) * S, y = (l.y + sway * 0.6) * S;
    g.save(); g.translate(x, y); g.rotate(l.a + sway * 4);
    g.beginPath(); g.ellipse(0, 0, 58 * l.s, 20 * l.s, 0, 0, Math.PI * 2); g.fill();
    g.fillRect(-90 * l.s, -2, 60 * l.s, 4);
    g.restore();
  }
  // soft window-frame bar
  g.fillRect(S * 0.66, 0, S * 0.035, S);
  out.filter = 'blur(5px)'; out.drawImage(goboSharp, 0, 0); out.filter = 'none'; // one blur per frame
  goboTex.needsUpdate = true;
}
const sun = new THREE.SpotLight(0xffe1bb, 4.5, 0, 0.5, 0.45, 0);
sun.position.set(-4.6, 6.4, -5.6); sun.target.position.set(0.2, 0.6, 0.4);
sun.map = goboTex; sun.castShadow = true;
sun.shadow.mapSize.set(2048, 2048); sun.shadow.bias = -0.00015; sun.shadow.normalBias = 0.015; sun.shadow.radius = 5; sun.shadow.camera.near = 4; sun.shadow.camera.far = 20;
sun.shadow.intensity = 0.86;
scene.add(sun, sun.target);

const rimL = new THREE.RectAreaLight(0xfff1e2, 7, 0.7, 4.0); scene.add(rimL);
const rimR = new THREE.RectAreaLight(0xffffff, 6, 0.7, 4.0); scene.add(rimR);
const sweep = new THREE.RectAreaLight(0xffffff, 0, 0.35, 3.0); scene.add(sweep);
const frontFill = new THREE.RectAreaLight(0xfff0e0, 1.6, 3.0, 3.0); scene.add(frontFill);
frontFill.position.set(1.2, 2.6, 5.5); frontFill.lookAt(0, 1.4, 0);
function aim(l, p, target) { l.position.copy(p); l.lookAt(target); }

// ---------- post ----------
const rt = new THREE.WebGLRenderTarget(W, H, { type: THREE.HalfFloatType, samples: 4 });
const composer = new EffectComposer(renderer, rt);
composer.setPixelRatio(1); composer.setSize(W, H);
composer.addPass(new RenderPass(scene, camera));
const bokeh = new BokehPass(scene, camera, { focus: 5, aperture: 0.004, maxblur: 0.008 });
composer.addPass(bokeh);
const bloom = new UnrealBloomPass(new THREE.Vector2(W, H), 0.1, 0.5, 0.97);
composer.addPass(bloom);
composer.addPass(new OutputPass());
const grade = new ShaderPass({
  uniforms: { tDiffuse: { value: null }, uTime: { value: 0 }, uGrain: { value: 0.028 }, uVig: { value: 0.32 } },
  vertexShader: 'varying vec2 vUv; void main(){ vUv=uv; gl_Position=projectionMatrix*modelViewMatrix*vec4(position,1.0); }',
  fragmentShader: `uniform sampler2D tDiffuse; uniform float uTime, uGrain, uVig; varying vec2 vUv;
    float h(vec2 p){ p=fract(p*vec2(443.897,441.423)); p+=dot(p,p.yx+19.19); return fract((p.x+p.y)*p.x); }
    void main(){
      vec2 d = vUv-0.5; float r = dot(d*vec2(1.0,0.7),d*vec2(1.0,0.7));
      vec3 c;
      c.r = texture2D(tDiffuse, vUv - d*0.0012).r; c.g = texture2D(tDiffuse, vUv).g; c.b = texture2D(tDiffuse, vUv + d*0.0012).b;
      c *= 1.0 - uVig*smoothstep(0.05, 0.45, r);
      float g = h(vUv*vec2(1920.0,1080.0) + fract(uTime*7.31)*100.0) - 0.5;
      c += g*uGrain*(1.0-0.6*dot(c,vec3(0.333)));
      // gentle warm grade: lift shadows slightly, warm highlights
      c = mix(c, c*vec3(1.03,1.0,0.96), 0.6);
      gl_FragColor = vec4(c,1.0);
    }`,
});
composer.addPass(grade);

// ---------- direction: shots ----------
// S1 0–4 macro cap glint · S2 4–8 gel ribbon spirals up · S3 8–12 wide, bubbles + dappled sun
// S4 12–16 hero orbit · S5 16–20 packshot + super
const SHOTS = [
  { t0: 0, t1: 4, cam(u) {
      const e = easeInOut(u);
      const p = V(lerp(-0.85, 0.75, e), PLINTH_H + lerp(2.55, 2.35, e), lerp(1.55, 1.6, e));
      return { pos: p, tgt: V(lerp(-0.06, 0.06, u), PLINTH_H + 1.95, 0), fov: 30, aperture: 0.022, maxblur: 0.012 };
    } },
  { t0: 4, t1: 8, cam(u) {
      const p = vlerp(V(0.55, PLINTH_H + 0.8, 3.1), V(-0.2, PLINTH_H + 1.0, 2.55), easeInOut(u));
      return { pos: p, tgt: V(0, PLINTH_H + lerp(0.75, 1.05, u), 0), fov: 34, aperture: 0.012, maxblur: 0.01 };
    } },
  { t0: 8, t1: 12, cam(u) {
      const e = easeInOut(u);
      return { pos: V(lerp(2.3, 1.4, e), lerp(0.55, 1.55, e), lerp(5.6, 5.0, e)), tgt: V(-0.1, lerp(1.25, 1.6, e), 0), fov: 36, aperture: 0.006, maxblur: 0.009 };
    } },
  { t0: 12, t1: 16, cam(u) {
      const a = lerp(-0.95, 0.55, easeInOut(u)), R = lerp(4.3, 3.9, u);
      return { pos: V(Math.sin(a) * R, PLINTH_H + lerp(1.7, 1.3, u), Math.cos(a) * R), tgt: V(0, PLINTH_H + 1.12, 0), fov: 34, aperture: 0.009, maxblur: 0.01 };
    } },
  { t0: 16, t1: 20.01, cam(u) {
      const e = easeOut(u);
      return { pos: V(1.3, PLINTH_H + 1.12, lerp(6.6, 6.05, e)), tgt: V(1.3, PLINTH_H + 1.02, 0), fov: 30, aperture: 0.003, maxblur: 0.006 };
    } },
];

const card = document.getElementById('card'), fade = document.getElementById('fade');
const [cBrand, cLine, cKind, cTag] = card.children;

function renderAt(t) {
  const si = SHOTS.findIndex((s) => t >= s.t0 && t < s.t1);
  const S = SHOTS[si], u = (t - S.t0) / (S.t1 - S.t0);
  const c = S.cam(u);
  camera.position.copy(c.pos); camera.fov = c.fov; camera.updateProjectionMatrix(); camera.lookAt(c.tgt);
  bokeh.uniforms.focus.value = c.pos.distanceTo(c.focusAt || V(c.tgt.x, c.tgt.y, si === 4 ? 0 : c.tgt.z));
  if (si === 4) bokeh.uniforms.focus.value = c.pos.distanceTo(V(0, c.tgt.y, 0.25));
  bokeh.uniforms.aperture.value = c.aperture; bokeh.uniforms.maxblur.value = c.maxblur;

  // bottle pose per shot
  const yaw = [lerp(-0.35, 0.25, u), lerp(0.5, 0.05, easeOut(u)), lerp(-0.25, -0.1, u), lerp(0.2, -0.15, u), lerp(-0.12, -0.04, easeOut(u))][si];
  bottle.rotation.y = yaw;

  // light
  const goboStrength = [0.35, 0.55, 1, 1, 0.85][si];
  drawGobo(t, goboStrength);
  sun.intensity = [3.2, 4.2, 4.8, 4.8, 4.5][si];
  sun.position.set(-4.6 + Math.sin(t * 0.15) * 0.15, 6.4, -5.6);
  aim(rimL, V(-2.3, PLINTH_H + 1.6, -1.6), V(0, PLINTH_H + 1.2, 0));
  aim(rimR, V(2.2, PLINTH_H + 1.7, -1.9), V(0, PLINTH_H + 1.2, 0));
  // the S1 light sweep: a narrow strip travels across the cap and shoulder
  if (si === 0) { const sx = lerp(-2.6, 2.6, easeInOut(smooth(0.25, 0.95, u))); aim(sweep, V(sx, PLINTH_H + 2.6, 1.6), V(0, PLINTH_H + 2.0, 0)); sweep.intensity = 14; }
  else if (si === 3) { const sx = lerp(2.4, -2.4, easeInOut(u)); aim(sweep, V(sx, PLINTH_H + 1.4, 2.4), V(0, PLINTH_H + 1.1, 0)); sweep.intensity = 5; }
  else sweep.intensity = 0;
  scene.environmentRotation.y = [lerp(-0.6, 0.5, u), 0.2, 0, lerp(0.3, -0.3, u), 0.05][si];

  // caustic under the bottle, along the shadow direction
  const dir = new THREE.Vector2(4.6, 5.6).normalize();
  const ca = Math.atan2(dir.y, dir.x);
  caustic.position.set(dir.x * 0.62, PLINTH_H + 0.002, dir.y * 0.62);
  caustic.rotation.set(-Math.PI / 2, 0, -ca); caustic.scale.set(0.95, 0.42, 1);
  caustic.material.opacity = 0.9;

  // gel ribbon: only in S2
  const showRibbon = si === 1;
  ribbon.visible = ribbonHead.visible = showRibbon;
  if (showRibbon) {
    const head = lerp(0.03, 1, easeOut(smooth(0.0, 0.92, u)));
    const n = Math.max(1, Math.floor(head * RIB_SEG));
    ribbon.geometry.setDrawRange(0, n * RIB_RAD * 6);
    ribbon.rotation.y = ribbonHead.rotation.y = 0;
    const hp = helix.getPointAt(n / RIB_SEG); ribbonHead.position.copy(hp); ribbonHead.scale.setScalar(0.03);
    ribbonHead.scale.set(0.05, 0.03, 0.05);
  }

  // bubbles: rise, wobble, pop. Not in macro (S1); fewer in S2
  for (const b of bubbles) {
    const d = b.userData; const age = t - d.birth;
    let s = 0;
    if (si >= 1 && age > 0 && age < d.life) s = d.rad * smooth(0, 0.5, age) * (1 - smooth(d.life - 0.12, d.life, age));
    if (si === 1 && d.birth > 3.5) s = 0;
    b.visible = s > 0.001;
    if (!b.visible) continue;
    b.position.set(d.x + Math.sin(t * 1.3 + d.ph) * d.wob, d.y0 + age * d.sp, d.z + Math.cos(t * 1.1 + d.ph) * d.wob);
    b.scale.set(s * (1 + 0.03 * Math.sin(t * 7 + d.ph)), s * (1 - 0.03 * Math.sin(t * 7 + d.ph)), s);
  }

  // super / end card
  const st = t - 16;
  const show = (el, a, b, rise = 18) => { const k = easeOut(smooth(a, b, st)); el.style.opacity = k; el.style.transform = `translateY(${(1 - k) * rise}px)`; };
  if (si === 4) {
    show(cBrand, 0.35, 1.4, 26); cLine.style.opacity = 1; cLine.style.width = `${easeInOut(smooth(0.9, 1.9, st)) * 240}px`;
    show(cKind, 1.3, 2.1); show(cTag, 1.8, 2.8);
  } else { for (const el of card.children) el.style.opacity = 0; cLine.style.width = '0px'; }
  fade.style.opacity = 1 - smooth(0, 0.9, t);

  grade.uniforms.uTime.value = t;
  composer.render();
}

window.renderAt = renderAt;
window.__ad = { composer, bokeh, bloom, renderer, scene, rt, sun, bodyMat, bubbles };
window.ready = true;
