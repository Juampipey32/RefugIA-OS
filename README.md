<![CDATA[<div align="center">

# 🛡️ RefugIA OS

### Sistema Operativo de Supervivencia con IA Local

*Cuando la red cae, el conocimiento sobrevive.*

[![Python 3.10+](https://img.shields.io/badge/Python-3.10%2B-blue?logo=python)](https://python.org)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115-green?logo=fastapi)](https://fastapi.tiangolo.com)
[![Ollama](https://img.shields.io/badge/Ollama-Local%20LLM-orange)](https://ollama.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![100% Offline](https://img.shields.io/badge/Funciona-100%25%20Offline-red)]()

</div>

---

## 🌍 ¿Qué es RefugIA?

**RefugIA** es un sistema diseñado para funcionar **100% offline** en escenarios donde no hay internet, no hay red eléctrica y la única tecnología disponible es una computadora alimentada por energía solar.

Su núcleo es un **agente de IA local** (LLM vía Ollama) que usa **RAG** (Retrieval-Augmented Generation) para consultar manuales de supervivencia en PDF y darte instrucciones precisas sobre:

- 🩺 **Medicina táctica** — Primeros auxilios, suturas, tratamiento de infecciones
- 💧 **Purificación de agua** — Métodos de filtrado, destilación, químicos
- 🏗️ **Construcción de refugios** — Estructuras temporales, aislamiento térmico
- 🔥 **Fuego y calor** — Técnicas de encendido, tipos de fogatas
- 🥫 **Gestión de raciones** — Inventario de agua y comida con persistencia local

---

## 📐 Arquitectura

```
┌─────────────────────────────────────────────┐
│               FRONTEND (HTML/JS)            │
│         Interfaz CRT · Chat + Inventario    │
│              Puerto: 8000 (/)               │
└─────────────────┬───────────────────────────┘
                  │ POST /api/chat
┌─────────────────▼───────────────────────────┐
│              FastAPI Backend                 │
│         LangChain RetrievalQA Chain         │
├──────────┬──────────────────┬───────────────┤
│ ChromaDB │   HuggingFace   │  Ollama LLM   │
│ Vectores │   Embeddings    │  (phi3/llama3) │
│  (local) │ (all-MiniLM-L6) │   (local)     │
└──────────┴──────────────────┴───────────────┘
```

---

## ⚡ Requisitos de Hardware

| Componente | Mínimo | Recomendado |
|---|---|---|
| **RAM** | 4 GB | 8 GB+ |
| **CPU** | Dual Core | Quad Core |
| **Disco** | 2 GB libres | 5 GB+ |
| **GPU** | No necesaria | Opcional (acelera Ollama) |
| **SO** | Windows 10 / Linux / macOS | Cualquiera con Python 3.10+ |

---

## 🚀 Instalación Paso a Paso

### 1. Instalar Ollama

```bash
# Linux/macOS
curl -fsSL https://ollama.com/install.sh | sh

# Windows: descarga desde https://ollama.com/download
```

Descarga un modelo ligero:

```bash
ollama pull phi3
# O alternativa:
ollama pull llama3
```

### 2. Clonar el Repositorio

```bash
git clone https://github.com/tu-usuario/refugia-os.git
cd refugia-os
```

### 3. Crear Entorno Virtual

```bash
python -m venv venv

# Linux/macOS:
source venv/bin/activate

# Windows:
venv\Scripts\activate
```

### 4. Instalar Dependencias

```bash
pip install -r requirements.txt
```

### 5. Agregar Manuales PDF

Coloca tus PDFs de supervivencia en la carpeta `manuales/`:

```
manuales/
├── medicina_tactica.pdf
├── purificacion_agua.pdf
└── construccion_refugios.pdf
```

### 6. Indexar los Manuales

```bash
python src/indexador.py
```

Verás la salida del pipeline de indexación con estadísticas.

### 7. Iniciar el Servidor

```bash
python src/agente_api.py
```

### 8. Abrir la Interfaz

Abre tu navegador en: **http://127.0.0.1:8000**

---

## 🔧 Variables de Entorno

| Variable | Default | Descripción |
|---|---|---|
| `REFUGIA_MODEL` | `phi3` | Modelo de Ollama a usar |
| `OLLAMA_URL` | `http://localhost:11434` | URL del servidor Ollama |
| `REFUGIA_PORT` | `8000` | Puerto del servidor API |

---

## 📁 Estructura del Proyecto

```
RefugIA/
├── manuales/              # PDFs de supervivencia (los pone el usuario)
├── src/
│   ├── indexador.py        # Pipeline de indexación: PDF → ChromaDB
│   ├── agente_api.py       # Backend FastAPI + RAG Chain
│   └── db/                 # ChromaDB persiste los vectores aquí
├── frontend/
│   └── index.html          # UI completa: Chat IA + Inventario
├── requirements.txt        # Dependencias Python
└── README.md               # Este archivo
```

---

## 🛡️ Modo Degradado

RefugIA está diseñado para **nunca crashear**:

- **Sin manuales indexados** → El servidor inicia y avisa por consola. Responde con consejos básicos hardcodeados.
- **Sin Ollama corriendo** → El chat devuelve error amigable con instrucciones para iniciar Ollama.
- **Sin backend** → El frontend entra en **Modo Simulación** con respuestas predefinidas por keywords.

---

## 📜 Licencia

MIT — Usa este código para sobrevivir.

---

<div align="center">

*"En el fin del mundo, el conocimiento es el recurso más valioso."*

**RefugIA OS** — Hecho para cuando todo lo demás falle.

</div>
]]>
