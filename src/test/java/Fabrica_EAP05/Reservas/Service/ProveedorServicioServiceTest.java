package Fabrica_EAP05.Reservas.Service;

import Fabrica_EAP05.Reservas.DTO.AsignarProveedorResponse;
import Fabrica_EAP05.Reservas.Entities.ProveedorServicio;
import Fabrica_EAP05.Reservas.Entities.Servicio;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Repository.ProveedorServicioRepository;
import Fabrica_EAP05.Reservas.Repository.ServicioRepository;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProveedorServicioServiceTest {
    @Mock private ProveedorServicioRepository asignacionRepository;
    @Mock private ServicioRepository servicioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private ProveedorServicioService service;

    private UUID servicioId;
    private UUID proveedorId;
    private Servicio servicio;
    private Usuario proveedor;

    @BeforeEach
    void setUp() {
        servicioId = UUID.randomUUID();
        proveedorId = UUID.randomUUID();
        servicio = new Servicio();
        servicio.setId(servicioId);
        servicio.setNombre("Masaje");
        proveedor = new Usuario();
        proveedor.setId(proveedorId);
        proveedor.setNombre("Ana");
    }

    @Test @DisplayName("Asigna un proveedor a un servicio")
    void asignar_datosValidos_guardaYRetornaAsignacion() {
        // Arrange
        ProveedorServicio guardada = new ProveedorServicio(proveedor, servicio);
        guardada.setId(UUID.randomUUID());
        when(servicioRepository.findById(servicioId)).thenReturn(Optional.of(servicio));
        when(usuarioRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));
        when(asignacionRepository.existsByProveedorIdAndServicioId(proveedorId, servicioId)).thenReturn(false);
        when(asignacionRepository.save(any(ProveedorServicio.class))).thenReturn(guardada);

        // Act
        AsignarProveedorResponse resultado = service.asignarProveedorAServicio(servicioId, proveedorId);

        // Assert
        assertAll(() -> assertEquals(servicioId, resultado.getServicioId()),
                () -> assertEquals(proveedorId, resultado.getProveedorId()),
                () -> assertTrue(resultado.getMensaje().contains("exitosamente")));
        verify(asignacionRepository).save(any(ProveedorServicio.class));
    }

    @Test @DisplayName("Rechaza un servicio inexistente")
    void asignar_servicioInexistente_lanzaExcepcion() {
        // Arrange
        when(servicioRepository.findById(servicioId)).thenReturn(Optional.empty());

        // Act
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.asignarProveedorAServicio(servicioId, proveedorId));

        // Assert
        assertTrue(error.getMessage().contains("servicio"));
        verifyNoInteractions(usuarioRepository, asignacionRepository);
    }

    @Test @DisplayName("Rechaza un proveedor inexistente")
    void asignar_proveedorInexistente_lanzaExcepcion() {
        // Arrange
        when(servicioRepository.findById(servicioId)).thenReturn(Optional.of(servicio));
        when(usuarioRepository.findById(proveedorId)).thenReturn(Optional.empty());

        // Act
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.asignarProveedorAServicio(servicioId, proveedorId));

        // Assert
        assertTrue(error.getMessage().contains("proveedor"));
        verifyNoInteractions(asignacionRepository);
    }

    @Test @DisplayName("Impide una asignación duplicada")
    void asignar_asignacionDuplicada_lanzaExcepcion() {
        // Arrange
        when(servicioRepository.findById(servicioId)).thenReturn(Optional.of(servicio));
        when(usuarioRepository.findById(proveedorId)).thenReturn(Optional.of(proveedor));
        when(asignacionRepository.existsByProveedorIdAndServicioId(proveedorId, servicioId)).thenReturn(true);

        // Act
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.asignarProveedorAServicio(servicioId, proveedorId));

        // Assert
        assertTrue(error.getMessage().contains("ya se encuentra asignado"));
        verify(asignacionRepository, never()).save(any());
    }
}
