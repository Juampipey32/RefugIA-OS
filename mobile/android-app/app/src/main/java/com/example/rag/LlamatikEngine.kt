package com.example.rag

import com.llamatik.library.GenStream
import com.llamatik.library.LlamaBridge

/**
 * Implementación de [InferenceEngine] con Llamatik (llama.cpp on-device).
 *
 * ⚠️ PUNTO ÚNICO DE AJUSTE DE API ⚠️
 * Si la compilación falla por nombres de Llamatik (versión 1.7.0), el
 * único lugar a tocar es ESTA clase. Las firmas usadas, según el README:
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
        val genOk = LlamaBridge.initGenerateModel(genModelPath)
        val embOk = LlamaBridge.initEmbedModel(embedModelPath)
        ready = genOk && embOk
        return ready
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
