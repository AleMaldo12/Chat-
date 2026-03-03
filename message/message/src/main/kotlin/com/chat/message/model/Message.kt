package com.chat.message.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "messages")
data class Message(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne // Relación con la tabla User
    @JoinColumn(name = "user_id", nullable = false)
    val sender: User,

    @Column(nullable = false)
    val content: String,

    @Column(nullable = false)
    val timestamp: java.time.LocalDateTime = java.time.LocalDateTime.now()
)