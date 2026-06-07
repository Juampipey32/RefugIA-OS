package com.refugia.survival.rag

import android.content.Context

/**
 * RefugIA — Agente de supervivencia on-device.
 *
 * Este es el reemplazo directo de la llamada a la API de Gemini en la nube
 * que genera AI Studio. En vez de `geminiClient.generateContent(...)`, la
 * ViewModel llama a `agent.ask(query)` y obtiene la respuesta 100% local.
 *
 * Orquesta: embedding de la pregunta → recuperación RAG → prompt → LLM.
 *
 * Integración en una app de AI Studio (Kotlin + Compose):
 *   // 1) crear una vez (Application o ViewModel), fuera del hilo principal
 *   val agent = RefugiaAgent(context, LlamaCppEngine(context))
 *   agent.warmUp()
 *
 *   // 2) en la ViewModel, reemplazar la llamada a Gemini:
 *   viewModelScope.launch(Dispatchers.Default) {
 *       val respuesta = agent.ask(textoDelUsuario) { token -> /* stream a la UI */ }
 *   }
 */
class RefugiaAgent(
    context: Context,
    private val inference: InferenceEngine,
) {
    private val rag = RagEngine(context)

    /** Estado para reflejar en la UI (badge). */
    enum class State { CARGANDO, DESCARGAR_MODELO, LISTO }

    /** Carga el índice RAG. Llamar en background antes del primer ask(). */
    fun warmUp() {
        if (!rag.isLoaded) rag.load()
    }

    fun state(): State = when {
        !inference.isModelReady() -> State.DESCARGAR_MODELO
        !rag.isLoaded -> State.CARGANDO
        else -> State.LISTO
    }

    /**
     * Responde una consulta de supervivencia con RAG + LLM local.
     * Lanzar en un dispatcher de background (Dispatchers.Default).
     *
     * @param onToken callback opcional para streaming token a token.
     */
    fun ask(query: String, onToken: ((String) -> Unit)? = null): String {
        if (query.isBlank()) return "Escribe tu consulta de supervivencia."
        if (!inference.isModelReady()) {
            return "El modelo de IA aún no está listo. Descárgalo para activar el asistente."
        }
        warmUp()

        val queryEmbedding = inference.embed(query)
        val prompt = rag.buildPrompt(query, queryEmbedding)
        return inference.generate(prompt, onToken).trim()
    }
}
