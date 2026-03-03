package com.chat.message.Controller

import com.chat.message.dto.ConfirmRequest
import com.chat.message.dto.LoginRequest
import com.chat.message.dto.RegisterRequest
import com.chat.message.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<String> {
        return try {
            authService.register(request)
            ResponseEntity.ok("Usuario registrado exitosamente. Revisa tu email para el código de confirmación.")
        } catch (e: Exception) {
            // Esto te ayudará a ver en la consola si el SECRET_HASH o las credenciales fallan
            ResponseEntity.status(400).body("Error al registrar: ${e.message}")
        }
    }

    @PostMapping("/confirm")
    fun confirm(@RequestBody request: ConfirmRequest): ResponseEntity<String> {
        return try {
            authService.confirmRegistration(request)
            ResponseEntity.ok("Cuenta confirmada exitosamente. Ya puedes iniciar sesión.")
        } catch (e: Exception) {
            ResponseEntity.status(400).body("Error al confirmar: ${e.message}")
        }
    }

    @PostMapping("/resend-code")
    fun resendCode(@RequestParam email: String): ResponseEntity<String> {
        return try {
            authService.resendCode(email)
            ResponseEntity.ok("Código reenviado. Revisa tu bandeja de entrada.")
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
            ResponseEntity.status(401).body("Credenciales inválidas: ${e.message}")
        }
    }
}