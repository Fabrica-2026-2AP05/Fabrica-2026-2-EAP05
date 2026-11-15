package Fabrica_EAP05.Reservas.Service;

import Fabrica_EAP05.Reservas.DTO.CancelarReservaEventoResponse;
import Fabrica_EAP05.Reservas.DTO.ReservaEventoResponse;
import Fabrica_EAP05.Reservas.Entities.Evento;
import Fabrica_EAP05.Reservas.Entities.ReservaEvento;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Enums.EstadoReserva;
import Fabrica_EAP05.Reservas.Exception.ConflictoDisponibilidadException;
import Fabrica_EAP05.Reservas.Exception.CupoAgotadoException;
import Fabrica_EAP05.Reservas.Exception.EventoNoEncontradoException;
import Fabrica_EAP05.Reservas.Exception.PlazoCancelacionException;
import Fabrica_EAP05.Reservas.Exception.UsuarioInactivoException;
import Fabrica_EAP05.Reservas.Repository.EventoRepository;
import Fabrica_EAP05.Reservas.Repository.ReservaEventoRepository;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {
    @Mock private ReservaEventoRepository reservaEventoRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private EventoService eventoService;

    private String email;
    private Usuario cliente;
    private Evento evento;

    @BeforeEach
    void setUp() {
        email = "cliente@correo.com";
        cliente = new Usuario();
        cliente.setId(UUID.randomUUID());
        cliente.setActivo(true);

        evento = new Evento();
        evento.setId(UUID.randomUUID());
        evento.setNombre("Taller");
        evento.setCapacidad(10);
        evento.setActivo(true);
    }

    @Test @DisplayName("Escenario 1 - Reserva confirmada y los cupos disminuyen en uno")
    void reservarCupo_conCupos_confirmaReserva() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(eventoRepository.findByIdParaActualizar(evento.getId())).thenReturn(Optional.of(evento));
        when(reservaEventoRepository.existsByClienteIdAndEventoIdAndEstado(
                cliente.getId(), evento.getId(), EstadoReserva.confirmada)).thenReturn(false);
        when(reservaEventoRepository.countByEventoIdAndEstado(evento.getId(), EstadoReserva.confirmada))
                .thenReturn(4L);
        when(reservaEventoRepository.saveAndFlush(any(ReservaEvento.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ReservaEventoResponse resultado = eventoService.reservarCupo(email, evento.getId());

        // Assert
        ArgumentCaptor<ReservaEvento> captor = ArgumentCaptor.forClass(ReservaEvento.class);
        verify(reservaEventoRepository).saveAndFlush(captor.capture());
        ReservaEvento guardada = captor.getValue();
        assertAll(
                () -> assertNotNull(guardada.getId()),
                () -> assertEquals(cliente.getId(), guardada.getClienteId()),
                () -> assertEquals(evento.getId(), guardada.getEventoId()),
                () -> assertEquals(EstadoReserva.confirmada, guardada.getEstado()),
                () -> assertEquals(guardada.getId(), resultado.getId()),
                () -> assertEquals(evento.getId(), resultado.getEventoId()),
                () -> assertEquals(cliente.getId(), resultado.getClienteId()),
                () -> assertEquals("confirmada", resultado.getEstado()),
                () -> assertEquals(5L, resultado.getCuposRestantes()));
    }

    @Test @DisplayName("Escenario 1 - Toma el último cupo y quedan 0 restantes")
    void reservarCupo_ultimoCupo_quedanCeroRestantes() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(eventoRepository.findByIdParaActualizar(evento.getId())).thenReturn(Optional.of(evento));
        when(reservaEventoRepository.countByEventoIdAndEstado(evento.getId(), EstadoReserva.confirmada))
                .thenReturn(9L);
        when(reservaEventoRepository.saveAndFlush(any(ReservaEvento.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ReservaEventoResponse resultado = eventoService.reservarCupo(email, evento.getId());

        // Assert
        assertEquals(0L, resultado.getCuposRestantes());
    }

    @Test @DisplayName("Escenario 2 - Rechaza la reserva cuando el evento no tiene cupos")
    void reservarCupo_sinCupos_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(eventoRepository.findByIdParaActualizar(evento.getId())).thenReturn(Optional.of(evento));
        when(reservaEventoRepository.countByEventoIdAndEstado(evento.getId(), EstadoReserva.confirmada))
                .thenReturn(10L);

        // Act
        CupoAgotadoException error = assertThrows(CupoAgotadoException.class,
                () -> eventoService.reservarCupo(email, evento.getId()));

        // Assert
        assertEquals("No hay cupos disponibles para este evento", error.getMessage());
        verify(reservaEventoRepository, never()).saveAndFlush(any());
    }

    @Test @DisplayName("Rechaza la reserva de un evento inexistente")
    void reservarCupo_eventoInexistente_lanzaExcepcion() {
        // Arrange
        UUID eventoId = UUID.randomUUID();
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(eventoRepository.findByIdParaActualizar(eventoId)).thenReturn(Optional.empty());

        // Act
        EventoNoEncontradoException error = assertThrows(EventoNoEncontradoException.class,
                () -> eventoService.reservarCupo(email, eventoId));

        // Assert
        assertEquals("El evento no existe", error.getMessage());
        verifyNoInteractions(reservaEventoRepository);
    }

    @Test @DisplayName("Rechaza la reserva de un cliente inactivo")
    void reservarCupo_clienteInactivo_lanzaExcepcion() {
        // Arrange
        cliente.setActivo(false);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));

        // Act
        assertThrows(UsuarioInactivoException.class,
                () -> eventoService.reservarCupo(email, evento.getId()));

        // Assert
        verifyNoInteractions(eventoRepository, reservaEventoRepository);
    }

    @Test @DisplayName("Rechaza la reserva de un evento inactivo")
    void reservarCupo_eventoInactivo_lanzaExcepcion() {
        // Arrange
        evento.setActivo(false);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(eventoRepository.findByIdParaActualizar(evento.getId())).thenReturn(Optional.of(evento));

        // Act
        assertThrows(ConflictoDisponibilidadException.class,
                () -> eventoService.reservarCupo(email, evento.getId()));

        // Assert
        verifyNoInteractions(reservaEventoRepository);
    }

    @Test @DisplayName("Rechaza una segunda reserva confirmada del mismo cliente en el evento")
    void reservarCupo_reservaDuplicada_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(eventoRepository.findByIdParaActualizar(evento.getId())).thenReturn(Optional.of(evento));
        when(reservaEventoRepository.existsByClienteIdAndEventoIdAndEstado(
                cliente.getId(), evento.getId(), EstadoReserva.confirmada)).thenReturn(true);

        // Act
        assertThrows(ConflictoDisponibilidadException.class,
                () -> eventoService.reservarCupo(email, evento.getId()));

        // Assert
        verify(reservaEventoRepository, never()).saveAndFlush(any());
    }

    @Test @DisplayName("Rechaza un usuario autenticado que no existe")
    void reservarCupo_usuarioInexistente_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        assertThrows(IllegalArgumentException.class,
                () -> eventoService.reservarCupo(email, evento.getId()));

        // Assert
        verifyNoInteractions(eventoRepository, reservaEventoRepository);
    }

    // ---------- HU22: cancelar reserva de evento ----------

    private LocalDateTime ahora;

    private ReservaEvento reservaConfirmadaDelCliente() {
        ReservaEvento reserva = new ReservaEvento();
        reserva.setId(UUID.randomUUID());
        reserva.setClienteId(cliente.getId());
        reserva.setEventoId(evento.getId());
        reserva.setEstado(EstadoReserva.confirmada);
        return reserva;
    }

    // Reloj fijo: "ahora" es siempre el mismo instante durante el test
    private void fijarReloj() {
        Clock fijo = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneId.systemDefault());
        ReflectionTestUtils.setField(eventoService, "clock", fijo);
        ahora = LocalDateTime.now(fijo);
    }

    @Test @DisplayName("HU22 Escenario 1 - Cancela con más de 12 horas de anticipación y libera el cupo")
    void cancelarReservaEvento_conMasDe12Horas_cancelaReserva() {
        // Arrange
        fijarReloj();
        evento.setInicio(ahora.plusHours(13));
        ReservaEvento reserva = reservaConfirmadaDelCliente();
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(reservaEventoRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(eventoRepository.findById(evento.getId())).thenReturn(Optional.of(evento));
        when(reservaEventoRepository.saveAndFlush(reserva)).thenReturn(reserva);

        // Act
        CancelarReservaEventoResponse resultado =
                eventoService.cancelarReservaEvento(email, evento.getId(), reserva.getId());

        // Assert
        assertAll(() -> assertEquals(reserva.getId(), resultado.getId()),
                () -> assertEquals(evento.getId(), resultado.getEventoId()),
                () -> assertEquals(cliente.getId(), resultado.getClienteId()),
                () -> assertEquals("cancelada", resultado.getEstado()),
                // Deja de contar como confirmada: el cupo queda libre
                () -> assertEquals(EstadoReserva.cancelada, reserva.getEstado()));
        verify(reservaEventoRepository).saveAndFlush(reserva);
    }

    @Test @DisplayName("HU22 - Exactamente 12 horas antes todavía se puede cancelar")
    void cancelarReservaEvento_exactamente12Horas_cancelaReserva() {
        // Arrange
        fijarReloj();
        evento.setInicio(ahora.plusHours(12));
        ReservaEvento reserva = reservaConfirmadaDelCliente();
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(reservaEventoRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(eventoRepository.findById(evento.getId())).thenReturn(Optional.of(evento));
        when(reservaEventoRepository.saveAndFlush(reserva)).thenReturn(reserva);

        // Act
        CancelarReservaEventoResponse resultado =
                eventoService.cancelarReservaEvento(email, evento.getId(), reserva.getId());

        // Assert
        assertEquals("cancelada", resultado.getEstado());
    }

    @Test @DisplayName("HU22 Escenario 2 - Rechaza la cancelación con menos de 12 horas")
    void cancelarReservaEvento_conMenosDe12Horas_lanzaExcepcion() {
        // Arrange
        fijarReloj();
        evento.setInicio(ahora.plusHours(11).plusMinutes(59));
        ReservaEvento reserva = reservaConfirmadaDelCliente();
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(reservaEventoRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(eventoRepository.findById(evento.getId())).thenReturn(Optional.of(evento));

        // Act
        PlazoCancelacionException error = assertThrows(PlazoCancelacionException.class,
                () -> eventoService.cancelarReservaEvento(email, evento.getId(), reserva.getId()));

        // Assert
        assertTrue(error.getMessage().contains("menos de 12 horas"));
        assertEquals(EstadoReserva.confirmada, reserva.getEstado());
        verify(reservaEventoRepository, never()).saveAndFlush(any());
    }

    @Test @DisplayName("HU22 - Rechaza cancelar la reserva de otro cliente con mensaje genérico")
    void cancelarReservaEvento_reservaAjena_lanzaExcepcion() {
        // Arrange
        ReservaEvento reserva = reservaConfirmadaDelCliente();
        reserva.setClienteId(UUID.randomUUID());
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(reservaEventoRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));

        // Act
        EventoNoEncontradoException error = assertThrows(EventoNoEncontradoException.class,
                () -> eventoService.cancelarReservaEvento(email, evento.getId(), reserva.getId()));

        // Assert
        assertEquals("La reserva no existe", error.getMessage());
        assertEquals(EstadoReserva.confirmada, reserva.getEstado());
        verify(reservaEventoRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventoRepository);
    }

    @Test @DisplayName("HU22 - Rechaza una reserva que pertenece a otro evento con el mismo mensaje")
    void cancelarReservaEvento_reservaDeOtroEvento_lanzaExcepcion() {
        // Arrange
        ReservaEvento reserva = reservaConfirmadaDelCliente();
        UUID otroEventoId = UUID.randomUUID();
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(reservaEventoRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));

        // Act
        EventoNoEncontradoException error = assertThrows(EventoNoEncontradoException.class,
                () -> eventoService.cancelarReservaEvento(email, otroEventoId, reserva.getId()));

        // Assert
        assertEquals("La reserva no existe", error.getMessage());
        verify(reservaEventoRepository, never()).saveAndFlush(any());
    }

    @Test @DisplayName("HU22 - Rechaza una reserva inexistente con el mismo mensaje")
    void cancelarReservaEvento_reservaInexistente_lanzaExcepcion() {
        // Arrange
        UUID reservaId = UUID.randomUUID();
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(reservaEventoRepository.findById(reservaId)).thenReturn(Optional.empty());

        // Act
        EventoNoEncontradoException error = assertThrows(EventoNoEncontradoException.class,
                () -> eventoService.cancelarReservaEvento(email, evento.getId(), reservaId));

        // Assert
        assertEquals("La reserva no existe", error.getMessage());
    }

    @Test @DisplayName("HU22 - Rechaza cancelar una reserva que no está confirmada")
    void cancelarReservaEvento_reservaNoConfirmada_lanzaExcepcion() {
        // Arrange
        ReservaEvento reserva = reservaConfirmadaDelCliente();
        reserva.setEstado(EstadoReserva.cancelada);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(reservaEventoRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));

        // Act
        ConflictoDisponibilidadException error = assertThrows(ConflictoDisponibilidadException.class,
                () -> eventoService.cancelarReservaEvento(email, evento.getId(), reserva.getId()));

        // Assert
        assertTrue(error.getMessage().contains("no está confirmada"));
        verify(reservaEventoRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventoRepository);
    }

    @Test @DisplayName("HU22 - Rechaza la cancelación de un cliente inactivo")
    void cancelarReservaEvento_clienteInactivo_lanzaExcepcion() {
        // Arrange
        cliente.setActivo(false);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));

        // Act
        assertThrows(UsuarioInactivoException.class,
                () -> eventoService.cancelarReservaEvento(email, evento.getId(), UUID.randomUUID()));

        // Assert
        verifyNoInteractions(eventoRepository, reservaEventoRepository);
    }
}
