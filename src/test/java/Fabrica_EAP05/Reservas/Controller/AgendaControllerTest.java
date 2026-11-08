package Fabrica_EAP05.Reservas.Controller;

import Fabrica_EAP05.Reservas.DTO.AgendaResponse;
import Fabrica_EAP05.Reservas.Service.AgendaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendaControllerTest {
    @Mock private AgendaService agendaService;
    @Mock private Authentication authentication;
    @InjectMocks private AgendaController controller;
    private String email;

    @BeforeEach void setUp() {
        email = "proveedor@correo.com";
        when(authentication.getName()).thenReturn(email);
    }

    @Test void consultarAgenda_conReservas_retornaOk() {
        // Arrange
        AgendaResponse agenda = new AgendaResponse();
        when(agendaService.consultarAgendaProveedor(email)).thenReturn(List.of(agenda));
        // Act
        ResponseEntity<?> respuesta = controller.consultarAgenda(authentication);
        // Assert
        assertAll(() -> assertEquals(HttpStatus.OK, respuesta.getStatusCode()),
                () -> assertEquals(List.of(agenda), respuesta.getBody()));
    }

    @Test void consultarAgenda_sinReservas_retornaMensaje() {
        // Arrange
        when(agendaService.consultarAgendaProveedor(email)).thenReturn(List.of());
        // Act
        ResponseEntity<?> respuesta = controller.consultarAgenda(authentication);
        // Assert
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertTrue(respuesta.getBody().toString().contains("No existen reservas"));
    }

    @Test void consultarAgenda_proveedorInvalido_retornaBadRequest() {
        // Arrange
        when(agendaService.consultarAgendaProveedor(email)).thenThrow(new IllegalArgumentException("Proveedor no encontrado"));
        // Act
        ResponseEntity<?> respuesta = controller.consultarAgenda(authentication);
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertTrue(respuesta.getBody().toString().contains("Proveedor no encontrado"));
    }
}
