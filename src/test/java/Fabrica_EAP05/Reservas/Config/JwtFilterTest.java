package Fabrica_EAP05.Reservas.Config;

import Fabrica_EAP05.Reservas.Entities.Rol;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;
import Fabrica_EAP05.Reservas.Service.InactivityTrackingService;
import Fabrica_EAP05.Reservas.Service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {
    @Mock private JwtUtil jwtUtil;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private InactivityTrackingService inactivityTrackingService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @InjectMocks private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test @DisplayName("CP-01-A - Un token válido autentica al usuario con su rol")
    void filtrar_tokenValido_autenticaUsuario() throws Exception {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setEmail("cliente@correo.com"); usuario.setRol(Rol.cliente); usuario.setActivo(true);
        when(request.getHeader("Authorization")).thenReturn("Bearer jwt-valido");
        when(tokenBlacklistService.estaEnLista("jwt-valido")).thenReturn(false);
        when(jwtUtil.validarToken("jwt-valido")).thenReturn(true);
        when(jwtUtil.extraerEmail("jwt-valido")).thenReturn(usuario.getEmail());
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(inactivityTrackingService.validarActividad(usuario.getEmail())).thenReturn(true);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert

        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        assertAll(() -> assertNotNull(autenticacion),
                () -> assertEquals(usuario.getEmail(), autenticacion.getPrincipal()),
                () -> assertTrue(autenticacion.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_cliente"))));
        verify(filterChain).doFilter(request, response);
        verify(inactivityTrackingService).registrarActividad(usuario.getEmail());
    }

    @Test @DisplayName("CP-03-C - Un token revocado no permite continuar")
    void filtrar_tokenRevocado_respondeUnauthorized() throws Exception {
        // Arrange
        StringWriter contenido = new StringWriter();
        when(request.getHeader("Authorization")).thenReturn("Bearer jwt-revocado");
        when(tokenBlacklistService.estaEnLista("jwt-revocado")).thenReturn(true);
        when(response.getWriter()).thenReturn(new PrintWriter(contenido));

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(contenido.toString().contains("Sesión expirada"));
    }

    @Test
    @DisplayName("CP-03-B - Una solicitud sin token continúa sin autenticación")
    void filtrar_sinToken_continuaCadenaSinAutenticar() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil, usuarioRepository, inactivityTrackingService, tokenBlacklistService);
    }

    @Test
    @DisplayName("CP-03-C - Una sesión vencida por inactividad responde 401")
    void filtrar_sesionInactiva_respondeUnauthorized() throws Exception {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setEmail("cliente@correo.com");
        usuario.setRol(Rol.cliente);
        usuario.setActivo(true);
        StringWriter contenido = new StringWriter();
        when(request.getHeader("Authorization")).thenReturn("Bearer jwt-valido");
        when(tokenBlacklistService.estaEnLista("jwt-valido")).thenReturn(false);
        when(jwtUtil.validarToken("jwt-valido")).thenReturn(true);
        when(jwtUtil.extraerEmail("jwt-valido")).thenReturn(usuario.getEmail());
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(inactivityTrackingService.validarActividad(usuario.getEmail())).thenReturn(false);
        when(response.getWriter()).thenReturn(new PrintWriter(contenido));

        // Act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        verify(inactivityTrackingService, never()).registrarActividad(usuario.getEmail());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(contenido.toString().contains("inactividad"));
    }
}
