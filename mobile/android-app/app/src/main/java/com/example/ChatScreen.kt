package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.rag.ModelDownloader
import com.example.rag.RefugiaAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

data class Message(
    val id: String,
    val sender: String,
    val text: String,
    val isUser: Boolean,
    val streaming: Boolean = false, // true mientras el LLM piensa/escribe
)

@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val messages = remember { mutableStateListOf<Message>() }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // Agente RefugIA on-device (RAG + LLM local). Estado de readiness.
    val agent = remember { RefugiaAgent(context) }
    var ready by remember { mutableStateOf(false) }
    var statusLabel by remember { mutableStateOf("INICIANDO SISTEMA...") }
    // true mientras se genera una respuesta: bloquea el input y evita encimar.
    var generating by remember { mutableStateOf(false) }

    fun addSys(text: String) =
        messages.add(Message(UUID.randomUUID().toString(), "SYS", text, false))

    // Secuencia de arranque: índice RAG + (descarga) + carga de modelos.
    LaunchedEffect(Unit) {
        SoundManager.init()
        addSys("REFUGIA OS v1.0 — INICIANDO")
        addSys("CARGANDO MANUALES DE SUPERVIVENCIA...")

        coroutineScope.launch(Dispatchers.IO) {
            try {
                // 1) Asegurar modelos GGUF (descarga única en el primer arranque).
                if (!ModelDownloader.allPresent(context)) {
                    statusLabel = "DESCARGANDO MODELO DE IA..."
                    addSys("DESCARGANDO MODELO DE IA (SOLO LA PRIMERA VEZ, ~0.8 GB)...")
                    addSys("USA WIFI. SE PUEDE REANUDAR SI SE CORTA.")
                    ModelDownloader.ensureModels(context) { label, frac ->
                        statusLabel = "$label ${(frac * 100).toInt()}%"
                    }
                    addSys("MODELOS DESCARGADOS OK.")
                }
                // 2) Cargar índice + modelos.
                statusLabel = "CARGANDO MOTOR DE IA..."
                val ok = agent.warmUp(
                    genModelFile = ModelDownloader.genModelFile(context),
                    embedModelFile = ModelDownloader.embedModelFile(context),
                )
                ready = ok
                statusLabel = if (ok) "RAG ACTIVO — ON-DEVICE" else "MODO DEGRADADO"
                addSys(
                    if (ok) "SISTEMA OPERATIVO. ESCRIBE TU CONSULTA."
                    else "NO SE PUDO CARGAR EL MOTOR (razón desconocida)."
                )
            } catch (e: Exception) {
                ready = false
                statusLabel = "MODO DEGRADADO — ERROR"
                // Mostrar la causa REAL para poder diagnosticar.
                addSys("ERROR: ${e.message ?: e.javaClass.simpleName}")
                addSys("Verificá conexión/espacio y reiniciá la app para reintentar.")
            }
        }

        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignorar error de foco en preview o ciertos estados
        }
    }

    // Auto-scroll al fondo cuando llega un mensaje nuevo o crece el streaming.
    // Scroll instantáneo: durante el streaming se dispara muy seguido y una
    // animación competiría consigo misma.
    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header con estado del sistema
            Text(
                text = "REFUGIA OS [$statusLabel]",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Lista de mensajes
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    TypewriterMessage(msg)
                }
            }

            // Caja de entrada
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    // El prompt se atenúa mientras el sistema está ocupado.
                    text = if (generating) "WAIT> " else "USER> ",
                    color = if (generating) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
                BasicTextField(
                    value = inputText,
                    onValueChange = {
                        if (it.length > inputText.length) SoundManager.playKeystroke()
                        inputText = it
                    },
                    enabled = !generating, // bloqueado mientras responde
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = if (generating) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.primary
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val userText = inputText.trim()
                            if (userText.isNotBlank() && !generating) {
                                messages.add(Message(UUID.randomUUID().toString(), "USER", userText, true))
                                inputText = ""

                                if (!ready) {
                                    // Aún sin modelo: avisar el estado y no generar.
                                    addSys(statusLabel)
                                } else {
                                    generating = true
                                    // Mensaje del bot: arranca vacío con cursor
                                    // (pensando) y se completa con streaming.
                                    val botId = UUID.randomUUID().toString()
                                    messages.add(Message(botId, "REFUGIA", "", false, streaming = true))
                                    val botIndex = messages.lastIndex

                                    coroutineScope.launch {
                                        try {
                                            agent.ask(userText) { token ->
                                                val cur = messages[botIndex]
                                                messages[botIndex] = cur.copy(text = cur.text + token)
                                                SoundManager.playReceive()
                                            }
                                        } finally {
                                            // Apagar el cursor y desbloquear el input.
                                            val done = messages[botIndex]
                                            messages[botIndex] = done.copy(streaming = false)
                                            generating = false
                                        }
                                    }
                                }
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) { innerTextField() }
                    }
                )
            }
        }

        // Overlay de scanlines CRT
        CrtOverlay()
    }
}

@Composable
fun TypewriterMessage(message: Message) {
    // El bot llega en streaming token a token desde el agente; el usuario y
    // los mensajes de sistema, directos. Mientras está en streaming (incluido
    // el rato que "piensa" antes del primer token) mostramos un cursor de
    // bloque que parpadea, como una terminal real.
    val senderColor =
        if (message.isUser) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.primary

    val cursor = if (message.streaming) {
        val blink by produceState(initialValue = true) {
            while (true) {
                delay(500)
                value = !value
            }
        }
        if (blink) "▋" else " "
    } else ""

    Text(
        text = "${message.sender}> ${message.text}$cursor",
        color = senderColor,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
