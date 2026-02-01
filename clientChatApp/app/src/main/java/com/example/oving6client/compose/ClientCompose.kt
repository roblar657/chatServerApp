package com.example.oving6client.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.example.oving6client.Client
import kotlin.collections.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientCompose() {
    var messageInput by remember { mutableStateOf("") }

    //Liste av trippel av (melding, type(broadcast,private,server), erSendt (true/false))
    var allMessages by remember { mutableStateOf(listOf<Triple<String, String, Boolean>>()) }
    var client: Client? by remember { mutableStateOf(null) }
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var serverIp by remember { mutableStateOf("") }
    var currentRoomNr by remember { mutableStateOf<String?>(null) }
    var currentUsername by remember { mutableStateOf<String?>(null) }

    var expanded by remember { mutableStateOf(false) }
    var selectedCommand by remember { mutableStateOf("") }

    //Kommandoer, sendt fra server, bruker kan utføre opp mot server
    var commands by remember { mutableStateOf(listOf<String>()) }

    fun isShowCommandsEnabled(command: String): Boolean {
        return when (command) {
            "LEAVE","BROADCAST" -> currentRoomNr != null
            "JOIN","PRIVATE" -> currentUsername != null
            else -> true
        }
    }


    fun connectToServer() {
        val ipToUse = serverIp.ifBlank { "10.0.2.2" }
        isConnecting = true

        client = Client(
            SERVER_IP = ipToUse,
            onMessageReceived = { message ->
                val lowerMessage = message.lowercase()
                val messageType = when {
                    lowerMessage.startsWith("[private message]") -> "private"
                    lowerMessage.startsWith("[server]") -> "server"
                    else -> "broadcast"
                }
                allMessages = allMessages + Triple(message, messageType, false)
            },
            onCommandsReceived = { receivedCommands ->
                commands = receivedCommands
                isConnecting = false
                if (receivedCommands.isNotEmpty() && selectedCommand.isEmpty()) {
                    selectedCommand = receivedCommands[0]
                }
            },
            onDisconnected = {
                commands = emptyList()
                selectedCommand = ""
                isConnected = false
                isConnecting = false
                currentRoomNr = null
                currentUsername = null
            }
        )

        client?.start()
        isConnected = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(top = 32.dp)
        ) {

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Text("Meldinger", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(allMessages) { (message, type, isSent) ->
                        Text(
                            text = message,
                            color = when {
                                isSent && type == "private" -> Color(0xFF388E3C)
                                isSent && type == "broadcast" -> Color(0xFF1565C0)
                                !isSent && type == "broadcast" -> Color(0xFF1565C0)
                                !isSent && type == "private" -> Color(0xFF0D47A1)
                                !isSent && type == "server" -> Color(0xFF000000)
                                else -> Color(0xFF212121)
                            })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            if (isConnected || isConnecting) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        if (isConnected && !isConnecting) {
                            expanded = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = when {
                            isConnecting -> "Kobler til..."
                            else -> selectedCommand
                        },
                        onValueChange = { },

                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = if (expanded)
                                    Icons.Default.KeyboardArrowUp
                                else
                                    Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        },
                        placeholder = {
                            Text("Velg kommando")
                        },
                        enabled = true,
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        commands.forEach { command ->
                            val enabled = isShowCommandsEnabled(command)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        command,
                                        color = if (enabled) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        }
                                    )
                                },
                                onClick = {
                                    if (enabled) {
                                        selectedCommand = command
                                        expanded = false
                                    }
                                },
                                enabled = enabled
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                OutlinedTextField(
                    value = serverIp,
                    onValueChange = { serverIp = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Server IP (10.0.2.2)")
                    },
                    label = { Text("Server IP") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }


            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp),
                placeholder = {
                    Text(
                        when (selectedCommand) {
                            "SETUSERNAME" -> if (currentUsername == null) "brukernavn" else "nytt brukernavn"
                            "BROADCAST" -> "melding til alle"
                            "PRIVATE" -> "[brukernavn] [melding]"
                            "JOIN" -> "[romnummer]"
                            "LEAVE" -> "forlat rom"
                            else -> "Skriv melding..."
                        }
                    )
                },
                enabled = isConnected && selectedCommand.isNotEmpty(),
                maxLines = 7
            )

            Spacer(modifier = Modifier.height(8.dp))


            val buttonEnabled =
                if (!isConnected && !isConnecting) {
                    true
                }
                else {
                    commands.isNotEmpty() && selectedCommand.isNotEmpty() && isShowCommandsEnabled(selectedCommand)
                }

            Button(
                onClick = {
                    if (!isConnected && !isConnecting) {
                        connectToServer()
                    }
                    else {
                        val fullMessage = when (selectedCommand) {
                            "JOIN" -> {
                                if (messageInput.isNotBlank())
                                    currentRoomNr = messageInput.trim()
                                "$selectedCommand $messageInput"
                            }
                            "LEAVE" -> {
                                currentRoomNr = null
                                selectedCommand
                            }
                            "SETUSERNAME" -> {
                                if (messageInput.isNotBlank())
                                    currentUsername = messageInput.trim()
                                "$selectedCommand $messageInput"
                            }
                            else -> {
                                if (messageInput.isNotBlank())
                                    "$selectedCommand $messageInput"
                                else selectedCommand
                            }
                        }

                        client?.processInput(fullMessage)

                        when (selectedCommand) {
                            "BROADCAST" -> {
                                val username = currentUsername ?: "username"
                                val msg =
                                    if (currentRoomNr != null) {
                                        "[rom $currentRoomNr] $username (me) > $messageInput"
                                    }
                                    else {
                                        "$username (me) > $messageInput"
                                    }
                                allMessages = allMessages + Triple(msg, "broadcast", true)
                            }
                            "PRIVATE" -> {
                                val username = currentUsername ?: "username"
                                val parts = messageInput.split(" ", limit = 2)
                                if (parts.size >= 2) {
                                    val recipient = parts[0]
                                    val message = parts[1]
                                    val msg = "[private message to $recipient] $username (me) > $message"
                                    allMessages = allMessages + Triple(msg, "private", true)
                                }
                            }
                        }

                        messageInput = ""
                    }
                },
                enabled = buttonEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (buttonEnabled) Color(0xFF555555)
                    else Color(0xFFDDDDDD),
                    contentColor = Color.White
                ),
                shape = RectangleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    if (!isConnected && !isConnecting) "KOBLE TIL" else "SEND"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { messageInput = "" },
                enabled = isConnected && messageInput.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF777777),
                    contentColor = Color.White
                ),
                shape = RectangleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("TØM")
            }
        }
    }
}