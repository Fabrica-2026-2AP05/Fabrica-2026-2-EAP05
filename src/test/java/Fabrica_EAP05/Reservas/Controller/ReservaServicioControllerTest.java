package Fabrica_EAP05.Reservas.Controller;

import Fabrica_EAP05.Reservas.DTO.CancelarReservaResponse;
import Fabrica_EAP05.Reservas.DTO.CrearReservaRequest;
import Fabrica_EAP05.Reservas.DTO.CrearReservaResponse;
import Fabrica_EAP05.Reservas.Service.ReservaServicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServicioControllerTest {
    @Mock private ReservaServicioService service;
    @InjectMocks private ReservaServicioController controller;
    private String email;
    private UUID reservaId;
    private CrearReservaRequest request;

    @BeforeEach void setUp() {
        email = "cliente@correo.com";
        reservaId = UUID.randomUUID();
        request = new CrearReservaRequest();
        request.setServicioId(UUID.randomUUID());
        request.setHorarioId(UUID.randomUUID());
    }

    @Test void crearReserva_datosValidos_retornaCreated() {
        // Arrange
        CrearReservaResponse resultado = mock(CrearReservaResponse.class);
        when(service.crearReserva(email, request)).thenReturn(resultado);
        // Act
        ResponseEntity<?> respuesta = controller.crearReserva(request, email);
        // Assert
        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertSame(resultado, respuesta.getBody());
    }

    @Test void crearReserva_conflicto_retornaConflict() {
        // Arrange
        when(service.crearReserva(email, request)).thenThrow(new IllegalStateException("Horario ocupado"));
        // Act
        ResponseEntity<?> respuesta = controller.crearReserva(request, email);
        // Assert
        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
    }

    @Test void crearReserva_argumentoInvalido_retornaBadRequest() {
        // Arrange
        when(service.crearReserva(email, request)).thenThrow(new IllegalArgumentException("Servicio inexistente"));
        // Act
        ResponseEntity<?> respuesta = controller.crearReserva(request, email);
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
    }

    @Test void crearReserva_errorInesperado_retornaInternalServerError() {
        // Arrange
        when(service.crearReserva(email, request)).thenThrow(new RuntimeException("Fallo inesperado"));
        // Act
        ResponseEntity<?> respuesta = controller.crearReserva(request, email);
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
    }

    @Test void crearReserva_errorBaseDatos_sePropaga() {
        // Arrange
        DataAccessResourceFailureException error = new DataAccessResourceFailureException("Sin conexión");
        when(service.crearReserva(email, request)).thenThrow(error);
        // Act y Assert
        assertSame(error, assertThrows(DataAccessResourceFailureException.class,
                () -> controller.crearReserva(request, email)));
    }

    @Test void cancelarReserva_confirmada_retornaOk() {
        // Arrange
        CancelarReservaResponse resultado = mock(CancelarReservaResponse.class);
        when(service.cancelarReserva(email, reservaId)).thenReturn(resultado);
        // Act
        ResponseEntity<?> respuesta = controller.cancelarReserva(reservaId, email);
        // Assert
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(resultado, respuesta.getBody());
    }

    @Test void cancelarReserva_estadoInvalido_retornaConflict() {
        // Arrange
        when(service.cancelarReserva(email, reservaId)).thenThrow(new IllegalStateException("Ya cancelada"));
        // Act
        ResponseEntity<?> respuesta = controller.cancelarReserva(reservaId, email);
        // Assert
        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
    }

    @Test void cancelarReserva_inexistente_retornaBadRequest() {
        // Arrange
        when(service.cancelarReserva(email, reservaId)).thenThrow(new IllegalArgumentException("No existe"));
        // Act
        ResponseEntity<?> respuesta = controller.cancelarReserva(reservaId, email);
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
    }
}
