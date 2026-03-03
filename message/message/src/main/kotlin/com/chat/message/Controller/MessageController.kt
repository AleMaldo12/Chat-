package com.chat.message.controller

import com.chat.message.model.Message
import com.chat.message.service.MessageService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/messages")
class MessageController(
    // ESTO ES LO QUE FALTABA: Inyectar el servicio aquí
    private val messageService: MessageService
) {

    @PostMapping("/send")
    fun sendMessage(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<Message> {
        val content = body["content"] ?: return ResponseEntity.badRequest().build()

        // jwt.subject es el ID único de Cognito (sub)
        val senderId = jwt.subject

        val savedMessage = messageService.saveMessage(senderId, content)
        return ResponseEntity.ok(savedMessage)
    }

    @GetMapping("/all")
    fun getAllMessages(): ResponseEntity<List<Message>> {
        return ResponseEntity.ok(messageService.getAllMessages())
    }
}