package com.example.oving6server

import com.example.oving6server.interfaces.IUser
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.net.Socket
import java.nio.charset.Charset

class User(override val socket: Socket, override var userName: String? = null) : IUser {

    override var roomId: Int? = null

    private val incomingMessages = Channel<String>(Channel.UNLIMITED)
    private val outgoingMessages = Channel<String>(Channel.UNLIMITED)

    override fun getIncomingMessages(): Channel<String> = incomingMessages

    override fun getOutgoingMessages(): Channel<String> = outgoingMessages


    override fun sendMessage(msg: String, charset: Charset) {
        try {
            socket.getOutputStream().write((msg + "\n").toByteArray(charset))
            socket.getOutputStream().flush()
        } catch (e: IOException) {

        }
    }


    override fun getId(): String {
        return userName ?: (socket.inetAddress.hostAddress + ":" + socket.port.toString())
    }

    override fun closeSocketConnection() {
        try {
            socket.close()
        } catch (_: IOException) {}
    }
}
