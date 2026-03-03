package com.chat.message.service

import com.chat.message.model.Message
import com.chat.message.repository.MessageRepository
import org.springframework.stereotype.Service

@Service
class MessageService(private val messageRepository: MessageRepository) {

    fun saveMessage(senderId: String, content: String): Message {
        val message = Message(senderId = senderId, content = content)
        return messageRepository.save(message)
    }

    fun getAllMessages(): List<Message> = messageRepository.findAll()
}