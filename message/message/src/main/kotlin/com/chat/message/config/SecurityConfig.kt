package com.chat.message.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                // Las rutas de auth (login, register) deben ser públicas
                auth.requestMatchers("/auth/**").permitAll()
                // CUALQUIER otra ruta (como enviar mensajes) requerirá el Token
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                // Esto activa el filtro que lee el "Bearer <token>"
                oauth2.jwt { }
            }

        return http.build()
    }
}