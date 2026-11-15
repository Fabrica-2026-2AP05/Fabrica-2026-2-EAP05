package Fabrica_EAP05.Reservas.Service;

import Fabrica_EAP05.Reservas.DTO.ReservaEventoResponse;
import Fabrica_EAP05.Reservas.Entities.Evento;
import Fabrica_EAP05.Reservas.Entities.ReservaEvento;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Enums.EstadoReserva;
import Fabrica_EAP05.Reservas.Exception.ConflictoDisponibilidadException;
import Fabrica_EAP05.Reservas.Exception.CupoAgotadoException;
import Fabrica_EAP05.Reservas.Exception.EventoNoEncontradoException;
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
}
