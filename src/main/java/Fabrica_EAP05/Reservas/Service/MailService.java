package Fabrica_EAP05.Reservas.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Service
public class MailService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    @Value("${brevo.sender-email}")
    private String senderEmail;

    @Value("${brevo.sender-name:Reservas EAP05}")
    private String senderName;

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    public void enviarEmailResetPassword(String destinatario, String nombreUsuario, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);
        headers.set("accept", "application/json");

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

        Map<String, Object> body = Map.of(
                "sender", Map.of("email", senderEmail, "name", senderName),
                "to", List.of(Map.of("email", destinatario)),
                "subject", "Recuperación de contraseña - Fabrica EAP05",
                "htmlContent", html
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        // Deja propagar cualquier error HTTP: AuthService.solicitarRecuperacion
        // ya lo captura como best-effort y solo lo registra en logs.
        restTemplate.postForEntity(BREVO_URL, request, String.class);
    }
}