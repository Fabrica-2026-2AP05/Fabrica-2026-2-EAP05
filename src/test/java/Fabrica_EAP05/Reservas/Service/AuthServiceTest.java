package Fabrica_EAP05.Reservas.Service;

import Fabrica_EAP05.Reservas.Config.JwtUtil;
import Fabrica_EAP05.Reservas.DTO.LoginDTO;
import Fabrica_EAP05.Reservas.DTO.LoginResponseDTO;
import Fabrica_EAP05.Reservas.Entities.Rol;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Exception.CredencialesInvalidasException;
import Fabrica_EAP05.Reservas.Exception.CuentaBloqueadaException;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private InactivityTrackingService inactivityTrackingService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private RestTemplate restTemplate;
    @Mock private SupabaseAuthService supabaseAuthService;
    @Mock private LoginAttemptService loginAttemptService;
    @InjectMocks private AuthService authService;
    private LoginDTO login;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "supabaseUrl", "https://supabase.test");
        ReflectionTestUtils.setField(authService, "supabaseAnonKey", "anon-key");
        login = new LoginDTO();
        login.setEmail("cliente@correo.com");
        login.setContrasena("Clave123*");
        usuario = new Usuario();
        usuario.setEmail(login.getEmail());
        usuario.setRol(Rol.cliente);
        usuario.setActivo(true);
    }

    @Test @DisplayName("CP-01-A - Genera token con credenciales correctas")
    void login_credencialesCorrectas_retornaToken() {
        // Arrange
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(ResponseEntity.ok("ok"));
        when(usuarioRepository.findByEmail(login.getEmail())).thenReturn(Optional.of(usuario));
        when(jwtUtil.generarToken(login.getEmail(), Rol.cliente)).thenReturn("jwt-valido");
        // Act
        LoginResponseDTO respuesta = authService.login(login);

        // Assert
        assertAll(() -> assertEquals("jwt-valido", respuesta.getToken()),
                () -> assertEquals(login.getEmail(), respuesta.getUsuario()),
                () -> assertEquals(Rol.cliente, respuesta.getRol()));
        verify(inactivityTrackingService).registrarActividad(login.getEmail());
        verify(loginAttemptService).registrarExito(login.getEmail());
        verify(loginAttemptService, never()).registrarFallo(anyString());
    }

    @Test @DisplayName("CP-01-B - Rechaza credenciales incorrectas y cuenta el fallo")
    void login_credencialesIncorrectas_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByEmail(login.getEmail())).thenReturn(Optional.of(usuario));
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null));

        // Act
        CredencialesInvalidasException error = assertThrows(CredencialesInvalidasException.class,
                () -> authService.login(login));

        // Assert
        assertEquals("Credenciales inválidas", error.getMessage());
        verify(loginAttemptService).verificarBloqueo(login.getEmail());
        verify(loginAttemptService).registrarFallo(login.getEmail());
        verify(loginAttemptService, never()).registrarExito(anyString());
        verifyNoInteractions(jwtUtil, inactivityTrackingService);
    }

    @Test @DisplayName("CP-01-E - Rechaza usuario inexistente con mensaje genérico y cuenta el fallo")
    void login_usuarioNoExiste_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByEmail(login.getEmail())).thenReturn(Optional.empty());

        // Act
        CredencialesInvalidasException error = assertThrows(CredencialesInvalidasException.class,
                () -> authService.login(login));

        // Assert
        assertEquals("Credenciales inválidas", error.getMessage());
        verify(loginAttemptService).registrarFallo(login.getEmail());
        verifyNoInteractions(restTemplate, jwtUtil, inactivityTrackingService);
    }

    @Test @DisplayName("CP-01-F - Rechaza un usuario inactivo con mensaje genérico y cuenta el fallo")
    void login_usuarioInactivo_lanzaExcepcion() {
        // Arrange
        usuario.setActivo(false);
        when(usuarioRepository.findByEmail(login.getEmail())).thenReturn(Optional.of(usuario));

        // Act
        CredencialesInvalidasException error = assertThrows(CredencialesInvalidasException.class,
                () -> authService.login(login));

        // Assert
        assertEquals("Credenciales inválidas", error.getMessage());
        verify(loginAttemptService).registrarFallo(login.getEmail());
        verifyNoInteractions(restTemplate, jwtUtil, inactivityTrackingService);
    }

    @Test @DisplayName("CP-01-D - Rechaza el login de una cuenta bloqueada sin consultar credenciales")
    void login_cuentaBloqueada_lanzaExcepcion() {
        // Arrange
        doThrow(new CuentaBloqueadaException("Cuenta bloqueada temporalmente"))
                .when(loginAttemptService).verificarBloqueo(login.getEmail());

        // Act
        assertThrows(CuentaBloqueadaException.class, () -> authService.login(login));

        // Assert
        verify(loginAttemptService, never()).registrarFallo(anyString());
        verifyNoInteractions(usuarioRepository, restTemplate, jwtUtil, inactivityTrackingService);
    }

    @Test @DisplayName("CP-03-A y CP-03-C - Cierra sesión y revoca el token")
    void logout_conToken_cierraActividadYRevocaToken() {
        // Arrange: los mocks y el usuario se preparan en setUp().

        // Act
        authService.logout(login.getEmail(), "jwt-valido");

        // Assert
        verify(inactivityTrackingService).cerrarSesion(login.getEmail());
        verify(tokenBlacklistService).agregarTokenALista("jwt-valido");
    }

    @Test @DisplayName("CP-03-A - Cierra la actividad aunque no reciba token")
    void logout_sinToken_cierraActividadSinRevocar() {
        // Arrange: los mocks y el usuario se preparan en setUp().

        // Act
        authService.logout(login.getEmail(), null);

        // Assert
        verify(inactivityTrackingService).cerrarSesion(login.getEmail());
        verifyNoInteractions(tokenBlacklistService);
    }

}
