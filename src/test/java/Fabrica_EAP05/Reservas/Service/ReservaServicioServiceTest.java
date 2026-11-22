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
import Fabrica_EAP05.Reservas.Entities.Evento;
import Fabrica_EAP05.Reservas.Entities.ReservaEvento;
import Fabrica_EAP05.Reservas.Enums.EstadoHorario;
import Fabrica_EAP05.Reservas.Enums.EstadoReserva;
import Fabrica_EAP05.Reservas.Exception.UsuarioInactivoException;
import Fabrica_EAP05.Reservas.Repository.DisponibilidadRecursoRepository;
import Fabrica_EAP05.Reservas.Repository.HorarioRepository;
import Fabrica_EAP05.Reservas.Repository.ReservaServicioRepository;
import Fabrica_EAP05.Reservas.Repository.ServicioRepository;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;
import Fabrica_EAP05.Reservas.Repository.RecursoRepository;
import Fabrica_EAP05.Reservas.Repository.ReservaEventoRepository;
import Fabrica_EAP05.Reservas.Repository.EventoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NoSuchElementException;
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
    @Mock private RecursoRepository recursoRepository;
    @Mock private ReservaEventoRepository reservaEventoRepository;
    @Mock private EventoRepository eventoRepository;
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

    @Test @DisplayName("Rechaza una solicitud sin horario")
    void crearReserva_sinHorario_lanzaExcepcion() {
        request.setHorarioId(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> reservaService.crearReserva(email, request));

        assertEquals("El horario es obligatorio", error.getMessage());
        verifyNoInteractions(usuarioRepository, servicioRepository, horarioRepository);
    }

    @Test @DisplayName("Rechaza crear una reserva para un usuario inexistente")
    void crearReserva_usuarioInexistente_lanzaExcepcion() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> reservaService.crearReserva(email, request));

        verifyNoInteractions(servicioRepository, horarioRepository);
    }

    @Test @DisplayName("Rechaza crear una reserva para un servicio inactivo")
    void crearReserva_servicioInactivo_lanzaExcepcion() {
        servicio.setActivo(false);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(servicioRepository.findById(servicio.getId())).thenReturn(Optional.of(servicio));

        assertThrows(UsuarioInactivoException.class,
                () -> reservaService.crearReserva(email, request));

        verifyNoInteractions(horarioRepository);
    }

    @Test @DisplayName("Rechaza crear una reserva para un horario inexistente")
    void crearReserva_horarioInexistente_lanzaExcepcion() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(servicioRepository.findById(servicio.getId())).thenReturn(Optional.of(servicio));
        when(horarioRepository.findByIdParaActualizar(horario.getId())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> reservaService.crearReserva(email, request));
    }

    @Test @DisplayName("Rechaza un recurso ocupado aunque su periodo tenga cobertura")
    void crearReserva_recursoOcupado_lanzaExcepcion() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(servicioRepository.findById(servicio.getId())).thenReturn(Optional.of(servicio));
        when(horarioRepository.findByIdParaActualizar(horario.getId())).thenReturn(Optional.of(horario));
        when(disponibilidadRepository.findByRecursoIdAndInicioLessThanEqualAndFinGreaterThanEqual(
                recurso.getId(), horario.getInicio(), horario.getFin()))
                .thenReturn(List.of(new DisponibilidadRecurso()));
        when(reservaRepository.contarReservasDeRecursoEnRango(
                recurso.getId(), horario.getInicio(), horario.getFin())).thenReturn(1L);

        assertThrows(IllegalStateException.class,
                () -> reservaService.crearReserva(email, request));

        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test @DisplayName("Informa un error si la reserva guardada no puede recargarse")
    void crearReserva_reservaNoRecuperable_lanzaExcepcion() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(servicioRepository.findById(servicio.getId())).thenReturn(Optional.of(servicio));
        when(horarioRepository.findByIdParaActualizar(horario.getId())).thenReturn(Optional.of(horario));
        when(disponibilidadRepository.findByRecursoIdAndInicioLessThanEqualAndFinGreaterThanEqual(
                recurso.getId(), horario.getInicio(), horario.getFin()))
                .thenReturn(List.of(new DisponibilidadRecurso()));
        when(reservaRepository.contarReservasDeRecursoEnRango(any(), any(), any())).thenReturn(0L);
        when(reservaRepository.saveAndFlush(any(ReservaServicio.class))).thenAnswer(i -> i.getArgument(0));
        when(reservaRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> reservaService.crearReserva(email, request));

        verify(entityManager).detach(any(ReservaServicio.class));
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

    @Test
    @DisplayName("Permite cancelar cuando faltan exactamente 12 horas para el inicio")
    void cancelarReserva_exactamenteDoceHorasAntes_permiteLaCancelacion() {
        LocalDateTime ahoraPrueba = LocalDateTime.of(2026, 10, 31, 20, 0);
        horario.setInicio(ahoraPrueba.plusHours(12));
        var ahoraPruebaInstant = ahoraPrueba.atOffset(ZoneOffset.UTC).toInstant();
        var inicioReservaInstant = horario.getInicio().atOffset(ZoneOffset.UTC).toInstant();
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(reservaRepository.findByIdParaActualizar(reservaId)).thenReturn(Optional.of(reserva));

        // Simula el contrato del trigger de BD: el límite se rechaza solo si faltan menos de 12 horas.
        when(reservaRepository.saveAndFlush(any(ReservaServicio.class))).thenAnswer(invocation -> {
            ReservaServicio actualizada = invocation.getArgument(0);
            Duration tiempoRestante = Duration.between(ahoraPruebaInstant, inicioReservaInstant);
            if (tiempoRestante.compareTo(Duration.ofHours(12)) < 0) {
                throw new IllegalStateException("faltan menos de 12 horas");
            }
            actualizada.setCanceladaEn(ahoraPrueba);
            return actualizada;
        });

        CancelarReservaResponse resultado = reservaService.cancelarReserva(email, reservaId);

        assertAll(
                () -> assertEquals(Duration.ofHours(12), Duration.between(ahoraPruebaInstant, inicioReservaInstant)),
                () -> assertEquals(reservaId, resultado.getId()),
                () -> assertEquals("cancelada", resultado.getEstado()),
                () -> assertNotNull(resultado.getCanceladaEn())
        );
        verify(reservaRepository).saveAndFlush(reserva);
        verify(entityManager).refresh(reserva);
    }

    @Test @DisplayName("Impide cancelar una reserva de otro cliente")
    void cancelarReserva_reservaAjena_lanzaExcepcion() {
        // Arrange
        reserva.setClienteId(UUID.randomUUID());
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(reservaRepository.findByIdParaActualizar(reservaId)).thenReturn(Optional.of(reserva));

        // Act
        NoSuchElementException error = assertThrows(NoSuchElementException.class,
                () -> reservaService.cancelarReserva(email, reservaId));

        // Assert
        assertEquals("La reserva no existe", error.getMessage());
        assertEquals(EstadoReserva.confirmada, reserva.getEstado());
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test @DisplayName("Rechaza cancelar una reserva inexistente con el mismo mensaje")
    void cancelarReserva_reservaInexistente_lanzaExcepcion() {
        // Arrange
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(reservaRepository.findByIdParaActualizar(reservaId)).thenReturn(Optional.empty());

        // Act
        NoSuchElementException error = assertThrows(NoSuchElementException.class,
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

    @Test @DisplayName("Rechaza cancelar cuando el usuario autenticado no existe")
    void cancelarReserva_usuarioInexistente_lanzaExcepcion() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> reservaService.cancelarReserva(email, reservaId));

        verifyNoInteractions(reservaRepository);
    }

    @Test @DisplayName("Rechaza cancelar cuando el cliente está inactivo")
    void cancelarReserva_clienteInactivo_lanzaExcepcion() {
        cliente.setActivo(false);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));

        assertThrows(UsuarioInactivoException.class,
                () -> reservaService.cancelarReserva(email, reservaId));

        verifyNoInteractions(reservaRepository);
    }

    @Test @DisplayName("Consulta horarios disponibles de un servicio sin recurso")
    void consultarDisponibilidad_servicioSinRecurso_retornaHorarios() {
        servicio.setRecurso(null);
        horario.setProveedorId(UUID.randomUUID());
        when(servicioRepository.findById(servicio.getId())).thenReturn(Optional.of(servicio));
        when(horarioRepository.findDisponiblesPorServicio(eq(servicio.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(horario));

        var resultado = reservaService.consultarDisponibilidad(servicio.getId());

        assertAll(() -> assertEquals(1, resultado.size()),
                () -> assertEquals(horario.getId(), resultado.get(0).getId()));
    }

    @Test @DisplayName("Rechaza disponibilidad de un servicio inexistente o inactivo")
    void consultarDisponibilidad_servicioInvalido_lanzaExcepcion() {
        UUID inexistente = UUID.randomUUID();
        when(servicioRepository.findById(inexistente)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class,
                () -> reservaService.consultarDisponibilidad(inexistente));

        servicio.setActivo(false);
        when(servicioRepository.findById(servicio.getId())).thenReturn(Optional.of(servicio));
        assertThrows(IllegalStateException.class,
                () -> reservaService.consultarDisponibilidad(servicio.getId()));
    }

    @Test @DisplayName("Filtra horarios cuyo recurso está ocupado")
    void consultarDisponibilidad_recursoOcupado_filtraHorario() {
        when(servicioRepository.findById(servicio.getId())).thenReturn(Optional.of(servicio));
        when(horarioRepository.findDisponiblesPorServicio(eq(servicio.getId()), any(LocalDateTime.class)))
                .thenReturn(List.of(horario));
        when(disponibilidadRepository.findByRecursoIdAndInicioLessThanEqualAndFinGreaterThanEqual(
                recurso.getId(), horario.getInicio(), horario.getFin()))
                .thenReturn(List.of(new DisponibilidadRecurso()));
        when(reservaRepository.contarReservasDeRecursoEnRango(
                recurso.getId(), horario.getInicio(), horario.getFin())).thenReturn(1L);

        assertTrue(reservaService.consultarDisponibilidad(servicio.getId()).isEmpty());
    }

    @Test @DisplayName("Construye el historial completo del cliente")
    void consultarHistorialCliente_conReserva_retornaDetalle() {
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        when(reservaRepository.findByClienteIdOrderByCreadaEnDesc(cliente.getId())).thenReturn(List.of(reserva));
        when(servicioRepository.findById(servicio.getId())).thenReturn(Optional.of(servicio));
        when(horarioRepository.findById(horario.getId())).thenReturn(Optional.of(horario));
        when(recursoRepository.findById(recurso.getId())).thenReturn(Optional.of(recurso));

        var resultado = reservaService.consultarHistorialCliente(email);

        assertAll(() -> assertEquals(1, resultado.size()),
                () -> assertEquals("Masaje", resultado.get(0).getNombreServicio()),
                () -> assertEquals("Sala 1", resultado.get(0).getNombreRecurso()));
    }

    @Test @DisplayName("Rechaza el historial de un cliente inexistente o inactivo")
    void consultarHistorialCliente_usuarioInvalido_lanzaExcepcion() {
        when(usuarioRepository.findByEmail("nadie@test.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> reservaService.consultarHistorialCliente("nadie@test.com"));

        cliente.setActivo(false);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        assertThrows(UsuarioInactivoException.class,
                () -> reservaService.consultarHistorialCliente(email));
    }

    @Test @DisplayName("Construye el historial del proveedor con datos relacionados")
    void consultarHistorialProveedor_conReserva_retornaDetalle() {
        Usuario proveedor = new Usuario();
        proveedor.setId(UUID.randomUUID());
        proveedor.setActivo(true);
        cliente.setNombre("Cliente Uno");
        when(usuarioRepository.findByEmail("proveedor@test.com")).thenReturn(Optional.of(proveedor));
        when(reservaRepository.findHistorialByProveedorId(proveedor.getId())).thenReturn(List.of(reserva));
        when(servicioRepository.findById(servicio.getId())).thenReturn(Optional.of(servicio));
        when(usuarioRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(horarioRepository.findById(horario.getId())).thenReturn(Optional.of(horario));
        when(recursoRepository.findById(recurso.getId())).thenReturn(Optional.of(recurso));

        var resultado = reservaService.consultarHistorialProveedor("proveedor@test.com");

        assertAll(() -> assertEquals(1, resultado.size()),
                () -> assertEquals("Cliente Uno", resultado.get(0).getNombreCliente()),
                () -> assertEquals("Masaje", resultado.get(0).getNombreServicio()));
    }

    @Test @DisplayName("Une reservas de servicios y eventos en el historial del negocio")
    void consultarHistorialNegocio_conServicioYEvento_retornaAmbos() {
        Usuario admin = new Usuario();
        admin.setActivo(true);
        Evento evento = new Evento();
        evento.setId(UUID.randomUUID());
        evento.setNombre("Taller");
        evento.setInicio(LocalDateTime.of(2026, 11, 2, 8, 0));
        evento.setFin(LocalDateTime.of(2026, 11, 2, 10, 0));
        ReservaEvento reservaEvento = new ReservaEvento();
        reservaEvento.setId(UUID.randomUUID());
        reservaEvento.setEventoId(evento.getId());
        reservaEvento.setClienteId(cliente.getId());
        reservaEvento.setEstado(EstadoReserva.confirmada);
        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(reservaRepository.findAll()).thenReturn(List.of(reserva));
        when(reservaEventoRepository.findAll()).thenReturn(List.of(reservaEvento));
        when(servicioRepository.findById(servicio.getId())).thenReturn(Optional.of(servicio));
        when(usuarioRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(horarioRepository.findById(horario.getId())).thenReturn(Optional.of(horario));
        when(recursoRepository.findById(recurso.getId())).thenReturn(Optional.of(recurso));
        when(eventoRepository.findById(evento.getId())).thenReturn(Optional.of(evento));

        var resultado = reservaService.consultarHistorialNegocio("admin@test.com");

        assertAll(() -> assertEquals(2, resultado.size()),
                () -> assertTrue(resultado.stream().anyMatch(r -> "SERVICIO".equals(r.getTipo()))),
                () -> assertTrue(resultado.stream().anyMatch(r -> "EVENTO".equals(r.getTipo()))));
    }

    @Test @DisplayName("Calcula ocupación y omite servicios sin actividad")
    void consultarOcupacionServicios_conActividad_calculaPorcentaje() {
        Usuario admin = new Usuario();
        admin.setActivo(true);
        Servicio sinActividad = new Servicio(UUID.randomUUID(), "Vacío", "", 30, null, true);
        LocalDateTime desde = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 12, 1, 0, 0);
        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(servicioRepository.findAll()).thenReturn(List.of(servicio, sinActividad));
        when(horarioRepository.contarHorariosOfertadosPorServicioEnPeriodo(
                servicio.getId(), EstadoHorario.bloqueado, desde, hasta)).thenReturn(4L);
        when(reservaRepository.contarReservasConfirmadasPorServicioEnPeriodo(
                servicio.getId(), EstadoReserva.confirmada, desde, hasta)).thenReturn(3L);

        var resultado = reservaService.consultarOcupacionServicios("admin@test.com", desde, hasta);

        assertAll(() -> assertEquals(1, resultado.size()),
                () -> assertEquals(75.0, resultado.get(0).getPorcentajeOcupacion()));
    }

    @Test @DisplayName("Valida fechas y administrador en el reporte de ocupación")
    void consultarOcupacionServicios_parametrosInvalidos_lanzaExcepcion() {
        LocalDateTime desde = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 12, 1, 0, 0);
        assertThrows(IllegalArgumentException.class,
                () -> reservaService.consultarOcupacionServicios(email, null, hasta));
        assertThrows(IllegalArgumentException.class,
                () -> reservaService.consultarOcupacionServicios(email, hasta, desde));

        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> reservaService.consultarOcupacionServicios("admin@test.com", desde, hasta));

        cliente.setActivo(false);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(cliente));
        assertThrows(UsuarioInactivoException.class,
                () -> reservaService.consultarOcupacionServicios(email, desde, hasta));
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
