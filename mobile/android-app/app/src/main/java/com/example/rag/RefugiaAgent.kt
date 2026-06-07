package com.example.rag

import android.content.Context
import com.llamatik.library.GenStream
import com.llamatik.library.LlamaBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * RefugIA — Agente de supervivencia 100% on-device.
 *
 * Orquesta el pipeline RAG con un LLM local (llama.cpp vía Llamatik):
 *   embedding de la pregunta → recuperación de contexto → generación.
 *
 * Reemplaza la "IA" de demo (generateSurvivalReply) por inferencia real
 * sobre los manuales, sin internet (salvo la descarga inicial del modelo).
 *
 * Modelos (GGUF, se descargan al primer arranque, ver ModelDownloader):
 *   - Generación: un LLM chico cuantizado (Gemma 2 2B Q4 / Qwen2.5 1.5B Q4)
 *   - Embeddings: all-MiniLM-L6-v2 (GGUF), 384 dims, normalizado — DEBE
 *     coincidir con src/exportar_rag.py o la búsqueda no recupera nada.
 */
class RefugiaAgent(private val context: Context) {

    enum class State { DESCARGAR_MODELO, CARGANDO, LISTO }

    private val rag = RagEngine(context)
    @Volatile private var modelsReady = false

    /**
     * Carga índice + modelos GGUF. Llamar en background (Dispatchers.IO).
     * Devuelve true si quedó todo listo para responder.
     */
    suspend fun warmUp(genModelFile: File, embedModelFile: File): Boolean =
        withContext(Dispatchers.IO) {
            if (!rag.isLoaded) rag.load()
            if (!genModelFile.exists() || !embedModelFile.exists()) {
                modelsReady = false
                return@withContext false
            }
            val genOk = LlamaBridge.initGenerateModel(genModelFile.absolutePath)
            val embOk = LlamaBridge.initEmbedModel(embedModelFile.absolutePath)
            modelsReady = genOk && embOk && rag.isLoaded
            modelsReady
        }

    fun state(): State = when {
        !modelsReady -> if (rag.isLoaded) State.DESCARGAR_MODELO else State.CARGANDO
        else -> State.LISTO
    }

    /**
     * Responde una consulta con RAG + LLM local, en streaming.
     * Llamar desde una corrutina; emite tokens por [onToken].
     */
    suspend fun ask(query: String, onToken: (String) -> Unit): String =
        withContext(Dispatchers.Default) {
            if (query.isBlank()) return@withContext "Escribe tu consulta de supervivencia."
            if (!modelsReady) {
                return@withContext "El modelo de IA aún no está listo. " +
                    "Descárgalo para activar el asistente de supervivencia."
            }

            val queryEmbedding = LlamaBridge.embed(query)
            val contextBlock = rag.contextFor(queryEmbedding)

            suspendCancellableCoroutine { cont ->
                val sb = StringBuilder()
                LlamaBridge.generateStreamWithContext(
                    systemPrompt = RagEngine.SYSTEM_PROMPT,
                    contextBlock = contextBlock,
                    userPrompt = query,
                    callback = object : GenStream {
                        override fun onDelta(text: String) {
                            sb.append(text)
                            onToken(text)
                        }
                        override fun onComplete() {
                            if (cont.isActive) cont.resume(sb.toString().trim())
                        }
                        override fun onError(message: String) {
                            if (cont.isActive) cont.resume(
                                "Error de inferencia: $message"
                            )
                        }
                    }
                )
            }
        }
}
