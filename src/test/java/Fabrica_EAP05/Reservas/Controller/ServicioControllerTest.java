package Fabrica_EAP05.Reservas.Controller;

import Fabrica_EAP05.Reservas.DTO.ModificarServicioRequest;
import Fabrica_EAP05.Reservas.DTO.ModificarServicioResponse;
import Fabrica_EAP05.Reservas.DTO.RegistrarServicioRequest;
import Fabrica_EAP05.Reservas.DTO.RegistrarServicioResponse;
import Fabrica_EAP05.Reservas.Exception.RecursoDuplicadoException;
import Fabrica_EAP05.Reservas.Service.ServicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioControllerTest {
    @Mock private ServicioService service;
    @InjectMocks private ServicioController controller;
    private UUID servicioId;
    private RegistrarServicioRequest registrarRequest;
    private ModificarServicioRequest modificarRequest;

    @BeforeEach void setUp() {
        servicioId = UUID.randomUUID();
        registrarRequest = new RegistrarServicioRequest();
        modificarRequest = new ModificarServicioRequest();
    }

    @Test void validarRecurso_valido_retornaOk() {
        // Arrange
        ServicioController.ValidarRecursoRequest request = new ServicioController.ValidarRecursoRequest();
        request.setRecursoId(UUID.randomUUID());
        // Act
        ResponseEntity<?> respuesta = controller.validarRecurso(request);
        // Assert
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        verify(service).validarRecursoRequerido(request.getRecursoId());
    }

    @Test void validarRecurso_invalido_retornaBadRequest() {
        // Arrange
        ServicioController.ValidarRecursoRequest request = new ServicioController.ValidarRecursoRequest();
        request.setRecursoId(UUID.randomUUID());
        doThrow(new IllegalArgumentException("Recurso inactivo")).when(service).validarRecursoRequerido(request.getRecursoId());
        // Act
        ResponseEntity<?> respuesta = controller.validarRecurso(request);
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
    }

    @Test void registrarServicio_valido_retornaCreated() {
        // Arrange
        RegistrarServicioResponse resultado = mock(RegistrarServicioResponse.class);
        when(service.registrarServicio(registrarRequest)).thenReturn(resultado);
        // Act
        ResponseEntity<?> respuesta = controller.registrarServicio(registrarRequest);
        // Assert
        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertSame(resultado, respuesta.getBody());
    }

    @Test void registrarServicio_duplicado_retornaConflict() {
        // Arrange
        when(service.registrarServicio(registrarRequest)).thenThrow(new RecursoDuplicadoException("Duplicado"));
        // Act
        ResponseEntity<?> respuesta = controller.registrarServicio(registrarRequest);
        // Assert
        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
    }

    @Test void registrarServicio_invalido_retornaBadRequest() {
        // Arrange
        when(service.registrarServicio(registrarRequest)).thenThrow(new IllegalArgumentException("Nombre vacío"));
        // Act
        ResponseEntity<?> respuesta = controller.registrarServicio(registrarRequest);
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
    }

    @Test void registrarServicio_errorInesperado_retornaInternalServerError() {
        // Arrange
        when(service.registrarServicio(registrarRequest)).thenThrow(new RuntimeException("Error"));
        // Act
        ResponseEntity<?> respuesta = controller.registrarServicio(registrarRequest);
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
    }

    @Test void modificarServicio_valido_retornaOk() {
        // Arrange
        ModificarServicioResponse resultado = mock(ModificarServicioResponse.class);
        when(service.modificarServicio(servicioId, modificarRequest)).thenReturn(resultado);
        // Act
        ResponseEntity<?> respuesta = controller.modificarServicio(servicioId, modificarRequest);
        // Assert
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertSame(resultado, respuesta.getBody());
    }

    @Test void modificarServicio_inexistente_retornaNotFound() {
        // Arrange
        when(service.modificarServicio(servicioId, modificarRequest)).thenThrow(new NoSuchElementException("No existe"));
        // Act
        ResponseEntity<?> respuesta = controller.modificarServicio(servicioId, modificarRequest);
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
    }

    @Test void modificarServicio_conConflicto_retornaConflict() {
        // Arrange
        when(service.modificarServicio(servicioId, modificarRequest)).thenThrow(new IllegalStateException("Tiene reservas"));
        // Act
        ResponseEntity<?> respuesta = controller.modificarServicio(servicioId, modificarRequest);
        // Assert
        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
    }

    @Test void modificarServicio_invalido_retornaBadRequest() {
        // Arrange
        when(service.modificarServicio(servicioId, modificarRequest)).thenThrow(new IllegalArgumentException("Duración inválida"));
        // Act
        ResponseEntity<?> respuesta = controller.modificarServicio(servicioId, modificarRequest);
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
    }

    @Test void modificarServicio_errorInesperado_retornaInternalServerError() {
        // Arrange
        when(service.modificarServicio(servicioId, modificarRequest)).thenThrow(new RuntimeException("Error"));
        // Act
        ResponseEntity<?> respuesta = controller.modificarServicio(servicioId, modificarRequest);
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
    }
}
