# RefugIA — Núcleo IA local para apps Android (AI Studio)

Este paquete reemplaza la **IA en la nube (Gemini API)** que genera Google
AI Studio por **inferencia 100% on-device**, para que RefugIA siga su lema:
*"cuando la red cae, el conocimiento sobrevive"*.

## Qué incluye

| Archivo | Rol | Estado |
|---------|-----|--------|
| `RagEngine.kt` | RAG: carga el índice, búsqueda coseno, arma el prompt | ✅ lógica validada |
| `InferenceEngine.kt` | Interfaz del motor (embed + generate) | ✅ |
| `RefugiaAgent.kt` | Orquestador — el reemplazo de la llamada a Gemini | ✅ |
| *(falta)* `LlamaCppEngine.kt` | Implementación real con llama.cpp (JNI) | ⏳ requiere NDK |

> La lógica del RAG (`RagEngine`) fue validada portándola a Java y
> comparándola contra el motor JS y Python: produce scores idénticos
> (agua 0.64, fuego 0.73, etc.). Recupera el manual correcto por consulta.

## Cómo integrarlo en la app de AI Studio

AI Studio genera una app **Kotlin + Jetpack Compose** con `ViewModel` y una
llamada a la API de Gemini. El swap es:

### 1. Copiar archivos y el índice
- Copiá `RagEngine.kt`, `InferenceEngine.kt`, `RefugiaAgent.kt` al paquete
  `com.refugia.survival.rag` (ajustá el package al de tu app).
- Copiá `mobile/www/assets/rag_index.json` a `app/src/main/assets/`.
  (Generalo con `python src/exportar_rag.py` desde la raíz del repo.)

### 2. Reemplazar la llamada a Gemini en la ViewModel
```kotlin
// ANTES (AI Studio, nube):
// val response = generativeModel.generateContent(prompt).text

// DESPUÉS (RefugIA, local):
private val agent = RefugiaAgent(application, LlamaCppEngine(application))

fun enviar(query: String) {
    viewModelScope.launch(Dispatchers.Default) {
        agent.warmUp()                       // carga el índice (1 vez)
        val respuesta = agent.ask(query) { token ->
            // opcional: stream del token a la UI (StateFlow)
        }
        // publicar `respuesta` en el StateFlow que observa Compose
    }
}
```

### 3. Reflejar el estado en la UI
`agent.state()` devuelve `CARGANDO` / `DESCARGAR_MODELO` / `LISTO`, para
mostrar el mismo badge que la versión web.

## El motor que falta: `LlamaCppEngine`

Implementá la interfaz `InferenceEngine` envolviendo llama.cpp:

```kotlin
class LlamaCppEngine(context: Context) : InferenceEngine {
    // Carga libllama vía System.loadLibrary("llama-jni")
    // - generate(): pasa el prompt al modelo GGUF (Gemma 2 2B / Qwen2.5 1.5B Q4)
    // - embed():    usa el modo embedding con all-MiniLM-L6-v2 GGUF (384 dims, normalizado)
    // - isModelReady(): true si el .gguf ya fue descargado y cargado
}
```

Opciones para la capa JNI (en orden de menor esfuerzo):
1. **Dependencia existente**: usar un binding mantenido de llama.cpp para
   Android (buscar `llama.cpp android jni` / wrappers GGUF) y solo
   implementar `embed`/`generate`.
2. **MediaPipe LLM Inference API** de Google para `generate` (soporta Gemma
   on-device), y ONNX Runtime Mobile para `embed` (all-MiniLM-L6-v2).
3. **Compilar llama.cpp con el NDK** y exponer JNI propio (más control).

> Requisito crítico: `embed()` DEBE ser all-MiniLM-L6-v2, 384 dims,
> normalizado — idéntico a `src/exportar_rag.py`. Si cambia el modelo de
> embeddings, hay que reexportar el índice con ese mismo modelo.

## Descarga del modelo (primer arranque)

El `.gguf` (~0.7–1.5 GB) no se empaqueta en el APK. En el primer arranque,
descargalo a `context.filesDir` mostrando progreso, validá el hash y
recién ahí `isModelReady()` pasa a `true`. (APK liviano + descarga única
con internet, coherente con el resto del proyecto.)
