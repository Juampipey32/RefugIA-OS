// ============================================================
// RefugIA Mobile — Motor de inferencia on-device (capa JS)
// ============================================================
// Implementa el RAG en el cliente y expone window.RefugIAEngine,
// que el frontend (../../frontend/index.html) usa de forma
// transparente: si existe, el chat corre 100% en el teléfono.
//
// Piezas conectables (inyectadas por la capa nativa Capacitor):
//   - RefugIANative.embed(text)      -> Float32Array (embeddings)
//   - RefugIANative.generate(prompt) -> string (LLM llama.cpp)
//   - RefugIANative.modelReady()     -> bool
// Mientras el plugin nativo no esté, el motor reporta "cargando"
// y el frontend cae a su modo simulación. Así el HTML es único.
// ============================================================

(function () {
  'use strict';

  const INDEX_URL = 'assets/rag_index.json'; // embebido en el APK
  const TOP_K = 4;            // chunks de contexto por consulta
  const MIN_SCORE = 0.25;     // descarta contexto irrelevante

  const SYSTEM_PROMPT =
    'Eres RefugIA, un asistente de supervivencia offline para escenarios ' +
    'post-apocalípticos. Responde SOLO con la información del contexto. ' +
    'Sé conciso y directo; las vidas dependen de instrucciones claras. ' +
    'Nunca sugieras llamar a emergencias o servicios externos: no existen. ' +
    'Si el contexto no alcanza, dilo y pide más detalles. Responde en español ' +
    'usando pasos numerados cuando convenga.';

  let index = null;       // { dims, count, chunks: [{text, source, embedding}] }
  let matrix = null;      // Float32Array aplanada [count * dims] para coseno
  let loadError = null;

  // --- Acceso al puente nativo (lo inyecta Capacitor) ---
  function native() {
    return window.RefugIANative || null;
  }

  // --- Carga y cachea el índice RAG embebido ---
  async function loadIndex() {
    if (index) return index;
    const resp = await fetch(INDEX_URL);
    if (!resp.ok) throw new Error('No se pudo cargar el índice RAG: ' + resp.status);
    index = await resp.json();

    // Aplanamos los embeddings a un solo Float32Array (más rápido y liviano).
    const { count, dims, chunks } = index;
    matrix = new Float32Array(count * dims);
    for (let i = 0; i < count; i++) {
      const e = chunks[i].embedding;
      for (let j = 0; j < dims; j++) matrix[i * dims + j] = e[j];
      chunks[i].embedding = null; // liberar el array duplicado
    }
    return index;
  }

  // --- Búsqueda por similitud coseno (vectores ya normalizados → dot) ---
  function topK(queryVec, k) {
    const { count, dims, chunks } = index;
    const scores = new Array(count);
    for (let i = 0; i < count; i++) {
      let dot = 0;
      const off = i * dims;
      for (let j = 0; j < dims; j++) dot += matrix[off + j] * queryVec[j];
      scores[i] = { i, score: dot };
    }
    scores.sort((a, b) => b.score - a.score);
    return scores.slice(0, k)
      .filter((s) => s.score >= MIN_SCORE)
      .map((s) => ({ ...chunks[s.i], score: s.score }));
  }

  function buildPrompt(query, contextChunks) {
    const context = contextChunks.map((c) => c.text).join('\n\n---\n\n');
    return (
      SYSTEM_PROMPT +
      '\n\nCONTEXTO DE LOS MANUALES:\n' + context +
      '\n\nPREGUNTA DEL SUPERVIVIENTE:\n' + query +
      '\n\nRESPUESTA DE REFUGIA:'
    );
  }

  // ----------------------------------------------------------
  //  API pública: window.RefugIAEngine
  // ----------------------------------------------------------
  const Engine = {
    /** Estado para el badge de la UI. */
    async status() {
      const n = native();
      const modelReady = n && typeof n.modelReady === 'function'
        ? await n.modelReady() : false;
      let indexReady = !!index;
      if (!indexReady && !loadError) {
        try { await loadIndex(); indexReady = true; }
        catch (e) { loadError = e; }
      }
      return {
        ready: !!(modelReady && indexReady),
        modelReady: !!modelReady,
        indexReady,
        label: !modelReady ? 'DESCARGAR MODELO' : (!indexReady ? 'CARGANDO' : 'ON-DEVICE'),
      };
    },

    /** Responde una consulta usando RAG + LLM local. */
    async chat(query) {
      const n = native();
      if (!n || typeof n.generate !== 'function' || typeof n.embed !== 'function') {
        throw new Error('Motor nativo no disponible'); // → frontend cae a simulación
      }
      await loadIndex();

      const qVec = await n.embed(query);
      const ctx = topK(qVec, TOP_K);
      const prompt = buildPrompt(query, ctx);
      return await n.generate(prompt);
    },

    /** Recupación pura (para debug / tests sin LLM). */
    async retrieve(query, k = TOP_K) {
      const n = native();
      if (!n || typeof n.embed !== 'function') throw new Error('embed nativo no disponible');
      await loadIndex();
      return topK(await n.embed(query), k);
    },
  };

  window.RefugIAEngine = Engine;
})();
