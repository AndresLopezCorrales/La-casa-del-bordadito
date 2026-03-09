const functions = require("firebase-functions");
const nodemailer = require("nodemailer");

// Configurar el transporte de correo
const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: "andrespds333@gmail.com",
    pass: "xenobladE04*",
  },
});

exports.enviarCorreoBienvenida = functions.database
  .ref("/Usuarios/{uid}")
  .onCreate((snapshot, context) => {
    const usuario = snapshot.val();
    const emailDelUsuario = usuario.email;
    const nombres = usuario.nombres || "Usuario";

    const mailOptions = {
      from: "La Casa del Bordadito <andrespds333@gmail.com>",
      to: emailDelUsuario,
      subject: "¡Bienvenido a La Casa del Bordadito!",
      html: `
        <div style="font-family: Arial, sans-serif;
          padding: 20px; background-color: #f4f4f4;">
          <div style="max-width: 600px; margin: 0 auto;
            background-color: white; padding: 30px;
            border-radius: 10px;">
            <h1 style="color: #4CAF50;">¡Hola ${nombres}!</h1>
            <h2>Bienvenido a La Casa del Bordadito</h2>
            <p style="font-size: 16px; line-height: 1.6;">
              Estamos muy felices de tenerte con nosotros.
              Tu cuenta ha sido creada exitosamente.
            </p>
            <p style="font-size: 16px; line-height: 1.6;">
              Ahora puedes disfrutar de todas las
              funcionalidades de nuestra aplicación.
            </p>
            <div style="margin-top: 30px; padding-top: 20px;
              border-top: 1px solid #ddd;">
              <p style="color: #666; font-size: 14px;">
                Gracias por registrarte,<br>
                <strong>El equipo de La Casa del Bordadito</strong>
              </p>
            </div>
          </div>
        </div>
      `,
    };

    return transporter.sendMail(mailOptions)
      .then(() => {
        console.log("Correo enviado a:", emailDelUsuario);
        return null;
      })
      .catch((error) => {
        console.error("Error al enviar correo:", error);
        return null;
      });
  });