# ============================================================
# RefugIA OS — Backend API (FastAPI + LangChain + Ollama)
# ============================================================
# Servidor FastAPI que expone un agente de IA de supervivencia.
# Usa RAG (Retrieval-Augmented Generation) para responder
# preguntas basándose en los manuales PDF indexados.
#
# El LLM corre 100% local vía Ollama (phi3 o llama3).
# La base vectorial es ChromaDB con embeddings MiniLM.
#
# Uso:  python src/agente_api.py
# URL:  http://127.0.0.1:8000
# ============================================================

import os

# Desactivar la telemetría de ChromaDB (evita spam de errores en consola
# y mantiene el sistema 100% offline). Debe ir antes de importar chroma.
os.environ.setdefault("ANONYMIZED_TELEMETRY", "False")

import sys
import logging
from pathlib import Path
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from pydantic import BaseModel

# --- LangChain ---
from langchain_community.vectorstores import Chroma
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_ollama import OllamaLLM
from langchain.chains import RetrievalQA
from langchain.prompts import PromptTemplate

# ============================================================
#  CONFIGURACIÓN
# ============================================================

# Directorio base del proyecto
BASE_DIR = Path(__file__).resolve().parent.parent

# Ruta a la base de datos vectorial creada por indexador.py
CHROMA_DB_DIR = BASE_DIR / "src" / "db"

# Ruta al frontend
FRONTEND_DIR = BASE_DIR / "frontend"

# Modelo de embeddings (debe ser el MISMO que usó el indexador)
EMBEDDING_MODEL = "all-MiniLM-L6-v2"

# Modelo de LLM local vía Ollama
# Opciones: "phi3", "llama3", "mistral", "gemma2"
# El usuario debe tener Ollama instalado y el modelo descargado
OLLAMA_MODEL = os.environ.get("REFUGIA_MODEL", "phi3")

# URL del servidor Ollama (por defecto local)
OLLAMA_BASE_URL = os.environ.get("OLLAMA_URL", "http://localhost:11434")

# Puerto del servidor FastAPI
API_PORT = int(os.environ.get("REFUGIA_PORT", "8000"))

# Configuración de logging
logging.basicConfig(
    level=logging.INFO,
    format="[%(asctime)s] [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("RefugIA")

# Silenciar la telemetría de ChromaDB. En algunas versiones de chromadb el
# cliente de telemetría es incompatible con posthog y escupe errores ruidosos
# ("Failed to send telemetry event ...") aunque esté desactivada. No afecta
# al funcionamiento, así que callamos su logger.
logging.getLogger("chromadb.telemetry").setLevel(logging.CRITICAL)


# ============================================================
#  PROMPT DE SISTEMA — Personalidad del Agente
# ============================================================

REFUGIA_PROMPT = PromptTemplate(
    template="""Eres RefugIA, un asistente de supervivencia offline diseñado para escenarios post-apocalípticos.

REGLAS ESTRICTAS:
1. Responde SOLO usando la información del contexto proporcionado.
2. Sé conciso y directo. Las vidas dependen de instrucciones claras.
3. NUNCA sugieras llamar a emergencias, hospitales o servicios externos. No existen.
4. Si no tienes información suficiente en el contexto, di: "No tengo datos sobre eso en mis manuales. Describe tu situación con más detalle."
5. Prioriza la seguridad del usuario. Si algo es peligroso, advierte claramente.
6. Responde en español.
7. Usa formato de lista o pasos numerados cuando sea apropiado.

CONTEXTO DE LOS MANUALES:
{context}

PREGUNTA DEL SUPERVIVIENTE:
{question}

RESPUESTA DE REFUGIA:""",
    input_variables=["context", "question"],
)


# ============================================================
#  ESTADO GLOBAL DE LA APLICACIÓN
# ============================================================

# Variable global para la cadena RAG
qa_chain = None

# Vectorstore global — se reutiliza en /api/status para evitar recargar el modelo
vectorstore = None

# Flag para saber si estamos en modo degradado
modo_degradado = False


def _get_vector_count() -> int:
    """Devuelve la cantidad de vectores almacenados reutilizando el vectorstore global."""
    if vectorstore is None:
        return 0
    try:
        return vectorstore._collection.count()
    except AttributeError:
        # Fallback para versiones futuras de ChromaDB que remuevan _collection
        return len(vectorstore.get()["ids"])


def inicializar_rag():
    """
    Inicializa la cadena RAG completa:
    1. Carga embeddings
    2. Conecta a ChromaDB
    3. Configura Ollama LLM
    4. Crea la cadena RetrievalQA

    Si la DB no existe, entra en modo degradado sin crashear.
    """
    global qa_chain, vectorstore, modo_degradado

    # --- Verificar si existe la base de datos vectorial ---
    if not CHROMA_DB_DIR.exists() or not any(CHROMA_DB_DIR.iterdir()):
        logger.warning("=" * 50)
        logger.warning("  ⚠️  BASE DE DATOS VECTORIAL NO ENCONTRADA")
        logger.warning(f"  Ruta esperada: {CHROMA_DB_DIR}")
        logger.warning("  El servidor iniciará en MODO DEGRADADO.")
        logger.warning("  Para indexar manuales ejecuta:")
        logger.warning("    python src/indexador.py")
        logger.warning("=" * 50)
        modo_degradado = True
        return

    try:
        # --- Paso 1: Cargar modelo de embeddings ---
        logger.info(f"Cargando embeddings: {EMBEDDING_MODEL}...")
        embeddings = HuggingFaceEmbeddings(
            model_name=EMBEDDING_MODEL,
            model_kwargs={"device": "cpu"},
            encode_kwargs={"normalize_embeddings": True},
        )

        # --- Paso 2: Conectar a ChromaDB existente ---
        logger.info(f"Conectando a ChromaDB: {CHROMA_DB_DIR}...")
        vectorstore = Chroma(
            persist_directory=str(CHROMA_DB_DIR),
            embedding_function=embeddings,
            collection_name="manuales_supervivencia",
        )

        # Verificar que hay documentos
        doc_count = _get_vector_count()
        if doc_count == 0:
            logger.warning("ChromaDB está vacía. Ejecuta el indexador primero.")
            modo_degradado = True
            return

        logger.info(f"ChromaDB conectada: {doc_count} vectores disponibles")

        # --- Paso 3: Configurar LLM local (Ollama) ---
        logger.info(f"Conectando a Ollama (modelo: {OLLAMA_MODEL})...")
        llm = OllamaLLM(
            model=OLLAMA_MODEL,
            base_url=OLLAMA_BASE_URL,
            temperature=0.3,  # Baja temperatura para respuestas más precisas
            num_predict=512,  # Limitar tokens para respuestas concisas
        )

        # --- Paso 4: Crear cadena RetrievalQA ---
        logger.info("Construyendo cadena RAG...")
        retriever = vectorstore.as_retriever(
            search_type="similarity",
            search_kwargs={
                "k": 4,  # Recuperar 4 fragmentos más relevantes
            },
        )

        qa_chain = RetrievalQA.from_chain_type(
            llm=llm,
            chain_type="stuff",  # "stuff" mete todo el contexto en un prompt
            retriever=retriever,
            chain_type_kwargs={"prompt": REFUGIA_PROMPT},
            return_source_documents=False,
        )

        modo_degradado = False
        logger.info("✅ Cadena RAG inicializada correctamente")

    except Exception as e:
        logger.error(f"Error al inicializar RAG: {e}")
        logger.warning("El servidor continuará en MODO DEGRADADO")
        modo_degradado = True


# ============================================================
#  CICLO DE VIDA DE LA APLICACIÓN
# ============================================================

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Maneja el ciclo de vida del servidor.
    Inicializa RAG al arrancar, limpia al cerrar.
    """
    # --- Startup ---
    logger.info("=" * 50)
    logger.info("  🛡️  RefugIA OS — Iniciando servidor...")
    logger.info("=" * 50)
    inicializar_rag()

    if modo_degradado:
        logger.info("Servidor en MODO DEGRADADO (sin RAG)")
    else:
        logger.info(f"Servidor OPERATIVO en http://127.0.0.1:{API_PORT}")

    logger.info("=" * 50)

    yield  # Servidor activo

    # --- Shutdown ---
    logger.info("RefugIA OS — Servidor detenido. Buena suerte ahí fuera.")


# ============================================================
#  APLICACIÓN FASTAPI
# ============================================================

app = FastAPI(
    title="RefugIA OS",
    description="Sistema Operativo de Supervivencia — API de IA Local",
    version="1.0.0",
    lifespan=lifespan,
)

# --- CORS: Permitir peticiones locales del frontend ---
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # En producción offline, esto es seguro
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ============================================================
#  MODELOS DE DATOS
# ============================================================

class ChatRequest(BaseModel):
    """Modelo para las peticiones de chat entrantes."""
    query: str


class ChatResponse(BaseModel):
    """Modelo para las respuestas del chat."""
    respuesta: str
    modo: str  # "rag" o "degradado"


class StatusResponse(BaseModel):
    """Modelo para el endpoint de estado."""
    status: str
    modo: str
    modelo_llm: str
    vectores: int


# ============================================================
#  ENDPOINTS
# ============================================================

@app.post("/api/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """
    Endpoint principal de chat.
    Recibe una pregunta y devuelve la respuesta del agente.

    Si el sistema está en modo degradado (sin RAG), responde
    con un mensaje indicando que los manuales no están cargados.
    """
    query = request.query.strip()

    if not query:
        raise HTTPException(status_code=400, detail="La consulta no puede estar vacía")

    logger.info(f"Consulta recibida: {query[:80]}...")

    # --- Modo degradado: sin RAG disponible ---
    if modo_degradado or qa_chain is None:
        logger.warning("Respondiendo en modo degradado (sin RAG)")
        respuesta = (
            "⚠️ RefugIA está en MODO DEGRADADO. "
            "Los manuales de supervivencia no están indexados. "
            "Ejecuta 'python src/indexador.py' para activar la IA completa.\n\n"
            "Mientras tanto, puedo darte estos consejos básicos:\n"
            "• Agua: Hierve durante 1 minuto mínimo. Filtra con tela si está turbia.\n"
            "• Refugio: Prioriza protección del viento y lluvia. Usa materiales aislantes.\n"
            "• Heridas: Limpia con agua limpia, aplica presión si sangra, cubre con tela limpia.\n"
            "• Fuego: Reúne yesca seca, leña fina y gruesa ANTES de intentar encender."
        )
        return ChatResponse(respuesta=respuesta, modo="degradado")

    # --- Modo RAG: consulta completa al agente ---
    try:
        resultado = qa_chain.invoke({"query": query})

        # RetrievalQA devuelve un dict con 'result' como clave
        respuesta = resultado.get("result", "No pude generar una respuesta.")

        logger.info(f"Respuesta generada ({len(respuesta)} chars)")
        return ChatResponse(respuesta=respuesta, modo="rag")

    except Exception as e:
        logger.error(f"Error en la cadena RAG: {e}")
        # No crashear, devolver error amigable
        return ChatResponse(
            respuesta=(
                "⚠️ Error al procesar tu consulta. "
                "Verifica que Ollama esté corriendo con: ollama run "
                f"{OLLAMA_MODEL}\n\n"
                f"Error técnico: {str(e)[:200]}"
            ),
            modo="error",
        )


@app.get("/api/status", response_model=StatusResponse)
async def status():
    """
    Endpoint de estado del sistema.
    Útil para que el frontend sepa si el backend está operativo.
    Reutiliza el vectorstore global — no recarga el modelo de embeddings.
    """
    vectores = 0
    if not modo_degradado:
        try:
            vectores = _get_vector_count()
        except Exception:
            pass

    return StatusResponse(
        status="operativo" if not modo_degradado else "degradado",
        modo="rag" if not modo_degradado else "degradado",
        modelo_llm=OLLAMA_MODEL,
        vectores=vectores,
    )


# ============================================================
#  SERVIR FRONTEND ESTÁTICO
# ============================================================

# Servir el archivo index.html desde la carpeta /frontend/
@app.get("/")
async def serve_frontend():
    """Sirve la interfaz web del frontend."""
    index_path = FRONTEND_DIR / "index.html"
    if index_path.exists():
        return FileResponse(str(index_path))
    else:
        return {"mensaje": "Frontend no encontrado. Coloca index.html en /frontend/"}


# --- PWA: Service Worker, Manifest, Icons ---

@app.get("/manifest.json")
async def serve_manifest():
    """Sirve el manifest PWA."""
    path = FRONTEND_DIR / "manifest.json"
    if path.exists():
        return FileResponse(str(path), media_type="application/manifest+json")
    return {"error": "manifest.json not found"}


@app.get("/sw.js")
async def serve_sw():
    """Sirve el Service Worker (debe estar en la raíz para scope '/')."""
    path = FRONTEND_DIR / "sw.js"
    if path.exists():
        return FileResponse(str(path), media_type="application/javascript")
    return {"error": "sw.js not found"}


@app.get("/icons/{filename}")
async def serve_icon(filename: str):
    """Sirve los iconos de la PWA."""
    path = FRONTEND_DIR / "icons" / filename
    if path.exists():
        media_type = "image/svg+xml" if filename.endswith(".svg") else "image/png"
        return FileResponse(str(path), media_type=media_type)
    return {"error": f"Icon {filename} not found"}


# ============================================================
#  PUNTO DE ENTRADA
# ============================================================

if __name__ == "__main__":
    import uvicorn

    logger.info(f"Modelo LLM configurado: {OLLAMA_MODEL}")
    logger.info(f"Ollama URL: {OLLAMA_BASE_URL}")
    logger.info(f"Puerto: {API_PORT}")

    uvicorn.run(
        "agente_api:app",
        host="0.0.0.0",       # Accesible desde cualquier dispositivo en la red local
        port=API_PORT,
        reload=False,          # Sin hot-reload en producción
        log_level="info",
        access_log=True,
    )
