package Fabrica_EAP05.Reservas.Service;

import Fabrica_EAP05.Reservas.DTO.ConteoReservasServicio;
import Fabrica_EAP05.Reservas.DTO.ModificarServicioRequest;
import Fabrica_EAP05.Reservas.DTO.ModificarServicioResponse;
import Fabrica_EAP05.Reservas.DTO.RegistrarServicioRequest;
import Fabrica_EAP05.Reservas.DTO.RegistrarServicioResponse;
import Fabrica_EAP05.Reservas.DTO.UsoServicioResponse;
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

import java.time.LocalDateTime;
import java.util.List;
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

    // ---------- HU27: uso de servicios ----------

    private final LocalDateTime desde = LocalDateTime.of(2026, 11, 1, 0, 0);
    private final LocalDateTime hasta = LocalDateTime.of(2026, 12, 1, 0, 0);

    private Servicio servicioLlamado(String nombre) {
        Servicio s = new Servicio();
        s.setId(UUID.randomUUID());
        s.setNombre(nombre);
        return s;
    }

    @Test @DisplayName("HU27 Escenario 1 - Cantidad de reservas por servicio, de más a menos usado")
    void reporteUso_conReservas_retornaConteosOrdenados() {
        // Arrange
        Servicio corte = servicioLlamado("Corte");
        Servicio masaje = servicioLlamado("Masaje");
        when(reservaRepository.contarReservasPorServicioEnPeriodo(desde, hasta)).thenReturn(List.of(
                new ConteoReservasServicio(corte.getId(), 2L),
                new ConteoReservasServicio(masaje.getId(), 5L)));
        when(servicioRepository.findAll()).thenReturn(List.of(corte, masaje));

        // Act
        List<UsoServicioResponse> reporte = servicioService.reporteUso(desde, hasta);

        // Assert
        assertAll(() -> assertEquals(2, reporte.size()),
                () -> assertEquals(masaje.getId(), reporte.get(0).getServicioId()),
                () -> assertEquals("Masaje", reporte.get(0).getNombre()),
                () -> assertEquals(5L, reporte.get(0).getReservas()),
                () -> assertEquals(corte.getId(), reporte.get(1).getServicioId()),
                () -> assertEquals(2L, reporte.get(1).getReservas()));
    }

    @Test @DisplayName("HU27 - Un servicio sin reservas en el periodo aparece con 0")
    void reporteUso_servicioSinReservas_apareceEnCero() {
        // Arrange
        Servicio corte = servicioLlamado("Corte");
        Servicio uñas = servicioLlamado("Uñas");
        when(reservaRepository.contarReservasPorServicioEnPeriodo(desde, hasta)).thenReturn(List.of(
                new ConteoReservasServicio(corte.getId(), 3L)));
        when(servicioRepository.findAll()).thenReturn(List.of(uñas, corte));

        // Act
        List<UsoServicioResponse> reporte = servicioService.reporteUso(desde, hasta);

        // Assert
        assertAll(() -> assertEquals(2, reporte.size()),
                () -> assertEquals(corte.getId(), reporte.get(0).getServicioId()),
                () -> assertEquals(3L, reporte.get(0).getReservas()),
                () -> assertEquals(uñas.getId(), reporte.get(1).getServicioId()),
                () -> assertEquals(0L, reporte.get(1).getReservas()));
    }

    @Test @DisplayName("HU27 Escenario 2 - Sin reservas en el periodo: lista vacía")
    void reporteUso_sinReservas_retornaListaVacia() {
        // Arrange
        when(reservaRepository.contarReservasPorServicioEnPeriodo(desde, hasta)).thenReturn(List.of());

        // Act
        List<UsoServicioResponse> reporte = servicioService.reporteUso(desde, hasta);

        // Assert
        assertTrue(reporte.isEmpty());
        verify(servicioRepository, never()).findAll();
    }

    @Test @DisplayName("HU27 - Rechaza un periodo donde 'desde' no es anterior a 'hasta'")
    void reporteUso_desdeNoAnteriorAHasta_lanzaExcepcion() {
        // Act
        IllegalArgumentException igual = assertThrows(IllegalArgumentException.class,
                () -> servicioService.reporteUso(desde, desde));
        IllegalArgumentException invertido = assertThrows(IllegalArgumentException.class,
                () -> servicioService.reporteUso(hasta, desde));

        // Assert
        assertAll(() -> assertEquals("La fecha 'desde' debe ser anterior a la fecha 'hasta'", igual.getMessage()),
                () -> assertEquals("La fecha 'desde' debe ser anterior a la fecha 'hasta'", invertido.getMessage()));
        verifyNoInteractions(reservaRepository, servicioRepository);
    }

    @Test @DisplayName("HU27 - Rechaza fechas nulas")
    void reporteUso_fechasNulas_lanzaExcepcion() {
        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> servicioService.reporteUso(null, hasta));
        assertThrows(IllegalArgumentException.class, () -> servicioService.reporteUso(desde, null));
        verifyNoInteractions(reservaRepository, servicioRepository);
    }
}
