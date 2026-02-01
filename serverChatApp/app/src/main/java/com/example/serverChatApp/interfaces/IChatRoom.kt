import com.example.serverChatApp.interfaces.IUser

/**
 * Et chatrom, hvor brukere kan skrive til hverandre
 */
interface IChatRoom {

    /**
     * Id til chat rommet
     */
    val id: Int

    /**
     * Legger til en bruker
     *
     * @param user brukeren som skal legges til.
     */
    fun addUser(user: IUser)

    /**
     * Fjerner en bruker
     *
     * @param user brukeren som skal fjernes.
     */
    fun removeUser(user: IUser)

    /**
     * Sjekker om en spesifikk bruker er i chat-rommet.
     *
     * @param user brukeren som skal sjekkes.
     * @return true eller false, avhengig om brukeren er i rommet
     */
    fun contains(user: IUser): Boolean

    /**
     * Sender en melding til alle brukere i  chat-rommet.
     *
     * @param message meldingen som skal sendes.
     * @param sentFrom brukeren som sender meldingen, eller `null` hvis meldingen kommer fra systemet.
     */
    fun broadcast(message: String, sentFrom: IUser? = null)

    /**
     * Returnerer en liste over id til alle brukere i chat-rommet
     *
     * @return liste over id til alle brukere i rommet.
     */
    fun getUserIds(): List<String>
}
