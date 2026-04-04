#!/usr/bin/env node
// ============================================================
// RefugIA OS — npm postinstall setup
// ============================================================
// Runs automatically after: npm install -g refugia-os
// Sets up Python venv, installs pip deps, pulls Ollama model.
// ============================================================

const { execSync, spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const os = require('os');

const ROOT = path.resolve(__dirname, '..');
const VENV = path.join(ROOT, 'venv');
const IS_WIN = os.platform() === 'win32';

// Colors
const AMBER  = '\x1b[33m';
const GREEN  = '\x1b[32m';
const RED    = '\x1b[31m';
const CYAN   = '\x1b[36m';
const RESET  = '\x1b[0m';

const ok   = (msg) => console.log(`  ${GREEN}[OK]${RESET}  ${msg}`);
const info = (msg) => console.log(`  ${CYAN}[..]${RESET}  ${msg}`);
const warn = (msg) => console.log(`  ${AMBER}[!]${RESET}   ${msg}`);
const fail = (msg) => { console.log(`  ${RED}[✗]${RESET}  ${msg}`); process.exit(1); };

console.log(`\n${AMBER}`);
console.log('  ╔══════════════════════════════════════╗');
console.log('  ║   RefugIA OS — Setup                 ║');
console.log('  ╚══════════════════════════════════════╝');
console.log(`${RESET}`);

// ============================================================
//  Step 1: Find Python 3.10+
// ============================================================
info('Looking for Python 3.10+...');

const pythonCmds = IS_WIN
  ? ['python', 'python3', 'py']
  : ['python3', 'python'];

let pythonCmd = null;

for (const cmd of pythonCmds) {
  try {
    const result = spawnSync(cmd, ['--version'], { encoding: 'utf8' });
    if (result.status === 0) {
      const match = result.stdout.match(/Python (\d+)\.(\d+)/);
      if (match) {
        const [, major, minor] = match.map(Number);
        if (major >= 3 && minor >= 10) {
          pythonCmd = cmd;
          ok(`Python found: ${result.stdout.trim()}`);
          break;
        }
      }
    }
  } catch (_) {}
}

if (!pythonCmd) {
  console.log(`
  ${RED}Python 3.10+ not found.${RESET}

  Install it for your OS:
    macOS:   brew install python  or  https://www.python.org/downloads/
    Linux:   sudo apt install python3 python3-venv  (Ubuntu/Debian)
             sudo dnf install python3              (Fedora)
    Windows: https://www.python.org/downloads/  (check "Add to PATH")

  Then run: npm install -g refugia-os
`);
  process.exit(1);
}

// ============================================================
//  Step 2: Create virtual environment
// ============================================================
if (!fs.existsSync(VENV)) {
  info('Creating Python virtual environment...');
  const result = spawnSync(pythonCmd, ['-m', 'venv', VENV], { stdio: 'inherit' });
  if (result.status !== 0) fail('Failed to create virtual environment.');
  ok('Virtual environment created');
} else {
  ok('Virtual environment already exists');
}

// ============================================================
//  Step 3: Install Python dependencies
// ============================================================
const pipCmd = IS_WIN
  ? path.join(VENV, 'Scripts', 'pip.exe')
  : path.join(VENV, 'bin', 'pip');

const reqFile = path.join(ROOT, 'requirements.txt');

// Upgrade pip first — old pip on Windows can't find pre-built wheels
info('Upgrading pip...');
spawnSync(pipCmd, ['install', '--upgrade', 'pip', '-q'], { stdio: 'inherit', cwd: ROOT });

info('Installing Python dependencies (this may take a few minutes)...');
const pipResult = spawnSync(pipCmd, ['install', '-r', reqFile, '-q', '--prefer-binary'], {
  stdio: 'inherit',
  cwd: ROOT,
});
if (pipResult.status !== 0) fail('pip install failed. Check your internet connection and try again.');
ok('Python dependencies installed');

// ============================================================
//  Step 4: Check Ollama
// ============================================================
info('Checking Ollama...');
const ollamaCheck = spawnSync('ollama', ['--version'], { encoding: 'utf8' });

if (ollamaCheck.status !== 0 || ollamaCheck.error) {
  if (IS_WIN) {
    warn('Ollama not found. Install it from: https://ollama.com/download');
    warn('After installing, run: refugia index && refugia start');
  } else {
    info('Installing Ollama...');
    try {
      execSync('curl -fsSL https://ollama.com/install.sh | sh', { stdio: 'inherit' });
      ok('Ollama installed');
    } catch (_) {
      warn('Could not auto-install Ollama. Install manually: https://ollama.com/download');
    }
  }
} else {
  ok(`Ollama found: ${(ollamaCheck.stdout || '').trim()}`);
}

// ============================================================
//  Done
// ============================================================
console.log(`
${GREEN}  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}
${GREEN}  Setup complete!${RESET}

  Next steps:
    ${AMBER}refugia index${RESET}   — Index the survival manuals (first time only)
    ${AMBER}refugia start${RESET}   — Launch RefugIA OS

  Or if you just installed:
    ${AMBER}refugia doctor${RESET}  — Check everything is ready
${GREEN}  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}
`);
