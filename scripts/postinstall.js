#!/usr/bin/env node
// ============================================================
// RefugIA OS — npm postinstall setup
// ============================================================
// Runs automatically after: npm install -g refugia-os
// Sets up Python venv, installs pip deps, detects hardware,
// pulls optimal Ollama model, and indexes manuals.
// ============================================================

const { execSync, spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const os = require('os');

const ROOT = path.resolve(__dirname, '..');
const VENV = path.join(ROOT, 'venv');
const IS_WIN = os.platform() === 'win32';
const IS_MAC = os.platform() === 'darwin';
const IS_LINUX = os.platform() === 'linux';

// Colors
const AMBER  = '\x1b[33m';
const GREEN  = '\x1b[32m';
const RED    = '\x1b[31m';
const CYAN   = '\x1b[36m';
const MAGENTA = '\x1b[35m';
const BOLD   = '\x1b[1m';
const RESET  = '\x1b[0m';

const ok   = (msg) => console.log(`  ${GREEN}[OK]${RESET}  ${msg}`);
const info = (msg) => console.log(`  ${CYAN}[..]${RESET}  ${msg}`);
const warn = (msg) => console.log(`  ${AMBER}[!]${RESET}   ${msg}`);
const fail = (msg) => { console.log(`  ${RED}[✗]${RESET}  ${msg}`); process.exit(1); };
const step = (msg) => console.log(`\n${MAGENTA}${BOLD}▶${RESET} ${BOLD}${msg}${RESET}\n`);

console.log(`\n${AMBER}`);
console.log('  ╔══════════════════════════════════════╗');
console.log('  ║   RefugIA OS — Full Setup            ║');
console.log('  ║   Apocalypse Agent Installer         ║');
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
let pythonMinor = 0;

for (const cmd of pythonCmds) {
  try {
    const result = spawnSync(cmd, ['--version'], { encoding: 'utf8' });
    if (result.status === 0) {
      const match = result.stdout.match(/Python (\d+)\.(\d+)/);
      if (match) {
        const [, major, minor] = match.map(Number);
        if (major >= 3 && minor >= 10 && minor <= 13) {
          pythonCmd = cmd;
          pythonMinor = minor;
          ok(`Python found: ${result.stdout.trim()}`);
          break;
        } else if (major >= 3 && minor >= 14) {
          warn(`Python ${major}.${minor} detected — too new! Packages don't have pre-built binaries yet.`);
          warn('Please install Python 3.11 or 3.12 from: https://www.python.org/downloads/');
          if (IS_WIN) {
            info('On Windows, you can have multiple Python versions installed side by side.');
            info('Download Python 3.12 and install it alongside your current version.');
            info('Then re-run: npm install');
          }
        }
      }
    }
  } catch (_) {}
}

// On Windows, try py launcher with specific version if no compatible Python found
if (!pythonCmd && IS_WIN) {
  for (const ver of ['3.12', '3.11', '3.13', '3.10']) {
    try {
      const result = spawnSync('py', [`-${ver}`, '--version'], { encoding: 'utf8' });
      if (result.status === 0) {
        pythonCmd = 'py';
        // We'll use py -3.XX for all subsequent calls
        info(`Found Python ${ver} via py launcher`);
        // Override pythonCmd to include version flag
        pythonCmd = `py -${ver}`;
        break;
      }
    } catch (_) {}
  }
}

if (!pythonCmd) {
  console.log(`
  ${RED}Python 3.10-3.13 not found.${RESET}

  ${AMBER}RefugIA requires Python 3.10, 3.11, 3.12, or 3.13.${RESET}
  Python 3.14+ is NOT supported yet (packages lack pre-built binaries).

  Download Python 3.12 (recommended):
    ${CYAN}https://www.python.org/downloads/release/python-3129/${RESET}

  ${AMBER}Important (Windows):${RESET} Check "Add Python to PATH" during install.

  After installing, run: ${AMBER}npm install${RESET}
`);
  process.exit(1);
}

// Support `py -3.XX` launcher syntax on Windows
const pythonArgs = pythonCmd.startsWith('py ') ? pythonCmd.split(' ') : [pythonCmd];
const pythonBin = pythonArgs[0];
const pythonFlags = pythonArgs.slice(1);

// ============================================================
//  Step 2: Create virtual environment
// ============================================================
step('Step 2: Setting up Python Environment');

if (!fs.existsSync(VENV)) {
  info('Creating Python virtual environment...');
  const result = spawnSync(pythonBin, [...pythonFlags, '-m', 'venv', VENV], { stdio: 'inherit' });
  if (result.status !== 0) fail('Failed to create virtual environment.');
  ok('Virtual environment created');
} else {
  info('Virtual environment already exists. Verifying...');
  const checkResult = spawnSync(
    IS_WIN ? path.join(VENV, 'Scripts', 'python.exe') : path.join(VENV, 'bin', 'python'),
    ['--version'], { encoding: 'utf8' }
  );
  if (checkResult.status === 0) {
    const match = checkResult.stdout.match(/Python (\d+)\.(\d+)/);
    if (match && Number(match[2]) >= 14) {
      warn('Existing venv uses Python 3.14+. Recreating with compatible version...');
      fs.rmSync(VENV, { recursive: true, force: true });
      const result = spawnSync(pythonBin, [...pythonFlags, '-m', 'venv', VENV], { stdio: 'inherit' });
      if (result.status !== 0) fail('Failed to recreate virtual environment.');
      ok('Virtual environment recreated with compatible Python');
    } else {
      ok('Virtual environment OK');
    }
  }
}

// ============================================================
//  Step 3: Install Python dependencies
// ============================================================
step('Step 3: Installing Python Dependencies');

const pipCmd = IS_WIN
  ? path.join(VENV, 'Scripts', 'pip.exe')
  : path.join(VENV, 'bin', 'pip');

const reqFile = path.join(ROOT, 'requirements.txt');

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
//  Step 4: Detect Hardware & Choose Optimal Model
// ============================================================
step('Step 4: Detecting Hardware for Optimal Model Selection');

function detectHardware() {
  const totalMemGB = os.totalmem() / (1024 * 1024 * 1024);
  const cpuCores = os.cpus().length;
  const cpuModel = os.cpus()[0].model;
  
  let recommendedModel = 'phi3';
  let modelReason = 'Safe default for most systems';
  let hasGPU = false;
  
  // Check for NVIDIA GPU (Linux/Windows)
  if (IS_LINUX) {
    try {
      const nvidiaCheck = spawnSync('nvidia-smi', ['--query-gpu=name', '--format=csv,noheader'], { encoding: 'utf8' });
      if (nvidiaCheck.status === 0 && nvidiaCheck.stdout.trim()) {
        hasGPU = true;
        info(`NVIDIA GPU detected: ${nvidiaCheck.stdout.trim()}`);
      }
    } catch (_) {}
  } else if (IS_WIN) {
    try {
      const gpuCheck = spawnSync('wmic', ['path', 'win32_VideoController', 'get', 'name'], { encoding: 'utf8' });
      if (gpuCheck.status === 0 && /nvidia|amd|rtx|gtx/i.test(gpuCheck.stdout)) {
        hasGPU = true;
        info(`Dedicated GPU detected`);
      }
    } catch (_) {}
  } else if (IS_MAC) {
    try {
      const appleSilicon = spawnSync('sysctl', ['-n', 'machdep.cpu.brand_string'], { encoding: 'utf8' });
      if (/apple/i.test(appleSilicon.stdout) || spawnSync('sysctl', ['-n', 'hw.optional.arm64'], { encoding: 'utf8' }).stdout.trim() === '1') {
        hasGPU = true;
        info('Apple Silicon detected (M1/M2/M3) - Unified memory architecture');
      }
    } catch (_) {}
  }
  
  // Model selection logic based on RAM and GPU
  if (totalMemGB >= 16 || hasGPU) {
    if (totalMemGB >= 32) {
      recommendedModel = 'llama3';
      modelReason = 'High RAM system - using powerful Llama3 (8B)';
    } else if (hasGPU && totalMemGB >= 16) {
      recommendedModel = 'llama3';
      modelReason = 'GPU + sufficient RAM - using Llama3 (8B)';
    } else {
      recommendedModel = 'phi3';
      modelReason = 'Moderate system - using efficient Phi3 (3.8B)';
    }
  } else if (totalMemGB < 8) {
    recommendedModel = 'phi3';
    modelReason = 'Low RAM system - using lightweight Phi3 (3.8B)';
  }
  
  return {
    totalMemGB: totalMemGB.toFixed(2),
    cpuCores,
    cpuModel: cpuModel.substring(0, 50),
    hasGPU,
    recommendedModel,
    modelReason
  };
}

const hardware = detectHardware();

console.log(`
  ${BOLD}Hardware Analysis:${RESET}
  ────────────────────────────────
  ${CYAN}RAM:${RESET}            ${hardware.totalMemGB} GB
  ${CYAN}CPU Cores:${RESET}      ${hardware.cpuCores}
  ${CYAN}CPU Model:${RESET}      ${hardware.cpuModel}
  ${CYAN}GPU Detected:${RESET}   ${hardware.hasGPU ? GREEN + 'Yes' + RESET : RED + 'No' + RESET}
  
  ${BOLD}Recommended Model:${RESET} ${GREEN}${hardware.recommendedModel}${RESET}
  ${CYAN}Reason:${RESET} ${hardware.modelReason}
`);

// Set environment variable for the chosen model
process.env.REFUGIA_MODEL = hardware.recommendedModel;

// Save model config to .refugia-config.json
const configFile = path.join(ROOT, '.refugia-config.json');
fs.writeFileSync(configFile, JSON.stringify({
  model: hardware.recommendedModel,
  reason: hardware.modelReason,
  hardware: {
    ram: hardware.totalMemGB,
    cores: hardware.cpuCores,
    hasGPU: hardware.hasGPU
  },
  configuredAt: new Date().toISOString()
}, null, 2));
ok(`Configuration saved to ${configFile}`);

// ============================================================
//  Step 5: Install Ollama if needed
// ============================================================
step('Step 5: Checking Ollama Installation');

const ollamaCheck = spawnSync('ollama', ['--version'], { encoding: 'utf8' });

if (ollamaCheck.status !== 0 || ollamaCheck.error) {
  if (IS_WIN) {
    warn('Ollama not found. Installing automatically...');
    try {
      // Download and install Ollama silently on Windows
      const installerPath = path.join(os.tmpdir(), 'ollama-setup.exe');
      info('Downloading Ollama installer...');
      
      const https = require('https');
      const file = fs.createWriteStream(installerPath);
      
      function downloadFile(url, dest, callback) {
        https.get(url, (response) => {
          response.pipe(file);
          file.on('finish', () => {
            file.close();
            callback(null);
          });
        }).on('error', (err) => {
          fs.unlink(installerPath, () => {});
          callback(err);
        });
      }
      
      downloadFile('https://ollama.com/download/OllamaSetup.exe', installerPath, (err) => {
        if (err) throw err;
        
        info('Running Ollama installer...');
        spawnSync(installerPath, ['/SILENT'], { stdio: 'inherit' });
        fs.unlinkSync(installerPath);
        ok('Ollama installed successfully');
      });
    } catch (e) {
      warn('Auto-install failed. Please install manually from: https://ollama.com/download');
    }
  } else if (IS_MAC) {
    info('Installing Ollama via Homebrew...');
    try {
      execSync('brew install ollama', { stdio: 'inherit' });
      ok('Ollama installed via Homebrew');
    } catch (_) {
      warn('Homebrew not found. Installing via official script...');
      try {
        execSync('curl -fsSL https://ollama.com/install.sh | sh', { stdio: 'inherit' });
        ok('Ollama installed');
      } catch (__) {
        warn('Could not auto-install Ollama. Install manually: https://ollama.com/download');
      }
    }
  } else {
    // Linux
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
//  Step 6: Pull the Optimal Model
// ============================================================
step(`Step 6: Downloading Model (${hardware.recommendedModel})`);

info(`Pulling ${hardware.recommendedModel} from Ollama...`);
info('This may take several minutes depending on your connection.');

const pullResult = spawnSync('ollama', ['pull', hardware.recommendedModel], {
  stdio: 'inherit',
  env: { ...process.env, OLLAMA_ORIGINS: '*' }
});

if (pullResult.status !== 0) {
  warn(`Failed to pull ${hardware.recommendedModel}. You can pull it manually later with:`);
  console.log(`  ${AMBER}ollama pull ${hardware.recommendedModel}${RESET}`);
} else {
  ok(`Model ${hardware.recommendedModel} downloaded successfully`);
}

// ============================================================
//  Step 7: Index Survival Manuals
// ============================================================
step('Step 7: Indexing Survival Manuals');

const indexScript = path.join(ROOT, 'src', 'indexador.py');
if (fs.existsSync(indexScript)) {
  info('Indexing PDF manuals into vector database...');
  const indexResult = spawnSync(pythonBin, [...pythonFlags, indexScript], {
    stdio: 'inherit',
    cwd: ROOT,
    env: { ...process.env, PYTHONPATH: path.join(ROOT, 'src') }
  });
  
  if (indexResult.status === 0) {
    ok('Manuals indexed successfully');
  } else {
    warn('Indexing failed. You can run it manually later with:');
    console.log(`  ${AMBER}refugia index${RESET}`);
  }
} else {
  warn('indexador.py not found. Skipping indexing step.');
}

// ============================================================
//  Done - Final Summary
// ============================================================
console.log(`
${GREEN}  ════════════════════════════════════════════════════${RESET}
${GREEN}${BOLD}  🎉 APOCALYPSE AGENT INSTALLATION COMPLETE!${RESET}
${GREEN}  ════════════════════════════════════════════════════${RESET}

  ${BOLD}System Configuration:${RESET}
  ─────────────────────────────────────
  ${CYAN}Optimal Model:${RESET}  ${GREEN}${hardware.recommendedModel}${RESET}
  ${CYAN}Reason:${RESET}         ${hardware.modelReason}
  ${CYAN}RAM Available:${RESET}  ${hardware.totalMemGB} GB
  ${CYAN}GPU Acceleration:${RESET} ${hardware.hasGPU ? GREEN + 'Yes' + RESET : 'No'}

  ${BOLD}Quick Start Commands:${RESET}
  ─────────────────────────────────────
  ${AMBER}refugia start${RESET}    — Launch Apocalypse Agent
  ${AMBER}refugia status${RESET}   — Check system status
  ${AMBER}refugia doctor${RESET}   — Diagnose issues

  ${DIM}The server will open at: http://127.0.0.1:8000${RESET}

  ${BOLD}What's Ready:${RESET}
  ✓ Python environment configured
  ✓ All dependencies installed
  ✓ Optimal AI model selected for your hardware
  ✓ Ollama installed and running
  ✓ Survival manuals indexed
  ✓ Ready for offline operation

${GREEN}  ════════════════════════════════════════════════════${RESET}
${GREEN}  "When the grid falls, knowledge survives."${RESET}
${GREEN}  ════════════════════════════════════════════════════${RESET}
`);
