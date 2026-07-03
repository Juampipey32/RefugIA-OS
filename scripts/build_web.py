# ============================================================
# RefugIA OS — Generador de datos para la web de emergencia
# ============================================================
# Lee los manuales Markdown de /manuales/ y genera docs/data.js
# con las secciones estructuradas que consume docs/index.html.
#
# Uso:  python scripts/build_web.py
# ============================================================

import json
import re
import sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
MANUALES_DIR = BASE_DIR / "manuales"
OUT_FILE = BASE_DIR / "docs" / "data.js"

# Orden de prioridad en la web (lo más urgente primero).
# Manuales no listados van al final, en orden alfabético.
ORDEN = [
    "terremotos.md",
    "primeros_auxilios.md",
    "agua_potable.md",
    "refugio.md",
    "fuego.md",
    "comida_y_navegacion.md",
]

ICONOS = {
    "terremotos.md": "🌍",
    "primeros_auxilios.md": "🩹",
    "agua_potable.md": "💧",
    "refugio.md": "⛺",
    "fuego.md": "🔥",
    "comida_y_navegacion.md": "🧭",
}


def listar_manuales() -> list[Path]:
    """Manuales .md en orden de prioridad definido arriba."""
    archivos = {f.name: f for f in MANUALES_DIR.glob("*.md")}
    ordenados = [archivos.pop(n) for n in ORDEN if n in archivos]
    ordenados.extend(archivos[n] for n in sorted(archivos))
    return ordenados


def parsear_manual(ruta: Path) -> dict:
    """
    Divide un manual en secciones por encabezado '## '.
    El primer '# ' es el título del manual; el texto antes del
    primer '## ' se guarda como introducción.
    """
    texto = ruta.read_text(encoding="utf-8")
    lineas = texto.splitlines()

    titulo = ruta.stem.replace("_", " ").title()
    secciones = []
    actual = {"titulo": "Introducción", "texto": []}

    for linea in lineas:
        if linea.startswith("# ") and not secciones and not actual["texto"]:
            titulo = linea[2:].strip()
        elif linea.startswith("## "):
            if actual["texto"] and "".join(actual["texto"]).strip():
                secciones.append(actual)
            actual = {"titulo": linea[3:].strip(), "texto": []}
        else:
            actual["texto"].append(linea)

    if actual["texto"] and "".join(actual["texto"]).strip():
        secciones.append(actual)

    return {
        "id": ruta.stem,
        "titulo": titulo,
        "icono": ICONOS.get(ruta.name, "📖"),
        "secciones": [
            {"titulo": s["titulo"], "texto": "\n".join(s["texto"]).strip()}
            for s in secciones
        ],
    }


def main() -> None:
    # La consola de Windows (cp1252) no soporta emojis; evitar crash al imprimir.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")

    archivos = listar_manuales()
    if not archivos:
        print(f"[ERROR] No hay manuales .md en {MANUALES_DIR}")
        sys.exit(1)

    manuales = [parsear_manual(f) for f in archivos]
    total_secciones = sum(len(m["secciones"]) for m in manuales)

    OUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(manuales, ensure_ascii=False, separators=(",", ":"))
    OUT_FILE.write_text(
        "// Generado por scripts/build_web.py — NO editar a mano.\n"
        "// Fuente: manuales/*.md\n"
        f"window.REFUGIA_DATA = {payload};\n",
        encoding="utf-8",
    )

    kb = OUT_FILE.stat().st_size / 1024
    print(f"[OK] {len(manuales)} manuales, {total_secciones} secciones -> {OUT_FILE} ({kb:.0f} KB)")
    for m in manuales:
        print(f"     {m['icono']} {m['titulo']}: {len(m['secciones'])} secciones")


if __name__ == "__main__":
    main()
