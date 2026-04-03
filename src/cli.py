#!/usr/bin/env python3
# ============================================================
# RefugIA OS — CLI Interface
# ============================================================
# Main CLI entry point with subcommands:
#   refugia start   — Launch the server
#   refugia index   — Index PDF manuals
#   refugia status  — Show system status
#   refugia doctor  — Diagnose issues
# ============================================================

import argparse
import os
import sys
import subprocess
import shutil
import webbrowser
from pathlib import Path

# --- Project paths ---
BASE_DIR = Path(__file__).resolve().parent.parent
SRC_DIR = BASE_DIR / "src"
DB_DIR = SRC_DIR / "db"
MANUALES_DIR = BASE_DIR / "manuales"

# --- ANSI Colors ---
AMBER = "\033[0;33m"
GREEN = "\033[0;32m"
RED = "\033[0;31m"
CYAN = "\033[0;36m"
BOLD = "\033[1m"
DIM = "\033[2m"
NC = "\033[0m"

VERSION = "1.0.0"

BANNER = f"""{AMBER}
  ╔══════════════════════════════════════════╗
  ║                                          ║
  ║   ██████  ███████ ███████ ██    ██       ║
  ║   ██   ██ ██      ██      ██    ██       ║
  ║   ██████  █████   █████   ██    ██       ║
  ║   ██   ██ ██      ██      ██    ██       ║
  ║   ██   ██ ███████ ██       ██████        ║
  ║            {GREEN}╔══╗{AMBER}                          ║
  ║         {GREEN}IA{AMBER} ║{GREEN}OS{AMBER}║  {DIM}v{VERSION}{NC}{AMBER}                  ║
  ║            {GREEN}╚══╝{AMBER}                          ║
  ║                                          ║
  ║   {NC}{BOLD}Survival Operating System{NC}{AMBER}              ║
  ║   {DIM}"When the grid falls, knowledge{NC}{AMBER}        ║
  ║    {DIM}survives."{NC}{AMBER}                            ║
  ║                                          ║
  ╚══════════════════════════════════════════╝
{NC}"""


def print_banner():
    print(BANNER)


def check_ollama():
    """Check if Ollama is installed and running."""
    if not shutil.which("ollama"):
        return False, "not_installed"
    try:
        result = subprocess.run(
            ["ollama", "list"],
            capture_output=True, text=True, timeout=5
        )
        if result.returncode == 0:
            return True, result.stdout
        return False, "not_running"
    except (subprocess.TimeoutExpired, FileNotFoundError):
        return False, "not_running"


def check_db():
    """Check if ChromaDB has been indexed."""
    if not DB_DIR.exists():
        return False, 0
    db_files = list(DB_DIR.glob("*"))
    # Filter out .gitkeep
    db_files = [f for f in db_files if f.name != ".gitkeep"]
    return len(db_files) > 0, len(db_files)


def check_pdfs():
    """Check for PDF manuals."""
    if not MANUALES_DIR.exists():
        return []
    return list(MANUALES_DIR.glob("*.pdf"))


# ============================================================
#  Command: start
# ============================================================
def cmd_start(args):
    """Launch the RefugIA OS server."""
    print_banner()

    model = os.environ.get("REFUGIA_MODEL", "phi3")
    port = int(os.environ.get("REFUGIA_PORT", "8000"))

    print(f"  {CYAN}Model:{NC}  {model}")
    print(f"  {CYAN}Port:{NC}   {port}")
    print(f"  {CYAN}URL:{NC}    http://127.0.0.1:{port}")
    print()

    # Quick health checks
    ollama_ok, _ = check_ollama()
    db_ok, _ = check_db()

    if not ollama_ok:
        print(f"  {RED}[WARN]{NC} Ollama no detectado. El servidor iniciará en modo degradado.")
        print(f"        Instala Ollama: https://ollama.com/download")
        print()

    if not db_ok:
        print(f"  {RED}[WARN]{NC} Base de datos no indexada. Ejecuta: ./refugia index")
        print()

    print(f"  {GREEN}Iniciando servidor...{NC}")
    print(f"  {DIM}Presiona Ctrl+C para detener.{NC}")
    print()

    # Open browser after a short delay
    if not args.no_browser:
        import threading
        def open_browser():
            import time
            time.sleep(2)
            webbrowser.open(f"http://127.0.0.1:{port}")
        threading.Thread(target=open_browser, daemon=True).start()

    # Launch uvicorn
    os.chdir(SRC_DIR)
    sys.path.insert(0, str(SRC_DIR))

    import uvicorn
    uvicorn.run(
        "agente_api:app",
        host="0.0.0.0",
        port=port,
        reload=False,
        log_level="info",
        access_log=True,
    )


# ============================================================
#  Command: index
# ============================================================
def cmd_index(args):
    """Index PDF manuals into the vector database."""
    print_banner()
    print(f"  {CYAN}Indexando manuales de supervivencia...{NC}\n")

    sys.path.insert(0, str(SRC_DIR))
    from indexador import main as indexar
    indexar()


# ============================================================
#  Command: status
# ============================================================
def cmd_status(args):
    """Show system status."""
    print_banner()

    model = os.environ.get("REFUGIA_MODEL", "phi3")
    port = os.environ.get("REFUGIA_PORT", "8000")

    print(f"  {BOLD}System Status{NC}")
    print(f"  {'─' * 40}")

    # Ollama
    ollama_ok, ollama_info = check_ollama()
    if ollama_ok:
        print(f"  {GREEN}●{NC} Ollama        {GREEN}Running{NC}")
        # Check if model is available
        if model in ollama_info:
            print(f"  {GREEN}●{NC} Model         {GREEN}{model}{NC}")
        else:
            print(f"  {RED}●{NC} Model         {RED}{model} (not downloaded){NC}")
    elif ollama_info == "not_installed":
        print(f"  {RED}●{NC} Ollama        {RED}Not installed{NC}")
    else:
        print(f"  {RED}●{NC} Ollama        {RED}Not running{NC}")

    # Database
    db_ok, db_files = check_db()
    if db_ok:
        print(f"  {GREEN}●{NC} Vector DB     {GREEN}Indexed ({db_files} files){NC}")
    else:
        print(f"  {RED}●{NC} Vector DB     {RED}Not indexed{NC}")

    # PDFs
    pdfs = check_pdfs()
    if pdfs:
        print(f"  {GREEN}●{NC} Manuals       {GREEN}{len(pdfs)} PDF(s){NC}")
        for pdf in pdfs:
            size_mb = pdf.stat().st_size / (1024 * 1024)
            print(f"    {DIM}└ {pdf.name} ({size_mb:.1f} MB){NC}")
    else:
        print(f"  {RED}●{NC} Manuals       {RED}None found{NC}")

    # Config
    print(f"\n  {BOLD}Configuration{NC}")
    print(f"  {'─' * 40}")
    print(f"    Model:   {model}")
    print(f"    Port:    {port}")
    print(f"    Ollama:  {os.environ.get('OLLAMA_URL', 'http://localhost:11434')}")
    print()


# ============================================================
#  Command: doctor
# ============================================================
def cmd_doctor(args):
    """Diagnose common issues."""
    print_banner()
    print(f"  {BOLD}RefugIA Doctor — Diagnosing...{NC}\n")

    issues = []

    # Check Python version
    py_version = sys.version_info
    if py_version >= (3, 10):
        print(f"  {GREEN}✓{NC} Python {py_version.major}.{py_version.minor}.{py_version.micro}")
    else:
        print(f"  {RED}✗{NC} Python {py_version.major}.{py_version.minor} (need 3.10+)")
        issues.append("Upgrade Python to 3.10+")

    # Check key packages
    packages = ["fastapi", "langchain", "chromadb", "sentence_transformers"]
    for pkg in packages:
        try:
            __import__(pkg)
            print(f"  {GREEN}✓{NC} {pkg}")
        except ImportError:
            print(f"  {RED}✗{NC} {pkg} not installed")
            issues.append(f"Install {pkg}: pip install -r requirements.txt")

    # Check Ollama
    ollama_ok, ollama_info = check_ollama()
    if ollama_ok:
        print(f"  {GREEN}✓{NC} Ollama running")
    elif ollama_info == "not_installed":
        print(f"  {RED}✗{NC} Ollama not installed")
        issues.append("Install Ollama: curl -fsSL https://ollama.com/install.sh | sh")
    else:
        print(f"  {RED}✗{NC} Ollama not running")
        issues.append("Start Ollama: ollama serve")

    # Check model
    model = os.environ.get("REFUGIA_MODEL", "phi3")
    if ollama_ok and model in ollama_info:
        print(f"  {GREEN}✓{NC} Model {model} available")
    else:
        print(f"  {RED}✗{NC} Model {model} not available")
        issues.append(f"Download model: ollama pull {model}")

    # Check PDFs
    pdfs = check_pdfs()
    if pdfs:
        print(f"  {GREEN}✓{NC} {len(pdfs)} PDF manual(s) found")
    else:
        print(f"  {RED}✗{NC} No PDFs in manuales/")
        issues.append("Add PDF survival manuals to the manuales/ folder")

    # Check DB
    db_ok, _ = check_db()
    if db_ok:
        print(f"  {GREEN}✓{NC} Vector database indexed")
    else:
        print(f"  {RED}✗{NC} Vector database not indexed")
        issues.append("Index manuals: ./refugia index")

    # Summary
    print()
    if not issues:
        print(f"  {GREEN}{BOLD}All checks passed! RefugIA is ready.{NC}")
        print(f"  Run: ./refugia start")
    else:
        print(f"  {RED}{BOLD}Found {len(issues)} issue(s):{NC}")
        for i, issue in enumerate(issues, 1):
            print(f"    {i}. {issue}")
    print()


# ============================================================
#  Argument Parser
# ============================================================
def main():
    parser = argparse.ArgumentParser(
        prog="refugia",
        description="RefugIA OS — Offline Survival AI System",
    )
    parser.add_argument(
        "--version", action="version", version=f"RefugIA OS v{VERSION}"
    )

    subparsers = parser.add_subparsers(dest="command", help="Available commands")

    # start
    sp_start = subparsers.add_parser("start", help="Launch the RefugIA server")
    sp_start.add_argument(
        "--no-browser", action="store_true",
        help="Don't auto-open the browser"
    )

    # index
    subparsers.add_parser("index", help="Index PDF survival manuals")

    # status
    subparsers.add_parser("status", help="Show system status")

    # doctor
    subparsers.add_parser("doctor", help="Diagnose common issues")

    args = parser.parse_args()

    if args.command is None:
        print_banner()
        parser.print_help()
        print()
        return

    commands = {
        "start": cmd_start,
        "index": cmd_index,
        "status": cmd_status,
        "doctor": cmd_doctor,
    }
    commands[args.command](args)


if __name__ == "__main__":
    main()
