package Fabrica_EAP05.Reservas.Service;

import Fabrica_EAP05.Reservas.DTO.UsuarioRegistroRequest;
import Fabrica_EAP05.Reservas.DTO.UsuarioRegistroResponse;
import Fabrica_EAP05.Reservas.Entities.Rol;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Exception.RecursoDuplicadoException;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EntityManager entityManager;
    @Mock private SupabaseAuthService supabaseAuthService;
    @InjectMocks private UsuarioServiceImpl usuarioService;
    private UsuarioRegistroRequest request;
    private UUID authId;

    @BeforeEach
    void setUp() {
        authId = UUID.randomUUID();
        request = new UsuarioRegistroRequest();
        request.setNombre("Ana Pérez");
        request.setEmail("ana@correo.com");
        request.setContrasena("Password123!");
        request.setTelefono("3001234567");
        request.setDireccion("Calle 10 # 20-30");
    }

    @Test @DisplayName("CP-04-A y CP-05-A - Registra usuario y asigna rol")
    void registrarUsuario_datosValidos_guardaYRetornaUsuario() throws Exception {
        // Arrange
        prepararRegistroExitoso(Rol.cliente);

        // Act
        UsuarioRegistroResponse respuesta = usuarioService.registrarUsuario(request, Rol.cliente);

        // Assert
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        assertAll(() -> assertEquals(authId, respuesta.getId()),
                () -> assertEquals(request.getEmail(), respuesta.getEmail()),
                () -> assertEquals(Rol.cliente, respuesta.getRol()),
                () -> assertTrue(captor.getValue().getActivo()));
    }

    @Test @DisplayName("CP-04-C y CP-05-C - Rechaza correo duplicado")
    void registrarUsuario_correoDuplicado_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // Act
        RecursoDuplicadoException error = assertThrows(RecursoDuplicadoException.class,
                () -> usuarioService.registrarUsuario(request, Rol.cliente));

        // Assert
        assertEquals("El email ya está registrado", error.getMessage());
        verifyNoInteractions(supabaseAuthService, entityManager);
    }

    @Test @DisplayName("CP-04-C - Evita usuario huérfano si falla guardado local")
    void registrarUsuario_fallaGuardadoLocal_eliminaUsuarioAuth() {
        // Arrange
        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(supabaseAuthService.crearUsuarioAuth(request.getEmail(), request.getContrasena())).thenReturn(authId);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenThrow(new RuntimeException("Error de base de datos"));
        // Act
        assertThrows(RuntimeException.class, () -> usuarioService.registrarUsuario(request, Rol.cliente));

        // Assert
        verify(supabaseAuthService).eliminarUsuarioAuth(authId);
    }

    @Test @DisplayName("CP-05-A - Registra proveedor con el rol forzado")
    void registrarUsuario_proveedorValido_asignaRolProveedor() {
        // Arrange
        prepararRegistroExitoso(Rol.proveedor);
        // Act
        UsuarioRegistroResponse respuesta = usuarioService.registrarUsuario(request, Rol.proveedor);

        // Assert
        assertEquals(authId, respuesta.getId());
        assertEquals(Rol.proveedor, respuesta.getRol());
        verify(usuarioRepository).saveAndFlush(any(Usuario.class));
        verify(supabaseAuthService, never()).eliminarUsuarioAuth(any());
    }

    private void prepararRegistroExitoso(Rol rol) {
        when(usuarioRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(supabaseAuthService.crearUsuarioAuth(request.getEmail(), request.getContrasena())).thenReturn(authId);
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findById(authId)).thenAnswer(invocation -> {
            Usuario usuario = new Usuario();
            usuario.setId(authId); usuario.setNombre(request.getNombre()); usuario.setEmail(request.getEmail());
            usuario.setTelefono(request.getTelefono()); usuario.setDireccion(request.getDireccion());
            usuario.setRol(rol); usuario.setActivo(true);
            return Optional.of(usuario);
        });
    }
}
