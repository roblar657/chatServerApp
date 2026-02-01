package com.example.oving6server

import IChatRoom
import com.example.oving6server.interfaces.IUser

class ChatRoom(override val id: Int) : IChatRoom {

    private val users = mutableListOf<IUser>()

    override fun addUser(user: IUser) {
        users.add(user)
    }

    override fun removeUser(user: IUser) {
        users.remove(user)
    }

    override fun contains(user: IUser): Boolean = users.contains(user)


    override fun broadcast(message: String, sentFrom: IUser?) {
        users.filter { it != sentFrom }.forEach { it.sendMessage(message,Server.CHARSET) }
    }

    override fun getUserIds(): List<String> = users.mapNotNull { it.userName }
}
