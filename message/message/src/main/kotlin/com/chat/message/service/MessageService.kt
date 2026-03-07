package com.chat.message.service

import com.chat.message.model.Message
import com.chat.message.model.User
import com.chat.message.repository.MessageRepository
import com.chat.message.repository.UserRepository
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service

@Service
class MessageService(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) {

    fun saveMessage(jwt: Jwt, content: String): Message {
        val userId = jwt.subject
        // Intentamos obtener el nickname del token o de los claims
        val nickname = jwt.getClaim<String>("username") ?: "Usuario Desconocido"
        val email = jwt.getClaim<String>("email") ?: ""

        // Si el usuario no existe en nuestra DB local, lo guardamos
        val user = userRepository.findById(userId).orElseGet {
            userRepository.save(User(id = userId, nickname = nickname, email = email))
        }

        val message = Message(sender = user, content = content)
        return messageRepository.save(message)
    }

    fun getAllMessages(): List<Message> = messageRepository.findAll()
}