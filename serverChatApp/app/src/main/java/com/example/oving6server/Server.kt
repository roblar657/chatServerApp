package com.example.oving6server

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.nio.charset.Charset

/**
 * Chat server
 *
 * Tilbyr følgende:
 *
 * 	- Mulighet til å ha flere brukere
 * 	- En har flere chat rom, hvor en kan broadcaste meldinger
 * 	- En kan sende private meldinger mellom brukere
 * 	- Loggføring av server-trafikk
 */
class Server(
	private val context: Context,
	private val port: Int = 12345,
	private val messageCallback: (String) -> Unit
) {

	companion object {

		val CHARSET: Charset = Charsets.UTF_8
	}

	private val users = mutableListOf<User>()
	private val rooms = mutableMapOf<Int, ChatRoom>()
	private val connections = mutableMapOf<String, User>()

	//Funksjonalitet en ønsker å tilby brukeren
	private val commands = listOf("SETUSERNAME", "EXIT", "PRIVATE", "JOIN", "LEAVE", "BROADCAST")

	//Antall ganger en vil prøve å starte serveren, før en gir opp.
	private val MAX_RESTART_ATTEMPTS = 5
	private var restartAttempts = 0
	private var isRunning = false

	private var serverSocket: ServerSocket? = null

	/** Logger en melding via callback */
	private fun showMessage(msg: String) = messageCallback(msg)

	private fun logInfo(message: String) {
		Log.i("[Server info]","$message")
		showMessage("$message")
	}

	/** Logger en melding med avsender og mottaker */
	private fun logTraffic(from: String, to: String, message: String) {
		Log.i("[Server traffic]","[$from]->[$to]: $message")
		showMessage("[$from]->[$to]: $message")
	}
	/** Logger en melding med avsender og mottaker */
	private fun logError( message: String) {
		Log.e("[Server error]","[Server error]: $message")
		showMessage("[Server error]: $message")
	}

	/** Starter serveren asynkront */
	fun start() {
		if (isRunning) return
		isRunning = true
		CoroutineScope(Dispatchers.IO).launch { runServer() }
	}

	/** Stopper serveren og kobler fra alle brukere */
	fun stop() {
		isRunning = false
		CoroutineScope(Dispatchers.IO).launch {
			serverSocket?.close()
			users.toList().forEach { user ->
				try {
                    val exitMsg = context.getString(R.string.msg_server_stopped)
					logTraffic("Server", user.getId(), "EXIT[Server message] $exitMsg")

					user.sendMessage("EXIT[Server message] $exitMsg", CHARSET)
				} catch (e: Exception) {
					logError(e.message.toString())
				}
				removeUserFromServer(user)
			}
			users.clear()
			rooms.clear()
			connections.clear()
			logInfo(context.getString(R.string.msg_server_stopped))
		}
	}

	/** Kjører serveren og håndterer nye tilkoblinger */
	private suspend fun runServer() {
        //Ettersom en prøver flere ganger
		while (isRunning) {
			try {
				serverSocket = ServerSocket(port)

				serverSocket?.use { socket ->

                    restartAttempts = 0

					logInfo(context.getString(R.string.msg_server_started, port))

					while (isRunning) {

						val clientSocket = socket.accept()
						val user = User(clientSocket)
						users.add(user)

						logInfo(context.getString(R.string.msg_user_connected, user.getId()))
						val welcomeMsg = context.getString(R.string.msg_welcome)
						logTraffic("Server", user.getId(), welcomeMsg)
						user.sendMessage(welcomeMsg, CHARSET)

						val commandsMsg = "COMMANDS " + commands.joinToString(", ")
						logTraffic("Server", user.getId(), commandsMsg)
						user.sendMessage(commandsMsg, CHARSET)

						initUserCommunication(user)
					}
				}
			} catch (e: IOException) {
				logError(e.message.toString())
				restartAttempts++
				if (restartAttempts >= MAX_RESTART_ATTEMPTS)
					isRunning = false
				else delay(2000)
			}
		}
	}

	/** Initialiserer kommunikasjon med en bruker */
	@OptIn(ExperimentalCoroutinesApi::class)
	private fun initUserCommunication(user: User) {

		// Lytter på bruker, og plasserer alt i
		// brukerens lagrings plass (channel) for innkommende beskjeder
		CoroutineScope(Dispatchers.IO).launch {
			val reader = BufferedReader(InputStreamReader(user.socket.getInputStream(), CHARSET))
			try {
				while (isRunning && users.contains(user)) {

					val msg: String? = reader.readLine()


                    //msg == null betyr at en avslutter forbindelse
                    if(msg == null)
                        removeUserFromServer(user)
					else
                        user.getIncomingMessages().send(msg)
				}
			} catch (e: IOException) {
				logError(e.message.toString())
			} finally {
				// Lukker channel, slik at en ikke lenger prosesserer disse
				user.getIncomingMessages().close()
			}
		}
		//Fokuserer på å prosessere meldinger fra en spesifikk
		//bruker. Disse meldingene er lagret i brukerens
		//lagringsplass, i form av en channel.
		CoroutineScope(Dispatchers.Default).launch {
			val incomingMessages = user.getIncomingMessages()
			try {
				while (isRunning && users.contains(user)) {
					for (msg in incomingMessages) {

						if (isRunning && users.contains(user)) {
							val username = user.getId()
							logTraffic(username, "Server", msg)
							processMessage(user, msg)
						}
					}
				}
			}
			catch (e: IOException) {
				logError(e.message.toString())
			}

		}


		// Håndterer utgående meldinger, som sendes til en bruker
		// Hver bruker har en slik.
		CoroutineScope(Dispatchers.IO).launch {
			val outgoing = user.getOutgoingMessages()
			try {
				while (isRunning && users.contains(user)) {
					for (msg in outgoing) {
						if (users.contains(user)) {
							val username = user.getId()
							logTraffic("Server", username, msg)
							user.sendMessage(msg, CHARSET)
						}
					}
				}
			} catch (e: IOException) {
				logError(e.message.toString())
			}
			finally {
				// Fjerner bruker fra server
				removeUserFromServer(user)
			}
		}
	}

	/** Prosesserer meldinger fra en bruker */
	private fun processMessage(user: User, message: String) {
		val command = message.trim().split(" ").firstOrNull()?.uppercase() ?: ""

		when (command) {
			"SETUSERNAME" -> doSetUsername(user, message)
			"JOIN" -> doJoinChatRoom(user, message)
			"LEAVE" -> doLeaveChatRoom(user)
			"BROADCAST" -> doBroadcast(user, message)
			"PRIVATE" -> doPrivate(user, message)
			"EXIT" -> removeUserFromServer(user)
			else -> {
				val errMsg = context.getString(R.string.msg_invalid_command)
				user.getOutgoingMessages().trySend(errMsg)
				logTraffic("Server", user.getId(), errMsg)
			}
		}
	}

	/** Setter brukernavn for en bruker */
	private fun doSetUsername(user: User, message: String) {
		val newName = message.substringAfter("SETUSERNAME", "").trim()
		val username = user.getId()

		if (newName.isEmpty()) {
			val msg = context.getString(R.string.msg_setusername_missing_name)
			user.getOutgoingMessages().trySend(msg)
			logTraffic("Server", username, msg)
			return
		}
		if (connections.containsKey(newName)) {
			val msg = context.getString(R.string.msg_username_taken, newName)
			user.getOutgoingMessages().trySend(msg)
			logTraffic("Server", username, msg)
			return
		}

		val oldName = user.userName
		if (oldName != null) connections.remove(oldName)

		user.userName = newName
		connections[newName] = user

		val msg = context.getString(R.string.msg_registered_as, newName)
		user.getOutgoingMessages().trySend(msg)
		logTraffic("Server", newName, msg)

		//Broadcast til alle i rommet, bortsett fra denne brukeren, at denne
		//brukeren nå er i rommet.
		user.roomId?.let { roomId ->
			rooms[roomId]?.broadcast(
				context.getString(R.string.msg_user_changed_name, oldName, newName),
				sentFrom = user
			)
		}
	}

	/** Lar en bruker bli med i et chat-rom */
	private fun doJoinChatRoom(user: User, message: String) {
		if (user.userName == null) {
			val msg = context.getString(R.string.msg_must_set_username)
			user.getOutgoingMessages().trySend(msg)
			logTraffic("Server", user.getId(), msg)
			return
		}

		val roomId = message.substringAfter("JOIN", "").trim().toIntOrNull()
		if (roomId == null) {
			val msg = context.getString(R.string.msg_provide_room_number)
			user.getOutgoingMessages().trySend(msg)
			logTraffic("Server", user.getId(), msg)
			return
		}

		user.roomId?.let { oldRoomId ->
			rooms[oldRoomId]?.removeUser(user)
			rooms[oldRoomId]?.broadcast(
				context.getString(R.string.msg_user_left_room, user.userName),
				sentFrom = user
			)
		}

		val chatRoom = rooms.getOrPut(roomId) { ChatRoom(roomId) }
		chatRoom.addUser(user)
		user.roomId = roomId

		val otherUsers = chatRoom.getUserIds().filter { it != user.userName }
		val msg = if (otherUsers.isEmpty()) {
			context.getString(R.string.msg_joined_room_empty, roomId)
		} else {
			context.getString(
				R.string.msg_joined_room_with_users,
				roomId,
				otherUsers.joinToString(", ")
			)
		}
		user.getOutgoingMessages().trySend(msg)
		logTraffic("Server", user.getId(), msg)

		chatRoom.broadcast(context.getString(R.string.msg_user_joined_room, user.userName), sentFrom = user)
	}

	/** Lar en bruker forlate sitt nåværende chat-rom */
	private fun doLeaveChatRoom(user: User) {
		val roomId = user.roomId
		if (roomId == null) {
			val msg = context.getString(R.string.msg_not_in_room)
			user.getOutgoingMessages().trySend(msg)
			logTraffic("Server", user.getId(), msg)
			return
		}

		rooms[roomId]?.removeUser(user)
		rooms[roomId]?.broadcast(
			context.getString(R.string.msg_user_left_room, user.userName),
			sentFrom = user
		)
		val msg = context.getString(R.string.msg_left_room, roomId)
		user.getOutgoingMessages().trySend(msg)
		logTraffic("Server", user.getId(), msg)
		user.roomId = null
	}

	/** Sender en broadcast-melding til alle brukere i samme rom */
	private fun doBroadcast(user: User, message: String) {
		if (user.userName == null) {
			val msg = context.getString(R.string.msg_connect_first_broadcast)
			user.getOutgoingMessages().trySend(msg)
			logTraffic("Server", user.getId(), msg)
			return
		}

		val roomId = user.roomId
		if (roomId == null) {
			val msg = context.getString(R.string.msg_must_be_in_room)
			user.getOutgoingMessages().trySend(msg)
			logTraffic("Server", user.getId(), msg)
			return
		}

		val msgContent = message.substringAfter("BROADCAST", "").trim()
		if (msgContent.isEmpty()) {
			val msg = context.getString(R.string.msg_provide_message)
			user.getOutgoingMessages().trySend(msg)
			logTraffic("Server", user.getId(), msg)
			return
		}

		val broadcastMsg = context.getString(R.string.msg_room_broadcast, roomId, user.userName, msgContent)

		//Sender melding til alle i rommet, bortsett fra brukeren som har sendt
		rooms[roomId]?.broadcast(broadcastMsg, sentFrom = user)

		logTraffic(user.getId(), "Room $roomId", msgContent)
	}

	/** Sender en privat melding til en spesifikk bruker */
	private fun doPrivate(user: User, message: String) {
		if (user.userName == null) {
			val msg = context.getString(R.string.msg_connect_first_private)
			user.getOutgoingMessages().trySend(msg)
			logTraffic("Server", user.getId(), msg)
			return
		}

		val messageParts = message.substringAfter("PRIVATE", "").trim().split(" ", limit = 2)

		//Ugyldig format på privat melding
		if (messageParts.size < 2) {
			val msg = context.getString(R.string.msg_private_msg)
			user.getOutgoingMessages().trySend(msg)
			logTraffic("Server", user.getId(), msg)
			return
		}

		val otherUserName = messageParts[0]
		val msgContent = messageParts[1]
		val otherUser = connections[otherUserName]

		if (otherUser == null) {
			val msg = context.getString(R.string.msg_user_not_found, otherUserName)
			user.getOutgoingMessages().trySend(msg)
			logTraffic("Server", user.getId(), msg)
		}
		else if (otherUser.getId() == user.getId()){
			val msg = context.getString(R.string.msg_private_self)
			user.getOutgoingMessages().trySend(msg)
			logTraffic("Server", user.getId(), msg)
		}
		else {
			val formatted = context.getString(R.string.msg_private_message, user.userName, msgContent)
			otherUser.getOutgoingMessages().trySend(formatted)
			logTraffic(user.getId(), otherUserName, msgContent)
		}
	}

	/** Fjerner en bruker fra serveren */
	private fun removeUserFromServer(user: User) {
		logTraffic("Server",user.getId(),context.getString(R.string.msg_user_disconnected, user.getId()))

		//Fjern brukeren (med socketen) over liste med brukere (sockets)
		users.remove(user)

		//Fjern bruker fra map over brukernavn og bruker (socket)
		user.userName?.let { connections.remove(it) }

		//Hvis bruker er i et chatrom, fjern brukeren fra rommet.
		user.roomId?.let { roomId ->
			rooms[roomId]?.removeUser(user)
			rooms[roomId]?.broadcast(
				context.getString(R.string.msg_user_left_room,
					user.userName), sentFrom = user)
		}

		//Lukk socketen til brukeren
		user.closeSocketConnection()
	}
}
