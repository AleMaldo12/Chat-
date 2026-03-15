const nodemailer = require('nodemailer');

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