package Fabrica_EAP05.Reservas.Service;

import Fabrica_EAP05.Reservas.DTO.HorarioRequest;
import Fabrica_EAP05.Reservas.Entities.Horario;
import Fabrica_EAP05.Reservas.Entities.Rol;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Enums.EstadoHorario;
import Fabrica_EAP05.Reservas.Enums.EstadoReserva;
import Fabrica_EAP05.Reservas.Repository.HorarioRepository;
import Fabrica_EAP05.Reservas.Repository.ProveedorServicioRepository;
import Fabrica_EAP05.Reservas.Repository.ReservaServicioRepository;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorarioServiceTest {
    @Mock private HorarioRepository horarioRepository;
    @Mock private ProveedorServicioRepository proveedorServicioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ReservaServicioRepository reservaRepository;
    @InjectMocks private HorarioService horarioService;

    private UUID proveedorId;
    private UUID horarioId;
    private String email;
    private Usuario proveedor;
    private HorarioRequest request;
    private Horario horario;

    @BeforeEach
    void setUp() {
        proveedorId = UUID.randomUUID();
        horarioId = UUID.randomUUID();
        email = "proveedor@correo.com";
        proveedor = new Usuario();
        proveedor.setId(proveedorId);
        proveedor.setEmail(email);
        proveedor.setRol(Rol.proveedor);
        request = new HorarioRequest();
        request.setProveedorId(proveedorId);
        request.setInicio(LocalDateTime.of(2026, 11, 1, 8, 0));
        request.setFin(LocalDateTime.of(2026, 11, 1, 10, 0));
        horario = new Horario();
        horario.setId(horarioId);
        horario.setProveedorId(proveedorId);
        horario.setInicio(request.getInicio());
        horario.setFin(request.getFin());
        horario.setEstado(EstadoHorario.disponible);
    }

    @Test @DisplayName("Registra un horario válido")
    void registrarHorario_datosValidos_guardaHorarioDisponible() {
        // Arrange
        when(usuarioRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));
        when(proveedorServicioRepository.existsByProveedorId(proveedorId)).thenReturn(true);
        when(horarioRepository.findSolapados(proveedorId, request.getInicio(), request.getFin())).thenReturn(List.of());
        when(horarioRepository.save(any(Horario.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Horario resultado = horarioService.registrarHorario(request);

        // Assert
        assertAll(() -> assertEquals(proveedorId, resultado.getProveedorId()),
                () -> assertEquals(EstadoHorario.disponible, resultado.getEstado()),
                () -> assertNotNull(resultado.getId()));
    }

    @Test @DisplayName("Rechaza un rango de horas inválido")
    void registrarHorario_inicioPosteriorAlFin_lanzaExcepcion() {
        // Arrange
        request.setInicio(request.getFin());

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> horarioService.registrarHorario(request));

        // Assert
        assertTrue(error.getMessage().contains("inicio"));
        verifyNoInteractions(usuarioRepository, horarioRepository);
    }

    @Test @DisplayName("Rechaza un proveedor inexistente al registrar horario")
    void registrarHorario_proveedorInexistente_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findById(proveedorId)).thenReturn(Optional.empty());

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> horarioService.registrarHorario(request));

        // Assert
        assertTrue(error.getMessage().contains("no existe"));
        verifyNoInteractions(proveedorServicioRepository, horarioRepository);
    }

    @Test @DisplayName("Rechaza un usuario que no es proveedor")
    void registrarHorario_usuarioCliente_lanzaExcepcion() {
        // Arrange
        proveedor.setRol(Rol.cliente);
        when(usuarioRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> horarioService.registrarHorario(request));

        // Assert
        assertTrue(error.getMessage().contains("rol de proveedor"));
        verifyNoInteractions(proveedorServicioRepository, horarioRepository);
    }

    @Test @DisplayName("Rechaza un proveedor sin servicios")
    void registrarHorario_proveedorSinServicio_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));
        when(proveedorServicioRepository.existsByProveedorId(proveedorId)).thenReturn(false);

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> horarioService.registrarHorario(request));

        // Assert
        assertTrue(error.getMessage().contains("asociado"));
        verifyNoInteractions(horarioRepository);
    }

    @Test @DisplayName("Rechaza horarios solapados")
    void registrarHorario_periodoSolapado_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));
        when(proveedorServicioRepository.existsByProveedorId(proveedorId)).thenReturn(true);
        when(horarioRepository.findSolapados(proveedorId, request.getInicio(), request.getFin()))
                .thenReturn(List.of(horario));

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> horarioService.registrarHorario(request));

        // Assert
        assertTrue(error.getMessage().contains("solapa"));
        verify(horarioRepository, never()).save(any());
    }

    @Test @DisplayName("Bloquea un horario sin reserva confirmada")
    void bloquearHorario_sinReserva_confirmadaCambiaEstado() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(proveedor));
        when(horarioRepository.findByIdAndProveedorId(horarioId, proveedorId)).thenReturn(Optional.of(horario));
        when(reservaRepository.existsByHorarioIdAndEstado(horarioId, EstadoReserva.confirmada)).thenReturn(false);
        when(horarioRepository.save(horario)).thenReturn(horario);

        // Act
        Horario resultado = horarioService.bloquearHorario(horarioId, email);

        // Assert
        assertEquals(EstadoHorario.bloqueado, resultado.getEstado());
        verify(horarioRepository).save(horario);
    }

    @Test @DisplayName("Impide bloquear un horario con reserva confirmada")
    void bloquearHorario_conReservaConfirmada_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(proveedor));
        when(horarioRepository.findByIdAndProveedorId(horarioId, proveedorId)).thenReturn(Optional.of(horario));
        when(reservaRepository.existsByHorarioIdAndEstado(horarioId, EstadoReserva.confirmada)).thenReturn(true);

        // Act
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> horarioService.bloquearHorario(horarioId, email));

        // Assert
        assertTrue(error.getMessage().contains("reserva confirmada"));
        verify(horarioRepository, never()).save(any());
    }

    @Test @DisplayName("Impide bloquear un horario ajeno")
    void bloquearHorario_horarioAjeno_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(proveedor));
        when(horarioRepository.findByIdAndProveedorId(horarioId, proveedorId)).thenReturn(Optional.empty());

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> horarioService.bloquearHorario(horarioId, email));

        // Assert
        assertTrue(error.getMessage().contains("no pertenece"));
        verifyNoInteractions(reservaRepository);
    }

    @Test @DisplayName("Rechaza bloquear horario si el proveedor no existe")
    void bloquearHorario_proveedorInexistente_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> horarioService.bloquearHorario(horarioId, email));

        // Assert
        assertTrue(error.getMessage().contains(email));
        verifyNoInteractions(horarioRepository, reservaRepository);
    }
}
