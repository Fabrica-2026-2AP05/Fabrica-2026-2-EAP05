package Fabrica_EAP05.Reservas.Service;

import Fabrica_EAP05.Reservas.DTO.DisponibilidadRecursoResponse;
import Fabrica_EAP05.Reservas.DTO.RecursoDisponibilidadResponse;
import Fabrica_EAP05.Reservas.DTO.RegistrarRecursoRequest;
import Fabrica_EAP05.Reservas.DTO.RegistroRecursoResponse;
import Fabrica_EAP05.Reservas.Entities.Recurso;
import Fabrica_EAP05.Reservas.Exception.ConflictoDisponibilidadException;
import Fabrica_EAP05.Reservas.Exception.RecursoComprometidoException;
import Fabrica_EAP05.Reservas.Exception.RecursoDuplicadoException;
import Fabrica_EAP05.Reservas.Exception.RecursoNoEncontradoException;
import Fabrica_EAP05.Reservas.Repository.DisponibilidadRecursoRepository;
import Fabrica_EAP05.Reservas.Repository.RecursoRepository;
import Fabrica_EAP05.Reservas.Repository.ReservaServicioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecursoServiceTest {
    @Mock private RecursoRepository recursoRepository;
    @Mock private ReservaServicioRepository reservaRepository;
    @Mock private DisponibilidadRecursoRepository disponibilidadRepository;
    @Mock private EntityManager entityManager;
    @InjectMocks private RecursoService recursoService;

    private UUID recursoId;
    private Recurso recurso;
    private RegistrarRecursoRequest request;
    private OffsetDateTime inicio;
    private OffsetDateTime fin;

    @BeforeEach
    void setUp() {
        recursoId = UUID.randomUUID();
        recurso = new Recurso();
        recurso.setId(recursoId);
        recurso.setNombre("Sala 1");
        recurso.setDescripcion("Sala principal");
        recurso.setActivo(true);
        request = new RegistrarRecursoRequest();
        request.setNombre("Sala 1");
        request.setDescripcion("Sala principal");
        inicio = OffsetDateTime.parse("2026-11-01T08:00:00-05:00");
        fin = OffsetDateTime.parse("2026-11-01T12:00:00-05:00");
    }

    @Test @DisplayName("Registra un recurso nuevo")
    void registrarRecurso_nombreNuevo_guardaYRetornaRecurso() {
        // Arrange
        when(recursoRepository.existsByNombre(request.getNombre())).thenReturn(false);
        when(recursoRepository.saveAndFlush(any(Recurso.class))).thenAnswer(i -> i.getArgument(0));
        when(recursoRepository.findById(any(UUID.class))).thenReturn(Optional.of(recurso));

        // Act
        RegistroRecursoResponse resultado = recursoService.registrarRecurso(request);

        // Assert
        assertAll(() -> assertEquals(recursoId, resultado.getId()),
                () -> assertEquals("Sala 1", resultado.getNombre()),
                () -> assertTrue(resultado.isActivo()));
        verify(entityManager).detach(any(Recurso.class));
    }

    @Test @DisplayName("Rechaza un nombre de recurso duplicado")
    void registrarRecurso_nombreDuplicado_lanzaExcepcion() {
        // Arrange
        when(recursoRepository.existsByNombre(request.getNombre())).thenReturn(true);

        // Act
        RecursoDuplicadoException error = assertThrows(RecursoDuplicadoException.class,
                () -> recursoService.registrarRecurso(request));

        // Assert
        assertTrue(error.getMessage().contains("ya está registrado"));
        verify(recursoRepository, never()).saveAndFlush(any());
    }

    @Test @DisplayName("Desactiva un recurso sin reservas pendientes")
    void cambiarDisponibilidad_sinReservas_desactivaRecurso() {
        // Arrange
        when(recursoRepository.findByIdParaActualizar(recursoId)).thenReturn(Optional.of(recurso));
        when(reservaRepository.contarReservasVigentesDeRecurso(eq(recursoId), any())).thenReturn(0L);

        // Act
        RecursoDisponibilidadResponse resultado = recursoService.cambiarDisponibilidad(recursoId, false);

        // Assert
        assertFalse(resultado.getActivo());
        verify(recursoRepository).save(recurso);
    }

    @Test @DisplayName("Activa un recurso sin consultar reservas pendientes")
    void cambiarDisponibilidad_activarRecurso_guardaDirectamente() {
        // Arrange
        recurso.setActivo(false);
        when(recursoRepository.findByIdParaActualizar(recursoId)).thenReturn(Optional.of(recurso));

        // Act
        RecursoDisponibilidadResponse resultado = recursoService.cambiarDisponibilidad(recursoId, true);

        // Assert
        assertTrue(resultado.getActivo());
        verifyNoInteractions(reservaRepository);
        verify(recursoRepository).save(recurso);
    }

    @Test @DisplayName("Impide desactivar un recurso comprometido")
    void cambiarDisponibilidad_conReservaPendiente_lanzaExcepcion() {
        // Arrange
        when(recursoRepository.findByIdParaActualizar(recursoId)).thenReturn(Optional.of(recurso));
        when(reservaRepository.contarReservasVigentesDeRecurso(eq(recursoId), any())).thenReturn(2L);

        // Act
        RecursoComprometidoException error = assertThrows(RecursoComprometidoException.class,
                () -> recursoService.cambiarDisponibilidad(recursoId, false));

        // Assert
        assertTrue(error.getMessage().contains("2 reserva"));
        verify(recursoRepository, never()).save(any());
    }

    @Test @DisplayName("Rechaza el cambio de un recurso inexistente")
    void cambiarDisponibilidad_recursoInexistente_lanzaExcepcion() {
        // Arrange
        when(recursoRepository.findByIdParaActualizar(recursoId)).thenReturn(Optional.empty());

        // Act
        assertThrows(RecursoNoEncontradoException.class,
                () -> recursoService.cambiarDisponibilidad(recursoId, false));

        // Assert
        verifyNoInteractions(reservaRepository);
    }

    @Test @DisplayName("Define un periodo de disponibilidad")
    void definirDisponibilidad_periodoLibre_guardaPeriodo() {
        // Arrange
        when(recursoRepository.findByIdParaActualizar(recursoId)).thenReturn(Optional.of(recurso));
        when(disponibilidadRepository.contarSolapadas(eq(recursoId), any(), any())).thenReturn(0L);

        // Act
        DisponibilidadRecursoResponse resultado = recursoService.definirDisponibilidad(recursoId, inicio, fin);

        // Assert
        assertAll(() -> assertEquals(recursoId, resultado.getRecursoId()),
                () -> assertEquals(inicio, resultado.getInicio()),
                () -> assertEquals(fin, resultado.getFin()));
        verify(disponibilidadRepository).saveAndFlush(any());
    }

    @Test @DisplayName("Impide periodos de disponibilidad solapados")
    void definirDisponibilidad_periodoSolapado_lanzaExcepcion() {
        // Arrange
        when(recursoRepository.findByIdParaActualizar(recursoId)).thenReturn(Optional.of(recurso));
        when(disponibilidadRepository.contarSolapadas(eq(recursoId), any(), any())).thenReturn(1L);

        // Act
        ConflictoDisponibilidadException error = assertThrows(ConflictoDisponibilidadException.class,
                () -> recursoService.definirDisponibilidad(recursoId, inicio, fin));

        // Assert
        assertTrue(error.getMessage().contains("se cruza"));
        verify(disponibilidadRepository, never()).saveAndFlush(any());
    }

    @Test @DisplayName("Impide definir disponibilidad para un recurso inactivo")
    void definirDisponibilidad_recursoInactivo_lanzaExcepcion() {
        // Arrange
        recurso.setActivo(false);
        when(recursoRepository.findByIdParaActualizar(recursoId)).thenReturn(Optional.of(recurso));

        // Act
        ConflictoDisponibilidadException error = assertThrows(ConflictoDisponibilidadException.class,
                () -> recursoService.definirDisponibilidad(recursoId, inicio, fin));

        // Assert
        assertTrue(error.getMessage().contains("no está activo"));
        verifyNoInteractions(disponibilidadRepository);
    }
}
