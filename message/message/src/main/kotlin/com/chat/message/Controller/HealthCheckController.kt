package com.chat.message.Controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import javax.sql.DataSource
import java.sql.Connection

@RestController
class HealthCheckController(val dataSource: DataSource) {

    @GetMapping("/test-db")
    fun testConnection(): String {
        return try {
            val connection: Connection = dataSource.connection
            if (!connection.isClosed) {
                "✅ ¡Conexión exitosa a la base de datos 'chat' en AWS!"
            } else {
                "❌ La conexión está cerrada."
            }
        } catch (e: Exception) {
            "❌ Error de conexión: ${e.message}"
        }
    }
}