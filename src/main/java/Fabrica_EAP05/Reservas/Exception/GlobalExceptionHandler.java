package Fabrica_EAP05.Reservas.Exception;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String CLAVE_TIMESTAMP = "timestamp";
    private static final String CLAVE_MENSAJE = "mensaje";

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "El valor de '" + ex.getName() + "' no tiene un formato válido");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put(CLAVE_TIMESTAMP, OffsetDateTime.now());

        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage())
        );
        body.put("errores", errores);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleRecursoDuplicado(RecursoDuplicadoException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CuentaBloqueadaException.class)
    public ResponseEntity<Map<String, Object>> handleCuentaBloqueada(CuentaBloqueadaException ex) {
        return respuesta(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RecursoComprometidoException.class)
    public ResponseEntity<Map<String, Object>> handleRecursoComprometido(RecursoComprometidoException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ConflictoDisponibilidadException.class)
    public ResponseEntity<Map<String, Object>> handleConflictoDisponibilidad(ConflictoDisponibilidadException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException ex) {
        String causa = String.valueOf(ex.getMostSpecificCause().getMessage());

        if (causa.contains("faltan menos de 12 horas")) {
            return respuesta(HttpStatus.CONFLICT,
                    "No se puede cancelar: faltan menos de 12 horas para el inicio de la reserva");
        }
        if (causa.contains("uq_horario_confirmado")) {
            return respuesta(HttpStatus.CONFLICT, "El horario ya fue reservado por otro cliente");
        }
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, Object>> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        return respuesta(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioInactivo(UsuarioInactivoException ex) {
        return respuesta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(EventoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleEventoNoEncontrado(EventoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return respuesta(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CupoAgotadoException.class)
    public ResponseEntity<Map<String, Object>> handleCupoAgotado(CupoAgotadoException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PlazoCancelacionException.class)
    public ResponseEntity<Map<String, Object>> handlePlazoCancelacion(PlazoCancelacionException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CapacidadInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> handleCapacidadInsuficiente(CapacidadInsuficienteException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Mismo cuerpo que antes: HashMap con "timestamp" y "mensaje" (HashMap admite mensaje null)
    private static ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put(CLAVE_TIMESTAMP, OffsetDateTime.now());
        body.put(CLAVE_MENSAJE, mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
