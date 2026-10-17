package Fabrica_EAP05.Reservas.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    public void enviarEmailResetPassword(String destinatario, String nombreUsuario, String token) throws MessagingException {
        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, "UTF-8");

        helper.setTo(destinatario);
        helper.setFrom("reservas.fabrica.eap05@gmail.com");
        helper.setSubject("Recuperación de contraseña - Fabrica EAP05");

        String html = """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333;">
                    <h2>¡Hola %s!</h2>
                    <p>Recibimos una solicitud para restablecer tu contraseña.</p>
                    <p>Usa el siguiente token en la aplicación para completar el cambio:</p>
                    <p style="font-size: 18px; font-weight: bold; background-color:#f3f4f6; padding:12px; border-radius:4px; word-break:break-all;">
                        %s
                    </p>
                    <p>Este token expira en un tiempo limitado. Si no solicitaste este cambio, ignora este correo.</p>
                </body>
                </html>
                """.formatted(nombreUsuario, token);

        helper.setText(html, true);
        mailSender.send(mensaje);
    }
}
