package Fabrica_EAP05.Reservas.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    @Mock private RestTemplate restTemplate;
    @InjectMocks private MailService mailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailService, "brevoApiKey", "api-key-test");
        ReflectionTestUtils.setField(mailService, "senderEmail", "reservas@test.com");
        ReflectionTestUtils.setField(mailService, "senderName", "Reservas EAP05");
    }

    @Test @DisplayName("PU-04-16 Envía a Brevo el destinatario, asunto y token")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void enviarEmail_datosValidos_enviaSolicitudBrevo() {
        mailService.enviarEmailResetPassword("a@test.com", "Ana", "token-h1");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(BREVO_URL), captor.capture(), eq(String.class));

        HttpEntity<Map<String, Object>> solicitud = captor.getValue();
        Map<String, Object> body = solicitud.getBody();
        assertNotNull(body);

        Map<String, Object> sender = (Map<String, Object>) body.get("sender");
        List<Map<String, Object>> destinatarios = (List<Map<String, Object>>) body.get("to");
        String contenido = (String) body.get("htmlContent");

        assertAll(
                () -> assertEquals("api-key-test", solicitud.getHeaders().getFirst("api-key")),
                () -> assertEquals("application/json", solicitud.getHeaders().getFirst(HttpHeaders.ACCEPT)),
                () -> assertEquals("reservas@test.com", sender.get("email")),
                () -> assertEquals("Reservas EAP05", sender.get("name")),
                () -> assertEquals("a@test.com", destinatarios.get(0).get("email")),
                () -> assertEquals("Recuperación de contraseña - Fabrica EAP05", body.get("subject")),
                () -> assertTrue(contenido.contains("Ana")),
                () -> assertTrue(contenido.contains("token-h1"))
        );
    }

    @Test @DisplayName("Propaga los errores reportados por Brevo")
    void enviarEmail_errorDeBrevo_propagaExcepcion() {
        when(restTemplate.postForEntity(eq(BREVO_URL), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Brevo no disponible"));

        RestClientException error = assertThrows(RestClientException.class,
                () -> mailService.enviarEmailResetPassword("a@test.com", "Ana", "token-h1"));

        assertEquals("Brevo no disponible", error.getMessage());
    }
}
