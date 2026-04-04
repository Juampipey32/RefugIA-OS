# ============================================================
# RefugIA OS — Windows One-Shot Installer (PowerShell)
# ============================================================
# ONE COMMAND to install and run RefugIA OS:
#   irm https://raw.githubusercontent.com/juampipey32/apocalipsis-agent/main/setup.ps1 | iex
#
# This script installs: Python 3.12, Git, Ollama, clones the
# repo, creates venv, installs deps, indexes manuals, and
# starts the server. Fully automated.
# ============================================================

$ErrorActionPreference = "Stop"

# --- Colors ---
function Write-Amber  { param($msg) Write-Host "  $msg" -ForegroundColor Yellow }
function Write-OK     { param($msg) Write-Host "  [OK]  $msg" -ForegroundColor Green }
function Write-Info   { param($msg) Write-Host "  [..]  $msg" -ForegroundColor Cyan }
function Write-Fail   { param($msg) Write-Host "  [X]   $msg" -ForegroundColor Red; exit 1 }

# --- Banner ---
Write-Host @"

  `e[33m╔══════════════════════════════════════════╗
  ║                                          ║
  ║   ██████  ███████ ███████ ██    ██       ║
  ║   ██   ██ ██      ██      ██    ██       ║
  ║   ██████  █████   █████   ██    ██       ║
  ║   ██   ██ ██      ██      ██    ██       ║
  ║   ██   ██ ███████ ██       ██████        ║
  ║            `e[32m╔══╗`e[33m                          ║
  ║         `e[32mIA`e[33m ║`e[32mOS`e[33m║  `e[2mInstaller`e[0m`e[33m              ║
  ║            `e[32m╚══╝`e[33m                          ║
  ║                                          ║
  ║   Survival Operating System              ║
  ╚══════════════════════════════════════════╝`e[0m

"@

# ============================================================
#  Step 1: Check/Install Python 3.12
# ============================================================
Write-Amber "━━━ 1/6  Python ━━━"

$pythonCmd = $null
foreach ($cmd in @("py", "python", "python3")) {
    try {
        $ver = & $cmd --version 2>&1
        if ($ver -match "Python 3\.1[0-3]") {
            $pythonCmd = $cmd
            Write-OK "Python found: $ver"
            break
        }
    } catch {}
}

# Try py launcher with specific version
if (-not $pythonCmd) {
    try {
        $ver = & py -3.12 --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            $pythonCmd = "py -3.12"
            Write-OK "Python 3.12 found via py launcher"
        }
    } catch {}
}

if (-not $pythonCmd) {
    Write-Info "Python 3.12 not found. Installing via winget..."
    try {
        winget install Python.Python.3.12 --accept-package-agreements --accept-source-agreements --silent
        # Refresh PATH
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
        $pythonCmd = "py -3.12"
        Write-OK "Python 3.12 installed"
    } catch {
        Write-Fail "Could not install Python. Install manually from: https://www.python.org/downloads/release/python-3129/"
    }
}

# ============================================================
#  Step 2: Check/Install Git
# ============================================================
Write-Amber "━━━ 2/6  Git ━━━"

if (Get-Command git -ErrorAction SilentlyContinue) {
    Write-OK "Git found"
} else {
    Write-Info "Git not found. Installing via winget..."
    try {
        winget install Git.Git --accept-package-agreements --accept-source-agreements --silent
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
        Write-OK "Git installed"
    } catch {
        Write-Fail "Could not install Git. Install manually from: https://git-scm.com/downloads"
    }
}

# ============================================================
#  Step 3: Clone repo (if not already in it)
# ============================================================
Write-Amber "━━━ 3/6  Repository ━━━"

$repoDir = "$HOME\apocalipsis-agent"

if (Test-Path "$repoDir\src\agente_api.py") {
    Write-OK "Repository already exists at $repoDir"
} else {
    Write-Info "Cloning repository..."
    git clone https://github.com/juampipey32/apocalipsis-agent.git "$repoDir"
    Write-OK "Repository cloned"
}

Set-Location $repoDir

# ============================================================
#  Step 4: Create venv & install dependencies
# ============================================================
Write-Amber "━━━ 4/6  Dependencies ━━━"

# Determine python executable for venv
if ($pythonCmd -eq "py -3.12") {
    $pyExe = "py"
    $pyArgs = @("-3.12")
} else {
    $pyExe = $pythonCmd
    $pyArgs = @()
}

if (-not (Test-Path "venv")) {
    Write-Info "Creating virtual environment..."
    & $pyExe @pyArgs -m venv venv
    Write-OK "Virtual environment created"
} else {
    Write-OK "Virtual environment already exists"
}

# Activate and install
$pipExe = ".\venv\Scripts\pip.exe"
$pythonVenv = ".\venv\Scripts\python.exe"

Write-Info "Upgrading pip..."
& $pythonVenv -m pip install --upgrade pip -q 2>$null

Write-Info "Installing dependencies (this may take a few minutes)..."
& $pipExe install -r requirements.txt -q --prefer-binary
if ($LASTEXITCODE -ne 0) {
    Write-Fail "pip install failed. Check the output above for errors."
}
Write-OK "Dependencies installed"

# ============================================================
#  Step 5: Check/Install Ollama
# ============================================================
Write-Amber "━━━ 5/6  Ollama ━━━"

if (Get-Command ollama -ErrorAction SilentlyContinue) {
    Write-OK "Ollama found"
} else {
    Write-Info "Ollama not found. Installing via winget..."
    try {
        winget install Ollama.Ollama --accept-package-agreements --accept-source-agreements --silent
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
        Write-OK "Ollama installed"
        Write-Info "Starting Ollama..."
        Start-Process ollama -ArgumentList "serve" -WindowStyle Hidden
        Start-Sleep -Seconds 3
    } catch {
        Write-Amber "Could not auto-install Ollama. Install from: https://ollama.com/download"
    }
}

# Pull model
$model = if ($env:REFUGIA_MODEL) { $env:REFUGIA_MODEL } else { "phi3" }
Write-Info "Checking model: $model"

try {
    $modelList = ollama list 2>&1
    if ($modelList -match $model) {
        Write-OK "Model $model already downloaded"
    } else {
        Write-Info "Downloading model $model (this may take several minutes)..."
        ollama pull $model
        Write-OK "Model $model downloaded"
    }
} catch {
    Write-Amber "Could not pull model. Run manually: ollama pull $model"
}

# ============================================================
#  Step 6: Index manuals
# ============================================================
Write-Amber "━━━ 6/6  Indexing manuals ━━━"

$pdfs = Get-ChildItem -Path "manuales\*.pdf" -ErrorAction SilentlyContinue
if ($pdfs.Count -gt 0) {
    Write-Info "Indexing $($pdfs.Count) manual(s)..."
    & $pythonVenv src\indexador.py
    Write-OK "Manuals indexed"
} else {
    Write-Amber "No PDFs found in manuales\. Add your manuals later and run: .\venv\Scripts\python.exe src\indexador.py"
}

# ============================================================
#  Done! Start server
# ============================================================
Write-Host ""
Write-Host "  `e[32m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`e[0m"
Write-Host "  `e[32m`e[1m  INSTALLATION COMPLETE!`e[0m"
Write-Host "  `e[32m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`e[0m"
Write-Host ""
Write-Host "  Starting RefugIA OS..."
Write-Host "  Open your browser at: `e[33mhttp://127.0.0.1:8000`e[0m"
Write-Host "  Press Ctrl+C to stop the server."
Write-Host ""

# Open browser after delay
Start-Job -ScriptBlock {
    Start-Sleep -Seconds 3
    Start-Process "http://127.0.0.1:8000"
} | Out-Null

# Start server
& $pythonVenv src\agente_api.py
