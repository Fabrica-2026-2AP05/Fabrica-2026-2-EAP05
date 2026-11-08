package Fabrica_EAP05.Reservas.Exception;

import Fabrica_EAP05.Reservas.DTO.LoginDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test @DisplayName("Credenciales inválidas conservan la respuesta actual 400")
    void credencialesInvalidas_retornanBadRequest() {
        // Arrange
        IllegalArgumentException excepcion = new IllegalArgumentException("Credenciales inválidas");

        // Act
        ResponseEntity<Map<String, Object>> respuesta =
                handler.handleIllegalArgument(excepcion);

        // Assert
        assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode()),
                () -> assertEquals("Credenciales inválidas", respuesta.getBody().get("mensaje")),
                () -> assertNotNull(respuesta.getBody().get("timestamp")));
    }

    @Test @DisplayName("CP-04-C y CP-05-C - Correo duplicado responde 409")
    void correoDuplicado_retornaConflict() {
        // Arrange
        RecursoDuplicadoException excepcion = new RecursoDuplicadoException("El email ya está registrado");

        // Act
        ResponseEntity<Map<String, Object>> respuesta =
                handler.handleRecursoDuplicado(excepcion);

        // Assert
        assertAll(() -> assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode()),
                () -> assertEquals("El email ya está registrado", respuesta.getBody().get("mensaje")));
    }

    @Test
    @DisplayName("CP-01-D, CP-04-B y CP-05-B - Campos inválidos responden 400 con sus errores")
    void camposInvalidos_retornanBadRequestConErroresPorCampo() {
        // Arrange
        LoginDTO login = new LoginDTO();
        BeanPropertyBindingResult erroresValidacion =
                new BeanPropertyBindingResult(login, "loginDTO");
        erroresValidacion.addError(new FieldError(
                "loginDTO", "email", "El correo no es válido"));
        erroresValidacion.addError(new FieldError(
                "loginDTO", "contrasena", "La contraseña es obligatoria"));
        MethodArgumentNotValidException excepcion =
                new MethodArgumentNotValidException(null, erroresValidacion);

        // Act
        ResponseEntity<Map<String, Object>> respuesta = handler.handleValidation(excepcion);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody().get("timestamp"));
        assertInstanceOf(Map.class, respuesta.getBody().get("errores"));

        @SuppressWarnings("unchecked")
        Map<String, String> errores =
                (Map<String, String>) respuesta.getBody().get("errores");
        assertAll(
                () -> assertEquals("El correo no es válido", errores.get("email")),
                () -> assertEquals("La contraseña es obligatoria", errores.get("contrasena"))
        );
    }
}
