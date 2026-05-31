#!/usr/bin/env node
// ============================================================
// RefugIA OS — CLI Entry Point (Node.js)
// ============================================================
// Usage: refugia start | index | status | doctor
// Installed via: npm install -g refugia-os
// ============================================================

const { spawnSync, spawn } = require('child_process');
const path = require('path');
const os = require('os');
const fs = require('fs');

const ROOT    = path.resolve(__dirname, '..');
const VENV    = path.join(ROOT, 'venv');
const IS_WIN  = os.platform() === 'win32';
const PYTHON  = IS_WIN
  ? path.join(VENV, 'Scripts', 'python.exe')
  : path.join(VENV, 'bin', 'python');

// Colors
const AMBER  = '\x1b[33m';
const GREEN  = '\x1b[32m';
const RED    = '\x1b[31m';
const CYAN   = '\x1b[36m';
const DIM    = '\x1b[2m';
const BOLD   = '\x1b[1m';
const RESET  = '\x1b[0m';

const VERSION = require('../package.json').version;

const BANNER = `
${AMBER}  ╔══════════════════════════════════════════╗
  ║                                          ║
  ║   ██████  ███████ ███████ ██    ██       ║
  ║   ██   ██ ██      ██      ██    ██       ║
  ║   ██████  █████   █████   ██    ██       ║
  ║   ██   ██ ██      ██      ██    ██       ║
  ║   ██   ██ ███████ ██       ██████        ║
  ║            ${GREEN}╔══╗${AMBER}                          ║
  ║         ${GREEN}IA${AMBER} ║${GREEN}OS${AMBER}║  ${DIM}v${VERSION}${RESET}${AMBER}                  ║
  ║            ${GREEN}╚══╝${AMBER}                          ║
  ║                                          ║
  ║   ${RESET}${BOLD}Survival Operating System${RESET}${AMBER}              ║
  ║   ${DIM}"When the grid falls, knowledge        ${RESET}${AMBER}║
  ║    ${DIM}survives."${RESET}${AMBER}                            ║
  ║                                          ║
  ╚══════════════════════════════════════════╝${RESET}
`;

function printBanner() {
  console.log(BANNER);
}

function checkVenv() {
  if (!fs.existsSync(PYTHON)) {
    console.log(`\n  ${RED}Error: Virtual environment not found.${RESET}`);
    console.log(`  Run: ${AMBER}npm install -g refugia-os${RESET} to set it up.\n`);
    process.exit(1);
  }
}

function runPython(args, opts = {}) {
  const result = spawnSync(PYTHON, args, {
    stdio: 'inherit',
    cwd: ROOT,
    ...opts,
  });
  return result.status;
}

function runPythonCLI(command, extraArgs = []) {
  checkVenv();
  const cliPath = path.join(ROOT, 'src', 'cli.py');
  return runPython([cliPath, command, ...extraArgs]);
}

// ============================================================
//  Commands
// ============================================================

function cmdStart(args) {
  printBanner();
  checkVenv();

  const model  = process.env.REFUGIA_MODEL || 'phi3';
  const port   = process.env.REFUGIA_PORT  || '8000';
  const host   = process.env.REFUGIA_HOST  || '0.0.0.0';

  console.log(`  ${CYAN}Model:${RESET}  ${model}`);
  console.log(`  ${CYAN}Port:${RESET}   ${port}`);
  console.log(`  ${CYAN}URL:${RESET}    http://127.0.0.1:${port}`);
  console.log();

  const noBrowser = args.includes('--no-browser');

  if (!noBrowser) {
    // Open browser after 2s delay
    setTimeout(() => {
      const url = `http://127.0.0.1:${port}`;
      const openCmd = IS_WIN ? 'start' : os.platform() === 'darwin' ? 'open' : 'xdg-open';
      spawnSync(openCmd, [url], { shell: IS_WIN });
    }, 2000);
  }

  console.log(`  ${GREEN}Starting server...${RESET}`);
  console.log(`  ${DIM}Press Ctrl+C to stop.${RESET}\n`);

  // Run uvicorn via Python
  const uvicornProc = spawn(
    PYTHON,
    ['-m', 'uvicorn', 'agente_api:app',
     '--host', host,
     '--port', port,
     '--log-level', 'info',
    ],
    { stdio: 'inherit', cwd: path.join(ROOT, 'src') }
  );

  uvicornProc.on('close', (code) => process.exit(code || 0));

  process.on('SIGINT',  () => uvicornProc.kill('SIGINT'));
  process.on('SIGTERM', () => uvicornProc.kill('SIGTERM'));
}

function cmdIndex() {
  printBanner();
  console.log(`  ${CYAN}Indexing survival manuals...${RESET}\n`);
  checkVenv();
  const code = runPython([path.join(ROOT, 'src', 'indexador.py')]);
  process.exit(code);
}

function cmdStatus() {
  runPythonCLI('status');
}

function cmdDoctor() {
  runPythonCLI('doctor');
}

function cmdHelp() {
  printBanner();
  console.log(`  ${BOLD}Usage:${RESET} refugia <command>\n`);
  console.log(`  ${AMBER}Commands:${RESET}`);
  console.log(`    ${GREEN}start${RESET}      Launch the server and open browser`);
  console.log(`    ${GREEN}index${RESET}      Index PDF survival manuals`);
  console.log(`    ${GREEN}status${RESET}     Show system status`);
  console.log(`    ${GREEN}doctor${RESET}     Diagnose common issues`);
  console.log(`    ${GREEN}help${RESET}       Show this help\n`);
  console.log(`  ${AMBER}Options:${RESET}`);
  console.log(`    ${DIM}--no-browser   Don't auto-open the browser on start${RESET}\n`);
  console.log(`  ${AMBER}Env vars:${RESET}`);
  console.log(`    ${DIM}REFUGIA_MODEL  Ollama model (default: phi3)${RESET}`);
  console.log(`    ${DIM}REFUGIA_PORT   Server port  (default: 8000)${RESET}\n`);
}

// ============================================================
//  Argument parsing
// ============================================================
const args = process.argv.slice(2);
const command = args[0];
const rest = args.slice(1);

switch (command) {
  case 'start':   cmdStart(rest);   break;
  case 'index':   cmdIndex();        break;
  case 'status':  cmdStatus();       break;
  case 'doctor':  cmdDoctor();       break;
  case 'help':
  case '--help':
  case '-h':
  case undefined: cmdHelp();         break;
  default:
    console.log(`\n  ${RED}Unknown command: ${command}${RESET}`);
    console.log(`  Run ${AMBER}refugia help${RESET} to see available commands.\n`);
    process.exit(1);
}
