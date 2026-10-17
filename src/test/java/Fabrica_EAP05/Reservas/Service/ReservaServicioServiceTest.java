package Fabrica_EAP05.Reservas.Service;

import Fabrica_EAP05.Reservas.DTO.CancelarReservaResponse;
import Fabrica_EAP05.Reservas.DTO.CrearReservaRequest;
import Fabrica_EAP05.Reservas.DTO.CrearReservaResponse;
import Fabrica_EAP05.Reservas.Entities.DisponibilidadRecurso;
import Fabrica_EAP05.Reservas.Entities.Horario;
import Fabrica_EAP05.Reservas.Entities.Recurso;
import Fabrica_EAP05.Reservas.Entities.ReservaServicio;
import Fabrica_EAP05.Reservas.Entities.Servicio;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Enums.EstadoHorario;
import Fabrica_EAP05.Reservas.Enums.EstadoReserva;
import Fabrica_EAP05.Reservas.Exception.UsuarioInactivoException;
import Fabrica_EAP05.Reservas.Repository.DisponibilidadRecursoRepository;
import Fabrica_EAP05.Reservas.Repository.HorarioRepository;
import Fabrica_EAP05.Reservas.Repository.ReservaServicioRepository;
import Fabrica_EAP05.Reservas.Repository.ServicioRepository;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServicioServiceTest {
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private HorarioRepository horarioRepository;
    @Mock private ReservaServicioRepository reservaRepository;
    @Mock private DisponibilidadRecursoRepository disponibilidadRepository;
    @Mock private EntityManager entityManager;
    @InjectMocks private ReservaServicioService reservaService;

    private String email;
    private UUID reservaId;
    private Usuario cliente;
    private Recurso recurso;
    private Servicio servicio;
    private Horario horario;
    private ReservaServicio reserva;
    private CrearReservaRequest request;

    @BeforeEach
    void setUp() {
        email = "cliente@correo.com";
        reservaId = UUID.randomUUID();
        cliente = new Usuario();
        cliente.setId(UUID.randomUUID());
        cliente.setEmail(email);
        cliente.setActivo(true);
        recurso = new Recurso();
        recurso.setId(UUID.randomUUID());
        recurso.setNombre("Sala 1");
        recurso.setDescripcion("Sala principal");
        recurso.setActivo(true);
        servicio = new Servicio(UUID.randomUUID(), "Masaje", "Relajante", 60, recurso, true);
        horario = new Horario();
        horario.setId(UUID.randomUUID());
        horario.setInicio(LocalDateTime.of(2026, 11, 1, 8, 0));
        horario.setFin(LocalDateTime.of(2026, 11, 1, 9, 0));
        horario.setEstado(EstadoHorario.disponible);
        reserva = new ReservaServicio();
        reserva.setId(reservaId);
        reserva.setClienteId(cliente.getId());
        reserva.setServicioId(servicio.getId());
        reserva.setHorarioId(horario.getId());
        reserva.setRecursoId(recurso.getId());
        reserva.setEstado(EstadoReserva.confirmada);
        request = new CrearReservaRequest();
        request.setServicioId(servicio.getId());
        request.setHorarioId(horario.getId());
    }

    @Test @DisplayName("Crea una reserva cuando todos los elementos están disponibles")
    void crearReserva_datosValidos_confirmaReservaYReservaHorario() {
        // Arrange
        prepararCreacionValida();

        // Act
        CrearReservaResponse resultado = reservaService.crearReserva(email, request);

        // Assert
        assertAll(() -> assertEquals(reservaId, resultado.getId()),
                () -> assertEquals("confirmada", resultado.getEstado()),
                () -> assertEquals(recurso.getId(), resultado.getRecursoId()),
                () -> assertEquals(EstadoHorario.reservado, horario.getEstado()));
        verify(horarioRepository).save(horario);
        verify(reservaRepository).saveAndFlush(any(ReservaServicio.class));
    }

    @Test @DisplayName("Rechaza una solicitud sin servicio")
    void crearReserva_sinServicio_lanzaExcepcion() {
        // Arrange
        request.setServicioId(null);

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> reservaService.crearReserva(email, request));

        // Assert
        assertEquals("El servicio es obligatorio", error.getMessage());
        verifyNoInteractions(usuarioRepository);
    }

    @Test @DisplayName("Rechaza un cliente inactivo")
    void crearReserva_clienteInactivo_lanzaExcepcion() {
        // Arrange
        cliente.setActivo(false);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));

        // Act
        assertThrows(UsuarioInactivoException.class,
                () -> reservaService.crearReserva(email, request));

        // Assert
        verifyNoInteractions(servicioRepository, horarioRepository);
    }

    @Test @DisplayName("Rechaza un servicio inexistente")
    void crearReserva_servicioInexistente_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(servicioRepository.findById(servicio.getId())).thenReturn(Optional.empty());

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> reservaService.crearReserva(email, request));

        // Assert
        assertTrue(error.getMessage().contains("servicio no existe"));
        verifyNoInteractions(horarioRepository);
    }

    @Test @DisplayName("Rechaza un horario que ya no está disponible")
    void crearReserva_horarioReservado_lanzaExcepcion() {
        // Arrange
        horario.setEstado(EstadoHorario.reservado);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(servicioRepository.findById(servicio.getId())).thenReturn(Optional.of(servicio));
        when(horarioRepository.findByIdParaActualizar(horario.getId())).thenReturn(Optional.of(horario));

        // Act
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> reservaService.crearReserva(email, request));

        // Assert
        assertTrue(error.getMessage().contains("no está disponible"));
        verifyNoInteractions(disponibilidadRepository);
    }

    @Test @DisplayName("Rechaza un recurso sin cobertura de disponibilidad")
    void crearReserva_recursoSinDisponibilidad_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(servicioRepository.findById(servicio.getId())).thenReturn(Optional.of(servicio));
        when(horarioRepository.findByIdParaActualizar(horario.getId())).thenReturn(Optional.of(horario));
        when(disponibilidadRepository.findByRecursoIdAndInicioLessThanEqualAndFinGreaterThanEqual(
                recurso.getId(), horario.getInicio(), horario.getFin())).thenReturn(List.of());
        when(reservaRepository.contarReservasDeRecursoEnRango(
                recurso.getId(), horario.getInicio(), horario.getFin())).thenReturn(0L);

        // Act
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> reservaService.crearReserva(email, request));

        // Assert
        assertTrue(error.getMessage().contains("recurso requerido"));
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test @DisplayName("Crea una reserva para un servicio que no requiere recurso")
    void crearReserva_servicioSinRecurso_confirmaReserva() {
        // Arrange
        servicio.setRecurso(null);
        reserva.setRecursoId(null);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(servicioRepository.findById(servicio.getId())).thenReturn(Optional.of(servicio));
        when(horarioRepository.findByIdParaActualizar(horario.getId())).thenReturn(Optional.of(horario));
        when(reservaRepository.saveAndFlush(any(ReservaServicio.class))).thenAnswer(i -> i.getArgument(0));
        when(reservaRepository.findById(any(UUID.class))).thenReturn(Optional.of(reserva));

        // Act
        CrearReservaResponse resultado = reservaService.crearReserva(email, request);

        // Assert
        assertNull(resultado.getRecursoId());
        verifyNoInteractions(disponibilidadRepository);
        verify(reservaRepository).saveAndFlush(any(ReservaServicio.class));
    }

    @Test @DisplayName("Cancela una reserva confirmada del cliente")
    void cancelarReserva_reservaPropiaConfirmada_cancelaReserva() {
        // Arrange
        reserva.setCanceladaEn(LocalDateTime.of(2026, 10, 30, 10, 0));
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(reservaRepository.findByIdParaActualizar(reservaId)).thenReturn(Optional.of(reserva));

        // Act
        CancelarReservaResponse resultado = reservaService.cancelarReserva(email, reservaId);

        // Assert
        assertAll(() -> assertEquals(reservaId, resultado.getId()),
                () -> assertEquals("cancelada", resultado.getEstado()),
                () -> assertNotNull(resultado.getCanceladaEn()));
        verify(entityManager).refresh(reserva);
    }

    @Test @DisplayName("Impide cancelar una reserva de otro cliente")
    void cancelarReserva_reservaAjena_lanzaExcepcion() {
        // Arrange
        reserva.setClienteId(UUID.randomUUID());
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(reservaRepository.findByIdParaActualizar(reservaId)).thenReturn(Optional.of(reserva));

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> reservaService.cancelarReserva(email, reservaId));

        // Assert
        assertEquals("La reserva no existe", error.getMessage());
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test @DisplayName("Impide cancelar una reserva que no está confirmada")
    void cancelarReserva_reservaYaCancelada_lanzaExcepcion() {
        // Arrange
        reserva.setEstado(EstadoReserva.cancelada);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(reservaRepository.findByIdParaActualizar(reservaId)).thenReturn(Optional.of(reserva));

        // Act
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> reservaService.cancelarReserva(email, reservaId));

        // Assert
        assertTrue(error.getMessage().contains("no está confirmada"));
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    private void prepararCreacionValida() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(servicioRepository.findById(servicio.getId())).thenReturn(Optional.of(servicio));
        when(horarioRepository.findByIdParaActualizar(horario.getId())).thenReturn(Optional.of(horario));
        when(disponibilidadRepository.findByRecursoIdAndInicioLessThanEqualAndFinGreaterThanEqual(
                recurso.getId(), horario.getInicio(), horario.getFin()))
                .thenReturn(List.of(new DisponibilidadRecurso()));
        when(reservaRepository.contarReservasDeRecursoEnRango(
                recurso.getId(), horario.getInicio(), horario.getFin())).thenReturn(0L);
        when(reservaRepository.saveAndFlush(any(ReservaServicio.class))).thenAnswer(i -> i.getArgument(0));
        when(reservaRepository.findById(any(UUID.class))).thenReturn(Optional.of(reserva));
    }
}
