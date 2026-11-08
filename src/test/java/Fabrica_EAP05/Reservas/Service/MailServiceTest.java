package Fabrica_EAP05.Reservas.Service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {
    @Mock private JavaMailSender mailSender;
    @InjectMocks private MailService mailService;
    private MimeMessage mensaje;

    @BeforeEach
    void setUp() {
        mensaje = new MimeMessage(Session.getInstance(new Properties()));
    }

    @Test @DisplayName("PU-04-16 Envía correo con destinatario, asunto y enlace")
    void enviarEmail_datosValidos_enviaCorreoConEnlace() throws Exception {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mensaje);
        String enlace = "https://app.test/reset?token=h1";
        // Act
        mailService.enviarEmailResetPassword("a@test.com", "Ana", enlace);
        // Assert
        verify(mailSender).send(mensaje);
        assertEquals("Recuperación de contraseña - Fabrica EAP05", mensaje.getSubject());
        assertEquals("a@test.com", mensaje.getAllRecipients()[0].toString());
        assertTrue(mensaje.getContent().toString().contains(enlace));
    }
}
