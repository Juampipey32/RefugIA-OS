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

# Desactivar la telemetría de ChromaDB (evita spam de errores en consola
# y mantiene el sistema 100% offline). Debe ir antes de importar chroma.
os.environ.setdefault("ANONYMIZED_TELEMETRY", "False")

import sys
import shutil
import logging
from pathlib import Path

# Silenciar la telemetría de ChromaDB (incompatible con posthog en algunas
# versiones; escupe errores ruidosos pero inofensivos).
logging.getLogger("chromadb.telemetry").setLevel(logging.CRITICAL)

# --- Langchain: Carga de documentos ---
from langchain_community.document_loaders import PyPDFLoader
from langchain_core.documents import Document

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


# Extensiones de manual soportadas. Además de PDF, aceptamos texto plano
# y Markdown: ideal para contenido curado en español sin el ruido de
# extracción que traen muchos PDFs.
EXTENSIONES = ("*.pdf", "*.md", "*.txt")


def _listar_manuales() -> list:
    """Devuelve la lista ordenada de manuales soportados en la carpeta."""
    archivos = []
    for patron in EXTENSIONES:
        archivos.extend(MANUALES_DIR.glob(patron))
    return sorted(archivos)


def verificar_manuales() -> bool:
    """
    Verifica que la carpeta de manuales existe y contiene al menos un
    archivo soportado (PDF, Markdown o TXT).
    """
    if not MANUALES_DIR.exists():
        print(f"[ERROR] La carpeta de manuales no existe: {MANUALES_DIR}")
        print("[INFO]  Crea la carpeta y coloca tus manuales (PDF/MD/TXT) ahí.")
        return False

    archivos = _listar_manuales()
    if not archivos:
        print(f"[AVISO] No se encontraron manuales (PDF/MD/TXT) en: {MANUALES_DIR}")
        print("[INFO]  Coloca al menos un manual.")
        return False

    print(f"[OK] Se encontraron {len(archivos)} manual(es):")
    for f in archivos:
        size_kb = f.stat().st_size / 1024
        print(f"     📄 {f.name} ({size_kb:.0f} KB)")
    return True


def _es_pdf_valido(ruta: Path) -> bool:
    """
    Comprueba que el archivo empieza con la cabecera mágica '%PDF'.
    Evita que descargas fallidas (páginas HTML guardadas como .pdf)
    rompan la indexación completa.
    """
    try:
        with open(ruta, "rb") as fh:
            return fh.read(5).startswith(b"%PDF")
    except OSError:
        return False


def _cargar_texto(ruta: Path) -> list:
    """Carga un archivo .md/.txt como un único Document UTF-8."""
    texto = ruta.read_text(encoding="utf-8", errors="ignore")
    if not texto.strip():
        return []
    return [Document(page_content=texto, metadata={"source": str(ruta)})]


def cargar_documentos():
    """
    Carga todos los manuales (PDF/MD/TXT) de la carpeta /manuales/ uno
    por uno. Si un archivo está corrupto o vacío, se omite con un aviso
    en lugar de abortar toda la indexación.
    Retorna una lista de objetos Document.
    """
    print("\n[1/4] Cargando manuales...")

    archivos = _listar_manuales()
    documentos = []
    omitidos = []

    for f in archivos:
        try:
            if f.suffix.lower() == ".pdf":
                if not _es_pdf_valido(f):
                    print(f"[AVISO] '{f.name}' no es un PDF válido (cabecera incorrecta). Omitido.")
                    omitidos.append(f.name)
                    continue
                partes = PyPDFLoader(str(f)).load()
            else:
                partes = _cargar_texto(f)

            if not partes:
                print(f"[AVISO] '{f.name}' está vacío. Omitido.")
                omitidos.append(f.name)
                continue

            documentos.extend(partes)
            print(f"     ✅ {f.name}: {len(partes)} sección(es)")
        except Exception as e:
            print(f"[AVISO] No se pudo leer '{f.name}': {e}. Omitido.")
            omitidos.append(f.name)

    if omitidos:
        print(f"[INFO] {len(omitidos)} archivo(s) omitido(s): {', '.join(omitidos)}")

    if not documentos:
        print("[ERROR] No se pudo extraer texto de ningún manual.")
        print("[INFO]  Verifica que los archivos no estén protegidos o corruptos.")
        sys.exit(1)

    total_chars = sum(len(doc.page_content) for doc in documentos)
    print(f"[OK] {len(documentos)} sección(es) cargada(s) ({total_chars:,} caracteres)")
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

    # Recrear el .gitkeep para que la carpeta siga versionada en git
    # (rmtree de arriba lo elimina en cada reindexación).
    (CHROMA_DB_DIR / ".gitkeep").write_text(
        "# ChromaDB almacenará los vectores aquí automáticamente\n"
    )

    # Verificar que los documentos se guardaron correctamente
    count = vectorstore._collection.count()
    print(f"[OK] Base de datos creada con {count} vectores")
    print(f"[OK] Persistida en: {CHROMA_DB_DIR}")
    return count


def construir_indice() -> int:
    """
    Ejecuta el pipeline completo de indexación y devuelve la cantidad de
    vectores creados (0 si no hay PDFs para indexar).

    A diferencia de main(), NO llama a sys.exit, por lo que puede invocarse
    de forma segura desde otros módulos (p. ej. el auto-indexado del
    servidor al arrancar).
    """
    print("=" * 60)
    print("  RefugIA OS — Indexador de Manuales de Supervivencia")
    print("=" * 60)
    print(f"  Directorio de manuales: {MANUALES_DIR}")
    print(f"  Base de datos destino:  {CHROMA_DB_DIR}")
    print("=" * 60)

    # Paso 0: Verificar que hay PDFs disponibles
    if not verificar_manuales():
        return 0

    # Paso 1: Cargar PDFs
    documentos = cargar_documentos()

    # Paso 2: Fragmentar en chunks
    fragmentos = fragmentar_documentos(documentos)

    # Paso 3: Preparar modelo de embeddings
    embeddings = crear_embeddings()

    # Paso 4: Crear base de datos vectorial
    return crear_base_vectorial(fragmentos, embeddings)


def main():
    """
    Pipeline principal de indexación (entrada por línea de comandos).
    """
    count = construir_indice()

    if count == 0:
        print("\n[ABORTADO] No hay manuales para indexar.")
        print("[AYUDA]    Coloca archivos PDF en la carpeta 'manuales/'")
        sys.exit(1)

    # Resumen final
    print("\n" + "=" * 60)
    print("  ✅ INDEXACIÓN COMPLETADA EXITOSAMENTE")
    print("=" * 60)
    print("  Ahora puedes iniciar el servidor con:")
    print("    python src/agente_api.py")
    print("=" * 60)


if __name__ == "__main__":
    main()
