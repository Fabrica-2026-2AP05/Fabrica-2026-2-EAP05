package Fabrica_EAP05.Reservas.Controller;

import Fabrica_EAP05.Reservas.DTO.LoginDTO;
import Fabrica_EAP05.Reservas.DTO.LoginResponseDTO;
import Fabrica_EAP05.Reservas.Entities.Rol;
import Fabrica_EAP05.Reservas.Service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    @Mock private AuthService authService;
    @InjectMocks private AuthController controller;
    private LoginDTO login;

    @BeforeEach
    void setUp() {
        login = new LoginDTO();
        login.setEmail("cliente@correo.com");
        login.setContrasena("Clave123*");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test @DisplayName("CP-01-A - Inicio de sesión exitoso con credenciales válidas")
    void login_datosValidos_retornaOk() {
        // Arrange
        LoginResponseDTO servicio = new LoginResponseDTO("jwt", login.getEmail(), Rol.cliente);
        when(authService.login(login)).thenReturn(servicio);

        // Act
        ResponseEntity<LoginResponseDTO> respuesta = controller.login(login);

        // Assert
        assertAll(() -> assertEquals(HttpStatus.OK, respuesta.getStatusCode()),
                () -> assertSame(servicio, respuesta.getBody()));
    }

    @Test @DisplayName("CP-03-A - Cierre de sesión exitoso")
    void logout_usuarioAutenticado_retornaOk() {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login.getEmail(), null, List.of()));

        // Act
        ResponseEntity<?> respuesta = controller.logout("Bearer jwt-valido");

        // Assert
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        verify(authService).logout(login.getEmail(), "jwt-valido");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test @DisplayName("CP-03-A - Cierra sesión autenticada sin encabezado Bearer")
    void logout_sinBearer_enviaTokenNulo() {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login.getEmail(), null, List.of()));

        // Act
        ResponseEntity<?> respuesta = controller.logout(null);

        // Assert
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        verify(authService).logout(login.getEmail(), null);
    }

    @Test @DisplayName("CP-03-B - Token inválido o expirado responde 401")
    void logout_sinAutenticacion_retornaUnauthorized() {
        // Arrange: el contexto de seguridad vacío se prepara en setUp().

        // Act
        ResponseEntity<?> respuesta = controller.logout("Bearer invalido");

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        verifyNoInteractions(authService);
    }
}
