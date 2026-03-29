const nodemailer = require('nodemailer');

const express = require('express');
const {
    generateRegistrationOptions,
    verifyRegistrationResponse,
    generateAuthenticationOptions,
    verifyAuthenticationResponse,
} = require('@simplewebauthn/server');

const app = express();
app.use(express.json());

// --- Base de datos simulada (En producción usa MongoDB) ---
const users = {}; 
const challenges = {}; 

const rpID = 'localhost'; // Cambia a tu dominio en producción
const origin = `http://${rpID}:3000`;

// --- 1. REGISTRO FACIAL (PASO 1: Generar Opciones) ---
app.get('/generate-registration-options', async (req, res) => {
    const { email, username } = req.query;

    const options = await generateRegistrationOptions({
        rpName: 'SecureWeb Chat',
        rpID,
        userID: email,
        userName: email,
        userDisplayName: username,
        attestationType: 'none',
        authenticatorSelection: {
            residentKey: 'required',
            userVerification: 'required',
            authenticatorAttachment: 'platform', // Obliga a usar biometría de la PC (Rostro/PIN)
        },
    });

    challenges[email] = options.challenge; // Guardar desafío para verificar después
    res.json(options);
});

// --- 2. REGISTRO FACIAL (PASO 2: Verificar y Guardar) ---
app.post('/verify-registration', async (req, res) => {
    const { body } = req;
    const email = req.query.email; // Asegúrate de enviarlo en la URL o body
    const expectedChallenge = challenges[email];

    try {
        const verification = await verifyRegistrationResponse({
            response: body,
            expectedChallenge,
            expectedOrigin: origin,
            expectedRPID: rpID,
        });

        if (verification.verified) {
            const { registrationInfo } = verification;
            // Guardamos la llave pública vinculada al usuario
            users[email] = {
                devices: [{
                    credentialID: registrationInfo.credentialID,
                    credentialPublicKey: registrationInfo.credentialPublicKey,
                    counter: registrationInfo.counter,
                }],
                username: email.split('@')[0]
            };
            res.status(200).send({ ok: true });
        }
    } catch (error) {
        console.error(error);
        res.status(400).send({ error: error.message });
    }
});

// --- 3. LOGIN FACIAL (PASO 1: Generar Desafío) ---
app.get('/generate-authentication-options', async (req, res) => {
    const { email } = req.query;
    const user = users[email];

    if (!user) return res.status(404).send('Usuario no encontrado');

    const options = await generateAuthenticationOptions({
        rpID,
        allowCredentials: user.devices.map(dev => ({
            id: dev.credentialID,
            type: 'public-key',
        })),
        userVerification: 'required',
    });

    challenges[email] = options.challenge;
    res.json(options);
});

// --- 4. LOGIN FACIAL (PASO 2: Verificar Identidad) ---
app.post('/verify-authentication', async (req, res) => {
    const { email, assertion } = req.body;
    const user = users[email];
    const expectedChallenge = challenges[email];

    try {
        const credential = user.devices.find(d => d.credentialID === assertion.id);

        const verification = await verifyAuthenticationResponse({
            response: assertion,
            expectedChallenge,
            expectedOrigin: origin,
            expectedRPID: rpID,
            authenticator: credential,
        });

        if (verification.verified) {
            res.json({ username: user.username });
        }
    } catch (error) {
        res.status(400).send({ error: error.message });
    }
});

app.listen(3000, () => console.log('Servidor corriendo en puerto 3000'));

// Configuración de transporte (Ejemplo con SMTP o Amazon SES)
const transporter = nodemailer.createTransport({
    host: "email-smtp.us-east-1.amazonaws.com", // Servidor de AWS SES
    port: 587,
    auth: {
        user: process.env.AWS_SES_USER,
        pass: process.env.AWS_SES_PASS
    }
});

app.post('/forgot-password', async (req, res) => {
    const { email } = req.body;
    
    // 1. Aquí buscarías al usuario en tu base de datos (MongoDB)
    // 2. Generarías un token único de recuperación
    const resetToken = "token_de_prueba_123"; 

    const mailOptions = {
        from: '"SecureWeb Support" <soporte@tudominio.com>',
        to: email,
        subject: "Recupera tu acceso a SecureWeb",
        html: `
            <h1>¿Olvidaste tu acceso?</h1>
            <p>Haz clic en el siguiente enlace para restablecer tu clave:</p>
            <a href="https://tudominio.com/reset/${resetToken}">Restablecer contraseña</a>
            <p>Si no solicitaste esto, ignora este mensaje.</p>
        `
    };

    try {
        await transporter.sendMail(mailOptions);
        res.status(200).send("Enviado");
    } catch (error) {
        console.error(error);
        res.status(500).send("Error");
    }
});