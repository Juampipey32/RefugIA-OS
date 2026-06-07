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
 *   - GEN_MODEL_URL:   Llama-3.2-1B-Instruct Q4_K_M (~807 MB) — buen
 *     multilingüe (español) y más liviano que modelos de 1.5B, para que la
 *     descarga única complete en conexiones inestables.
 *   - EMBED_MODEL_URL: all-MiniLM-L6-v2 Q8_0 (~25 MB, 384 dims) — coincide
 *     con el modelo de embeddings de src/exportar_rag.py (all-MiniLM-L6-v2),
 *     necesario para que las similitudes contra rag_index.json sean válidas.
 */
object ModelDownloader {

    const val GEN_MODEL_URL =
        "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
    const val EMBED_MODEL_URL =
        "https://huggingface.co/second-state/All-MiniLM-L6-v2-Embedding-GGUF/resolve/main/all-MiniLM-L6-v2-Q8_0.gguf"

    const val GEN_MODEL_NAME = "llama-3.2-1b-instruct-q4_k_m.gguf"
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

    /**
     * Descarga con **reanudación** + reintento *mientras haya progreso*.
     *
     * Clave para conexiones inestables (celular): aunque el server corte la
     * conexión a mitad ("Software caused connection abort"), guardamos lo
     * bajado en .part y reanudamos con Range. Solo nos rendimos si pasan
     * varios intentos SIN avanzar un solo byte. Así un archivo grande
     * termina aunque la conexión se corte decenas de veces.
     */
    private fun download(urlStr: String, dest: File, onProgress: (Float) -> Unit) {
        val tmp = File(dest.absolutePath + ".part")
        val maxNoProgress = 10 // intentos seguidos sin bajar nada → abandonar
        var noProgress = 0
        while (true) {
            val before = if (tmp.exists()) tmp.length() else 0L
            val complete = try {
                downloadOnce(urlStr, tmp, onProgress)
            } catch (e: java.io.InterruptedIOException) {
                throw e // timeout/cancelación: propagar
            } catch (e: Exception) {
                false // corte de red: se evalúa el progreso abajo y se reintenta
            }
            if (complete) break
            val after = if (tmp.exists()) tmp.length() else 0L
            if (after > before) {
                noProgress = 0 // avanzó algo → seguir reintentando sin penalizar
            } else {
                noProgress++
                if (noProgress >= maxNoProgress) {
                    throw java.io.IOException(
                        "No se pudo descargar ${dest.name}: la conexión se corta sin avanzar " +
                            "(${after / 1_000_000}MB bajados). Probá con mejor señal de WiFi."
                    )
                }
            }
            Thread.sleep(minOf(2000L * noProgress.coerceAtLeast(1), 5000L))
        }
        if (!tmp.renameTo(dest)) {
            tmp.copyTo(dest, overwrite = true)
            tmp.delete()
        }
    }

    /** Un intento de descarga. Devuelve true si el archivo quedó completo. */
    private fun downloadOnce(urlStr: String, tmp: File, onProgress: (Float) -> Unit): Boolean {
        val existing = if (tmp.exists()) tmp.length() else 0L
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000 // tolera stalls; si excede, corta y reanuda
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "RefugIA-Android")
            setRequestProperty("Accept-Encoding", "identity") // sin gzip → tamaños exactos
            if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
        }
        conn.connect()
        val code = conn.responseCode
        if (code !in 200..299) {
            throw java.io.IOException("HTTP $code (${conn.responseMessage})")
        }
        val resuming = code == 206 && existing > 0 // 206 = Partial Content
        if (!resuming && existing > 0) tmp.delete() // el server ignoró Range → de cero

        // Tamaño total real: preferimos el de Content-Range ("bytes a-b/total").
        val totalFromRange = conn.getHeaderField("Content-Range")
            ?.substringAfter('/', "")?.toLongOrNull()
        val total = totalFromRange
            ?: if (resuming) existing + conn.contentLengthLong.coerceAtLeast(1L)
               else conn.contentLengthLong.coerceAtLeast(1L)

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
        return tmp.length() >= total // completo solo si llegamos al tamaño real
    }
}
