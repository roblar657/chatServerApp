package com.example.serverChatApp.interfaces

import kotlinx.coroutines.channels.Channel
import java.net.Socket
import java.nio.charset.Charset

/**
 * En chat bruker
 */
interface IUser {

    /**
     * Identifikator (ip:port eller reelt brukernavn)
     */
    var userName: String?

    /**
     * Id til chat rom en eventuelt er i.
     */
    var roomId: Int?

    /**
     * En socket for socket-kommunikasjon
     */
    val socket: Socket

    /**
     * Sender en melding til denne brukeren med spesifisert charset.
     *
     * @param msg Meldingen som skal sendes
     * @param charset Tegnsett som skal brukes for meldingen
     */
    fun sendMessage(msg: String, charset: Charset)

    /**
     * Returnerer en unik ID for brukeren (brukernavn, ip:port)
     *
     * @return Unik identifikator for brukeren
     */
    fun getId(): String

    /**
     * Lukker socket-tilkoblingen opp mot brukeren.
     *
     */
    fun closeSocketConnection(): Unit

    /**
     * Lagringsplass (Kanal) for innkommende meldinger
     *
     * @return Lagringsplass (Kanal) for innkommende meldinger
     */
    fun getIncomingMessages(): Channel<String>

    /**
     * Lagringsplass (Kanal) for utgående meldinger
     *
     * @return Lagringsplass (Kanal) for utgående meldinger
     */
    fun getOutgoingMessages(): Channel<String>
}
