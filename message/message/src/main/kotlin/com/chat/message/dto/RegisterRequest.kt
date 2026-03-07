package com.chat.message.dto

data class RegisterRequest(
    val nickname: String = "",
    val email: String = "",
    val password: String = ""
)