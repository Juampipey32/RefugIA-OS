package com.refugia.survival.rag

/**
 * Abstracción del motor de inferencia local. La implementación real
 * (ver LlamaCppEngine más abajo en el README) envuelve llama.cpp vía JNI.
 *
 * Mantener esta interfaz permite:
 *   - Testear el RAG con un fake (sin modelo).
 *   - Cambiar de backend (llama.cpp / MediaPipe / ONNX) sin tocar el agente.
 *
 * IMPORTANTE: tanto [embed] como el modelo de embeddings del exportador
 * deben ser all-MiniLM-L6-v2, 384 dims, normalizado. Si no coinciden, la
 * búsqueda coseno no recupera nada útil.
 */
interface InferenceEngine {

    /** ¿El modelo está descargado y cargado en memoria? */
    fun isModelReady(): Boolean

    /**
     * Embedding de [text]: 384 floats normalizados (all-MiniLM-L6-v2).
     * Debe ser idéntico al usado por src/exportar_rag.py.
     */
    fun embed(text: String): FloatArray

    /**
     * Genera la respuesta del LLM para [prompt]. [onToken] permite streaming
     * token a token para mostrar la respuesta a medida que se genera.
     */
    fun generate(prompt: String, onToken: ((String) -> Unit)? = null): String
}
