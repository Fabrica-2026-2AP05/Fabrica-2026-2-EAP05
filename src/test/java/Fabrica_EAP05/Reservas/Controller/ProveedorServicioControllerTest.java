package Fabrica_EAP05.Reservas.Controller;

import Fabrica_EAP05.Reservas.DTO.AsignarProveedorRequest;
import Fabrica_EAP05.Reservas.DTO.AsignarProveedorResponse;
import Fabrica_EAP05.Reservas.Service.ProveedorServicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProveedorServicioControllerTest {
    @Mock private ProveedorServicioService service;
    @InjectMocks private ProveedorServicioController controller;
    private AsignarProveedorRequest request;

    @BeforeEach void setUp() {
        request = new AsignarProveedorRequest();
        request.setServicioId(UUID.randomUUID());
        request.setProveedorId(UUID.randomUUID());
    }

    @Test void asignar_datosValidos_retornaCreated() {
        // Arrange
        AsignarProveedorResponse resultado = mock(AsignarProveedorResponse.class);
        when(service.asignarProveedorAServicio(request.getServicioId(), request.getProveedorId())).thenReturn(resultado);
        // Act
        ResponseEntity<?> respuesta = controller.asignarProveedor(request);
        // Assert
        assertAll(() -> assertEquals(HttpStatus.CREATED, respuesta.getStatusCode()),
                () -> assertSame(resultado, respuesta.getBody()));
    }

    @Test void asignar_datosInvalidos_retornaBadRequest() {
        // Arrange
        when(service.asignarProveedorAServicio(request.getServicioId(), request.getProveedorId()))
                .thenThrow(new RuntimeException("Asignación duplicada"));
        // Act
        ResponseEntity<?> respuesta = controller.asignarProveedor(request);
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
    }
}
