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
import kotlinx.coroutines.launch
import java.util.UUID

data class Message(val id: String, val sender: String, val text: String, val isUser: Boolean)

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

    fun addSys(text: String) =
        messages.add(Message(UUID.randomUUID().toString(), "SYS", text, false))

    // Secuencia de arranque: índice RAG + (descarga) + carga de modelos.
    LaunchedEffect(Unit) {
        SoundManager.init()
        addSys("REFUGIA OS v1.0 — INICIANDO")
        addSys("CARGANDO MANUALES DE SUPERVIVENCIA...")

        coroutineScope.launch(Dispatchers.IO) {
            // 1) Asegurar modelos GGUF (descarga única en el primer arranque).
            if (!ModelDownloader.allPresent(context)) {
                statusLabel = "DESCARGANDO MODELO DE IA..."
                addSys("DESCARGANDO MODELO DE IA (SOLO LA PRIMERA VEZ)...")
                ModelDownloader.ensureModels(context) { label, frac ->
                    statusLabel = "$label ${(frac * 100).toInt()}%"
                }
            }
            // 2) Cargar índice + modelos.
            statusLabel = "CARGANDO MOTOR DE IA..."
            val ok = agent.warmUp(
                genModelFile = ModelDownloader.genModelFile(context),
                embedModelFile = ModelDownloader.embedModelFile(context),
            )
            ready = ok
            statusLabel = if (ok) "RAG ACTIVO — ON-DEVICE" else "MODO DEGRADADO — FALTA MODELO"
            addSys(
                if (ok) "SISTEMA OPERATIVO. ESCRIBE TU CONSULTA."
                else "NO SE PUDO CARGAR EL MODELO. REVISA LA CONEXIÓN Y REINICIA."
            )
        }

        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignorar error de foco en preview o ciertos estados
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
                    text = "USER> ",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
                BasicTextField(
                    value = inputText,
                    onValueChange = {
                        if (it.length > inputText.length) SoundManager.playKeystroke()
                        inputText = it
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.primary
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val userText = inputText.trim()
                            if (userText.isNotBlank()) {
                                messages.add(Message(UUID.randomUUID().toString(), "USER", userText, true))
                                inputText = ""

                                coroutineScope.launch {
                                    listState.scrollToItem(messages.size - 1)

                                    if (!ready) {
                                        addSys(statusLabel)
                                        listState.scrollToItem(messages.size - 1)
                                        return@launch
                                    }

                                    // Mensaje del bot que se completa con streaming.
                                    val botId = UUID.randomUUID().toString()
                                    messages.add(Message(botId, "REFUGIA", "", false))
                                    val botIndex = messages.size - 1

                                    agent.ask(userText) { token ->
                                        val cur = messages[botIndex]
                                        messages[botIndex] = cur.copy(text = cur.text + token)
                                        SoundManager.playReceive()
                                    }
                                    listState.scrollToItem(messages.size - 1)
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
    // los mensajes de sistema, directos. Se elimina el typewriter artificial
    // para no duplicar el efecto de streaming real del LLM.
    val senderColor =
        if (message.isUser) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.primary
    Text(
        text = "${message.sender}> ${message.text}",
        color = senderColor,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
