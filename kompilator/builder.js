// Wykrywanie typu projektu, uruchamianie kompilacji i szukanie gotowego .jar
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');

const BUILD_FILES = ['build.gradle', 'build.gradle.kts', 'pom.xml'];
const SKIP_DIRS = new Set(['.git', '.gradle', 'build', 'target', 'node_modules', 'run', '__MACOSX']);

// Znajduje najpłytszy katalog z plikiem budowania (zip często ma folder w środku)
function findProjectRoot(dir, depth = 0) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  if (entries.some((e) => e.isFile() && BUILD_FILES.includes(e.name))) return dir;
  if (depth >= 4) return null;
  for (const e of entries) {
    if (!e.isDirectory() || SKIP_DIRS.has(e.name)) continue;
    const found = findProjectRoot(path.join(dir, e.name), depth + 1);
    if (found) return found;
  }
  return null;
}

function readIf(file) {
  try { return fs.readFileSync(file, 'utf8'); } catch { return ''; }
}

// Rozpoznaje loader i wersję Javy na podstawie plików projektu
function detectProject(root) {
  const gradle = readIf(path.join(root, 'build.gradle')) + readIf(path.join(root, 'build.gradle.kts'));
  const props = readIf(path.join(root, 'gradle.properties'));
  const pom = readIf(path.join(root, 'pom.xml'));
  const all = gradle + '\n' + props + '\n' + pom;

  let tool = 'gradle';
  if (!gradle && pom) tool = 'maven';

  let loader = 'Java';
  if (/fabric-loom|fabric_version|fabric-api/i.test(all)) loader = 'Fabric';
  else if (/quilt-loom|org\.quiltmc/i.test(all)) loader = 'Quilt';
  else if (/net\.neoforged/i.test(all)) loader = 'NeoForge';
  else if (/net\.minecraftforge|ForgeGradle/i.test(all)) loader = 'Forge';
  else if (/papermc|io\.papermc|paper-api/i.test(all)) loader = 'Paper';
  else if (/spigot|bukkit/i.test(all)) loader = 'Spigot/Bukkit';
  else if (/velocity/i.test(all)) loader = 'Velocity';

  let mc = (all.match(/minecraft_version\s*=\s*([\w.\-]+)/) || [])[1] || null;

  let java = null;
  const m =
    all.match(/JavaLanguageVersion\.of\(\s*(\d+)\s*\)/) ||
    all.match(/JavaVersion\.VERSION_(?:1_)?(\d+)/) ||
    all.match(/options\.release\s*(?:=|\.set\()\s*(\d+)/) ||
    all.match(/<maven\.compiler\.(?:release|target)>(?:1\.)?(\d+)</) ||
    all.match(/<release>(\d+)<\/release>/);
  if (m) java = parseInt(m[1], 10);
  if (!java && mc) java = javaForMinecraft(mc);

  return {
    tool,
    loader,
    minecraft: mc,
    java: java || 21,
    hasWrapper: fs.existsSync(path.join(root, 'gradlew')),
  };
}

// MC 1.20.5+ → Java 21, 1.18–1.20.4 → 17, 1.17 → 16 (17 działa), starsze → 8
function javaForMinecraft(v) {
  const [, minor = 0, patch = 0] = v.split('.').map((x) => parseInt(x, 10) || 0);
  if (minor > 20 || (minor === 20 && patch >= 5)) return 21;
  if (minor >= 17) return 17;
  return 8;
}

function normalizeJava(v) {
  const n = parseInt(v, 10);
  if (n <= 8) return 8;
  if (n <= 17) return 17;
  return 21;
}

// Buduje polecenie: lokalnie albo w izolowanym kontenerze Dockera
function buildCommand(root, info, cfg) {
  const java = normalizeJava(info.java);
  let inner;
  if (info.tool === 'maven') {
    inner = ['mvn', '-B', '-q', '-DskipTests', 'package'];
  } else if (info.hasWrapper) {
    inner = ['sh', './gradlew', 'build', '-x', 'test', '--no-daemon', '--console=plain'];
  } else {
    inner = ['gradle', 'build', '-x', 'test', '--no-daemon', '--console=plain'];
  }

  if (cfg.mode === 'docker') {
    const image = info.tool === 'maven'
      ? `maven:3.9-eclipse-temurin-${java}`
      : `gradle:8.14-jdk${java}`;
    return {
      cmd: 'docker',
      args: [
        'run', '--rm',
        '--name', `kompilator-${cfg.jobId}`,
        `--memory=${cfg.memory}`, `--cpus=${cfg.cpus}`, '--pids-limit=512',
        '--security-opt=no-new-privileges', '--cap-drop=ALL',
        '-v', `${root}:/work`,
        '-v', `kompilator-cache-gradle:/home/gradle/.gradle`,
        '-v', `kompilator-cache-m2:/root/.m2`,
        '-w', '/work',
        '-e', 'GRADLE_OPTS=-Dorg.gradle.daemon=false',
        image, ...inner,
      ],
      env: process.env,
      java,
    };
  }

  // Tryb lokalny: JAVA_8_HOME / JAVA_17_HOME / JAVA_21_HOME wybierają JDK
  const env = { ...process.env };
  const home = process.env[`JAVA_${java}_HOME`];
  if (home) {
    env.JAVA_HOME = home;
    env.PATH = `${path.join(home, 'bin')}${path.delimiter}${env.PATH}`;
  }
  return { cmd: inner[0], args: inner.slice(1), env, java };
}

function run(cmd, args, opts, onLine, timeoutMs) {
  return new Promise((resolve) => {
    const child = spawn(cmd, args, { ...opts, detached: process.platform !== 'win32' });
    let buf = '';
    const feed = (chunk) => {
      buf += chunk.toString();
      const lines = buf.split(/\r?\n/);
      buf = lines.pop();
      lines.forEach(onLine);
    };
    child.stdout.on('data', feed);
    child.stderr.on('data', feed);
    const timer = setTimeout(() => {
      onLine(`[kompilator] Przekroczono limit czasu (${Math.round(timeoutMs / 60000)} min) – przerywam.`);
      try { process.kill(-child.pid, 'SIGKILL'); } catch { child.kill('SIGKILL'); }
    }, timeoutMs);
    child.on('error', (err) => {
      clearTimeout(timer);
      onLine(`[kompilator] Nie udało się uruchomić "${cmd}": ${err.message}`);
      resolve(-1);
    });
    child.on('close', (code) => {
      clearTimeout(timer);
      if (buf) onLine(buf);
      resolve(code);
    });
  });
}

// Wybiera właściwy jar: pomija -sources/-javadoc/-dev, preferuje największy
function findJar(root, tool) {
  const dirs = tool === 'maven' ? ['target'] : ['build/libs'];
  // Projekty wielomodułowe (np. Architectury: fabric/build/libs, forge/build/libs)
  for (const sub of fs.readdirSync(root, { withFileTypes: true })) {
    if (sub.isDirectory() && !SKIP_DIRS.has(sub.name)) {
      dirs.push(path.join(sub.name, tool === 'maven' ? 'target' : 'build/libs'));
    }
  }
  const jars = [];
  for (const d of dirs) {
    const full = path.join(root, d);
    if (!fs.existsSync(full)) continue;
    for (const f of fs.readdirSync(full)) {
      if (!f.endsWith('.jar')) continue;
      if (/-(sources|javadoc|dev|dev-shadow|plain|original|all-dev)\.jar$/i.test(f) || f.startsWith('original-')) continue;
      const p = path.join(full, f);
      jars.push({ path: p, name: f, size: fs.statSync(p).size, rel: path.relative(root, p) });
    }
  }
  jars.sort((a, b) => b.size - a.size);
  return jars;
}

module.exports = { findProjectRoot, detectProject, buildCommand, run, findJar, normalizeJava };
