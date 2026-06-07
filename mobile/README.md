# RefugIA Mobile — App offline para Android/iOS

App de supervivencia con IA **100% on-device**: un LLM chico cuantizado y
el RAG de los manuales corren dentro del teléfono, sin servidor ni internet
(salvo la descarga inicial del modelo).

## Arquitectura elegida

- **Envoltorio**: [Capacitor](https://capacitorjs.com) — reutiliza el
  frontend web existente (`../frontend/index.html`) dentro de un APK/IPA.
- **Motor LLM**: `llama.cpp` embebido como plugin nativo (inferencia local).
- **Modelo**: chico y cuantizado (recomendado Gemma 2 2B Q4 o Qwen2.5 1.5B
  Q4 para buen español). **No se empaqueta** en el APK: se descarga en el
  primer arranque (APK liviano + descarga única con internet).
- **RAG sin base de datos**: los chunks + embeddings se pre-calculan en la
  PC con `../src/exportar_rag.py` y se embeben como asset
  (`assets/rag_index.json`). En el teléfono la búsqueda es por similitud
  coseno en memoria — no hace falta ChromaDB ni indexar en el dispositivo.

## Flujo de una consulta (en el teléfono)

1. El usuario escribe una pregunta.
2. Se calcula el embedding de la pregunta (modelo de embeddings local).
3. Búsqueda coseno contra `rag_index.json` → top-k chunks relevantes.
4. Se arma el prompt con esos chunks como contexto.
5. El LLM local (llama.cpp) genera la respuesta.

Es el mismo patrón RAG que el servidor, pero todo dentro del APK.

## Generar el índice RAG embebible

Desde la raíz del repo, con el venv activado:

```bash
python src/exportar_rag.py
# genera mobile/assets/rag_index.json (se regenera al cambiar manuales/)
```

> `assets/rag_index.json` es un artefacto generado: NO se versiona.
> Regeneralo tras editar los manuales.

## Estado

- [x] Exportador de RAG (chunks + embeddings → JSON portable) — **listo y probado**
- [x] Contenido base de manuales en español — **listo**
- [ ] Proyecto Capacitor (config + build del APK)
- [ ] Plugin nativo llama.cpp (inferencia on-device)
- [ ] Descarga y gestión del modelo en el primer arranque
- [ ] Búsqueda coseno + armado de prompt en el cliente

## Requisitos para compilar (en tu máquina)

- Node.js + `@capacitor/cli`
- Android Studio + SDK (para el APK) / Xcode (para iOS)
- NDK para compilar el plugin nativo de llama.cpp
