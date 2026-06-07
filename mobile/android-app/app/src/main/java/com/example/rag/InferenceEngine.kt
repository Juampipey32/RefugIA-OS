package com.example.rag

/**
 * Abstracción del motor de inferencia local. Desacopla el agente del
 * backend concreto (llama.cpp vía Llamatik, MediaPipe, ONNX, ...).
 *
 * IMPORTANTE: el modelo de [embed] DEBE ser all-MiniLM-L6-v2, 384 dims,
 * normalizado — idéntico al de src/exportar_rag.py — o la búsqueda coseno
 * no recupera nada útil.
 */
interface InferenceEngine {

    /** ¿Los modelos están cargados y listos para inferir? */
    fun isReady(): Boolean

    /** Carga los modelos GGUF (generación + embeddings). Llamar en background. */
    fun load(genModelPath: String, embedModelPath: String): Boolean

    /** Embedding de [text]: 384 floats normalizados (all-MiniLM-L6-v2). */
    fun embed(text: String): FloatArray

    /**
     * Genera la respuesta del LLM dado el prompt de sistema, el bloque de
     * contexto recuperado y la pregunta del usuario. Emite tokens por
     * [onToken] (streaming) y devuelve el texto completo al terminar.
     */
    fun generate(
        systemPrompt: String,
        contextBlock: String,
        userPrompt: String,
        onToken: (String) -> Unit,
    ): String
}
