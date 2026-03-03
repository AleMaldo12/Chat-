package com.chat.message.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "messages")
data class Message(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val senderId: String, // Aquí guardaremos el 'sub' del JWT (ID de Cognito)

    @Column(nullable = false)
    val content: String,

    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now()
)