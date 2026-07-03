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

## 🚨 Emergencia AHORA — úsala sin instalar nada

> **🌍 Guía de emergencia web (terremotos, primeros auxilios, agua segura):**
>
> ### 👉 [juampipey32.github.io/apocalipsis-agent](https://juampipey32.github.io/apocalipsis-agent/) 👈
>
> - **No requiere instalación.** Se abre en cualquier teléfono o computadora.
> - **Funciona sin internet** después de abrirla una vez (guárdala ANTES de necesitarla).
> - **Compártela por WhatsApp:** el link puede salvarle tiempo crítico a alguien.
> - Incluye: qué hacer durante y después de un terremoto, réplicas, personas atrapadas, primeros auxilios, agua segura, inventario de recursos y kit de emergencia.
> - **[🖨️ Cartel con QR para imprimir/compartir](https://juampipey32.github.io/apocalipsis-agent/cartel-qr.png)** — pégalo en refugios, farmacias, carteleras.
>
> La guía se genera desde los manuales de `manuales/` con `python scripts/build_web.py`. Todo lo que sigue abajo (servidor local + IA) es la versión avanzada y **opcional**.

---

## 🚀 Installation — Pick Your Method

Every method ends the same way: the server runs at **[http://localhost:8000](http://localhost:8000)** and works 100% offline afterwards.

| # | Method | Best for | One-liner |
|---|--------|----------|-----------|
| 1 | **Universal script** | Linux / macOS — fastest | `curl -fsSL https://raw.githubusercontent.com/juampipey32/apocalipsis-agent/main/refugia.sh \| bash` |
| 2 | **Windows (PowerShell)** | Windows 10 / 11 | `irm https://raw.githubusercontent.com/juampipey32/apocalipsis-agent/main/setup.ps1 \| iex` |
| 3 | **Docker Compose** | Isolated & reproducible | `docker compose up --build` |
| 4 | **npm / Node** | Node developers | `npm install && npm start` |
| 5 | **Makefile** | One target each | `make install && make start` |
| 6 | **Manual** | Full control | see steps below |

> **Requirements:** Python **3.10–3.13** and ~2–5 GB free disk. The model download needs internet **once**; after that RefugIA is fully offline.

---

### 1. Universal script (Linux / macOS)

Auto-detects Docker (uses it if present), otherwise installs natively: Python, Ollama, dependencies, the optimal AI model, indexes your PDFs and starts the server.

```bash
# Remote (no clone needed)
curl -fsSL https://raw.githubusercontent.com/juampipey32/apocalipsis-agent/main/refugia.sh | bash

# Or after cloning the repo
git clone https://github.com/juampipey32/apocalipsis-agent.git
cd apocalipsis-agent
./refugia.sh
```

There is also `./install.sh` — a native-only installer (no Docker auto-detection) kept for compatibility.

### 2. Windows (PowerShell)

Open PowerShell and run:

```powershell
irm https://raw.githubusercontent.com/juampipey32/apocalipsis-agent/main/setup.ps1 | iex
```

It installs Python 3.12, Git and Ollama, clones the repo, sets everything up and starts the server. (Windows users can also use **method 4** if they already have Node, or **WSL** + method 1.)

### 3. Docker Compose

Runs RefugIA **and** Ollama together in containers — nothing else to install but Docker.

```bash
git clone https://github.com/juampipey32/apocalipsis-agent.git
cd apocalipsis-agent
docker compose up --build        # add -d to run in the background
```

First boot pulls the model (~2–4 GB) into a named volume, then it's offline. Your `manuales/` folder and the vector DB are mounted as volumes, so re-indexing persists. Pick a model with `REFUGIA_MODEL=llama3 docker compose up --build`.

### 4. npm / Node

The `npm install` step runs a `postinstall` script that creates the Python venv, installs dependencies, detects your hardware, pulls the best Ollama model and indexes the manuals.

```bash
git clone https://github.com/juampipey32/apocalipsis-agent.git
cd apocalipsis-agent
npm install            # full setup via postinstall
npm start              # launch the server (alias of: node bin/refugia.js start)
```

Other scripts: `npm run index`, `npm run status`, `npm run doctor`. To get a global `refugia` command, run `npm link` inside the repo, then use `refugia start`, `refugia status`, etc.

### 5. Makefile

If you cloned the repo and have `make`:

```bash
make install     # runs ./install.sh
make start       # launch the server
make index       # (re)index the PDF manuals
make status      # system status
make doctor      # diagnose issues
make clean       # remove venv + vector DB cache
```

### 6. Manual (step by step)

For full control, or to debug:

```bash
git clone https://github.com/juampipey32/apocalipsis-agent.git
cd apocalipsis-agent

# 1) Install Ollama (https://ollama.com/download) and pull a model
ollama pull phi3

# 2) Python environment + dependencies
python3 -m venv venv
source venv/bin/activate            # Windows: venv\Scripts\activate
pip install --upgrade pip
pip install -r requirements.txt     # PyTorch installs CPU-only (~200 MB)

# 3) Index the PDF survival manuals into ChromaDB
python src/indexador.py

# 4) Start the server
python src/agente_api.py            # open http://localhost:8000
```

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
| 📚 **Manual Indexing** | Auto-indexes any survival guide you add (PDF, Markdown or TXT) |
| 📱 **Mobile-ready RAG** | Pre-computed RAG index exportable for an offline on-device app (see `mobile/`) |

### 🤖 Smart Model Detection

The **npm installer** detects your hardware (RAM + GPU) and picks the model that will actually run well on it:

| Hardware | Model Selected | Why |
|----------|----------------|-----|
| Dedicated GPU + RAM ≥ 16 GB | `llama3` (8B) | Stronger reasoning, GPU-accelerated |
| RAM ≥ 32 GB | `llama3` (8B) | Plenty of headroom |
| Everything else (default) | `phi3` (3.8B) | Fast and light, runs anywhere |

You can always override the choice with the `REFUGIA_MODEL` environment variable (e.g. `mistral`, `gemma2`) — see [Environment Variables](#environment-variables). The script/Docker installers default to `phi3` and don't auto-upgrade.

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

From inside the cloned repo, use the `./refugia` wrapper (it activates the venv for you):

| Command | Description |
|---|---|
| `./refugia start` | Launch the server and open the browser |
| `./refugia index` | Index PDF survival manuals |
| `./refugia status` | Show system status |
| `./refugia doctor` | Diagnose common issues |

Equivalents via npm: `npm start`, `npm run index`, `npm run status`, `npm run doctor`.
After `npm link` (or a future `npm install -g`), the same commands work globally as `refugia start`, `refugia status`, etc. Add `--no-browser` to `start` to skip opening the browser.

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

(See [Smart Model Detection](#-smart-model-detection) above for exactly which model the installer picks.)

---

## Project Structure

```
apocalipsis-agent/
├── refugia.sh             # Universal auto-installer (Docker or native)
├── install.sh             # Native-only installer (Linux/macOS)
├── setup.ps1              # Windows installer (PowerShell)
├── refugia                # CLI wrapper (activates venv → src/cli.py)
├── docker-compose.yml     # Docker deployment (RefugIA + Ollama)
├── Dockerfile             # Backend image
├── .dockerignore          # Keeps the build context small
├── Makefile               # make start, make install, etc.
├── package.json           # npm scripts + postinstall setup
├── bin/
│   └── refugia.js         # Node CLI entry point
├── scripts/
│   └── postinstall.js     # npm hardware detection + full setup
├── src/
│   ├── cli.py             # CLI logic (argparse)
│   ├── agente_api.py      # FastAPI backend + RAG
│   ├── indexador.py       # PDF → ChromaDB indexer
│   └── db/                # ChromaDB vectorstore (generated)
├── docs/                  # 🚨 Emergency web guide (GitHub Pages, no install)
│   ├── index.html         # Static app: guide + search + inventory
│   ├── data.js            # Generated from manuales/ (scripts/build_web.py)
│   └── sw.js              # Offline-first service worker
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

RefugIA is designed to **never crash** and to set itself up:

- **Auto-indexing** — If PDFs exist in `manuales/` but no vector DB is found, the server **builds the index automatically on startup**. No matter how you launch it (script, npm, Docker or manually), it ends up fully operational — no manual indexing step required.
- **No manuals & no PDFs** — Server starts, responds with hardcoded survival tips
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

<div align="center">

## 🇪🇸 Español

</div>

### 🚨 ¿Emergencia? Usá la guía web sin instalar nada

**[juampipey32.github.io/apocalipsis-agent](https://juampipey32.github.io/apocalipsis-agent/)** — se abre en cualquier teléfono, funciona **sin internet** después de la primera carga, y se comparte por WhatsApp. Qué hacer en un terremoto, réplicas, primeros auxilios, agua segura, inventario y kit de emergencia. Lo que sigue abajo es la versión avanzada con IA local (opcional).

### Instalación — Elegí tu método

Todos los métodos terminan igual: el servidor queda en **[http://localhost:8000](http://localhost:8000)** y funciona 100% offline después.

| # | Método | Ideal para | Comando |
|---|--------|-----------|---------|
| 1 | **Script universal** | Linux / macOS — el más rápido | `curl -fsSL https://raw.githubusercontent.com/juampipey32/apocalipsis-agent/main/refugia.sh \| bash` |
| 2 | **Windows (PowerShell)** | Windows 10 / 11 | `irm https://raw.githubusercontent.com/juampipey32/apocalipsis-agent/main/setup.ps1 \| iex` |
| 3 | **Docker Compose** | Aislado y reproducible | `docker compose up --build` |
| 4 | **npm / Node** | Desarrolladores Node | `npm install && npm start` |
| 5 | **Makefile** | Un target para cada cosa | `make install && make start` |
| 6 | **Manual** | Control total | ver pasos abajo |

> **Requisitos:** Python **3.10–3.13** y ~2–5 GB de disco libre. La descarga del modelo necesita internet **una sola vez**; después RefugIA es 100% offline.

#### 1. Script universal (Linux / macOS)

Detecta Docker (lo usa si está), o instala de forma nativa: Python, Ollama, dependencias, el modelo óptimo, indexa tus PDFs y arranca el servidor.

```bash
# Remoto (sin clonar)
curl -fsSL https://raw.githubusercontent.com/juampipey32/apocalipsis-agent/main/refugia.sh | bash

# O después de clonar el repo
git clone https://github.com/juampipey32/apocalipsis-agent.git
cd apocalipsis-agent
./refugia.sh
```

También existe `./install.sh`, un instalador solo-nativo (sin auto-detección de Docker).

#### 2. Windows (PowerShell)

Abrí PowerShell y ejecutá:

```powershell
irm https://raw.githubusercontent.com/juampipey32/apocalipsis-agent/main/setup.ps1 | iex
```

Instala Python 3.12, Git y Ollama, clona el repo, configura todo y arranca el servidor. (En Windows también podés usar el **método 4** si ya tenés Node, o **WSL** + método 1.)

#### 3. Docker Compose

Levanta RefugIA **y** Ollama en contenedores — solo necesitás Docker.

```bash
git clone https://github.com/juampipey32/apocalipsis-agent.git
cd apocalipsis-agent
docker compose up --build        # agregá -d para correr en segundo plano
```

El primer arranque baja el modelo (~2–4 GB) a un volumen; después es offline. Tu carpeta `manuales/` y la base vectorial se montan como volúmenes, así que la reindexación persiste. Elegí modelo con `REFUGIA_MODEL=llama3 docker compose up --build`.

#### 4. npm / Node

El `npm install` corre un `postinstall` que crea el venv de Python, instala las dependencias, detecta tu hardware, baja el mejor modelo de Ollama e indexa los manuales.

```bash
git clone https://github.com/juampipey32/apocalipsis-agent.git
cd apocalipsis-agent
npm install            # setup completo vía postinstall
npm start              # arranca el servidor
```

Otros scripts: `npm run index`, `npm run status`, `npm run doctor`. Para tener el comando global `refugia`, corré `npm link` dentro del repo.

#### 5. Makefile

Con el repo clonado y `make` disponible:

```bash
make install     # corre ./install.sh
make start       # arranca el servidor
make index       # (re)indexa los manuales PDF
make status      # estado del sistema
make doctor      # diagnostica problemas
make clean       # borra venv + cache de la base vectorial
```

#### 6. Manual (paso a paso)

Para control total o para depurar:

```bash
git clone https://github.com/juampipey32/apocalipsis-agent.git
cd apocalipsis-agent

# 1) Instalá Ollama (https://ollama.com/download) y bajá un modelo
ollama pull phi3

# 2) Entorno de Python + dependencias
python3 -m venv venv
source venv/bin/activate            # Windows: venv\Scripts\activate
pip install --upgrade pip
pip install -r requirements.txt     # PyTorch se instala en versión CPU (~200 MB)

# 3) Indexá los manuales PDF en ChromaDB
python src/indexador.py

# 4) Arrancá el servidor
python src/agente_api.py            # abrí http://localhost:8000
```

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

El **instalador de npm** detecta tu hardware (RAM + GPU) y elige el modelo que realmente va a correr bien:

| Hardware | Modelo Elegido | Por qué |
|----------|----------------|---------|
| GPU dedicada + RAM ≥ 16 GB | `llama3` (8B) | Más razonamiento, acelerado por GPU |
| RAM ≥ 32 GB | `llama3` (8B) | Sobra memoria |
| Todo lo demás (default) | `phi3` (3.8B) | Rápido y liviano, corre en cualquier lado |

Siempre podés forzar otro modelo con la variable `REFUGIA_MODEL` (ej. `mistral`, `gemma2`). Los instaladores por script/Docker usan `phi3` por defecto.

---

## Comandos CLI

Desde el repo clonado, usá el wrapper `./refugia` (activa el venv solo):

| Comando | Descripción |
|---------|-------------|
| `./refugia start` | Iniciar el servidor y abrir el navegador |
| `./refugia index` | Indexar manuales PDF de supervivencia |
| `./refugia status` | Mostrar estado del sistema |
| `./refugia doctor` | Diagnosticar problemas comunes |

Equivalentes con npm: `npm start`, `npm run index`, `npm run status`, `npm run doctor`. Tras `npm link` funcionan global como `refugia start`, etc.

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
