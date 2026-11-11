// Kompilator.pl – serwer: przyjmuje .zip ze źródłami moda, kompiluje, oddaje .jar
const fs = require('fs');
const os = require('os');
const path = require('path');
const crypto = require('crypto');
const express = require('express');
const multer = require('multer');
const AdmZip = require('adm-zip');
const B = require('./builder');

const cfg = {
  port: parseInt(process.env.PORT || '3000', 10),
  mode: process.env.BUILD_MODE || 'local', // 'local' albo 'docker' (zalecane publicznie)
  maxUploadMb: parseInt(process.env.MAX_UPLOAD_MB || '50', 10),
  maxUnzippedMb: parseInt(process.env.MAX_UNZIPPED_MB || '300', 10),
  concurrency: parseInt(process.env.CONCURRENCY || '1', 10),
  timeoutMin: parseInt(process.env.BUILD_TIMEOUT_MIN || '15', 10),
  keepMin: parseInt(process.env.KEEP_MIN || '30', 10),
  memory: process.env.DOCKER_MEMORY || '3g',
  cpus: process.env.DOCKER_CPUS || '2',
  workDir: process.env.WORK_DIR || path.join(os.tmpdir(), 'kompilator'),
};
fs.mkdirSync(cfg.workDir, { recursive: true });

const jobs = new Map();
const queue = [];
let running = 0;

function newJob(original, javaChoice) {
  const id = crypto.randomBytes(8).toString('hex');
  const job = {
    id, original, javaChoice,
    status: 'queued', // queued | building | done | failed
    log: [], listeners: new Set(),
    createdAt: Date.now(), info: null, jar: null, error: null,
    dir: path.join(cfg.workDir, id),
  };
  jobs.set(id, job);
  return job;
}

function emit(job, type, data) {
  const msg = `event: ${type}\ndata: ${JSON.stringify(data)}\n\n`;
  for (const res of job.listeners) res.write(msg);
}

function log(job, line) {
  if (job.log.length > 20000) return; // limit pamięci
  line = line.split(path.join(job.dir, 'src') + path.sep).join('').split(job.dir).join('');
  job.log.push(line);
  emit(job, 'log', line);
}

function setStatus(job, status, extra = {}) {
  job.status = status;
  Object.assign(job, extra);
  emit(job, 'status', publicJob(job));
}

function publicJob(job) {
  return {
    id: job.id, status: job.status, original: job.original,
    info: job.info, error: job.error,
    position: job.status === 'queued' ? queue.indexOf(job) + 1 : 0,
    jar: job.jar ? { name: job.jar.name, size: job.jar.size } : null,
  };
}

// Rozpakowanie z ochroną przed zip-slip i "zip bombami"
function safeExtract(zipPath, dest) {
  const zip = new AdmZip(zipPath);
  let total = 0;
  for (const e of zip.getEntries()) {
    total += e.header.size;
    if (total > cfg.maxUnzippedMb * 1024 * 1024) throw new Error(`Po rozpakowaniu archiwum przekracza ${cfg.maxUnzippedMb} MB.`);
    const target = path.resolve(dest, e.entryName);
    if (!target.startsWith(path.resolve(dest) + path.sep)) throw new Error('Archiwum zawiera niedozwolone ścieżki.');
  }
  zip.extractAllTo(dest, true);
}

async function processJob(job) {
  running++;
  setStatus(job, 'building');
  const src = path.join(job.dir, 'src');
  try {
    log(job, `[kompilator] Rozpakowuję ${job.original}…`);
    safeExtract(path.join(job.dir, 'upload.zip'), src);
    fs.rmSync(path.join(job.dir, 'upload.zip'), { force: true });

    const root = B.findProjectRoot(src);
    if (!root) throw new Error('Nie znaleziono build.gradle, build.gradle.kts ani pom.xml. Wrzuć cały projekt moda (z plikami Gradle), a nie tylko pliki .java.');

    const info = B.detectProject(root);
    if (job.javaChoice !== 'auto') info.java = parseInt(job.javaChoice, 10);
    info.java = B.normalizeJava(info.java);
    job.info = info;
    setStatus(job, 'building');
    log(job, `[kompilator] Wykryto: ${info.loader}${info.minecraft ? ' · MC ' + info.minecraft : ''} · ${info.tool === 'maven' ? 'Maven' : 'Gradle'}${info.hasWrapper ? ' (wrapper)' : ''} · Java ${info.java}`);

    if (info.hasWrapper) fs.chmodSync(path.join(root, 'gradlew'), 0o755);

    const c = B.buildCommand(root, info, { ...cfg, jobId: job.id });
    log(job, `[kompilator] $ ${c.cmd} ${c.args.slice(-8).join(' ')}`);
    const code = await B.run(c.cmd, c.args, { cwd: root, env: c.env }, (l) => log(job, l), cfg.timeoutMin * 60000);
    if (code !== 0) throw new Error(`Kompilacja nie powiodła się (kod ${code}). Sprawdź log powyżej.`);

    const jars = B.findJar(root, info.tool);
    if (!jars.length) throw new Error('Kompilacja przeszła, ale nie znalazłem żadnego pliku .jar w build/libs.');
    job.jar = jars[0];
    if (jars.length > 1) log(job, `[kompilator] Znaleziono ${jars.length} pliki .jar, wybieram: ${jars[0].rel}`);
    log(job, `[kompilator] Gotowe! ${jars[0].name} (${(jars[0].size / 1024).toFixed(1)} KB)`);
    setStatus(job, 'done');
  } catch (err) {
    log(job, `[kompilator] BŁĄD: ${err.message}`);
    setStatus(job, 'failed', { error: err.message });
  } finally {
    running--;
    pump();
  }
}

function pump() {
  while (running < cfg.concurrency && queue.length) {
    const job = queue.shift();
    queue.forEach((j) => emit(j, 'status', publicJob(j)));
    processJob(job);
  }
}

// Sprzątanie starych zadań
setInterval(() => {
  const limit = Date.now() - cfg.keepMin * 60000;
  for (const [id, job] of jobs) {
    if (job.createdAt < limit && job.status !== 'building' && job.status !== 'queued') {
      fs.rm(job.dir, { recursive: true, force: true }, () => {});
      jobs.delete(id);
    }
  }
}, 60000).unref();

const app = express();
app.disable('x-powered-by');
app.use(express.static(path.join(__dirname, 'public')));

const upload = multer({
  storage: multer.diskStorage({
    destination: (req, file, cb) => {
      req.job = newJob(file.originalname, 'auto');
      fs.mkdirSync(req.job.dir, { recursive: true });
      cb(null, req.job.dir);
    },
    filename: (req, file, cb) => cb(null, 'upload.zip'),
  }),
  limits: { fileSize: cfg.maxUploadMb * 1024 * 1024, files: 1 },
  fileFilter: (req, file, cb) => {
    if (/\.zip$/i.test(file.originalname)) cb(null, true);
    else cb(new Error('Akceptuję tylko pliki .zip ze źródłami projektu.'));
  },
});

app.post('/api/compile', (req, res) => {
  upload.single('mod')(req, res, (err) => {
    if (err) {
      if (req.job) { jobs.delete(req.job.id); fs.rm(req.job.dir, { recursive: true, force: true }, () => {}); }
      const msg = err.code === 'LIMIT_FILE_SIZE' ? `Plik jest za duży (max ${cfg.maxUploadMb} MB).` : err.message;
      return res.status(400).json({ error: msg });
    }
    if (!req.file || !req.job) return res.status(400).json({ error: 'Nie wybrano pliku.' });
    const java = String(req.body.java || 'auto');
    req.job.javaChoice = ['8', '17', '21'].includes(java) ? java : 'auto';
    queue.push(req.job);
    res.json(publicJob(req.job));
    pump();
  });
});

app.get('/api/jobs/:id', (req, res) => {
  const job = jobs.get(req.params.id);
  if (!job) return res.status(404).json({ error: 'Nie ma takiego zadania (mogło wygasnąć).' });
  res.json(publicJob(job));
});

// Log na żywo (Server-Sent Events)
app.get('/api/jobs/:id/events', (req, res) => {
  const job = jobs.get(req.params.id);
  if (!job) return res.status(404).end();
  res.set({ 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache', Connection: 'keep-alive', 'X-Accel-Buffering': 'no' });
  res.flushHeaders();
  for (const l of job.log) res.write(`event: log\ndata: ${JSON.stringify(l)}\n\n`);
  res.write(`event: status\ndata: ${JSON.stringify(publicJob(job))}\n\n`);
  job.listeners.add(res);
  const ping = setInterval(() => res.write(': ping\n\n'), 20000);
  req.on('close', () => { clearInterval(ping); job.listeners.delete(res); });
});

app.get('/api/jobs/:id/download', (req, res) => {
  const job = jobs.get(req.params.id);
  if (!job || job.status !== 'done' || !job.jar) return res.status(404).json({ error: 'Plik niedostępny.' });
  res.download(job.jar.path, job.jar.name);
});

app.get('/api/jobs/:id/log', (req, res) => {
  const job = jobs.get(req.params.id);
  if (!job) return res.status(404).end();
  res.type('text/plain; charset=utf-8').attachment(`kompilator-${job.id}.log`).send(job.log.join('\n'));
});

app.get('/api/status', (req, res) => {
  res.json({ queue: queue.length, running, mode: cfg.mode, maxUploadMb: cfg.maxUploadMb });
});

app.listen(cfg.port, () => {
  console.log(`Kompilator.pl działa na http://localhost:${cfg.port} (tryb: ${cfg.mode})`);
  if (cfg.mode !== 'docker') console.log('UWAGA: tryb lokalny uruchamia cudzy kod Gradle bez izolacji – publicznie używaj BUILD_MODE=docker.');
});
