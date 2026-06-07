package com.refugia.survival.rag

import android.content.Context
import org.json.JSONObject
import java.io.InputStream

/**
 * RefugIA — Núcleo RAG on-device (Kotlin).
 *
 * Espejo de mobile/www/refugia-engine.js, ya probado end-to-end.
 * Carga el índice embebido (assets/rag_index.json), hace búsqueda por
 * similitud coseno en memoria (sin base de datos) y arma el prompt con
 * los fragmentos más relevantes de los manuales.
 *
 * No depende de internet. El único componente externo es el motor de
 * inferencia (ver [InferenceEngine]) que ejecuta el LLM y los embeddings.
 *
 * Uso típico:
 *   val rag = RagEngine(context).apply { load() }
 *   val prompt = rag.buildPrompt(query, queryEmbedding)
 *   val answer = inference.generate(prompt)
 */
class RagEngine(private val context: Context) {

    /** Un fragmento de manual con su embedding. */
    data class Chunk(
        val text: String,
        val source: String,
        val embedding: FloatArray,
    )

    /** Resultado de recuperación: fragmento + score de similitud. */
    data class Hit(val chunk: Chunk, val score: Float)

    private val chunks = ArrayList<Chunk>()
    var dims: Int = 0
        private set
    var embeddingModel: String = ""
        private set
    val isLoaded: Boolean get() = chunks.isNotEmpty()

    companion object {
        private const val ASSET_NAME = "rag_index.json"
        private const val TOP_K = 4
        private const val MIN_SCORE = 0.25f

        private val SYSTEM_PROMPT =
            "Eres RefugIA, un asistente de supervivencia offline para escenarios " +
            "post-apocalípticos. Responde SOLO con la información del contexto. " +
            "Sé conciso y directo; las vidas dependen de instrucciones claras. " +
            "Nunca sugieras llamar a emergencias o servicios externos: no existen. " +
            "Si el contexto no alcanza, dilo y pide más detalles. Responde en español " +
            "usando pasos numerados cuando convenga."
    }

    /**
     * Carga el índice RAG desde assets/. Llamar una sola vez (es costoso).
     * Hacerlo fuera del hilo principal.
     */
    fun load() {
        if (isLoaded) return
        context.assets.open(ASSET_NAME).use { stream -> parse(stream) }
    }

    private fun parse(stream: InputStream) {
        val json = JSONObject(stream.bufferedReader(Charsets.UTF_8).readText())
        dims = json.getInt("dims")
        embeddingModel = json.optString("embedding_model")
        val arr = json.getJSONArray("chunks")
        chunks.ensureCapacity(arr.length())
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            val embArr = c.getJSONArray("embedding")
            val emb = FloatArray(embArr.length())
            for (j in emb.indices) emb[j] = embArr.getDouble(j).toFloat()
            chunks.add(
                Chunk(
                    text = c.getString("text"),
                    source = c.optString("source"),
                    embedding = emb,
                )
            )
        }
    }

    /**
     * Recupera los [k] fragmentos más similares a [queryEmbedding].
     * Asume embeddings normalizados (coseno == producto punto), igual que
     * el exportador (all-MiniLM-L6-v2, normalize_embeddings=True).
     */
    fun retrieve(queryEmbedding: FloatArray, k: Int = TOP_K): List<Hit> {
        require(queryEmbedding.size == dims) {
            "El embedding de consulta tiene ${queryEmbedding.size} dims, se esperaban $dims"
        }
        val hits = ArrayList<Hit>(chunks.size)
        for (chunk in chunks) {
            var dot = 0f
            val e = chunk.embedding
            for (j in 0 until dims) dot += e[j] * queryEmbedding[j]
            hits.add(Hit(chunk, dot))
        }
        return hits.asSequence()
            .sortedByDescending { it.score }
            .take(k)
            .filter { it.score >= MIN_SCORE }
            .toList()
    }

    /**
     * Arma el prompt final con el contexto recuperado. Idéntico en
     * estructura al del servidor y al motor JS, para respuestas equivalentes.
     */
    fun buildPrompt(query: String, queryEmbedding: FloatArray): String {
        val context = retrieve(queryEmbedding).joinToString("\n\n---\n\n") { it.chunk.text }
        return buildString {
            append(SYSTEM_PROMPT)
            append("\n\nCONTEXTO DE LOS MANUALES:\n")
            append(context)
            append("\n\nPREGUNTA DEL SUPERVIVIENTE:\n")
            append(query)
            append("\n\nRESPUESTA DE REFUGIA:")
        }
    }
}
