package com.example.rag

import android.content.Context
import org.json.JSONObject
import java.io.InputStream

/**
 * RefugIA — Núcleo RAG on-device.
 *
 * Carga el índice embebido (assets/rag_index.json), hace búsqueda por
 * similitud coseno en memoria (sin base de datos) y arma el prompt con
 * los fragmentos más relevantes de los manuales de supervivencia.
 *
 * Lógica validada contra el motor web (JS) y el indexador del servidor:
 * produce scores idénticos y recupera el manual correcto por consulta.
 */
class RagEngine(private val context: Context) {

    data class Chunk(val text: String, val source: String, val embedding: FloatArray)
    data class Hit(val chunk: Chunk, val score: Float)

    private val chunks = ArrayList<Chunk>()
    var dims: Int = 0; private set
    var embeddingModel: String = ""; private set
    val isLoaded: Boolean get() = chunks.isNotEmpty()

    companion object {
        const val ASSET_NAME = "rag_index.json"
        private const val TOP_K = 4
        private const val MIN_SCORE = 0.25f

        val SYSTEM_PROMPT =
            "Eres RefugIA, un asistente de supervivencia offline para escenarios " +
            "post-apocalípticos. Responde SOLO con la información del contexto. " +
            "Sé conciso y directo; las vidas dependen de instrucciones claras. " +
            "Nunca sugieras llamar a emergencias o servicios externos: no existen. " +
            "Si el contexto no alcanza, dilo y pide más detalles. Responde en español " +
            "usando pasos numerados cuando convenga."
    }

    /** Carga el índice desde assets/. Costoso: llamar una vez, en background. */
    fun load() {
        if (isLoaded) return
        context.assets.open(ASSET_NAME).use { parse(it) }
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
            val emb = FloatArray(embArr.length()) { embArr.getDouble(it).toFloat() }
            chunks.add(Chunk(c.getString("text"), c.optString("source"), emb))
        }
    }

    /** Recupera los [k] fragmentos más similares (coseno = dot, normalizados). */
    fun retrieve(queryEmbedding: FloatArray, k: Int = TOP_K): List<Hit> {
        if (queryEmbedding.size != dims) return emptyList()
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

    /** Devuelve solo el bloque de contexto (para generateWithContext de Llamatik). */
    fun contextFor(queryEmbedding: FloatArray): String =
        retrieve(queryEmbedding).joinToString("\n\n---\n\n") { it.chunk.text }
}
