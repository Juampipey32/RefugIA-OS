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
     * Lanzar en Dispatchers.IO. **Lanza excepción con detalle si algo falla**
     * (no la traga), para que la UI muestre la causa real.
     */
    suspend fun ensureModels(
        context: Context,
        onProgress: (label: String, fraction: Float) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        // Embeddings primero (chico): si la red anda, baja en segundos.
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
    }

    /** Descarga con reintentos + reanudación. Lanza IOException con detalle. */
    private fun download(urlStr: String, dest: File, onProgress: (Float) -> Unit) {
        val tmp = File(dest.absolutePath + ".part")
        val maxAttempts = 4
        var attempt = 0
        while (true) {
            attempt++
            try {
                downloadOnce(urlStr, tmp, onProgress)
                break
            } catch (e: Exception) {
                if (attempt >= maxAttempts) {
                    throw java.io.IOException(
                        "No se pudo descargar ${dest.name} tras $maxAttempts intentos: ${e.message}", e
                    )
                }
                Thread.sleep(2000L * attempt) // backoff: 2s, 4s, 6s
            }
        }
        if (!tmp.renameTo(dest)) {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
    }

    private fun downloadOnce(urlStr: String, tmp: File, onProgress: (Float) -> Unit) {
        val existing = if (tmp.exists()) tmp.length() else 0L
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 120_000 // tolera stalls de red en celulares
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "RefugIA-Android")
            if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
        }
        conn.connect()
        val code = conn.responseCode
        if (code !in 200..299) {
            throw java.io.IOException("HTTP $code (${conn.responseMessage})")
        }
        val resuming = code == 206 && existing > 0 // 206 = Partial Content
        if (!resuming && existing > 0) tmp.delete() // el server ignoró Range → de cero
        val remaining = conn.contentLengthLong.coerceAtLeast(1L)
        val total = if (resuming) existing + remaining else remaining

        // Chequeo de espacio libre (con 50 MB de colchón).
        val free = tmp.parentFile?.usableSpace ?: Long.MAX_VALUE
        if (free < (total - existing) + 50_000_000L) {
            throw java.io.IOException(
                "Espacio insuficiente: faltan ${(total - existing) / 1_000_000}MB y hay ${free / 1_000_000}MB libres"
            )
        }

        conn.inputStream.use { input ->
            java.io.FileOutputStream(tmp, resuming).use { out ->
                val buf = ByteArray(64 * 1024)
                var read: Int
                var done = if (resuming) existing else 0L
                while (input.read(buf).also { read = it } >= 0) {
                    out.write(buf, 0, read)
                    done += read
                    onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                }
                out.flush()
            }
        }
    }
}
