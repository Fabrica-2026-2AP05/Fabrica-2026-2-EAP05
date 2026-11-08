package Fabrica_EAP05.Reservas.Service;

import Fabrica_EAP05.Reservas.Exception.RecursoDuplicadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupabaseAuthServiceTest {
    @Mock private RestTemplate restTemplate;
    @InjectMocks private SupabaseAuthService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "supabaseUrl", "https://supabase.test");
        ReflectionTestUtils.setField(service, "serviceRoleKey", "service-key");
        ReflectionTestUtils.setField(service, "anonKey", "anon-key");
    }

    @Test @DisplayName("PU-04-06 Crear usuario retorna UUID")
    void crearUsuarioAuth_respuestaOk_retornaUuidSinPassword() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(post(anyString())).thenReturn(ResponseEntity.ok(Map.of("id", id.toString())));
        // Act
        UUID resultado = service.crearUsuarioAuth("a@test.com", "Password123!");
        // Assert
        assertEquals(id, resultado);
    }

    @Test @DisplayName("PU-04-07 Email existente produce conflicto")
    void crearUsuarioAuth_emailExistente_lanzaConflicto() {
        // Arrange
        when(post(anyString())).thenThrow(error(HttpStatus.UNPROCESSABLE_ENTITY));
        // Act
        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> service.crearUsuarioAuth("a@test.com", "Password123!"));
        // Assert
        assertTrue(ex.getMessage().contains("registrado"));
    }

    @Test @DisplayName("PU-04-12 Error externo no duplicado produce IllegalStateException")
    void crearUsuarioAuth_errorNoDuplicado_lanzaIllegalState() {
        // Arrange
        when(post(anyString())).thenThrow(error(HttpStatus.FORBIDDEN));
        // Act y Assert
        assertThrows(IllegalStateException.class,
                () -> service.crearUsuarioAuth("a@test.com", "Password123!"));
    }

    @Test @DisplayName("PU-04-14 Recovery retorna hashed token")
    void generarTokenRecovery_ok_retornaHashedToken() {
        // Arrange
        when(post(contains("generate_link"))).thenReturn(ResponseEntity.ok(Map.of("hashed_token", "h1")));
        // Act
        String token = service.generarTokenRecovery("a@test.com");
        // Assert
        assertEquals("h1", token);
    }

    @Test @DisplayName("PU-04-15 Recovery sin token produce error")
    void generarTokenRecovery_respuestaSinToken_lanzaIllegalState() {
        // Arrange
        when(post(contains("generate_link"))).thenReturn(ResponseEntity.ok(Map.of()));
        // Act y Assert
        assertThrows(IllegalStateException.class, () -> service.generarTokenRecovery("a@test.com"));
    }

    @Test @DisplayName("PU-04-15 Error 4xx al generar recovery produce error")
    void generarTokenRecovery_errorHttp_lanzaIllegalState() {
        // Arrange
        when(post(contains("generate_link"))).thenThrow(error(HttpStatus.BAD_REQUEST));
        // Act y Assert
        assertThrows(IllegalStateException.class, () -> service.generarTokenRecovery("a@test.com"));
    }

    @Test @DisplayName("PU-04-13 Eliminar usuario invoca DELETE")
    void eliminarUsuarioAuth_ok_invocaDelete() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(restTemplate.exchange(contains(id.toString()), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());
        // Act
        service.eliminarUsuarioAuth(id);
        // Assert
        verify(restTemplate).exchange(contains(id.toString()), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));
    }

    @Test @DisplayName("PU-04-08 Fallo al eliminar usuario no se propaga")
    void eliminarUsuarioAuth_falla_noPropagaExcepcion() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RestClientException("sin red"));
        // Act y Assert
        assertDoesNotThrow(() -> service.eliminarUsuarioAuth(UUID.randomUUID()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ResponseEntity<Map<String, Object>> post(String url) {
        return restTemplate.exchange(url, eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class));
    }

    private HttpClientErrorException error(HttpStatus status) {
        return HttpClientErrorException.create(status, status.getReasonPhrase(), null, null, null);
    }
}
