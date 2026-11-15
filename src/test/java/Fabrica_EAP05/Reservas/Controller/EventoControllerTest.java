package Fabrica_EAP05.Reservas.Controller;

import Fabrica_EAP05.Reservas.DTO.ReservaEventoResponse;
import Fabrica_EAP05.Reservas.Exception.CupoAgotadoException;
import Fabrica_EAP05.Reservas.Exception.EventoNoEncontradoException;
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
        when(authentication.getName()).thenReturn(email);
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
}
