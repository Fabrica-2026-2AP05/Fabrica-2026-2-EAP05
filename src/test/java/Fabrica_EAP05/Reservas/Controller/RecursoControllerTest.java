package Fabrica_EAP05.Reservas.Controller;

import Fabrica_EAP05.Reservas.DTO.*;
import Fabrica_EAP05.Reservas.Service.RecursoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecursoControllerTest {
    @Mock private RecursoService service;
    @InjectMocks private RecursoController controller;
    private UUID id;

    @BeforeEach void setUp() { id = UUID.randomUUID(); }

    @Test void registrarRecurso_datosValidos_retornaCreated() {
        // Arrange
        RegistrarRecursoRequest request = new RegistrarRecursoRequest();
        RegistroRecursoResponse resultado = mock(RegistroRecursoResponse.class);
        when(service.registrarRecurso(request)).thenReturn(resultado);
        // Act
        ResponseEntity<RegistroRecursoResponse> respuesta = controller.RegistrarRecurso(request);
        // Assert
        assertAll(() -> assertEquals(HttpStatus.CREATED, respuesta.getStatusCode()),
                () -> assertSame(resultado, respuesta.getBody()));
    }

    @Test void cambiarDisponibilidad_datosValidos_retornaOk() {
        // Arrange
        CambiarDisponibilidadRecursoRequest request = new CambiarDisponibilidadRecursoRequest();
        request.setActivo(false);
        RecursoDisponibilidadResponse resultado = mock(RecursoDisponibilidadResponse.class);
        when(service.cambiarDisponibilidad(id, false)).thenReturn(resultado);
        // Act
        ResponseEntity<RecursoDisponibilidadResponse> respuesta = controller.cambiarDisponibilidad(id, request);
        // Assert
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(resultado, respuesta.getBody());
    }

    @Test void definirDisponibilidad_datosValidos_retornaCreated() {
        // Arrange
        DefinirDisponibilidadRequest request = new DefinirDisponibilidadRequest();
        request.setInicio(OffsetDateTime.parse("2026-11-01T08:00:00-05:00"));
        request.setFin(OffsetDateTime.parse("2026-11-01T10:00:00-05:00"));
        DisponibilidadRecursoResponse resultado = mock(DisponibilidadRecursoResponse.class);
        when(service.definirDisponibilidad(id, request.getInicio(), request.getFin())).thenReturn(resultado);
        // Act
        ResponseEntity<DisponibilidadRecursoResponse> respuesta = controller.definirDisponibilidad(id, request);
        // Assert
        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertSame(resultado, respuesta.getBody());
    }
}
