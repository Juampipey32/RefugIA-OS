#!/usr/bin/env node
// ============================================================
// RefugIA Mobile — Ensamblador de la carpeta www/ (Capacitor)
// ============================================================
// Toma el frontend ÚNICO del proyecto (../../frontend) y lo
// prepara para la app móvil:
//   1. Copia index.html, sw.js, manifest.json, icons/
//   2. Inyecta <script src="refugia-engine.js"> antes de </body>
//      para activar el motor on-device.
//   3. Verifica que el índice RAG embebido exista.
//
// Así el HTML/CSS/JS de la UI nunca se duplica: la web y el
// móvil comparten exactamente la misma interfaz.
// ============================================================

const fs = require('fs');
const path = require('path');

const MOBILE_DIR = path.resolve(__dirname, '..');
const REPO_DIR = path.resolve(MOBILE_DIR, '..');
const FRONTEND_DIR = path.join(REPO_DIR, 'frontend');
const WWW_DIR = path.join(MOBILE_DIR, 'www');
const ASSETS_DIR = path.join(WWW_DIR, 'assets');
const RAG_INDEX = path.join(ASSETS_DIR, 'rag_index.json');

const GREEN = '\x1b[32m', RED = '\x1b[31m', CYAN = '\x1b[36m', AMBER = '\x1b[33m', RESET = '\x1b[0m';
const ok = (m) => console.log(`  ${GREEN}[OK]${RESET} ${m}`);
const info = (m) => console.log(`  ${CYAN}[..]${RESET} ${m}`);
const warn = (m) => console.log(`  ${AMBER}[!]${RESET}  ${m}`);
const fail = (m) => { console.error(`  ${RED}[x]${RESET}  ${m}`); process.exit(1); };

function copyIfExists(src, dest) {
  if (!fs.existsSync(src)) return false;
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  const stat = fs.statSync(src);
  if (stat.isDirectory()) {
    fs.cpSync(src, dest, { recursive: true });
  } else {
    fs.copyFileSync(src, dest);
  }
  return true;
}

console.log(`\n${AMBER}RefugIA Mobile — preparando www/${RESET}\n`);

if (!fs.existsSync(FRONTEND_DIR)) fail(`No se encontró el frontend: ${FRONTEND_DIR}`);
fs.mkdirSync(WWW_DIR, { recursive: true });

// 1) Copiar assets del frontend
for (const item of ['sw.js', 'manifest.json', 'icons']) {
  if (copyIfExists(path.join(FRONTEND_DIR, item), path.join(WWW_DIR, item))) {
    ok(`Copiado ${item}`);
  }
}

// 2) index.html con el motor on-device inyectado
let html = fs.readFileSync(path.join(FRONTEND_DIR, 'index.html'), 'utf8');
const scriptTag = '<script src="refugia-engine.js"></script>';
if (!html.includes('refugia-engine.js')) {
  if (html.includes('</body>')) {
    html = html.replace('</body>', `    ${scriptTag}\n</body>`);
  } else {
    html += `\n${scriptTag}\n`;
  }
  info('Motor on-device inyectado en index.html');
}
fs.writeFileSync(path.join(WWW_DIR, 'index.html'), html);
ok('index.html preparado');

// refugia-engine.js ya vive en www/ (versionado); avisar si falta
if (!fs.existsSync(path.join(WWW_DIR, 'refugia-engine.js'))) {
  fail('Falta www/refugia-engine.js (debería estar versionado en el repo)');
}
ok('refugia-engine.js presente');

// 3) Verificar índice RAG embebido
if (!fs.existsSync(RAG_INDEX)) {
  warn('Falta assets/rag_index.json — generalo con:');
  warn('  npm run export:rag      (o: python src/exportar_rag.py desde la raíz)');
} else {
  const mb = (fs.statSync(RAG_INDEX).size / (1024 * 1024)).toFixed(2);
  ok(`Índice RAG presente (${mb} MB)`);
}

console.log(`\n${GREEN}www/ listo.${RESET} Próximo: npx cap sync\n`);
