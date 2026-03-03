package com.chat.message.dto

data class RegisterRequest(
    val email: String,
    val password: String,
    val nickName: String // Agregamos el campo obligatorio
)