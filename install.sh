#!/usr/bin/env bash
# ============================================================
# RefugIA OS — Instalador Automático / Auto Installer
# ============================================================
# Usage:
#   ./install.sh
#   curl -fsSL https://raw.githubusercontent.com/juampipey32/apocalipsis-agent/main/install.sh | bash
# ============================================================

set -euo pipefail

# --- Colors ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
AMBER='\033[0;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

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
  ║         IA ║OS║  Installer               ║
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
info "Directorio del proyecto: $SCRIPT_DIR"

# ============================================================
#  Step 1: Check OS
# ============================================================
step "1/6  Checking operating system"

OS="$(uname -s)"
case "$OS" in
    Linux*)     OS_NAME="Linux";;
    Darwin*)    OS_NAME="macOS";;
    *)          fail "OS no soportado: $OS. RefugIA requiere Linux o macOS.";;
esac
ok "Sistema operativo: $OS_NAME"

# ============================================================
#  Step 2: Check Python 3.10+
# ============================================================
step "2/6  Checking Python"

PYTHON_CMD=""
for cmd in python3 python; do
    if command -v "$cmd" &> /dev/null; then
        PY_VERSION=$("$cmd" -c 'import sys; print(f"{sys.version_info.major}.{sys.version_info.minor}")')
        PY_MAJOR=$("$cmd" -c 'import sys; print(sys.version_info.major)')
        PY_MINOR=$("$cmd" -c 'import sys; print(sys.version_info.minor)')
        if [ "$PY_MAJOR" -ge 3 ] && [ "$PY_MINOR" -ge 10 ]; then
            PYTHON_CMD="$cmd"
            break
        fi
    fi
done

if [ -z "$PYTHON_CMD" ]; then
    fail "Python 3.10+ es requerido pero no se encontró.\nInstala Python: https://www.python.org/downloads/"
fi
ok "Python encontrado: $PYTHON_CMD ($PY_VERSION)"

# ============================================================
#  Step 3: Check/Install Ollama
# ============================================================
step "3/6  Checking Ollama"

if command -v ollama &> /dev/null; then
    ok "Ollama ya está instalado"
else
    warn "Ollama no encontrado. Instalando..."
    if [ "$OS_NAME" = "Linux" ]; then
        curl -fsSL https://ollama.com/install.sh | sh
    elif [ "$OS_NAME" = "macOS" ]; then
        if command -v brew &> /dev/null; then
            brew install ollama
        else
            fail "Instala Ollama manualmente desde https://ollama.com/download"
        fi
    fi

    if command -v ollama &> /dev/null; then
        ok "Ollama instalado correctamente"
    else
        fail "No se pudo instalar Ollama. Instálalo manualmente: https://ollama.com/download"
    fi
fi

# ============================================================
#  Step 4: Create virtual environment & install deps
# ============================================================
step "4/6  Setting up Python environment"

if [ ! -d "venv" ]; then
    info "Creando entorno virtual..."
    "$PYTHON_CMD" -m venv venv
    ok "Entorno virtual creado en ./venv"
else
    ok "Entorno virtual ya existe"
fi

# Activate venv
source venv/bin/activate

info "Instalando dependencias (esto puede tardar unos minutos)..."
pip install --upgrade pip -q
pip install -r requirements.txt -q
ok "Dependencias instaladas"

# ============================================================
#  Step 5: Download Ollama model
# ============================================================
step "5/6  Downloading AI model"

MODEL="${REFUGIA_MODEL:-phi3}"
info "Modelo: $MODEL"

# Check if ollama is running, start if not
if ! ollama list &> /dev/null; then
    warn "Ollama no está corriendo. Intentando iniciar..."
    if [ "$OS_NAME" = "Linux" ]; then
        ollama serve &> /dev/null &
        sleep 3
    else
        warn "Inicia Ollama manualmente y vuelve a ejecutar el instalador."
    fi
fi

if ollama list 2>/dev/null | grep -q "$MODEL"; then
    ok "Modelo $MODEL ya descargado"
else
    info "Descargando modelo $MODEL (esto puede tardar varios minutos)..."
    if ollama pull "$MODEL"; then
        ok "Modelo $MODEL descargado"
    else
        warn "No se pudo descargar el modelo. Descárgalo manualmente: ollama pull $MODEL"
    fi
fi

# ============================================================
#  Step 6: Index survival manuals
# ============================================================
step "6/6  Indexing survival manuals"

PDF_COUNT=$(find manuales/ -name "*.pdf" 2>/dev/null | wc -l)
if [ "$PDF_COUNT" -eq 0 ]; then
    warn "No se encontraron PDFs en manuales/. Agrega tus manuales y ejecuta: ./refugia index"
else
    info "Indexando $PDF_COUNT manual(es) PDF..."
    "$PYTHON_CMD" src/indexador.py
    ok "Manuales indexados"
fi

# ============================================================
#  Done!
# ============================================================
echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}${BOLD}  INSTALACIÓN COMPLETADA / INSTALLATION COMPLETE${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "  Para iniciar RefugIA OS:"
echo -e "  ${AMBER}./refugia start${NC}"
echo ""
echo -e "  To start RefugIA OS:"
echo -e "  ${AMBER}./refugia start${NC}"
echo ""
echo -e "  Otros comandos / Other commands:"
echo -e "    ./refugia status   — Estado del sistema / System status"
echo -e "    ./refugia index    — Re-indexar manuales / Re-index manuals"
echo -e "    ./refugia doctor   — Diagnosticar problemas / Diagnose issues"
echo ""
