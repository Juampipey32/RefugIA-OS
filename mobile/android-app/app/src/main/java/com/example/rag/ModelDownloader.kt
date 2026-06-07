package com.example.rag

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Descarga los modelos GGUF al primer arranque (APK liviano + descarga
 * única con internet). Después la app funciona 100% offline.
 *
 * Los archivos viven en context.filesDir/models/. Si ya existen, no se
 * vuelven a bajar.
 *
 * NOTA: URLs de descarga directa de Hugging Face (verificadas, HTTP 200).
 *   - GEN_MODEL_URL:   Qwen2.5-1.5B-Instruct Q4_K_M (~1.12 GB) — repo
 *     oficial de Qwen, buen rendimiento en español.
 *   - EMBED_MODEL_URL: all-MiniLM-L6-v2 Q8_0 (~25 MB, 384 dims) — coincide
 *     con el modelo de embeddings de src/exportar_rag.py (all-MiniLM-L6-v2),
 *     necesario para que las similitudes contra rag_index.json sean válidas.
 */
object ModelDownloader {

    const val GEN_MODEL_URL =
        "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
    const val EMBED_MODEL_URL =
        "https://huggingface.co/second-state/All-MiniLM-L6-v2-Embedding-GGUF/resolve/main/all-MiniLM-L6-v2-Q8_0.gguf"

    const val GEN_MODEL_NAME = "qwen2.5-1.5b-instruct-q4_k_m.gguf"
    const val EMBED_MODEL_NAME = "all-MiniLM-L6-v2-Q8_0.gguf"

    fun modelsDir(context: Context): File =
        File(context.filesDir, "models").apply { mkdirs() }

    fun genModelFile(context: Context) = File(modelsDir(context), GEN_MODEL_NAME)
    fun embedModelFile(context: Context) = File(modelsDir(context), EMBED_MODEL_NAME)

    fun allPresent(context: Context): Boolean =
        genModelFile(context).exists() && embedModelFile(context).exists()

    /**
     * Descarga ambos modelos si faltan. [onProgress] recibe (etiqueta, 0..1).
     * Lanzar en Dispatchers.IO.
     */
    suspend fun ensureModels(
        context: Context,
        onProgress: (label: String, fraction: Float) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!embedModelFile(context).exists()) {
                download(EMBED_MODEL_URL, embedModelFile(context)) { f ->
                    onProgress("Modelo de embeddings", f)
                }
            }
            if (!genModelFile(context).exists()) {
                download(GEN_MODEL_URL, genModelFile(context)) { f ->
                    onProgress("Modelo de lenguaje", f)
                }
            }
            allPresent(context)
        } catch (e: Exception) {
            false
        }
    }

    private fun download(urlStr: String, dest: File, onProgress: (Float) -> Unit) {
        val tmp = File(dest.absolutePath + ".part")
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        conn.connect()
        val total = conn.contentLengthLong.coerceAtLeast(1L)
        conn.inputStream.use { input ->
            tmp.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                var read: Int
                var done = 0L
                while (input.read(buf).also { read = it } >= 0) {
                    out.write(buf, 0, read)
                    done += read
                    onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
        if (!tmp.renameTo(dest)) {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
    }
}
