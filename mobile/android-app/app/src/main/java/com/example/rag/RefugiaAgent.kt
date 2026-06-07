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

    /** Un turno de la conversación (para memoria multi-turno). */
    private data class Turn(val user: String, val assistant: String)
    private val history = ArrayList<Turn>()

    companion object {
        /** Cuántos turnos previos recordar (acota el prompt para el LLM de 1B). */
        private const val MAX_TURNS = 4
        /** Tope de caracteres por respuesta guardada en el historial. */
        private const val MAX_ANSWER_CHARS = 500
    }

    /** Borra la memoria de la conversación (para un "reset"). */
    fun resetConversation() = history.clear()

    /**
     * Carga índice + modelos GGUF. Llamar en background (Dispatchers.IO).
     * Devuelve true si quedó todo listo para responder.
     */
    suspend fun warmUp(genModelFile: File, embedModelFile: File): Boolean =
        withContext(Dispatchers.IO) {
            if (!rag.isLoaded) rag.load()
            if (!rag.isLoaded) {
                throw IllegalStateException("No se pudo cargar el índice RAG (assets/rag_index.json).")
            }
            if (!genModelFile.exists()) {
                throw IllegalStateException("Falta el modelo de generación: ${genModelFile.name}")
            }
            if (!embedModelFile.exists()) {
                throw IllegalStateException("Falta el modelo de embeddings: ${embedModelFile.name}")
            }
            // inference.load lanza IllegalStateException con detalle si falla.
            inference.load(genModelFile.absolutePath, embedModelFile.absolutePath)
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
        withContext(Dispatchers.IO) {
            if (query.isBlank()) return@withContext "Escribe tu consulta de supervivencia."
            if (!inference.isReady()) {
                return@withContext "El modelo de IA aún no está listo. " +
                    "Descárgalo para activar el asistente de supervivencia."
            }

            // Recuperación: en repreguntas cortas/anafóricas ("¿y si no tengo
            // eso?"), sumar el turno previo mejora qué manual se recupera.
            val retrievalText = history.lastOrNull()?.let { "${it.user} $query" } ?: query
            val queryEmbedding = inference.embed(retrievalText)
            val contextBlock = rag.contextFor(queryEmbedding)

            val answer = inference.generate(
                systemPrompt = RagEngine.SYSTEM_PROMPT,
                contextBlock = contextBlock,
                userPrompt = buildUserPrompt(query),
                onToken = onToken,
            )

            // Guardar el turno en memoria (acotada). Truncamos la copia del
            // historial (no la respuesta mostrada) para no desbordar el
            // contexto chico del modelo de 1B en turnos siguientes.
            history.add(Turn(query, answer.take(MAX_ANSWER_CHARS)))
            while (history.size > MAX_TURNS) history.removeAt(0)
            answer
        }

    /**
     * Arma el prompt del usuario incluyendo el historial reciente para dar
     * continuidad conversacional. El contexto RAG va por separado (solo de la
     * pregunta actual), así no se contamina con turnos viejos.
     */
    private fun buildUserPrompt(query: String): String {
        if (history.isEmpty()) return query
        val sb = StringBuilder("Conversación previa (para dar continuidad):\n")
        for (t in history) {
            sb.append("Usuario: ").append(t.user).append('\n')
            sb.append("RefugIA: ").append(t.assistant).append('\n')
        }
        sb.append("\nNueva consulta del usuario: ").append(query)
        return sb.toString()
    }
}
