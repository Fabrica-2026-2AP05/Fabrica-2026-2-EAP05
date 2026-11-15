package Fabrica_EAP05.Reservas.Controller;

import Fabrica_EAP05.Reservas.DTO.CancelarReservaEventoResponse;
import Fabrica_EAP05.Reservas.DTO.OcupacionEventoResponse;
import Fabrica_EAP05.Reservas.DTO.ReservaEventoResponse;
import Fabrica_EAP05.Reservas.Exception.CupoAgotadoException;
import Fabrica_EAP05.Reservas.Exception.EventoNoEncontradoException;
import Fabrica_EAP05.Reservas.Exception.PlazoCancelacionException;
import Fabrica_EAP05.Reservas.Service.EventoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoControllerTest {
    @Mock private EventoService eventoService;
    @Mock private Authentication authentication;
    @InjectMocks private EventoController controller;

    private String email;
    private UUID eventoId;

    @BeforeEach void setUp() {
        email = "cliente@correo.com";
        eventoId = UUID.randomUUID();
        // lenient: los tests de ocupación (HU28) no usan Authentication
        lenient().when(authentication.getName()).thenReturn(email);
    }

    @Test void reservarCupo_conCupos_retornaCreated() {
        // Arrange
        ReservaEventoResponse resultado = new ReservaEventoResponse(
                UUID.randomUUID(), eventoId, UUID.randomUUID(), "confirmada", 3L);
        when(eventoService.reservarCupo(email, eventoId)).thenReturn(resultado);
        // Act
        ResponseEntity<ReservaEventoResponse> respuesta = controller.reservarCupo(eventoId, authentication);
        // Assert
        assertAll(() -> assertEquals(HttpStatus.CREATED, respuesta.getStatusCode()),
                () -> assertSame(resultado, respuesta.getBody()));
    }

    // Los errores los convierte GlobalExceptionHandler (409 / 404); el controller solo los propaga
    @Test void reservarCupo_sinCupos_propagaExcepcion() {
        // Arrange
        when(eventoService.reservarCupo(email, eventoId))
                .thenThrow(new CupoAgotadoException("No hay cupos disponibles para este evento"));
        // Act + Assert
        assertThrows(CupoAgotadoException.class, () -> controller.reservarCupo(eventoId, authentication));
    }

    @Test void reservarCupo_eventoInexistente_propagaExcepcion() {
        // Arrange
        when(eventoService.reservarCupo(email, eventoId))
                .thenThrow(new EventoNoEncontradoException("El evento no existe"));
        // Act + Assert
        assertThrows(EventoNoEncontradoException.class, () -> controller.reservarCupo(eventoId, authentication));
    }

    @Test void cancelarReservaEvento_dentroDePlazo_retornaOk() {
        // Arrange
        UUID reservaId = UUID.randomUUID();
        CancelarReservaEventoResponse resultado = new CancelarReservaEventoResponse(
                reservaId, eventoId, UUID.randomUUID(), "cancelada");
        when(eventoService.cancelarReservaEvento(email, eventoId, reservaId)).thenReturn(resultado);
        // Act
        ResponseEntity<CancelarReservaEventoResponse> respuesta =
                controller.cancelarReservaEvento(eventoId, reservaId, authentication);
        // Assert
        assertAll(() -> assertEquals(HttpStatus.OK, respuesta.getStatusCode()),
                () -> assertSame(resultado, respuesta.getBody()));
    }

    // El 409 lo arma GlobalExceptionHandler; el controller solo propaga
    @Test void cancelarReservaEvento_fueraDePlazo_propagaExcepcion() {
        // Arrange
        UUID reservaId = UUID.randomUUID();
        when(eventoService.cancelarReservaEvento(email, eventoId, reservaId))
                .thenThrow(new PlazoCancelacionException("No se puede cancelar: faltan menos de 12 horas"));
        // Act + Assert
        assertThrows(PlazoCancelacionException.class,
                () -> controller.cancelarReservaEvento(eventoId, reservaId, authentication));
    }

    // ---------- HU28: ocupación de eventos ----------

    private final LocalDateTime desde = LocalDateTime.of(2026, 12, 1, 0, 0);
    private final LocalDateTime hasta = LocalDateTime.of(2027, 1, 1, 0, 0);

    @Test void reporteOcupacion_conDatos_retornaLista() {
        // Arrange
        List<OcupacionEventoResponse> reporte = List.of(
                new OcupacionEventoResponse(eventoId, "Taller", 3, 1L, 33.3));
        when(eventoService.reporteOcupacion(desde, hasta)).thenReturn(reporte);
        // Act
        ResponseEntity<?> respuesta = controller.reporteOcupacion(desde, hasta);
        // Assert
        assertAll(() -> assertEquals(HttpStatus.OK, respuesta.getStatusCode()),
                () -> assertEquals(reporte, respuesta.getBody()));
    }

    @Test void reporteOcupacion_sinDatos_retornaMensaje() {
        // Arrange
        when(eventoService.reporteOcupacion(desde, hasta)).thenReturn(List.of());
        // Act
        ResponseEntity<?> respuesta = controller.reporteOcupacion(desde, hasta);
        // Assert
        assertAll(() -> assertEquals(HttpStatus.OK, respuesta.getStatusCode()),
                () -> assertEquals(Map.of("mensaje", "No existen datos de ocupación para el periodo consultado."),
                        respuesta.getBody()));
    }

    // El 400 lo arma GlobalExceptionHandler (IllegalArgumentException); el controller solo propaga
    @Test void reporteOcupacion_desdeNoAnteriorAHasta_propagaExcepcion() {
        // Arrange
        when(eventoService.reporteOcupacion(hasta, desde))
                .thenThrow(new IllegalArgumentException("La fecha 'desde' debe ser anterior a la fecha 'hasta'"));
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> controller.reporteOcupacion(hasta, desde));
    }
}
