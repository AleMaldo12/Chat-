package com.chat.message.dto

data class ConfirmRequest(
    val email: String,
    val confirmationCode: String
)