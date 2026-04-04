# ============================================================
# RefugIA OS — Dockerfile
# ============================================================
# Builds a container with the Python backend pre-configured.
# Ollama must run on the host (or as a separate container).
#
# Build:  docker build -t refugia-os .
# Run:    docker run -p 8000:8000 -v ./manuales:/app/manuales refugia-os
# ============================================================

FROM python:3.11-slim

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install Python dependencies first (cache layer)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy project files
COPY src/ ./src/
COPY frontend/ ./frontend/
COPY manuales/ ./manuales/

# Create DB directory
RUN mkdir -p src/db

# Default environment
ENV REFUGIA_MODEL=phi3
ENV OLLAMA_URL=http://host.docker.internal:11434
ENV REFUGIA_PORT=8000

# Index manuals on build (if PDFs exist)
RUN python src/indexador.py || echo "No PDFs to index — mount manuales/ volume and run indexing manually."

EXPOSE 8000

# Start the API server
CMD ["python", "src/agente_api.py"]
