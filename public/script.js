const socket = io();
let myUsername = '';
let sharedSecretKey = '';

/**
 * SECUREWEB - Módulo de Autenticación Biométrica Facial (WebAuthn)
 */

// --- Utilidades de Conversión (Esenciales para WebAuthn) ---

// Convierte Base64URL (del servidor) a Uint8Array (para el navegador)
function bufferFromBase64(base64) {
    const binary = window.atob(base64.replace(/-/g, '+').replace(/_/g, '/'));
    return Uint8Array.from(binary, c => c.charCodeAt(0));
}

// Convierte ArrayBuffer (del navegador) a Base64 (para enviar al servidor)
function bufferToBase64(buffer) {
    return btoa(String.fromCharCode(...new Uint8Array(buffer)))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=/g, '');
}

// --- 1. REGISTRO DE ROSTRO (Crear Passkey) ---

async function registrarRostro() {
    const email = document.getElementById('reg-email').value;
    const username = document.getElementById('reg-username').value;

    if (!email || !username) {
        return alert("Por favor, completa nombre y correo antes de registrar tu rostro.");
    }

    try {
        // A. Obtener opciones de creación desde el servidor Node.js
        const response = await fetch(`/generate-registration-options?email=${email}&username=${username}`);
        const options = await response.json();

        // B. Preparar las opciones (convertir strings a Buffers)
        options.challenge = bufferFromBase64(options.challenge);
        options.user.id = bufferFromBase64(options.user.id);

        // C. Invocar la interfaz de Windows Hello / FaceID
        const credential = await navigator.credentials.create({
            publicKey: options
        });

        // D. Codificar la respuesta para enviarla al servidor
        const credentialJSON = {
            id: credential.id,
            rawId: bufferToBase64(credential.rawId),
            type: credential.type,
            response: {
                attestationObject: bufferToBase64(credential.response.attestationObject),
                clientDataJSON: bufferToBase64(credential.response.clientDataJSON),
            }
        };

        // E. Verificar y guardar en el servidor
        const verifyRes = await fetch('/verify-registration', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(credentialJSON)
        });

        if (verifyRes.ok) {
            alert("¡Rostro registrado con éxito! Ya puedes usarlo para entrar.");
            showLogin();
        } else {
            alert("El servidor no pudo validar el registro facial.");
        }

    } catch (err) {
        console.error("Error en Registro Facial:", err);
        alert("El escaneo facial falló o fue cancelado por el usuario.");
    }
}

// --- 2. LOGIN CON ROSTRO (Autenticación) ---

async function loginRostro() {
    const email = document.getElementById('login-email').value;

    if (!email) {
        return alert("Ingresa tu correo para buscar tu registro facial.");
    }

    try {
        // A. Obtener desafío de login desde el servidor
        const response = await fetch(`/generate-authentication-options?email=${email}`);
        const options = await response.json();

        // B. Preparar desafío
        options.challenge = bufferFromBase64(options.challenge);
        
        if (options.allowCredentials) {
            options.allowCredentials.forEach(cred => {
                cred.id = bufferFromBase64(cred.id);
            });
        }

        // C. Activar la cámara para reconocimiento facial
        const assertion = await navigator.credentials.get({
            publicKey: options
        });

        // D. Codificar la respuesta (prueba de posesión)
        const assertionJSON = {
            id: assertion.id,
            rawId: bufferToBase64(assertion.rawId),
            type: assertion.type,
            response: {
                authenticatorData: bufferToBase64(assertion.response.authenticatorData),
                clientDataJSON: bufferToBase64(assertion.response.clientDataJSON),
                signature: bufferToBase64(assertion.response.signature),
                userHandle: assertion.response.userHandle ? bufferToBase64(assertion.response.userHandle) : null,
            }
        };

        // E. Validar contra el servidor
        const loginRes = await fetch('/verify-authentication', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, assertion: assertionJSON })
        });

        if (loginRes.ok) {
            const result = await loginRes.json();
            
            // Iniciar sesión en el chat
            currentUser = result.username;
            encryptionKey = "clave_de_sesión_temporal"; // Idealmente manejada por el backend
            
            hideAll();
            document.getElementById('chat-section').style.display = 'flex';
            alert("¡Identidad confirmada! Bienvenido al chat.");
        } else {
            alert("No se reconoció el rostro para esta cuenta.");
        }

    } catch (err) {
        console.error("Error en Login Facial:", err);
        alert("Error de autenticación: Asegúrate de tener configurado Windows Hello o FaceID.");
    }
}

// --- Funciones de navegación (ya existentes en tu código) ---
function hideAll() {
    document.getElementById('auth-section').style.display = 'none';
    document.getElementById('register-section').style.display = 'none';
    document.getElementById('chat-section').style.display = 'none';
}

function showLogin() {
    hideAll();
    document.getElementById('auth-section').style.display = 'flex';
}

// Mostrar/Ocultar secciones
function showRecovery() {
    document.getElementById('auth-section').style.display = 'none';
    document.getElementById('recovery-section').style.display = 'flex';
}

function hideRecovery() {
    document.getElementById('recovery-section').style.display = 'none';
    document.getElementById('auth-section').style.display = 'flex';
}

// Envío de petición al servidor
async function sendRecoveryEmail() {
    const email = document.getElementById('recovery-email').value;
    
    if (!email) {
        return alert("Por favor, ingresa tu correo.");
    }

    try {
        const response = await fetch('/forgot-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email })
        });

        if (response.ok) {
            alert("Si el correo está registrado, recibirás instrucciones en breve.");
            hideRecovery();
        } else {
            alert("Hubo un error al procesar la solicitud.");
        }
    } catch (error) {
        console.error("Error:", error);
    }
}

function entrarChat() {
    myUsername = document.getElementById('username').value.trim();
    sharedSecretKey = document.getElementById('secret-key').value.trim();
    
    if(myUsername && sharedSecretKey) {
        document.getElementById('auth-section').style.display = 'none';
        document.getElementById('chat-section').style.display = 'flex';
        // Scroll automático al entrar
        setTimeout(() => {
            const chatBox = document.getElementById('chat-box');
            chatBox.scrollTop = chatBox.scrollHeight;
        }, 100);
    } else {
        alert("Introduce nombre y clave.");
    }
}

// Eventos para usar la tecla Enter
document.getElementById('secret-key').addEventListener('keypress', function (e) {
    if (e.key === 'Enter') entrarChat();
});

document.getElementById('message').addEventListener('keypress', function (e) {
    if (e.key === 'Enter') sendMessage();
});

// Lógica de cifrado (AES)
function encryptMessage(msg) {
    return CryptoJS.AES.encrypt(msg, sharedSecretKey).toString();
}

function decryptMessage(cipherText) {
    try {
        const bytes = CryptoJS.AES.decrypt(cipherText, sharedSecretKey);
        return bytes.toString(CryptoJS.enc.Utf8);
    } catch (e) {
        return null;
    }
}

// Enviar mensaje de texto
function sendMessage() {
    const messageInput = document.getElementById('message');
    const rawMessage = messageInput.value.trim();
    
    if(!rawMessage) return;

    const encryptedMessage = encryptMessage(rawMessage);
    
    socket.emit('chat_message', { 
        user: myUsername, 
        text: encryptedMessage, 
        type: 'text' 
    });
    
    messageInput.value = '';
    messageInput.focus();
}

// Enviar archivo multimedia
async function sendMedia() {
    const fileInput = document.getElementById('media-file');
    if(fileInput.files.length === 0) return;

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);

    try {
        const response = await fetch('/upload', { method: 'POST', body: formData });
        const data = await response.json();
        
        // Ciframos la URL devuelta por el servidor
        const encryptedUrl = encryptMessage(data.url);

        socket.emit('chat_message', { 
            user: myUsername, 
            text: encryptedUrl, 
            type: 'image' 
        });
        
        fileInput.value = ''; // Limpiar el input
    } catch (error) {
        console.error("Error subiendo el archivo", error);
    }
}

// Recibir mensajes y mostrarlos
socket.on('chat_message', (data) => {
    const decryptedContent = decryptMessage(data.text);
    const chatBox = document.getElementById('chat-box');
    
    if(!decryptedContent) {
        console.warn("Mensaje ignorado: No se pudo descifrar (Claves distintas).");
        return; 
    }

    const alignmentClass = (data.user === myUsername) ? 'mine' : 'other';
    const senderName = (data.user === myUsername) ? 'Tú' : data.user;

    let messageHTML = '';
    
    if(data.type === 'text') {
        messageHTML = `
            <div class="msg-wrapper ${alignmentClass}">
                <div class="msg">
                    <b>${senderName}</b>
                    ${decryptedContent}
                </div>
            </div>`;
    } else if (data.type === 'image') {
        messageHTML = `
            <div class="msg-wrapper ${alignmentClass}">
                <div class="msg">
                    <b>${senderName}</b>
                    <img src="${decryptedContent}" style="max-width: 100%; border-radius: 8px; margin-top: 5px;">
                </div>
            </div>`;
    }

    // Insertar el HTML y hacer scroll
    chatBox.insertAdjacentHTML('beforeend', messageHTML);
    chatBox.scrollTo({
        top: chatBox.scrollHeight,
        behavior: 'smooth'
    });
});