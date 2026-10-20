package Fabrica_EAP05.Reservas.Service;

import Fabrica_EAP05.Reservas.DTO.AgendaResponse;
import Fabrica_EAP05.Reservas.Entities.ReservaServicio;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Enums.EstadoReserva;
import Fabrica_EAP05.Reservas.Repository.ReservaServicioRepository;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {
    @Mock private ReservaServicioRepository reservaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private AgendaService agendaService;

    private String email;
    private Usuario proveedor;
    private ReservaServicio reserva;

    @BeforeEach
    void setUp() {
        email = "proveedor@correo.com";
        proveedor = new Usuario();
        proveedor.setId(UUID.randomUUID());
        reserva = new ReservaServicio();
        reserva.setId(UUID.randomUUID());
        reserva.setClienteId(UUID.randomUUID());
        reserva.setServicioId(UUID.randomUUID());
        reserva.setHorarioId(UUID.randomUUID());
        reserva.setEstado(EstadoReserva.confirmada);
    }

    @Test @DisplayName("Consulta la agenda del proveedor")
    void consultarAgenda_proveedorConReserva_retornaReserva() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(proveedor));
        when(reservaRepository.findByProveedorId(proveedor.getId())).thenReturn(List.of(reserva));

        // Act
        List<AgendaResponse> resultado = agendaService.consultarAgendaProveedor(email);

        // Assert
        assertAll(() -> assertEquals(1, resultado.size()),
                () -> assertEquals(reserva.getId(), resultado.get(0).getId()),
                () -> assertEquals("confirmada", resultado.get(0).getEstado()));
    }

    @Test @DisplayName("Retorna agenda vacía cuando no hay reservas")
    void consultarAgenda_sinReservas_retornaListaVacia() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(proveedor));
        when(reservaRepository.findByProveedorId(proveedor.getId())).thenReturn(List.of());

        // Act
        List<AgendaResponse> resultado = agendaService.consultarAgendaProveedor(email);

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test @DisplayName("Usa estado confirmado por defecto cuando la reserva no tiene estado")
    void consultarAgenda_reservaSinEstado_retornaConfirmada() {
        // Arrange
        reserva.setEstado(null);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(proveedor));
        when(reservaRepository.findByProveedorId(proveedor.getId())).thenReturn(List.of(reserva));

        // Act
        List<AgendaResponse> resultado = agendaService.consultarAgendaProveedor(email);

        // Assert
        assertEquals("confirmada", resultado.get(0).getEstado());
    }

    @Test @DisplayName("Rechaza un proveedor inexistente")
    void consultarAgenda_proveedorInexistente_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> agendaService.consultarAgendaProveedor(email));

        // Assert
        assertEquals("Proveedor no encontrado", error.getMessage());
        verifyNoInteractions(reservaRepository);
    }
}
