package com.chat.message.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableWebSecurity
open class SecurityConfig {

    @Bean
    open fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/messages/send").authenticated() // Protegemos el envío
                    .anyRequest().permitAll()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { } // Habilita la validación de tokens JWT
            }
        return http.build()
    }
    @Configuration
    open class CorsConfig : WebMvcConfigurer {
        override fun addCorsMappings(registry: CorsRegistry) {
            registry.addMapping("/**")
                .allowedOrigins("*") // En producción pon tu dominio real
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
        }
    }
}