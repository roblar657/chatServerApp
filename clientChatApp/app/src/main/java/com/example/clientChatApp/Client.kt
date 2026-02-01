package com.example.clientChatApp

import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.nio.charset.Charset

/**
 * Klient tilknyttet chat-server
 *
 */
class Client(

	private val SERVER_IP: String = "10.0.2.2",
	private val SERVER_PORT: Int = 12345,

	private val onMessageReceived: (String) -> Unit,
	private val onCommandsReceived: (List<String>) -> Unit,
	private val onDisconnected: () -> Unit
) {
	companion object {
		val CHARSET: Charset = Charsets.UTF_8
	}

	private var socket: Socket? = null
	private var isConnected = false

	fun start() {
		CoroutineScope(Dispatchers.IO).launch {
			try {
				socket = Socket(SERVER_IP, SERVER_PORT)
				isConnected = true

				readFromServer()
			} catch (e: IOException) {
				Log.e("[Client error]","[Client error]: ${e.message.toString()}")
				isConnected = false
				MainScope().launch { onDisconnected() }
			}
		}
	}
	/** Leser meldinger fra server,inntill forbindelsen brytes */
	private fun readFromServer() {
		try {
			socket?.let { sock ->
				val reader = BufferedReader(InputStreamReader(sock.getInputStream(), CHARSET))
				var isFinished = false
				while (!isFinished) {
					val message = reader.readLine()
					if(message == null){
						isFinished = true
					}

					//Serveren ønsker å lukke forbindelsen
					else if (message.startsWith("EXIT")) {
						val messageExit = message.substringAfter("EXIT")
						MainScope().launch {
							onMessageReceived(messageExit)
							onDisconnected()
						}
						closeSocket()
						isFinished = true
					}
					//Serveren ønsker å sende en liste over tilgjengelige kommandoer
					else if (message.startsWith("COMMANDS")) {
						val commands = message.substringAfter("COMMANDS")
							.trim()
							.split(",")
							.map { it.trim() }
							.filter { it.isNotEmpty() }

						MainScope().launch {
							onCommandsReceived(commands)
						}
					}
					//En vanlig melding
					else {
						MainScope().launch {
							onMessageReceived(message)
						}
					}
				}
			}
		} catch (e: IOException) {
			Log.e("[Client error]","[Client error]: ${e.message.toString()}")
			isConnected = false
			MainScope().launch { onDisconnected() }
		}
	}

	/** Lukker forbindelsen til server */
	private fun closeSocket() {
		try {
			isConnected = false
			socket?.close()
			socket = null
		} catch (e: IOException) {
			Log.e("[Client error]","[Client error]: ${e.message.toString()}")
		}
	}

	/** Sjekker forbindelsen til server */
	private fun checkConnection(): Boolean {
		val connected = isConnected && socket?.isClosed == false
		if (!connected) {
			CoroutineScope(Dispatchers.Main).launch { onDisconnected() }
		}
		return connected
	}

	/** Sender melding til server */
	private fun sendToServer(message: String) {
		try {
			socket?.let { sock ->
				val writer = PrintWriter(sock.getOutputStream(), true, CHARSET)
				writer.println(message)
			}
		} catch (e: IOException) {
			Log.e("[Client error]","[Client error]: ${e.message.toString()}")
			isConnected = false
		}
	}

	/** Behandler melding fra bruker */
	fun processInput(input: String) {
		CoroutineScope(Dispatchers.IO).launch {
			val message = input.trim()
			val command = message.split(" ").firstOrNull()?.uppercase() ?: return@launch

			if (checkConnection() && message.isNotEmpty()) {
				when (command) {
					"SETUSERNAME" -> sendToServer(message)
					"EXIT" -> {
						sendToServer("EXIT")
						closeSocket()
					}
					"JOIN", "BROADCAST", "PRIVATE" -> sendToServer(message)
					"LEAVE" -> sendToServer("LEAVE")
					else -> sendToServer(message)
				}
			}
		}
	}
}
