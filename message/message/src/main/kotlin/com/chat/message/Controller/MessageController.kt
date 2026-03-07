package com.chat.message.controller

import com.chat.message.dto.MessageRequest
import com.chat.message.model.Message
import com.chat.message.model.User
import com.chat.message.repository.MessageRepository
import com.chat.message.repository.UserRepository
import com.chat.message.service.MessageService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/messages")
open class MessageController(
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository
) {

    @PostMapping("/send")
    open fun sendMessage(
        @RequestBody payload: MessageRequest,
        @AuthenticationPrincipal jwt: Jwt // Extrae el token validado por Spring
    ) {
        // 1. Obtenemos el ID único de Cognito (sub)
        val cognitoId = jwt.subject

        // 2. Buscamos al usuario en RDS. Si no existe, lo creamos con datos del token.
        val user = userRepository.findById(cognitoId).orElseGet {
            val newUser = User(
                id = cognitoId,
                nickname = jwt.getClaimAsString("nickname") ?: "Usuario_Cognito",
                email = jwt.getClaimAsString("email") ?: ""
            )
            userRepository.save(newUser)
        }

        // 3. Guardamos el mensaje
        val message = Message(
            content = payload.content,
            sender = user
        )
        messageRepository.save(message)
    }

    @GetMapping("/all")
    open fun getAllMessages(): List<Message> = messageRepository.findAll()
}