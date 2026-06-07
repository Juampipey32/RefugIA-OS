# RefugIA — App Android nativa (IA 100% on-device)

App de supervivencia generada como cascarón en Google AI Studio (Kotlin +
Jetpack Compose, tema CRT terminal) y **modificada para correr IA local**:
su "IA" de demo (respuestas de ficción hardcodeadas) fue reemplazada por
un agente RAG real sobre los manuales de supervivencia, con un LLM chico
corriendo en el teléfono vía llama.cpp. Sin nube, sin API keys.

## Arquitectura

```
ChatScreen (Compose, CRT)           ← UI original de AI Studio (conservada)
        │  agent.ask(query) { token -> ... }   ← streaming a la UI
        ▼
RefugiaAgent                         ← orquesta el pipeline
        │
        ├─ RagEngine                 ← carga assets/rag_index.json,
        │                              búsqueda coseno en memoria (sin DB)
        └─ Llamatik (llama.cpp)      ← embed() + generate() on-device
                 ▲
         ModelDownloader             ← baja los .gguf al 1er arranque
```

- **RAG embebido**: `app/src/main/assets/rag_index.json` (1369 chunks de los
  manuales, 384 dims). Se genera con `python src/exportar_rag.py` desde la
  raíz del repo. La búsqueda es coseno en memoria, sin ChromaDB.
- **LLM**: [Llamatik](https://github.com/ferranpons/Llamatik) (`com.llamatik:library`),
  binding de llama.cpp con `generate` + `embed`.
- **Modelos GGUF**: NO se empaquetan en el APK. Se descargan al primer
  arranque (APK liviano + descarga única con internet) y quedan en
  `filesDir/models/`. URLs ya configuradas en `ModelDownloader.kt`
  (descarga directa de Hugging Face, verificadas):
  - Generación: **Llama-3.2-1B-Instruct Q4_K_M** (~807 MB, buen multilingüe;
    más liviano que un 1.5B para que la descarga única complete en redes
    inestables). La descarga reanuda y reintenta mientras haya progreso.
  - Embeddings: **all-MiniLM-L6-v2 Q8_0** (~25 MB, 384 dims) — coincide con
    `src/exportar_rag.py` (`all-MiniLM-L6-v2`), requisito para que la
    búsqueda coseno contra `rag_index.json` recupere chunks válidos.

## Compilar

### Opción A — GitHub Actions (sin Android Studio)
Cada push a `mobile/android-app/**` dispara el workflow
`.github/workflows/android-build.yml`, que compila el APK de debug en la
nube. Descargalo desde la pestaña **Actions → run → Artifacts →
`refugia-debug-apk`**. También se puede lanzar a mano con
**workflow_dispatch**.

### Opción B — Android Studio (local)
1. Abrí `mobile/android-app/` en Android Studio.
2. Dejá que importe y baje el SDK necesario.
3. Decodificá el keystore de debug:
   `base64 -d debug.keystore.base64 > debug.keystore`
4. `cp .env.example .env` (placeholder; ya no se usa Gemini).
5. Run en emulador o dispositivo.

## Estado

- [x] UI CRT (de AI Studio) conservada
- [x] Swap de la IA de demo → agente RAG on-device
- [x] Núcleo RAG (`RagEngine`) — lógica validada contra web/servidor
- [x] Integración LLM vía Llamatik (`RefugiaAgent`) — package
      `com.llamatik.library.platform`, minSdk 26
- [x] Descarga de modelos al primer arranque (`ModelDownloader`)
- [x] Índice RAG embebido en assets
- [x] **CI verde: GitHub Action compila el APK de debug (~63 MB) y lo
      publica como artefacto descargable**
- [x] **URLs de modelos GGUF configuradas y verificadas** (Llama-3.2-1B
      Q4_K_M + all-MiniLM-L6-v2 Q8_0, descarga directa de Hugging Face)
- [x] **Probado en dispositivo real** (Redmi Note 13): descarga + inferencia
      on-device funcionando
- [x] **Memoria conversacional** (multi-turno acotado, repreguntas)
- [x] **Feedback en UI** (cursor parpadeante al pensar, auto-scroll en
      streaming, input bloqueado mientras responde)
- [ ] Afinar parámetros de inferencia (n_ctx, temperatura) según uso

> El APK compila en GitHub Actions (ver pestaña **Actions → Artifacts →
> `refugia-debug-apk`**) y los modelos se descargan solos al primer
> arranque. Pendiente sólo la prueba en un dispositivo real para afinar
> parámetros de inferencia (n_ctx, temperatura, top_k del LLM).
