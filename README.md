<div align="center">

# RefugIA OS

### Offline Survival Operating System with Local AI

*When the grid falls, knowledge survives.*

[![Python 3.10-3.13](https://img.shields.io/badge/Python-3.10--3.13-blue?logo=python)](https://python.org)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115-green?logo=fastapi)](https://fastapi.tiangolo.com)
[![Ollama](https://img.shields.io/badge/Ollama-Local%20LLM-orange)](https://ollama.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![100% Offline](https://img.shields.io/badge/Works-100%25%20Offline-red)]()
[![PWA Ready](https://img.shields.io/badge/PWA-Mobile%20Ready-purple)]()

</div>

---

## Install

Choose the method that works best for you:

### Option A — npm (recommended)

> **Requires:** [Node.js 16+](https://nodejs.org/) and [Python 3.12](https://www.python.org/downloads/release/python-3129/) (3.10-3.13 supported, **not** 3.14+) — both have 1-click installers for Windows, macOS, Linux.

```bash
git clone https://github.com/juampipey32/apocalipsis-agent.git
cd apocalipsis-agent
npm install
npm start
```

`npm install` sets up everything automatically: Python venv, dependencies, and Ollama.
After install, you can also use `npx refugia start`, `npx refugia index`, etc.

<!-- When published to npm: npm install -g refugia-os && refugia start -->

### Option B — Docker (zero setup)

> **Requires:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) — includes a visual UI, works on all platforms.

```bash
git clone https://github.com/juampipey32/apocalipsis-agent.git
cd apocalipsis-agent
docker compose up
```

Open [http://localhost:8000](http://localhost:8000) when it's ready.

### Option C — Manual (Linux/macOS only)

```bash
git clone https://github.com/juampipey32/apocalipsis-agent.git
cd apocalipsis-agent
./install.sh
./refugia start
```

> **Requires:** Python 3.10+ and Git. See [Prerequisites](#prerequisites--before-you-start) below if you need help.

---

---

## What is RefugIA?

**RefugIA** is a fully offline survival assistant powered by a **local AI** (LLM via Ollama) that uses **RAG** (Retrieval-Augmented Generation) to answer survival questions from indexed PDF manuals. It works with **zero internet** after initial setup.

- **Tactical Medicine** — First aid, wound treatment, infection management
- **Water Purification** — Filtration, distillation, chemical treatment
- **Shelter Building** — Temporary structures, thermal insulation
- **Fire & Heat** — Ignition techniques, fire types
- **Ration Tracking** — Water and food inventory with local persistence

### Mobile Support (PWA)

RefugIA works as a **Progressive Web App**. On mobile, open the URL in your browser and tap **"Install"** or **"Add to Home Screen"** to use it like a native app — fully offline.

---

## Architecture

```
┌─────────────────────────────────────────────┐
│               FRONTEND (HTML/JS)            │
│         CRT Terminal UI · Chat + Inventory  │
│         PWA · Works offline on mobile       │
└─────────────────┬───────────────────────────┘
                  │ POST /api/chat
┌─────────────────▼───────────────────────────┐
│              FastAPI Backend                 │
│         LangChain RetrievalQA Chain         │
├──────────┬──────────────────┬───────────────┤
│ ChromaDB │   HuggingFace   │  Ollama LLM   │
│ Vectors  │   Embeddings    │  (phi3/llama3) │
│  (local) │ (all-MiniLM-L6) │   (local)     │
└──────────┴──────────────────┴───────────────┘
```

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

## Hardware Requirements

| Component | Minimum | Recommended |
|---|---|---|
| **RAM** | 4 GB | 8 GB+ |
| **CPU** | Dual Core | Quad Core |
| **Disk** | 2 GB free | 5 GB+ |
| **GPU** | Not required | Optional (accelerates Ollama) |

---

## Project Structure

```
refugia-os/
├── refugia                # CLI entry point
├── install.sh             # One-command installer
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

## Español

</div>

## Instalación

Elegí el método que más te sirva:

### Opción A — npm (recomendado)

> **Requiere:** [Node.js 16+](https://nodejs.org/) y [Python 3.12](https://www.python.org/downloads/release/python-3129/) (3.10-3.13 soportado, **no** 3.14+) — ambos tienen instaladores visuales para Windows, macOS y Linux.

```bash
git clone https://github.com/juampipey32/apocalipsis-agent.git
cd apocalipsis-agent
npm install
npm start
```

`npm install` configura todo automáticamente: entorno Python, dependencias y Ollama.
Después de instalar, también podés usar `npx refugia start`, `npx refugia index`, etc.

<!-- Cuando se publique en npm: npm install -g refugia-os && refugia start -->

### Opción B — Docker (sin configuración previa)

> **Requiere:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) — incluye interfaz visual, funciona en todas las plataformas.

```bash
git clone https://github.com/juampipey32/apocalipsis-agent.git
cd apocalipsis-agent
docker compose up
```

Abrí [http://localhost:8000](http://localhost:8000) cuando esté listo.

### Opción C — Manual (Linux/macOS)

```bash
git clone https://github.com/juampipey32/apocalipsis-agent.git
cd apocalipsis-agent
./install.sh
./refugia start
```

> **Requiere:** Python 3.10+ y Git.

---

## ¿Qué es RefugIA?

**RefugIA** es un sistema de supervivencia offline con **IA local** (LLM vía Ollama) que usa **RAG** (Retrieval-Augmented Generation) para consultar manuales de supervivencia en PDF. Funciona **100% sin internet** después de la instalación inicial.

- **Medicina Táctica** — Primeros auxilios, suturas, tratamiento de infecciones
- **Purificación de Agua** — Filtrado, destilación, tratamiento químico
- **Construcción de Refugios** — Estructuras temporales, aislamiento térmico
- **Fuego y Calor** — Técnicas de encendido, tipos de fogatas
- **Gestión de Raciones** — Inventario de agua y comida con persistencia local

### Soporte Móvil (PWA)

RefugIA funciona como **Progressive Web App**. En el celular, abrí la URL en el navegador y tocá **"Instalar"** o **"Agregar a pantalla de inicio"** para usarla como app nativa, 100% offline.

---

## Comandos CLI

| Comando | Descripción |
|---|---|
| `npx refugia start` | Iniciar el servidor y abrir el navegador |
| `npx refugia index` | Indexar manuales PDF de supervivencia |
| `npx refugia status` | Mostrar estado del sistema |
| `npx refugia doctor` | Diagnosticar problemas comunes |

O usá `npm start`, `npm run index`, `npm run status`, `npm run doctor`.

---

## Variables de Entorno

| Variable | Default | Descripción |
|---|---|---|
| `REFUGIA_MODEL` | `phi3` | Modelo Ollama (phi3, llama3, mistral, gemma2) |
| `OLLAMA_URL` | `http://localhost:11434` | Endpoint de Ollama |
| `REFUGIA_PORT` | `8000` | Puerto del servidor |

---

<div align="center">

*"En el fin del mundo, el conocimiento es el recurso más valioso."*

**RefugIA OS** — Hecho para cuando todo lo demás falle.

</div>
