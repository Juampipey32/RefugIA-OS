package com.example.rag

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * RefugIA — Agente de supervivencia 100% on-device.
 *
 * Orquesta el pipeline RAG con un LLM local (vía [InferenceEngine]):
 *   embedding de la pregunta → recuperación de contexto → generación.
 *
 * Reemplaza la "IA" de demo (generateSurvivalReply) por inferencia real
 * sobre los manuales, sin internet (salvo la descarga inicial del modelo).
 *
 * El backend concreto (llama.cpp vía Llamatik) está aislado en
 * [LlamatikEngine]; este agente no depende de él directamente.
 */
class RefugiaAgent(
    private val context: Context,
    private val inference: InferenceEngine = LlamatikEngine(),
) {

    enum class State { DESCARGAR_MODELO, CARGANDO, LISTO }

    private val rag = RagEngine(context)

    /**
     * Carga índice + modelos GGUF. Llamar en background (Dispatchers.IO).
     * Devuelve true si quedó todo listo para responder.
     */
    suspend fun warmUp(genModelFile: File, embedModelFile: File): Boolean =
        withContext(Dispatchers.IO) {
            if (!rag.isLoaded) rag.load()
            if (!genModelFile.exists() || !embedModelFile.exists()) return@withContext false
            val ok = inference.load(genModelFile.absolutePath, embedModelFile.absolutePath)
            ok && rag.isLoaded
        }

    fun state(): State = when {
        !inference.isReady() -> if (rag.isLoaded) State.DESCARGAR_MODELO else State.CARGANDO
        else -> State.LISTO
    }

    /**
     * Responde una consulta con RAG + LLM local, en streaming.
     * Llamar desde una corrutina; emite tokens por [onToken].
     */
    suspend fun ask(query: String, onToken: (String) -> Unit): String =
        withContext(Dispatchers.Default) {
            if (query.isBlank()) return@withContext "Escribe tu consulta de supervivencia."
            if (!inference.isReady()) {
                return@withContext "El modelo de IA aún no está listo. " +
                    "Descárgalo para activar el asistente de supervivencia."
            }

            val queryEmbedding = inference.embed(query)
            val contextBlock = rag.contextFor(queryEmbedding)
            inference.generate(
                systemPrompt = RagEngine.SYSTEM_PROMPT,
                contextBlock = contextBlock,
                userPrompt = query,
                onToken = onToken,
            )
        }
}
