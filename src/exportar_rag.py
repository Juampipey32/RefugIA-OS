# ============================================================
# RefugIA OS — Exportador de RAG para móvil (build-time)
# ============================================================
# Pre-calcula los chunks + embeddings de los manuales y los
# guarda en un único JSON que la app móvil (APK) embebe como
# asset. Así NO hace falta ChromaDB ni indexar en el teléfono:
# la búsqueda por similitud se hace en memoria sobre este JSON.
#
# Reutiliza EXACTAMENTE el mismo troceado y el mismo modelo de
# embeddings que el indexador del servidor, para que las
# respuestas sean equivalentes en PC y en móvil.
#
# Uso:  python src/exportar_rag.py
# Salida: mobile/assets/rag_index.json
# ============================================================

import os

# Misma config de telemetría/offline que el resto del proyecto.
os.environ.setdefault("ANONYMIZED_TELEMETRY", "False")

import sys
import json
import hashlib
from pathlib import Path

# Reutilizamos la lógica ya probada del indexador del servidor.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from indexador import (
    cargar_documentos,
    fragmentar_documentos,
    verificar_manuales,
    EMBEDDING_MODEL,
    CHUNK_SIZE,
    CHUNK_OVERLAP,
)

from langchain_huggingface import HuggingFaceEmbeddings

BASE_DIR = Path(__file__).resolve().parent.parent
# Se escribe directo en la carpeta que Capacitor empaqueta (mobile/www/assets).
OUTPUT_DIR = BASE_DIR / "mobile" / "www" / "assets"
OUTPUT_FILE = OUTPUT_DIR / "rag_index.json"

# Formato del índice. Si cambia la estructura, subir la versión para
# que la app pueda detectar incompatibilidades.
INDEX_VERSION = 1


def _redondear(vec, decimales: int = 5):
    """
    Redondea los floats del embedding para achicar el JSON sin
    perder precisión útil (5 decimales es más que suficiente para
    búsqueda por coseno con vectores normalizados).
    """
    return [round(float(x), decimales) for x in vec]


def main():
    print("=" * 60)
    print("  RefugIA OS — Exportador de RAG para móvil")
    print("=" * 60)
    print(f"  Modelo embeddings: {EMBEDDING_MODEL}")
    print(f"  chunk_size={CHUNK_SIZE}  overlap={CHUNK_OVERLAP}")
    print(f"  Salida: {OUTPUT_FILE}")
    print("=" * 60)

    if not verificar_manuales():
        print("\n[ABORTADO] No hay manuales para exportar.")
        sys.exit(1)

    # 1) Cargar y trocear (idéntico al servidor)
    documentos = cargar_documentos()
    fragmentos = fragmentar_documentos(documentos)

    # 2) Embeddings (mismo modelo que el servidor)
    print(f"\n[3/4] Calculando embeddings con {EMBEDDING_MODEL}...")
    embeddings = HuggingFaceEmbeddings(
        model_name=EMBEDDING_MODEL,
        model_kwargs={"device": "cpu"},
        encode_kwargs={"normalize_embeddings": True},  # normalizado → coseno = dot
    )
    textos = [f.page_content for f in fragmentos]
    vectores = embeddings.embed_documents(textos)
    dims = len(vectores[0]) if vectores else 0
    print(f"[OK] {len(vectores)} vectores de {dims} dimensiones")

    # 3) Armar el JSON portable
    print(f"\n[4/4] Escribiendo {OUTPUT_FILE}...")
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    items = []
    for frag, vec in zip(fragmentos, vectores):
        fuente = frag.metadata.get("source", "")
        items.append({
            "text": frag.page_content,
            "source": Path(fuente).name if fuente else "",
            "page": frag.metadata.get("page", None),
            "embedding": _redondear(vec),
        })

    payload = {
        "version": INDEX_VERSION,
        "embedding_model": EMBEDDING_MODEL,
        "dims": dims,
        "normalized": True,
        "count": len(items),
        "chunks": items,
    }

    with open(OUTPUT_FILE, "w", encoding="utf-8") as fh:
        json.dump(payload, fh, ensure_ascii=False, separators=(",", ":"))

    size_mb = OUTPUT_FILE.stat().st_size / (1024 * 1024)
    sha = hashlib.sha256(OUTPUT_FILE.read_bytes()).hexdigest()[:12]

    print("\n" + "=" * 60)
    print("  ✅ EXPORTACIÓN COMPLETADA")
    print("=" * 60)
    print(f"  Archivo:   {OUTPUT_FILE}")
    print(f"  Chunks:    {len(items)}")
    print(f"  Dims:      {dims}")
    print(f"  Tamaño:    {size_mb:.2f} MB")
    print(f"  SHA256:    {sha}…")
    print("=" * 60)
    print("  Este JSON se embebe en el APK como asset.")
    print("  La app hace búsqueda por coseno en memoria (sin ChromaDB).")
    print("=" * 60)


if __name__ == "__main__":
    main()
