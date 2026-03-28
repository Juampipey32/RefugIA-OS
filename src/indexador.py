# ============================================================
# RefugIA OS — Indexador de Manuales de Supervivencia
# ============================================================
# Este script lee todos los PDFs de la carpeta /manuales/,
# los fragmenta en chunks optimizados para modelos ligeros,
# genera embeddings con all-MiniLM-L6-v2 y los almacena
# en una base de datos vectorial ChromaDB persistente.
#
# Uso:  python src/indexador.py
# ============================================================

import os
import sys
import shutil
from pathlib import Path

# --- Langchain: Carga de documentos PDF ---
from langchain_community.document_loaders import PyPDFDirectoryLoader

# --- Langchain: Fragmentación de texto ---
from langchain.text_splitter import RecursiveCharacterTextSplitter

# --- Langchain: Base de datos vectorial ---
from langchain_community.vectorstores import Chroma

# --- Langchain: Embeddings ligeros de HuggingFace ---
from langchain_huggingface import HuggingFaceEmbeddings


# ============================================================
#  CONFIGURACIÓN
# ============================================================

# Ruta base del proyecto (un nivel arriba de /src/)
BASE_DIR = Path(__file__).resolve().parent.parent

# Carpeta donde el usuario coloca sus PDFs de supervivencia
MANUALES_DIR = BASE_DIR / "manuales"

# Carpeta donde ChromaDB persistirá los vectores
CHROMA_DB_DIR = BASE_DIR / "src" / "db"

# Modelo de embeddings ligero (~80MB, funciona offline después
# de la primera descarga). Ideal para hardware limitado.
EMBEDDING_MODEL = "all-MiniLM-L6-v2"

# Configuración del splitter de texto.
# chunk_size pequeño (500 caracteres) para que los fragmentos
# sean manejables por modelos ligeros como phi3/llama3.
# chunk_overlap de 50 para mantener contexto entre fragmentos.
CHUNK_SIZE = 500
CHUNK_OVERLAP = 50


def verificar_manuales() -> bool:
    """
    Verifica que la carpeta de manuales existe y contiene
    al menos un archivo PDF.
    """
    if not MANUALES_DIR.exists():
        print(f"[ERROR] La carpeta de manuales no existe: {MANUALES_DIR}")
        print("[INFO]  Crea la carpeta y coloca tus PDFs de supervivencia ahí.")
        return False

    pdfs = list(MANUALES_DIR.glob("*.pdf"))
    if not pdfs:
        print(f"[AVISO] No se encontraron PDFs en: {MANUALES_DIR}")
        print("[INFO]  Coloca al menos un manual en formato PDF.")
        return False

    print(f"[OK] Se encontraron {len(pdfs)} archivo(s) PDF:")
    for pdf in pdfs:
        # Mostrar tamaño en MB para referencia
        size_mb = pdf.stat().st_size / (1024 * 1024)
        print(f"     📄 {pdf.name} ({size_mb:.1f} MB)")
    return True


def cargar_documentos():
    """
    Carga todos los PDFs de la carpeta /manuales/ usando
    PyPDFDirectoryLoader de Langchain.
    Retorna una lista de objetos Document.
    """
    print("\n[1/4] Cargando documentos PDF...")

    loader = PyPDFDirectoryLoader(str(MANUALES_DIR))
    documentos = loader.load()

    if not documentos:
        print("[ERROR] No se pudo extraer texto de los PDFs.")
        print("[INFO]  Verifica que los PDFs no estén protegidos o corruptos.")
        sys.exit(1)

    total_chars = sum(len(doc.page_content) for doc in documentos)
    print(f"[OK] {len(documentos)} páginas cargadas ({total_chars:,} caracteres)")
    return documentos


def fragmentar_documentos(documentos: list) -> list:
    """
    Fragmenta los documentos en chunks pequeños optimizados
    para modelos ligeros (phi3, llama3, etc.).
    Usa RecursiveCharacterTextSplitter que respeta límites
    de párrafos, oraciones y palabras.
    """
    print(f"\n[2/4] Fragmentando texto (chunk_size={CHUNK_SIZE})...")

    splitter = RecursiveCharacterTextSplitter(
        chunk_size=CHUNK_SIZE,
        chunk_overlap=CHUNK_OVERLAP,
        # Separadores ordenados por prioridad: primero intenta
        # cortar por doble salto de línea, luego salto simple,
        # luego espacio, y finalmente por carácter.
        separators=["\n\n", "\n", " ", ""],
        length_function=len,
    )

    fragmentos = splitter.split_documents(documentos)

    print(f"[OK] {len(fragmentos)} fragmentos generados")
    # Mostrar estadísticas de los fragmentos
    lengths = [len(f.page_content) for f in fragmentos]
    print(f"     Tamaño promedio: {sum(lengths)/len(lengths):.0f} chars")
    print(f"     Tamaño mínimo:   {min(lengths)} chars")
    print(f"     Tamaño máximo:   {max(lengths)} chars")

    return fragmentos


def crear_embeddings():
    """
    Inicializa el modelo de embeddings de HuggingFace.
    all-MiniLM-L6-v2: modelo ligero (~80MB), 384 dimensiones.
    Perfecto para hardware limitado y escenarios offline.
    """
    print(f"\n[3/4] Cargando modelo de embeddings: {EMBEDDING_MODEL}...")
    print("[INFO] La primera vez descargará el modelo (~80MB).")
    print("[INFO] Después funcionará 100% offline.")

    embeddings = HuggingFaceEmbeddings(
        model_name=EMBEDDING_MODEL,
        model_kwargs={"device": "cpu"},  # Forzar CPU para compatibilidad
        encode_kwargs={"normalize_embeddings": True},  # Normalizar para mejor búsqueda
    )

    print("[OK] Modelo de embeddings cargado correctamente")
    return embeddings


def crear_base_vectorial(fragmentos: list, embeddings) -> None:
    """
    Crea (o recrea) la base de datos vectorial ChromaDB
    con los fragmentos indexados.
    Si ya existe una DB previa, la elimina y crea una nueva
    para garantizar datos frescos.
    """
    print(f"\n[4/4] Creando base de datos vectorial en: {CHROMA_DB_DIR}...")

    # Si existe una DB previa, eliminarla para reconstruir desde cero
    if CHROMA_DB_DIR.exists():
        print("[INFO] Eliminando base de datos anterior...")
        shutil.rmtree(CHROMA_DB_DIR)

    # Crear la base de datos vectorial con ChromaDB
    # persist_directory hace que los datos sobrevivan reinicios
    vectorstore = Chroma.from_documents(
        documents=fragmentos,
        embedding=embeddings,
        persist_directory=str(CHROMA_DB_DIR),
        collection_name="manuales_supervivencia",
    )

    # Verificar que los documentos se guardaron correctamente
    count = vectorstore._collection.count()
    print(f"[OK] Base de datos creada con {count} vectores")
    print(f"[OK] Persistida en: {CHROMA_DB_DIR}")


def main():
    """
    Pipeline principal de indexación.
    """
    print("=" * 60)
    print("  RefugIA OS — Indexador de Manuales de Supervivencia")
    print("=" * 60)
    print(f"  Directorio de manuales: {MANUALES_DIR}")
    print(f"  Base de datos destino:  {CHROMA_DB_DIR}")
    print("=" * 60)

    # Paso 0: Verificar que hay PDFs disponibles
    if not verificar_manuales():
        print("\n[ABORTADO] No hay manuales para indexar.")
        print("[AYUDA]    Coloca archivos PDF en la carpeta 'manuales/'")
        sys.exit(1)

    # Paso 1: Cargar PDFs
    documentos = cargar_documentos()

    # Paso 2: Fragmentar en chunks
    fragmentos = fragmentar_documentos(documentos)

    # Paso 3: Preparar modelo de embeddings
    embeddings = crear_embeddings()

    # Paso 4: Crear base de datos vectorial
    crear_base_vectorial(fragmentos, embeddings)

    # Resumen final
    print("\n" + "=" * 60)
    print("  ✅ INDEXACIÓN COMPLETADA EXITOSAMENTE")
    print("=" * 60)
    print("  Ahora puedes iniciar el servidor con:")
    print("    python src/agente_api.py")
    print("=" * 60)


if __name__ == "__main__":
    main()
