package com.app.cchat

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    // Cambia esto por la IP de tu computadora (servidor Node.js)
    private val SERVER_URL = "http://192.168.1.10:3000"

    private lateinit var mSocket: Socket
    private val client = OkHttpClient()

    private lateinit var chatContainer: LinearLayout
    private lateinit var chatScrollView: ScrollView
    private lateinit var etMessage: EditText

    private var currentUser = "UsuarioAndroid" // Esto lo obtendrías del Login

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chatContainer = findViewById(R.id.chatContainer)
        chatScrollView = findViewById(R.id.chatScrollView)
        etMessage = findViewById(R.id.etMessage)
        val btnSend: Button = findViewById(R.id.btnSend)

        // 1. Inicializar Socket.IO
        conectarSocket()

        // 2. Configurar el botón de enviar
        btnSend.setOnClickListener {
            val texto = etMessage.text.toString().trim()
            if (texto.isNotEmpty()) {
                enviarMensajeSocket(texto)
                etMessage.text.clear()
            }
        }

        // Ejemplo de cómo harías una petición a tu API (ej. Login)
        // realizarLoginApi("correo@ejemplo.com", "miClaveSegura")
    }

    private fun conectarSocket() {
        try {
            mSocket = IO.socket(SERVER_URL)

            // Escuchar el evento 'chat_message' de tu servidor Node.js
            mSocket.on("chat_message") { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val user = data.getString("user")
                    val text = data.getString("text")
                    // La UI solo se puede actualizar en el hilo principal
                    runOnUiThread { mostrarMensajeEnPantalla(user, text) }
                }
            }
            mSocket.connect()
        } catch (e: Exception) {
            Log.e("SocketIO", "Error al conectar", e)
        }
    }

    private fun enviarMensajeSocket(mensaje: String) {
        // En una app real, aquí encriptarías el mensaje antes de enviarlo
        val data = JSONObject()
        data.put("user", currentUser)
        data.put("text", mensaje)
        data.put("type", "text")

        mSocket.emit("chat_message", data)
    }

    private fun mostrarMensajeEnPantalla(usuario: String, mensaje: String) {
        val textView = TextView(this).apply {
            text = "$usuario: $mensaje"
            textSize = 16f
            setPadding(16, 16, 16, 16)

            // Diferenciar mis mensajes de los de otros visualmente
            if (usuario == currentUser) {
                setBackgroundColor(Color.parseColor("#dcf8c6")) // Verde claro
                textAlignment = TextView.TEXT_ALIGNMENT_VIEW_END
            } else {
                setBackgroundColor(Color.WHITE)
                textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
            }

            // Márgenes para separar las burbujas
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 8, 0, 8)
            layoutParams = params
        }

        chatContainer.addView(textView)

        // Hacer scroll automático hacia abajo
        chatScrollView.post { chatScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    // --- EJEMPLO DE CÓMO CONSUMIR TU API REST CON OKHTTP ---
    private fun realizarLoginApi(email: String, clave: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = JSONObject().apply {
                    put("email", email)
                    put("password", clave)
                }

                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$SERVER_URL/login")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Login Exitoso", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Error en Login", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("API", "Error de red", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSocket.disconnect()
        mSocket.off("chat_message")
    }
}