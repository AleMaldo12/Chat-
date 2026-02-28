const socket = io();
let myUsername = '';
let sharedSecretKey = '';

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