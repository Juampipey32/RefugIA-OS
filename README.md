<div align="center">

# 🛡️ RefugIA OS — Agente de Apocalipsis

### Sistema Operativo de Supervivencia con IA Local 100% Offline

> *"Cuando la red cae, el conocimiento sobrevive"*

[![Python 3.10-3.13](https://img.shields.io/badge/Python-3.10--3.13-blue?logo=python&logoColor=white)](https://python.org)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115+-green?logo=fastapi)](https://fastapi.tiangolo.com)
[![Ollama](https://img.shields.io/badge/Ollama-Local%20LLM-orange?logo=ollama)](https://ollama.com)
[![LangChain](https://img.shields.io/badge/LangChain-RAG-yellow)](https://langchain.com)
[![ChromaDB](https://img.shields.io/badge/ChromaDB-Vectors-purple)](https://trychroma.com)
[![PWA](https://img.shields.io/badge/PWA-Mobile%20Ready-brightgreen)]()
[![100% Offline](https://img.shields.io/badge/Offline-100%25-red?logo=offline)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[![Install](https://img.shields.io/badge/Install-1%20Command-success)](#install--one-command)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)](docker-compose.yml)
[![Platform](https://img.shields.io/badge/Platform-Windows%7CmacOS%7CLinux%7CAndroid%7CiOS-lightgrey)]()

</div>

---

## 🚀 Install — One Command

**Copy-paste ONE command.** It installs everything (Python, Ollama, AI model optimized for your hardware) and starts the server.

Copy-paste ONE command. It auto-detects your system and installs everything (Python, Ollama, AI model) or uses Docker if available.

**Any OS** (Linux/macOS/Windows with WSL):
```bash
curl -fsSL https://raw.githubusercontent.com/juampipey32/apocalipsis-agent/main/refugia.sh | bash
```

Or if you already cloned the repo:
```bash
./refugia.sh
```

That's it. The script automatically:
- Detects if Docker is available (uses it if yes)
- Falls back to native installation if needed
- Installs Python, Ollama, and dependencies
- Downloads AI models
- Indexes your PDF manuals
- Starts the server and opens your browser

The browser opens automatically at [http://localhost:8000](http://localhost:8000).

---

---

## 🧠 What is RefugIA?

**RefugIA** is an offline survival OS powered by **local AI** (LLM via Ollama) using **RAG** (Retrieval-Augmented Generation) to answer survival questions from PDF manuals. Works **100% offline** after install.

### 🎯 Core Features

| Feature | Description |
|---------|-------------|
| 🏥 **Tactical Medicine** | First aid, wound care, infection management, trauma response |
| 💧 **Water Purification** | Filtration, distillation, chemical treatment, sourcing |
| ⛺ **Shelter Building** | Temporary structures, insulation, weatherproofing |
| 🔥 **Fire & Heat** | Ignition techniques, fuel types, fire safety |
| 📦 **Supply Management** | Track water, food, medicine with depletion alerts |
| 🧭 **Navigation** | Orienteering, landmarks, emergency routes |
| 🌡️ **Weather Survival** | Hypothermia prevention, heat stroke, storm shelter |
| 📚 **Manual Indexing** | Auto-indexes any PDF survival guide you add |

### 🤖 Smart Model Detection

RefugIA **automatically detects** the best local LLM for your hardware:

| RAM Available | Model Selected | Performance |
|---------------|----------------|-------------|
| < 4 GB | `phi3` (3.8B) | Fast, minimal RAM |
| 4-8 GB | `llama3:8b` | Balanced |
| 8-16 GB | `mistral:7b` or `llama3:8b-instruct` | Enhanced reasoning |
| > 16 GB | `gemma2:9b` or `llama3:70b` (if GPU) | Maximum intelligence |

The installer checks your system and downloads the optimal model automatically!

---

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│              FRONTEND (HTML/CSS/JS PWA)                 │
│    CRT Terminal UI · Chat · Inventory · Offline-First   │
│    Responsive Mobile · Installable · Zero Dependencies  │
└──────────────────────┬──────────────────────────────────┘
                       │ POST /api/chat
┌──────────────────────▼──────────────────────────────────┐
│                   FastAPI Backend                        │
│         LangChain RAG Chain · Supply Tracker            │
├────────────┬─────────────────────┬──────────────────────┤
│  ChromaDB  │  HuggingFace        │   Ollama LLM         │
│  Vectors   │  Embeddings         │   Auto-Detect Model  │
│  (local)   │  (all-MiniLM-L6-v2) │   phi3/llama3/etc    │
└────────────┴─────────────────────┴──────────────────────┘
          ↓                ↓                    ↓
   Persistent DB     CPU/GPU Inference    Hardware-Optimized
```

### Key Components

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Vector DB** | ChromaDB | Stores embeddings from PDF manuals |
| **Embeddings** | all-MiniLM-L6-v2 | 384-dim vectors, ~80MB, offline |
| **LLM** | Ollama (phi3/llama3/mistral) | Local inference, no cloud |
| **RAG Engine** | LangChain | Retrieves + generates answers |
| **Frontend** | Vanilla JS + PWA | Works on any device, offline |
| **Supply Tracker** | localStorage | Persistent inventory on client |

---

## CLI Commands

| Command | Description |
|---|---|
| `npx refugia start` | Launch the server and open browser |
| `npx refugia index` | Index PDF survival manuals |
| `npx refugia status` | Show system status |
| `npx refugia doctor` | Diagnose common issues |

Or use `npm start`, `npm run index`, `npm run status`, `npm run doctor`.

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `REFUGIA_MODEL` | `phi3` | Ollama model (phi3, llama3, mistral, gemma2) |
| `OLLAMA_URL` | `http://localhost:11434` | Ollama API endpoint |
| `REFUGIA_PORT` | `8000` | Server port |

Example:
```bash
REFUGIA_MODEL=llama3 ./refugia start
```

---

## 💻 Hardware Requirements

| Component | Minimum | Recommended | High-End |
|-----------|---------|-------------|----------|
| **RAM** | 4 GB | 8 GB+ | 16 GB+ |
| **CPU** | Dual Core | Quad Core | 8+ cores |
| **Disk** | 2 GB free | 5 GB+ | 10 GB+ SSD |
| **GPU** | Not required | Optional (CUDA) | NVIDIA RTX |
| **Mobile** | Any smartphone with browser | PWA installed | Offline mode |

### Model Selection by Hardware

RefugIA auto-detects your hardware and selects the optimal model:

```python
if RAM < 4GB:      → phi3:3.8b          (fast, minimal)
elif RAM < 8GB:    → llama3:8b          (balanced)
elif RAM < 16GB:   → mistral:7b-instruct (smart)
else:              → gemma2:9b or llama3:70b (max intelligence)
```

---

## Project Structure

```
refugia-os/
├── refugia.sh             # Universal auto-installer (ONE COMMAND)
├── install.sh             # Legacy native installer (kept for compatibility)
├── setup.ps1              # Legacy Windows installer (kept for compatibility)
├── docker-compose.yml     # Docker deployment
├── Makefile               # make start, make install, etc.
├── src/
│   ├── cli.py             # CLI logic (argparse)
│   ├── agente_api.py      # FastAPI backend + RAG
│   ├── indexador.py        # PDF → ChromaDB indexer
│   └── db/                # ChromaDB vectorstore (generated)
├── frontend/
│   ├── index.html         # Single-file SPA (CRT theme)
│   ├── manifest.json      # PWA manifest
│   ├── sw.js              # Service worker (offline)
│   └── icons/             # App icons
├── manuales/              # PDF survival manuals
├── requirements.txt       # Python dependencies
├── LICENSE                # MIT
└── README.md
```

---

## Graceful Degradation

RefugIA is designed to **never crash**:

- **No indexed manuals** — Server starts, responds with hardcoded survival tips
- **No Ollama running** — Chat returns friendly error with setup instructions
- **No backend at all** — Frontend enters **Simulation Mode** with keyword-based responses

---

## Contributing

1. Fork the repo
2. Create your branch: `git checkout -b feature/amazing-feature`
3. Commit: `git commit -m 'Add amazing feature'`
4. Push: `git push origin feature/amazing-feature`
5. Open a Pull Request

---

## License

MIT — Use this code to survive.

---

---

<div align="center">

## 🇪🇸 Español

### Instalar — Un Solo Comando

Copiá y pegá UN comando. Detecta automáticamente tu sistema e instala todo (Python, Ollama, modelo IA) o usa Docker si está disponible.

**Cualquier SO** (Linux/macOS/Windows con WSL):
```bash
curl -fsSL https://raw.githubusercontent.com/juampipey32/apocalipsis-agent/main/refugia.sh | bash
```

O si ya clonaste el repo:
```bash
./refugia.sh
```

Listo. El script automáticamente:
- Detecta si Docker está disponible (lo usa si sí)
- Usa instalación nativa si es necesario
- Instala Python, Ollama y dependencias
- Descarga modelos de IA
- Indexa tus manuales PDF
- Inicia el servidor y abre tu navegador

El navegador se abre automáticamente en [http://localhost:8000](http://localhost:8000).

---

## ¿Qué es RefugIA?

**RefugIA** es un sistema de supervivencia offline con **IA local** (LLM vía Ollama) que usa **RAG** (Retrieval-Augmented Generation) para consultar manuales de supervivencia en PDF. Funciona **100% sin internet** después de la instalación inicial.

### 🎯 Características Principales

| Característica | Descripción |
|----------------|-------------|
| 🏥 **Medicina Táctica** | Primeros auxilios, suturas, tratamiento de infecciones, trauma |
| 💧 **Purificación de Agua** | Filtrado, destilación, tratamiento químico, búsqueda de fuentes |
| ⛺ **Construcción de Refugios** | Estructuras temporales, aislamiento térmico, impermeabilización |
| 🔥 **Fuego y Calor** | Técnicas de encendido, tipos de combustible, seguridad |
| 📦 **Gestión de Suministros** | Inventario de agua, comida, medicina con alertas de agotamiento |
| 🧭 **Navegación** | Orientación, puntos de referencia, rutas de emergencia |
| 🌡️ **Supervivencia Climática** | Prevención de hipotermia, golpe de calor, refugio ante tormentas |
| 📚 **Indexado de Manuales** | Auto-indexa cualquier guía PDF que agregues |

### 🤖 Detección Inteligente de Modelos

RefugIA **detecta automáticamente** el mejor LLM local para tu hardware:

| RAM Disponible | Modelo Seleccionado | Rendimiento |
|----------------|---------------------|-------------|
| < 4 GB | `phi3` (3.8B) | Rápido, mínimo RAM |
| 4-8 GB | `llama3:8b` | Balanceado |
| 8-16 GB | `mistral:7b-instruct` | Razonamiento mejorado |
| > 16 GB | `gemma2:9b` o `llama3:70b` (con GPU) | Máxima inteligencia |

¡El instalador chequea tu sistema y descarga el modelo óptimo automáticamente!

---

## Comandos CLI

| Comando | Descripción |
|---------|-------------|
| `npx refugia start` | Iniciar el servidor y abrir el navegador |
| `npx refugia index` | Indexar manuales PDF de supervivencia |
| `npx refugia status` | Mostrar estado del sistema |
| `npx refugia doctor` | Diagnosticar problemas comunes |

O usá `npm start`, `npm run index`, `npm run status`, `npm run doctor`.

---

## Variables de Entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| `REFUGIA_MODEL` | `phi3` | Modelo Ollama (phi3, llama3, mistral, gemma2) |
| `OLLAMA_URL` | `http://localhost:11434` | Endpoint de Ollama |
| `REFUGIA_PORT` | `8000` | Puerto del servidor |
| `REFUGIA_HOST` | `127.0.0.1` | Host del servidor (usá `0.0.0.0` para acceso desde móviles) |

Ejemplo:
```bash
REFUGIA_MODEL=llama3 REFUGIA_HOST=0.0.0.0 ./refugia start
```

---

<div align="center">

> *"En el fin del mundo, el conocimiento es el recurso más valioso."*

### 🛡️ RefugIA OS — Agente de Apocalipsis

Hecho para cuando todo lo demás falle.

[![GitHub Stars](https://img.shields.io/github/stars/juampipey32/apocalipsis-agent?style=social)](https://github.com/juampipey32/apocalipsis-agent)
[![GitHub Forks](https://img.shields.io/github/forks/juampipey32/apocalipsis-agent?style=social)](https://github.com/juampipey32/apocalipsis-agent)

**[Reportar Bug](https://github.com/juampipey32/apocalipsis-agent/issues) · [Sugerir Feature](https://github.com/juampipey32/apocalipsis-agent/issues) · [Discord](#)**

</div>
