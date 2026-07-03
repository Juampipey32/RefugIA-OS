#!/usr/bin/env bash
# ============================================================
# RefugIA OS — Universal Auto Installer
# ============================================================
# ONE COMMAND to install and run RefugIA on ANY system:
#   curl -fsSL https://raw.githubusercontent.com/juampipey32/RefugIA-OS/main/refugia.sh | bash
#
# This script automatically:
#   1. Detects the best installation method (Docker or native)
#   2. Installs all dependencies
#   3. Downloads AI models
#   4. Indexes survival manuals
#   5. Starts the server
# ============================================================

set -euo pipefail

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
AMBER='\033[0;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# --- Helpers ---
info()    { echo -e "${CYAN}[INFO]${NC}  $1"; }
ok()      { echo -e "${GREEN}[  OK]${NC}  $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $1"; }
fail()    { echo -e "${RED}[FAIL]${NC}  $1"; exit 1; }
step()    { echo -e "\n${AMBER}━━━ $1 ━━━${NC}"; }

# --- Banner ---
echo -e "${AMBER}"
cat << 'BANNER'

  ╔══════════════════════════════════════════╗
  ║                                          ║
  ║   ██████  ███████ ███████ ██    ██       ║
  ║   ██   ██ ██      ██      ██    ██       ║
  ║   ██████  █████   █████   ██    ██       ║
  ║   ██   ██ ██      ██      ██    ██       ║
  ║   ██   ██ ███████ ██       ██████        ║
  ║            ╔══╗                          ║
  ║         IA ║OS║  Universal Installer     ║
  ║            ╚══╝                          ║
  ║                                          ║
  ║   Survival Operating System              ║
  ║   "When the grid falls, knowledge        ║
  ║    survives."                            ║
  ║                                          ║
  ╚══════════════════════════════════════════╝

BANNER
echo -e "${NC}"

# --- Detect project directory ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
info "Project directory: $SCRIPT_DIR"

# ============================================================
#  Step 1: Detect OS
# ============================================================
step "1/7  Detecting operating system"

OS="$(uname -s)"
case "$OS" in
    Linux*)     OS_NAME="Linux";;
    Darwin*)    OS_NAME="macOS";;
    *)          fail "Unsupported OS: $OS. RefugIA requires Linux or macOS.";;
esac
ok "Operating system: $OS_NAME"

# ============================================================
#  Step 2: Check if running inside Docker
# ============================================================
step "2/7  Checking environment"

if [ -f /.dockerenv ] || grep -q docker /proc/1/cgroup 2>/dev/null; then
    ok "Running inside Docker container"
    RUNNING_IN_DOCKER=true
else
    ok "Running natively"
    RUNNING_IN_DOCKER=false
fi

# ============================================================
#  Step 3: Choose installation method
# ============================================================
step "3/7  Selecting installation method"

USE_DOCKER=false

# If already in Docker, skip Docker check
if [ "$RUNNING_IN_DOCKER" = true ]; then
    info "Already in Docker, using native installation inside container"
    USE_DOCKER=false
else
    # Check if Docker is available
    if command -v docker &> /dev/null; then
        if docker ps &> /dev/null; then
            ok "Docker is available and running"
            USE_DOCKER=true
        else
            warn "Docker daemon not running"
            USE_DOCKER=false
        fi
    else
        info "Docker not found, will use native installation"
        USE_DOCKER=false
    fi
fi

if [ "$USE_DOCKER" = true ]; then
    ok "Method: Docker Compose (recommended)"
else
    ok "Method: Native installation"
fi

# ============================================================
#  METHOD A: Docker Installation
# ============================================================
if [ "$USE_DOCKER" = true ]; then
    
    step "4/7  Starting Docker services"
    
    if [ ! -f "docker-compose.yml" ]; then
        fail "docker-compose.yml not found"
    fi
    
    info "Starting RefugIA with Docker Compose..."
    docker compose up --build -d
    
    ok "Containers started"
    
    step "5/7  Waiting for services"
    
    info "Waiting for Ollama to be ready (this may take a few minutes on first run)..."
    sleep 10
    
    # Wait for Ollama to be ready
    MAX_ATTEMPTS=30
    ATTEMPT=0
    while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
        if curl -s http://localhost:11434/api/tags &> /dev/null; then
            ok "Ollama is ready"
            break
        fi
        ATTEMPT=$((ATTEMPT + 1))
        sleep 2
    done
    
    if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
        warn "Ollama may still be starting up"
    fi
    
    step "6/7  Downloading AI model"
    
    MODEL="${REFUGIA_MODEL:-phi3}"
    info "Model: $MODEL"
    
    info "Pulling model $MODEL (this may take several minutes)..."
    docker exec refugia-ollama ollama pull "$MODEL" || warn "Could not pull model automatically"
    
    step "7/7  Indexing manuals"
    
    PDF_COUNT=$(find manuales/ -name "*.pdf" 2>/dev/null | wc -l)
    if [ "$PDF_COUNT" -eq 0 ]; then
        warn "No PDFs found in manuales/. Add your manuals and run: docker exec refugia-app python src/indexador.py"
    else
        info "Indexing $PDF_COUNT manual(s)..."
        docker exec refugia-app python src/indexador.py || warn "Indexing failed"
        ok "Manuals indexed"
    fi
    
    # Done!
    echo ""
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}${BOLD}  INSTALLATION COMPLETE!${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "  RefugIA OS is running at: ${AMBER}http://127.0.0.1:8000${NC}"
    echo -e "  To stop: ${BOLD}docker compose down${NC}"
    echo -e "  To view logs: ${BOLD}docker compose logs -f${NC}"
    echo ""
    
    # Open browser after delay
    (sleep 2 && xdg-open "http://127.0.0.1:8000" 2>/dev/null || open "http://127.0.0.1:8000" 2>/dev/null) &
    
    info "Attaching to logs (Ctrl+C to stop)..."
    docker compose logs -f
    
    exit 0
fi

# ============================================================
#  METHOD B: Native Installation
# ============================================================

# --- Check Python 3.10+ ---
step "4/7  Checking Python"

PYTHON_CMD=""
for cmd in python3 python; do
    if command -v "$cmd" &> /dev/null; then
        PY_VERSION=$("$cmd" -c 'import sys; print(f"{sys.version_info.major}.{sys.version_info.minor}")')
        PY_MAJOR=$("$cmd" -c 'import sys; print(sys.version_info.major)')
        PY_MINOR=$("$cmd" -c 'import sys; print(sys.version_info.minor)')
        if [ "$PY_MAJOR" -ge 3 ] && [ "$PY_MINOR" -ge 10 ] && [ "$PY_MINOR" -le 13 ]; then
            PYTHON_CMD="$cmd"
            break
        elif [ "$PY_MAJOR" -ge 3 ] && [ "$PY_MINOR" -ge 14 ]; then
            warn "Python $PY_VERSION detected — too new, packages lack pre-built binaries."
        fi
    fi
done

if [ -z "$PYTHON_CMD" ]; then
    warn "Python 3.10-3.13 not found. Installing..."
    if [ "$OS_NAME" = "macOS" ]; then
        if command -v brew &> /dev/null; then
            brew install python@3.12
            PYTHON_CMD="python3"
        else
            info "Installing Homebrew first..."
            /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
            brew install python@3.12
            PYTHON_CMD="python3"
        fi
    elif [ "$OS_NAME" = "Linux" ]; then
        if command -v apt &> /dev/null; then
            sudo apt update -qq && sudo apt install -y python3 python3-venv python3-pip
            PYTHON_CMD="python3"
        elif command -v dnf &> /dev/null; then
            sudo dnf install -y python3 python3-pip
            PYTHON_CMD="python3"
        elif command -v pacman &> /dev/null; then
            sudo pacman -Sy --noconfirm python python-pip
            PYTHON_CMD="python3"
        else
            fail "Could not auto-install Python. Install Python 3.12 manually and re-run this script."
        fi
    fi
    
    if [ -z "$PYTHON_CMD" ]; then
        fail "Python installation failed. Install Python 3.12 from: https://www.python.org/downloads/"
    fi
    PY_VERSION=$("$PYTHON_CMD" -c 'import sys; print(f"{sys.version_info.major}.{sys.version_info.minor}")')
    ok "Python installed: $PYTHON_CMD ($PY_VERSION)"
else
    ok "Python found: $PYTHON_CMD ($PY_VERSION)"
fi

# --- Check/Install Ollama ---
step "5/7  Checking Ollama"

if command -v ollama &> /dev/null; then
    ok "Ollama is already installed"
else
    warn "Ollama not found. Installing..."
    if [ "$OS_NAME" = "Linux" ]; then
        curl -fsSL https://ollama.com/install.sh | sh
    elif [ "$OS_NAME" = "macOS" ]; then
        if command -v brew &> /dev/null; then
            brew install ollama
        else
            fail "Install Ollama manually from https://ollama.com/download"
        fi
    fi
    
    if command -v ollama &> /dev/null; then
        ok "Ollama installed successfully"
    else
        fail "Could not install Ollama. Install it manually: https://ollama.com/download"
    fi
fi

# --- Create virtual environment & install deps ---
step "6/7  Setting up Python environment"

if [ ! -d "venv" ]; then
    info "Creating virtual environment..."
    "$PYTHON_CMD" -m venv venv
    ok "Virtual environment created in ./venv"
else
    ok "Virtual environment already exists"
fi

# Activate venv
source venv/bin/activate

info "Installing dependencies (this may take a few minutes)..."
pip install --upgrade pip -q
pip install -r requirements.txt -q --prefer-binary
ok "Dependencies installed"

# --- Download Ollama model ---
step "7/7  Downloading AI model"

MODEL="${REFUGIA_MODEL:-phi3}"
info "Model: $MODEL"

# Check if ollama is running, start if not
if ! ollama list &> /dev/null; then
    warn "Ollama is not running. Attempting to start..."
    if [ "$OS_NAME" = "Linux" ]; then
        ollama serve &> /dev/null &
        sleep 3
    else
        warn "Start Ollama manually and re-run the installer."
    fi
fi

if ollama list 2>/dev/null | grep -q "$MODEL"; then
    ok "Model $MODEL already downloaded"
else
    info "Downloading model $MODEL (this may take several minutes)..."
    if ollama pull "$MODEL"; then
        ok "Model $MODEL downloaded"
    else
        warn "Could not download the model. Download manually: ollama pull $MODEL"
    fi
fi

# --- Index survival manuals ---
echo ""
step "8/8  Indexing survival manuals"

PDF_COUNT=$(find manuales/ -name "*.pdf" 2>/dev/null | wc -l)
if [ "$PDF_COUNT" -eq 0 ]; then
    warn "No PDFs found in manuales/. Add your manuals and run: ./refugia index"
else
    info "Indexing $PDF_COUNT manual(s) PDF..."
    "$PYTHON_CMD" src/indexador.py
    ok "Manuals indexed"
fi

# ============================================================
#  Done!
# ============================================================
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}${BOLD}  INSTALLATION COMPLETE!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "  Starting RefugIA OS..."
echo -e "  Open your browser at: ${AMBER}http://127.0.0.1:${API_PORT:-8000}${NC}"
echo -e "  Press ${BOLD}Ctrl+C${NC} to stop the server."
echo ""

# Open browser after delay
(sleep 2 && xdg-open "http://127.0.0.1:${API_PORT:-8000}" 2>/dev/null || open "http://127.0.0.1:${API_PORT:-8000}" 2>/dev/null) &

# Start server
"$PYTHON_CMD" src/agente_api.py
