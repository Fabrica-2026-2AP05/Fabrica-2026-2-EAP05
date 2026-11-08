package Fabrica_EAP05.Reservas.Controller;

import Fabrica_EAP05.Reservas.DTO.UsuarioRegistroRequest;
import Fabrica_EAP05.Reservas.DTO.UsuarioRegistroResponse;
import Fabrica_EAP05.Reservas.Entities.Rol;
import Fabrica_EAP05.Reservas.Service.IUsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {
    @Mock private IUsuarioService usuarioService;
    @InjectMocks private UsuarioController controller;
    private UsuarioRegistroRequest request;

    @BeforeEach
    void setUp() {
        request = new UsuarioRegistroRequest();
        request.setNombre("Ana Pérez"); request.setEmail("ana@correo.com");
        request.setTelefono("3001234567"); request.setDireccion("Calle 10");
    }

    @Test @DisplayName("CP-04-A - Registro exitoso de cliente")
    void registrarCliente_datosValidos_retornaCreated() {
        // Arrange
        UsuarioRegistroResponse servicio = respuesta(Rol.cliente);
        when(usuarioService.registrarUsuario(request, Rol.cliente)).thenReturn(servicio);

        // Act
        ResponseEntity<UsuarioRegistroResponse> respuesta = controller.registrarCliente(request);

        // Assert
        assertAll(() -> assertEquals(HttpStatus.CREATED, respuesta.getStatusCode()),
                () -> assertSame(servicio, respuesta.getBody()));
    }

    @Test @DisplayName("CP-05-A - Registro exitoso de proveedor")
    void registrarProveedor_datosValidos_retornaCreated() {
        // Arrange
        UsuarioRegistroResponse servicio = respuesta(Rol.proveedor);
        when(usuarioService.registrarUsuario(request, Rol.proveedor)).thenReturn(servicio);

        // Act
        ResponseEntity<UsuarioRegistroResponse> respuesta = controller.registrarProveedor(request);

        // Assert
        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        verify(usuarioService).registrarUsuario(request, Rol.proveedor);
    }

    @Test @DisplayName("CP-05-E - Registrar proveedor exige rol administrador")
    void registrarProveedor_tieneRestriccionAdministrador() throws Exception {
        // Arrange
        Method metodo = UsuarioController.class.getMethod("registrarProveedor", UsuarioRegistroRequest.class);

        // Act
        PreAuthorize seguridad = metodo.getAnnotation(PreAuthorize.class);

        // Assert
        assertNotNull(seguridad);
        assertEquals("hasRole('administrador')", seguridad.value());
    }

    private UsuarioRegistroResponse respuesta(Rol rol) {
        return new UsuarioRegistroResponse(UUID.randomUUID(), request.getNombre(), request.getEmail(),
                request.getTelefono(), request.getDireccion(), rol, OffsetDateTime.now());
    }
}
