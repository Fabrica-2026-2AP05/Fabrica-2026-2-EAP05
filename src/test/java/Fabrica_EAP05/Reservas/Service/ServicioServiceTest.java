package Fabrica_EAP05.Reservas.Service;

import Fabrica_EAP05.Reservas.DTO.ModificarServicioRequest;
import Fabrica_EAP05.Reservas.DTO.ModificarServicioResponse;
import Fabrica_EAP05.Reservas.DTO.RegistrarServicioRequest;
import Fabrica_EAP05.Reservas.DTO.RegistrarServicioResponse;
import Fabrica_EAP05.Reservas.Entities.Recurso;
import Fabrica_EAP05.Reservas.Entities.Servicio;
import Fabrica_EAP05.Reservas.Exception.RecursoDuplicadoException;
import Fabrica_EAP05.Reservas.Repository.RecursoRepository;
import Fabrica_EAP05.Reservas.Repository.ReservaServicioRepository;
import Fabrica_EAP05.Reservas.Repository.ServicioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioServiceTest {
    @Mock private ServicioRepository servicioRepository;
    @Mock private RecursoRepository recursoRepository;
    @Mock private ReservaServicioRepository reservaRepository;
    @Mock private EntityManager entityManager;
    @InjectMocks private ServicioService servicioService;

    private UUID servicioId;
    private UUID recursoId;
    private Servicio servicio;
    private Recurso recurso;
    private RegistrarServicioRequest registrarRequest;
    private ModificarServicioRequest modificarRequest;

    @BeforeEach
    void setUp() {
        servicioId = UUID.randomUUID();
        recursoId = UUID.randomUUID();
        recurso = new Recurso();
        recurso.setId(recursoId);
        recurso.setNombre("Sala 1");
        recurso.setDescripcion("Sala principal");
        recurso.setActivo(true);
        servicio = new Servicio(servicioId, "Masaje", "Masaje relajante", 60, recurso, true);
        registrarRequest = new RegistrarServicioRequest();
        registrarRequest.setNombre("Masaje");
        registrarRequest.setDescripcion("Masaje relajante");
        registrarRequest.setDuracionMin(60);
        registrarRequest.setRecursoId(recursoId);
        modificarRequest = new ModificarServicioRequest();
        modificarRequest.setNombre("Masaje premium");
        modificarRequest.setDescripcion("Servicio actualizado");
        modificarRequest.setDuracionMin(90);
    }

    @Test @DisplayName("Registra un servicio con recurso activo")
    void registrarServicio_datosValidos_guardaYRetornaServicio() {
        // Arrange
        when(servicioRepository.existsByNombre(registrarRequest.getNombre())).thenReturn(false);
        when(recursoRepository.findById(recursoId)).thenReturn(Optional.of(recurso));
        when(servicioRepository.saveAndFlush(any(Servicio.class))).thenAnswer(i -> i.getArgument(0));
        when(servicioRepository.findById(any(UUID.class))).thenReturn(Optional.of(servicio));

        // Act
        RegistrarServicioResponse resultado = servicioService.registrarServicio(registrarRequest);

        // Assert
        assertAll(() -> assertEquals(servicioId, resultado.getId()),
                () -> assertEquals("Masaje", resultado.getNombre()),
                () -> assertEquals(recursoId, resultado.getRecursoId()));
        verify(entityManager).detach(any(Servicio.class));
    }

    @Test @DisplayName("Rechaza un servicio duplicado")
    void registrarServicio_nombreDuplicado_lanzaExcepcion() {
        // Arrange
        when(servicioRepository.existsByNombre(registrarRequest.getNombre())).thenReturn(true);

        // Act
        assertThrows(RecursoDuplicadoException.class,
                () -> servicioService.registrarServicio(registrarRequest));

        // Assert
        verify(servicioRepository, never()).saveAndFlush(any());
    }

    @Test @DisplayName("Rechaza un servicio sin nombre")
    void registrarServicio_nombreVacio_lanzaExcepcion() {
        // Arrange
        registrarRequest.setNombre(" ");
        when(servicioRepository.existsByNombre(" ")).thenReturn(false);

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicioService.registrarServicio(registrarRequest));

        // Assert
        assertTrue(error.getMessage().contains("obligatorio"));
        verifyNoInteractions(recursoRepository);
    }

    @Test @DisplayName("Rechaza un recurso inexistente")
    void validarRecurso_recursoInexistente_lanzaExcepcion() {
        // Arrange
        when(recursoRepository.findById(recursoId)).thenReturn(Optional.empty());

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicioService.validarRecursoRequerido(recursoId));

        // Assert
        assertTrue(error.getMessage().contains("no existe"));
    }

    @Test @DisplayName("Permite que un servicio no requiera recurso")
    void validarRecurso_recursoNulo_retornaTrue() {
        // Arrange: el servicio no requiere un recurso.

        // Act
        boolean resultado = servicioService.validarRecursoRequerido(null);

        // Assert
        assertTrue(resultado);
        verifyNoInteractions(recursoRepository);
    }

    @Test @DisplayName("Rechaza un recurso inactivo")
    void validarRecurso_recursoInactivo_lanzaExcepcion() {
        // Arrange
        recurso.setActivo(false);
        when(recursoRepository.findById(recursoId)).thenReturn(Optional.of(recurso));

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicioService.validarRecursoRequerido(recursoId));

        // Assert
        assertTrue(error.getMessage().contains("no está activo"));
    }

    @Test @DisplayName("Modifica los campos enviados de un servicio")
    void modificarServicio_sinReservas_actualizaCampos() {
        // Arrange
        Servicio actualizado = new Servicio(servicioId, "Masaje premium", "Servicio actualizado", 90, recurso, true);
        when(servicioRepository.findById(servicioId)).thenReturn(Optional.of(servicio), Optional.of(actualizado));
        when(reservaRepository.countByServicioId(servicioId)).thenReturn(0L);
        when(servicioRepository.existsByNombre(modificarRequest.getNombre())).thenReturn(false);
        when(servicioRepository.saveAndFlush(servicio)).thenReturn(servicio);

        // Act
        ModificarServicioResponse resultado = servicioService.modificarServicio(servicioId, modificarRequest);

        // Assert
        assertAll(() -> assertEquals("Masaje premium", resultado.getNombre()),
                () -> assertEquals(90, resultado.getDuracionMin()),
                () -> assertEquals("Servicio actualizado", resultado.getDescripcion()));
    }

    @Test @DisplayName("Impide modificar un servicio con reservas")
    void modificarServicio_conReservas_lanzaExcepcion() {
        // Arrange
        when(servicioRepository.findById(servicioId)).thenReturn(Optional.of(servicio));
        when(reservaRepository.countByServicioId(servicioId)).thenReturn(2L);

        // Act
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> servicioService.modificarServicio(servicioId, modificarRequest));

        // Assert
        assertTrue(error.getMessage().contains("2 reserva"));
        verify(servicioRepository, never()).saveAndFlush(any());
    }

    @Test @DisplayName("Rechaza una duración no positiva")
    void modificarServicio_duracionCero_lanzaExcepcion() {
        // Arrange
        modificarRequest.setNombre(null);
        modificarRequest.setDuracionMin(0);
        when(servicioRepository.findById(servicioId)).thenReturn(Optional.of(servicio));
        when(reservaRepository.countByServicioId(servicioId)).thenReturn(0L);

        // Act
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> servicioService.modificarServicio(servicioId, modificarRequest));

        // Assert
        assertTrue(error.getMessage().contains("mayor a 0"));
    }

    @Test @DisplayName("Rechaza modificar un servicio inexistente")
    void modificarServicio_inexistente_lanzaExcepcion() {
        // Arrange
        when(servicioRepository.findById(servicioId)).thenReturn(Optional.empty());

        // Act
        assertThrows(NoSuchElementException.class,
                () -> servicioService.modificarServicio(servicioId, modificarRequest));

        // Assert
        verifyNoInteractions(reservaRepository);
    }
}
