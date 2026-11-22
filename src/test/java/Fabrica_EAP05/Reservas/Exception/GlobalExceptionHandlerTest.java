package Fabrica_EAP05.Reservas.Exception;

import Fabrica_EAP05.Reservas.DTO.LoginDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Test @DisplayName("Un parámetro con formato inválido responde 400")
    void tipoInvalido_retornaBadRequest() {
        MethodArgumentTypeMismatchException excepcion = mock(MethodArgumentTypeMismatchException.class);
        when(excepcion.getName()).thenReturn("id");

        ResponseEntity<Map<String, Object>> respuesta = handler.handleTipoInvalido(excepcion);

        assertRespuesta(respuesta, HttpStatus.BAD_REQUEST,
                "El valor de 'id' no tiene un formato válido");
    }

    @Test @DisplayName("Cada excepción de dominio conserva su estado HTTP y mensaje")
    void excepcionesDeDominio_retornanEstadoCorrespondiente() {
        assertRespuesta(handler.handleCuentaBloqueada(new CuentaBloqueadaException("Cuenta bloqueada")),
                HttpStatus.TOO_MANY_REQUESTS, "Cuenta bloqueada");
        assertRespuesta(handler.handleRecursoNoEncontrado(new RecursoNoEncontradoException("No existe")),
                HttpStatus.NOT_FOUND, "No existe");
        assertRespuesta(handler.handleRecursoComprometido(new RecursoComprometidoException("Tiene reservas")),
                HttpStatus.CONFLICT, "Tiene reservas");
        assertRespuesta(handler.handleConflictoDisponibilidad(new ConflictoDisponibilidadException("Se solapa")),
                HttpStatus.CONFLICT, "Se solapa");
        assertRespuesta(handler.handleCredencialesInvalidas(new CredencialesInvalidasException("Credenciales inválidas")),
                HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        assertRespuesta(handler.handleUsuarioInactivo(new UsuarioInactivoException("Usuario inactivo")),
                HttpStatus.FORBIDDEN, "Usuario inactivo");
        assertRespuesta(handler.handleEventoNoEncontrado(new EventoNoEncontradoException("Evento inexistente")),
                HttpStatus.NOT_FOUND, "Evento inexistente");
        assertRespuesta(handler.handleAccessDenied(new AccessDeniedException("Sin permiso")),
                HttpStatus.FORBIDDEN, "Sin permiso");
        assertRespuesta(handler.handleCupoAgotado(new CupoAgotadoException("Sin cupos")),
                HttpStatus.CONFLICT, "Sin cupos");
        assertRespuesta(handler.handlePlazoCancelacion(new PlazoCancelacionException("Fuera de plazo")),
                HttpStatus.CONFLICT, "Fuera de plazo");
        assertRespuesta(handler.handleCapacidadInsuficiente(new CapacidadInsuficienteException("Capacidad insuficiente")),
                HttpStatus.CONFLICT, "Capacidad insuficiente");
    }

    @Test @DisplayName("Los errores de base de datos traducen las reglas conocidas")
    void erroresBaseDatos_retornanMensajesSeguros() {
        DataAccessResourceFailureException plazo = new DataAccessResourceFailureException(
                "fallo", new RuntimeException("faltan menos de 12 horas"));
        DataAccessResourceFailureException horario = new DataAccessResourceFailureException(
                "fallo", new RuntimeException("violación uq_horario_confirmado"));
        DataAccessResourceFailureException desconocido = new DataAccessResourceFailureException(
                "fallo", new RuntimeException("conexión perdida"));

        assertRespuesta(handler.handleDataAccess(plazo), HttpStatus.CONFLICT,
                "No se puede cancelar: faltan menos de 12 horas para el inicio de la reserva");
        assertRespuesta(handler.handleDataAccess(horario), HttpStatus.CONFLICT,
                "El horario ya fue reservado por otro cliente");
        assertRespuesta(handler.handleDataAccess(desconocido), HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor");
    }

    private void assertRespuesta(ResponseEntity<Map<String, Object>> respuesta,
                                  HttpStatus estado, String mensaje) {
        assertAll(
                () -> assertEquals(estado, respuesta.getStatusCode()),
                () -> assertNotNull(respuesta.getBody()),
                () -> assertEquals(mensaje, respuesta.getBody().get("mensaje")),
                () -> assertNotNull(respuesta.getBody().get("timestamp"))
        );
    }
}
