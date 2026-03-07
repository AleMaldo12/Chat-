package com.chat.message.controller

import com.chat.message.dto.ConfirmRequest
import com.chat.message.dto.LoginRequest
import com.chat.message.dto.RegisterRequest
import com.chat.message.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<String> {
        return try {
            authService.register(request)
            ResponseEntity.ok("Usuario registrado exitosamente.")
        } catch (e: Exception) {
            ResponseEntity.status(400).body("Error: ${e.message}")
        }
    }

    @PostMapping("/confirm")
    fun confirm(@RequestBody request: ConfirmRequest): ResponseEntity<String> {
        return try {
            authService.confirmRegistration(request)
            ResponseEntity.ok("Cuenta confirmada.")
        } catch (e: Exception) {
            ResponseEntity.status(400).body("Error: ${e.message}")
        }
    }

    @PostMapping("/resend-code")
    fun resendCode(@RequestParam email: String): ResponseEntity<String> {
        return try {
            authService.resendCode(email) // <--- Esta es la línea 40
            ResponseEntity.ok("Código reenviado.")
        } catch (e: Exception) {
            ResponseEntity.status(400).body("Error: ${e.message}")
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        return try {
            val token = authService.login(request)
            ResponseEntity.ok(mapOf("token" to token))
        } catch (e: Exception) {
            ResponseEntity.status(401).body("Error: ${e.message}")
        }
    }
}