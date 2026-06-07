package com.example.rag

import com.llamatik.library.platform.GenStream
import com.llamatik.library.platform.LlamaBridge

/**
 * Implementación de [InferenceEngine] con Llamatik (llama.cpp on-device).
 *
 * ⚠️ PUNTO ÚNICO DE AJUSTE DE API ⚠️
 * Si la compilación falla por nombres de Llamatik, el único lugar a tocar
 * es ESTA clase. Clases en el package com.llamatik.library.platform
 * (verificado contra el sources.jar del artefacto). Firmas usadas:
 *   - LlamaBridge.initGenerateModel(path): Boolean
 *   - LlamaBridge.initEmbedModel(path): Boolean
 *   - LlamaBridge.embed(text): FloatArray
 *   - LlamaBridge.generateStreamWithContext(systemPrompt, contextBlock,
 *         userPrompt, callback: GenStream)
 *   - GenStream { onDelta(text); onComplete(); onError(message) }
 *
 * Existe además una sobrecarga con lambdas (generateWithContextStream con
 * onDelta/onDone/onError). Si la de GenStream no existe en tu versión,
 * cambiá el cuerpo de generate() por esa variante.
 */
class LlamatikEngine : InferenceEngine {

    @Volatile private var ready = false

    override fun isReady(): Boolean = ready

    override fun load(genModelPath: String, embedModelPath: String): Boolean {
        val genOk = try {
            LlamaBridge.initGenerateModel(genModelPath)
        } catch (e: Throwable) {
            throw IllegalStateException("Fallo al cargar el modelo de generación: ${e.message}", e)
        }
        if (!genOk) {
            throw IllegalStateException(
                "El motor rechazó el modelo de generación (¿RAM insuficiente o GGUF incompatible?)."
            )
        }
        val embOk = try {
            LlamaBridge.initEmbedModel(embedModelPath)
        } catch (e: Throwable) {
            throw IllegalStateException("Fallo al cargar el modelo de embeddings: ${e.message}", e)
        }
        if (!embOk) {
            throw IllegalStateException("El motor rechazó el modelo de embeddings.")
        }
        ready = true
        return true
    }

    override fun embed(text: String): FloatArray = LlamaBridge.embed(text)

    override fun generate(
        systemPrompt: String,
        contextBlock: String,
        userPrompt: String,
        onToken: (String) -> Unit,
    ): String {
        val sb = StringBuilder()
        var error: String? = null
        var done = false

        LlamaBridge.generateStreamWithContext(
            systemPrompt = systemPrompt,
            contextBlock = contextBlock,
            userPrompt = userPrompt,
            callback = object : GenStream {
                override fun onDelta(text: String) { sb.append(text); onToken(text) }
                override fun onComplete() { done = true }
                override fun onError(message: String) { error = message; done = true }
            },
        )

        // generateStreamWithContext es bloqueante hasta onComplete/onError.
        return error?.let { "Error de inferencia: $it" } ?: sb.toString().trim()
    }
}
