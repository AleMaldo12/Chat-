package com.chat.message.repository

import com.chat.message.model.Message
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MessageRepository : JpaRepository<Message, Long> {
    // Esto nos permitirá buscar mensajes de un usuario específico después
    fun findBySenderId(senderId: String): List<Message>
}