package com.chat.message.model

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id
    val id: String, // El UUID de Cognito

    @Column(nullable = false)
    val nickname: String,

    @Column(nullable = false, unique = true)
    val email: String
)