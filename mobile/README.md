# RefugIA Mobile — App offline para Android/iOS

App de supervivencia con IA **100% on-device**: un LLM chico cuantizado y
el RAG de los manuales corren dentro del teléfono, sin servidor ni internet
(salvo la descarga inicial del modelo).

## Dos caminos para construirla

Ambos comparten el mismo RAG y el mismo índice exportado; cambia el envoltorio:

| Camino | Carpeta | UI | Cuándo usarlo |
|--------|---------|----|---------------|
| **Capacitor** | `www/` | Reutiliza la TUI web (`../frontend`) | Multiplataforma (Android+iOS), reuso total de la interfaz |
| **Nativo / AI Studio** | `android-native/` | Kotlin + Jetpack Compose | Generás el cascarón en AI Studio y reemplazás su IA-nube por la local |

> **Nota sobre Google AI Studio (2026):** compila APK nativos Kotlin/Compose
> y los publica a Play, pero las apps que genera usan la **API de Gemini en
> la nube** (necesitan internet). Para RefugIA usamos su cascarón nativo y
> reemplazamos esa llamada por inferencia local: ver `android-native/`.

## Arquitectura (camino Capacitor)

- **Envoltorio**: [Capacitor](https://capacitorjs.com) — reutiliza el
  frontend web existente (`../frontend/index.html`) dentro de un APK/IPA.
- **Motor LLM**: `llama.cpp` embebido como plugin nativo (inferencia local).
- **Modelo**: chico y cuantizado (recomendado Gemma 2 2B Q4 o Qwen2.5 1.5B
  Q4 para buen español). **No se empaqueta** en el APK: se descarga en el
  primer arranque (APK liviano + descarga única con internet).
- **RAG sin base de datos**: los chunks + embeddings se pre-calculan en la
  PC con `../src/exportar_rag.py` y se embeben como asset
  (`assets/rag_index.json`). En el teléfono la búsqueda es por similitud
  coseno en memoria — no hace falta ChromaDB ni indexar en el dispositivo.

## Flujo de una consulta (en el teléfono)

1. El usuario escribe una pregunta.
2. Se calcula el embedding de la pregunta (modelo de embeddings local).
3. Búsqueda coseno contra `rag_index.json` → top-k chunks relevantes.
4. Se arma el prompt con esos chunks como contexto.
5. El LLM local (llama.cpp) genera la respuesta.

Es el mismo patrón RAG que el servidor, pero todo dentro del APK.

## Generar el índice RAG embebible

Desde la raíz del repo, con el venv activado:

```bash
python src/exportar_rag.py
# genera mobile/assets/rag_index.json (se regenera al cambiar manuales/)
```

> `assets/rag_index.json` es un artefacto generado: NO se versiona.
> Regeneralo tras editar los manuales.

## Estado

- [x] Exportador de RAG (chunks + embeddings → JSON portable) — **listo y probado**
- [x] Contenido base de manuales en español — **listo**
- [x] Motor RAG on-device en JS (`www/refugia-engine.js`) — **listo y probado end-to-end**
- [x] Frontend único compartido web/móvil (motor conectable) — **listo**
- [x] Scaffold Capacitor (config + scripts de build) — **listo**
- [ ] Plugin nativo llama.cpp (inferencia + embeddings on-device) — *pendiente, requiere NDK*
- [ ] Descarga y gestión del modelo en el primer arranque — *pendiente*

> El motor JS está completo y probado con un plugin nativo simulado: hace
> la búsqueda coseno y arma el prompt correctamente. Falta solo la capa
> nativa que ejecute el modelo (llama.cpp) y exponga `window.RefugIANative`.

## Cómo se arma (en tu máquina con Android Studio)

```bash
cd mobile
npm install                 # dependencias de Capacitor

# 1) Generar el índice RAG embebido (desde la raíz del repo, con venv)
npm run export:rag          # -> www/assets/rag_index.json

# 2) Ensamblar www/ (copia el frontend único + inyecta el motor)
npm run prepare:www

# 3) Agregar la plataforma Android y sincronizar
npm run add:android
npm run sync

# 4) Compilar el APK de debug
npm run build:apk           # -> android/app/build/outputs/apk/debug/

# o abrir en Android Studio para firmar/depurar
npm run open:android
```

## El puente nativo que falta (`window.RefugIANative`)

El motor JS (`www/refugia-engine.js`) espera un objeto inyectado por un
plugin nativo de Capacitor con esta interfaz:

```js
window.RefugIANative = {
  modelReady: async () => boolean,        // ¿el modelo está descargado y cargado?
  embed:      async (text) => Float32Array, // embedding de 384 dims (all-MiniLM-L6-v2)
  generate:   async (prompt) => string,     // inferencia con llama.cpp
};
```

Recomendación de implementación:
- **Inferencia LLM**: [`llama.cpp`](https://github.com/ggerganov/llama.cpp)
  compilado para Android (NDK) vía JNI, o un wrapper existente (p. ej.
  `llama.rn` portado, o `cui-llama`). Modelo GGUF Q4 (Gemma 2 2B / Qwen2.5 1.5B).
- **Embeddings**: el modelo `all-MiniLM-L6-v2` en formato GGUF (modo
  embedding de llama.cpp) o vía ONNX Runtime Mobile. Debe producir 384
  dims normalizados, idéntico al exportador, para que la búsqueda coincida.

## Requisitos para compilar

- Node.js + `@capacitor/cli`
- Python + venv del repo (para `export:rag`)
- Android Studio + SDK/NDK (APK) / Xcode (iOS)
