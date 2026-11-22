package Fabrica_EAP05.Reservas.Controller;

import Fabrica_EAP05.Reservas.Config.JwtFilter;
import Fabrica_EAP05.Reservas.Config.JwtUtil;
import Fabrica_EAP05.Reservas.Entities.Horario;
import Fabrica_EAP05.Reservas.Entities.DisponibilidadRecurso;
import Fabrica_EAP05.Reservas.Entities.Recurso;
import Fabrica_EAP05.Reservas.Entities.Rol;
import Fabrica_EAP05.Reservas.Entities.Servicio;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Enums.EstadoHorario;
import Fabrica_EAP05.Reservas.Enums.EstadoReserva;
import Fabrica_EAP05.Reservas.Entities.ReservaServicio;
import Fabrica_EAP05.Reservas.Repository.DisponibilidadRecursoRepository;
import Fabrica_EAP05.Reservas.Repository.HorarioRepository;
import Fabrica_EAP05.Reservas.Repository.ReservaServicioRepository;
import Fabrica_EAP05.Reservas.Repository.ServicioRepository;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;
import Fabrica_EAP05.Reservas.Service.AuthService;
import Fabrica_EAP05.Reservas.Service.InactivityTrackingService;
import Fabrica_EAP05.Reservas.Service.LoginAttemptService;
import Fabrica_EAP05.Reservas.Service.MailService;
import Fabrica_EAP05.Reservas.Service.ReservaServicioService;
import Fabrica_EAP05.Reservas.Service.SupabaseAuthService;
import Fabrica_EAP05.Reservas.Service.TokenBlacklistService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservaServicioConcurrenteTest {

    private static final int REPETICIONES = 10;
    private static final String EMAIL_CLIENTE_A = "cliente-a@prueba.local";
    private static final String EMAIL_CLIENTE_B = "cliente-b@prueba.local";
    private static final String PASSWORD_CLIENTE_A = "Prueba-local-A-2026";
    private static final String PASSWORD_CLIENTE_B = "Prueba-local-B-2026";

    private final UUID servicioId = UUID.randomUUID();
    private final UUID horarioId = UUID.randomUUID();
    private final Map<String, Usuario> usuarios = new ConcurrentHashMap<>();
    private final Map<UUID, Horario> horariosPersistidos = new ConcurrentHashMap<>();
    private final Map<UUID, ReservaServicio> reservasPersistidas = new ConcurrentHashMap<>();
    private final Map<UUID, ReentrantLock> bloqueosHorario = new ConcurrentHashMap<>();
    private final Map<Thread, ReentrantLock> bloqueosHorarioPorHilo = new ConcurrentHashMap<>();
    private final ReentrantLock bloqueoRecurso = new ReentrantLock();
    private final Map<Thread, ReentrantLock> bloqueosRecursoPorHilo = new ConcurrentHashMap<>();

    private UsuarioRepository usuarioRepository;
    private ServicioRepository servicioRepository;
    private HorarioRepository horarioRepository;
    private ReservaServicioRepository reservaRepository;
    private DisponibilidadRecursoRepository disponibilidadRepository;
    private EntityManager entityManager;
    private InactivityTrackingService inactivityTrackingService;
    private TokenBlacklistService tokenBlacklistService;
    private RestTemplate restTemplate;
    private SupabaseAuthService supabaseAuthService;
    private MailService mailService;
    private JwtUtil jwtUtil;
    private MockMvc mockMvc;
    private Usuario clienteA;
    private Usuario clienteB;
    private String tokenClienteA;
    private String tokenClienteB;
    private Horario horario;

    @BeforeEach
    void setUp() throws Exception {
        clienteA = crearCliente(EMAIL_CLIENTE_A);
        clienteB = crearCliente(EMAIL_CLIENTE_B);
        usuarios.clear();
        usuarios.put(clienteA.getEmail(), clienteA);
        usuarios.put(clienteB.getEmail(), clienteB);

        servicioRepository = mock(ServicioRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        horarioRepository = mock(HorarioRepository.class);
        reservaRepository = mock(ReservaServicioRepository.class);
        disponibilidadRepository = mock(DisponibilidadRecursoRepository.class);
        entityManager = mock(EntityManager.class);
        inactivityTrackingService = mock(InactivityTrackingService.class);
        tokenBlacklistService = mock(TokenBlacklistService.class);
        restTemplate = mock(RestTemplate.class);
        supabaseAuthService = mock(SupabaseAuthService.class);
        mailService = mock(MailService.class);

        when(usuarioRepository.findByEmail(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(usuarios.get(invocation.getArgument(0))));
        when(servicioRepository.findById(servicioId))
                .thenReturn(Optional.of(new Servicio(servicioId, "Servicio concurrente",
                        "Escenario automatizado", 60, null, true)));
        // El fixture local reproduce el bloqueo SELECT FOR UPDATE sin conectarse a una base externa.
        when(horarioRepository.findByIdParaActualizar(any(UUID.class))).thenAnswer(invocation -> {
            UUID idHorario = invocation.getArgument(0);
            ReentrantLock bloqueo = bloqueosHorario.computeIfAbsent(idHorario, id -> new ReentrantLock());
            bloqueo.lockInterruptibly();
            bloqueosHorarioPorHilo.put(Thread.currentThread(), bloqueo);
            Horario encontrado = horariosPersistidos.get(idHorario);
            if (encontrado == null || encontrado.getEstado() != EstadoHorario.disponible) {
                liberarBloqueoHorarioDelHilo();
            }
            return Optional.ofNullable(encontrado);
        });
        doAnswer(invocation -> {
            Horario actualizado = invocation.getArgument(0);
            horariosPersistidos.put(actualizado.getId(), actualizado);
            return actualizado;
        }).when(horarioRepository).save(any(Horario.class));
        when(reservaRepository.findById(any(UUID.class)))
                .thenAnswer(invocation -> Optional.ofNullable(reservasPersistidas.get(invocation.getArgument(0))));
        when(reservaRepository.findAll())
                .thenAnswer(invocation -> new ArrayList<>(reservasPersistidas.values()));
        doAnswer(invocation -> {
            ReservaServicio reserva = invocation.getArgument(0);
            try {
                reservasPersistidas.put(reserva.getId(), reserva);
                return reserva;
            } finally {
                liberarBloqueosDelHilo();
            }
        }).when(reservaRepository).saveAndFlush(any(ReservaServicio.class));

        ReservaServicioService reservaService = new ReservaServicioService();
        ReflectionTestUtils.setField(reservaService, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(reservaService, "servicioRepository", servicioRepository);
        ReflectionTestUtils.setField(reservaService, "horarioRepository", horarioRepository);
        ReflectionTestUtils.setField(reservaService, "reservaServicioRepository", reservaRepository);
        ReflectionTestUtils.setField(reservaService, "disponibilidadRecursoRepository", disponibilidadRepository);
        ReflectionTestUtils.setField(reservaService, "entityManager", entityManager);

        AuthService authService = new AuthService();
        jwtUtil = new JwtUtil("test-only-secret-key-with-at-least-32-bytes");
        ReflectionTestUtils.setField(authService, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(authService, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(authService, "inactivityTrackingService", inactivityTrackingService);
        ReflectionTestUtils.setField(authService, "tokenBlacklistService", tokenBlacklistService);
        ReflectionTestUtils.setField(authService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(authService, "supabaseAuthService", supabaseAuthService);
        ReflectionTestUtils.setField(authService, "mailService", mailService);
        ReflectionTestUtils.setField(authService, "loginAttemptService", new LoginAttemptService());
        ReflectionTestUtils.setField(authService, "supabaseUrl", "https://auth.test.invalid");
        ReflectionTestUtils.setField(authService, "supabaseAnonKey", "test-anon-key");
        doReturn(org.springframework.http.ResponseEntity.ok("autenticado"))
                .when(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));

        ReservaServicioController reservaController = new ReservaServicioController();
        ReflectionTestUtils.setField(reservaController, "reservaServicioService", reservaService);
        AuthController authController = new AuthController();
        ReflectionTestUtils.setField(authController, "authService", authService);

        JwtFilter jwtFilter = new JwtFilter();
        ReflectionTestUtils.setField(jwtFilter, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(jwtFilter, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(jwtFilter, "inactivityTrackingService", inactivityTrackingService);
        ReflectionTestUtils.setField(jwtFilter, "tokenBlacklistService", tokenBlacklistService);
        when(inactivityTrackingService.validarActividad(anyString())).thenReturn(true);
        when(tokenBlacklistService.estaEnLista(anyString())).thenReturn(false);

        mockMvc = MockMvcBuilders.standaloneSetup(authController, reservaController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .addFilters(jwtFilter)
                .build();

        tokenClienteA = iniciarSesion(EMAIL_CLIENTE_A, PASSWORD_CLIENTE_A);
        tokenClienteB = iniciarSesion(EMAIL_CLIENTE_B, PASSWORD_CLIENTE_B);
        assertNotEquals(tokenClienteA, tokenClienteB, "Cada cliente debe tener una sesión JWT distinta");

        horario = new Horario();
        horario.setId(horarioId);
        horario.setInicio(LocalDateTime.of(2026, 11, 1, 8, 0));
        horario.setFin(LocalDateTime.of(2026, 11, 1, 9, 0));
        horario.setEstado(EstadoHorario.disponible);
        resetearDatosDePrueba();
    }

    @Test
    void dosClientesConcurrentes_soloUnaReservaConfirmada_enDiezEjecuciones() throws Exception {
        ExecutorService clientesConcurrentes = Executors.newFixedThreadPool(2);
        List<String> incumplimientos = new ArrayList<>();
        try {
            for (int ejecucion = 1; ejecucion <= REPETICIONES; ejecucion++) {
                resetearDatosDePrueba();

                CountDownLatch clientesListos = new CountDownLatch(2);
                CyclicBarrier inicioConcurrente = new CyclicBarrier(3);
                Future<Respuesta> respuestaA = clientesConcurrentes.submit(() ->
                        reservarConSincronizacion(clientesListos, inicioConcurrente, tokenClienteA));
                Future<Respuesta> respuestaB = clientesConcurrentes.submit(() ->
                        reservarConSincronizacion(clientesListos, inicioConcurrente, tokenClienteB));

                boolean listos = clientesListos.await(5, TimeUnit.SECONDS);
                if (listos) {
                    inicioConcurrente.await(5, TimeUnit.SECONDS);
                }

                Respuesta resultadoA = obtenerRespuesta(respuestaA, "cliente A", ejecucion, incumplimientos);
                Respuesta resultadoB = obtenerRespuesta(respuestaB, "cliente B", ejecucion, incumplimientos);
                List<ReservaServicio> reservas = reservaRepository.findAll();
                long exitos = contarEstados(List.of(resultadoA, resultadoB), 201);
                long conflictos = contarEstados(List.of(resultadoA, resultadoB), 409);
                long confirmadas = reservas.stream()
                        .filter(reserva -> reserva.getEstado() == EstadoReserva.confirmada)
                        .filter(reserva -> horarioId.equals(reserva.getHorarioId()))
                        .filter(reserva -> servicioId.equals(reserva.getServicioId()))
                        .count();
                UUID clienteGanador = resultadoA.status() == 201 ? clienteA.getId()
                        : resultadoB.status() == 201 ? clienteB.getId() : null;
                boolean consistentes = reservas.size() == 1
                        && confirmadas == 1
                        && horario.getEstado() == EstadoHorario.reservado
                        && reservas.stream().allMatch(reserva ->
                                clienteGanador != null && clienteGanador.equals(reserva.getClienteId()));
                boolean apiCorrecta = exitos == 1
                        && conflictos == 1
                        && resultadoA.reservaConfirmada(horarioId, servicioId)
                                != resultadoB.reservaConfirmada(horarioId, servicioId)
                        && resultadoA.conflictoControlado() != resultadoB.conflictoControlado();

                if (!listos) {
                    incumplimientos.add("Ejecución " + ejecucion + ": ambos clientes no llegaron a la barrera");
                }
                if (!apiCorrecta) {
                    incumplimientos.add("Ejecución " + ejecucion + ": respuestas HTTP inválidas; A="
                            + resultadoA.status() + ", B=" + resultadoB.status());
                }
                if (!consistentes) {
                    incumplimientos.add("Ejecución " + ejecucion + ": estado persistido inconsistente; reservas="
                            + reservas.size() + ", confirmadas para el horario=" + confirmadas
                            + ", estado del horario=" + horario.getEstado());
                }

                System.out.printf("Ejecución %02d: clienteA=%d, clienteB=%d, reservas=%d, confirmadas=%d, API=%s, persistencia=%s%n",
                        ejecucion, resultadoA.status(), resultadoB.status(), reservas.size(), confirmadas,
                        apiCorrecta ? "OK" : "FALLÓ", consistentes ? "OK" : "FALLÓ");
            }
        } finally {
            clientesConcurrentes.shutdownNow();
            clientesConcurrentes.awaitTermination(5, TimeUnit.SECONDS);
        }

        assertTrue(incumplimientos.isEmpty(),
                () -> "Fallaron criterios de concurrencia:%n" + String.join("%n", incumplimientos));
    }

    @Test
    void dosServiciosConcurrentes_queRequierenElMismoRecurso_soloUnoSeConfirmaEnDiezEjecuciones()
            throws Exception {
        UUID recursoId = UUID.randomUUID();
        UUID segundoServicioId = UUID.randomUUID();
        UUID segundoHorarioId = UUID.randomUUID();
        Recurso recurso = new Recurso();
        recurso.setId(recursoId);
        recurso.setNombre("Recurso compartido de prueba");
        recurso.setDescripcion("Fixture aislado para concurrencia");
        recurso.setActivo(true);

        Servicio servicioA = new Servicio(servicioId, "Servicio concurrente A",
                "Requiere recurso compartido", 60, recurso, true);
        Servicio servicioB = new Servicio(segundoServicioId, "Servicio concurrente B",
                "Requiere recurso compartido", 60, recurso, true);
        doReturn(Optional.of(servicioA)).when(servicioRepository).findById(servicioId);
        doReturn(Optional.of(servicioB)).when(servicioRepository).findById(segundoServicioId);

        Horario segundoHorario = new Horario();
        segundoHorario.setId(segundoHorarioId);
        segundoHorario.setProveedorId(UUID.randomUUID());
        segundoHorario.setInicio(horario.getInicio());
        segundoHorario.setFin(horario.getFin());
        segundoHorario.setEstado(EstadoHorario.disponible);
        DisponibilidadRecurso disponibilidad = new DisponibilidadRecurso();
        disponibilidad.setId(UUID.randomUUID());
        disponibilidad.setRecursoId(recursoId);
        disponibilidad.setInicio(horario.getInicio().minusHours(1));
        disponibilidad.setFin(horario.getFin().plusHours(1));

        when(disponibilidadRepository
                .findByRecursoIdAndInicioLessThanEqualAndFinGreaterThanEqual(
                        eq(recursoId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(disponibilidad));
        when(reservaRepository.contarReservasDeRecursoEnRango(
                eq(recursoId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    bloqueoRecurso.lockInterruptibly();
                    bloqueosRecursoPorHilo.put(Thread.currentThread(), bloqueoRecurso);
                    LocalDateTime inicio = invocation.getArgument(1);
                    LocalDateTime fin = invocation.getArgument(2);
                    return reservasPersistidas.values().stream()
                            .filter(reserva -> recursoId.equals(reserva.getRecursoId()))
                            .filter(reserva -> reserva.getEstado() == EstadoReserva.confirmada)
                            .map(reserva -> horariosPersistidos.get(reserva.getHorarioId()))
                            .filter(ocupado -> ocupado != null
                                    && ocupado.getInicio().isBefore(fin)
                                    && ocupado.getFin().isAfter(inicio))
                            .count();
                });

        ExecutorService clientesConcurrentes = Executors.newFixedThreadPool(2);
        List<String> incumplimientos = new ArrayList<>();
        try {
            for (int ejecucion = 1; ejecucion <= REPETICIONES; ejecucion++) {
                resetearDatosDePrueba();
                segundoHorario.setEstado(EstadoHorario.disponible);
                horariosPersistidos.put(segundoHorarioId, segundoHorario);

                CountDownLatch clientesListos = new CountDownLatch(2);
                CyclicBarrier inicioConcurrente = new CyclicBarrier(3);
                Future<Respuesta> respuestaA = clientesConcurrentes.submit(() ->
                        reservarConSincronizacion(clientesListos, inicioConcurrente,
                                tokenClienteA, servicioId, horarioId));
                Future<Respuesta> respuestaB = clientesConcurrentes.submit(() ->
                        reservarConSincronizacion(clientesListos, inicioConcurrente,
                                tokenClienteB, segundoServicioId, segundoHorarioId));

                boolean listos = clientesListos.await(5, TimeUnit.SECONDS);
                if (listos) {
                    inicioConcurrente.await(5, TimeUnit.SECONDS);
                }

                Respuesta resultadoA = obtenerRespuesta(respuestaA, "cliente A", ejecucion, incumplimientos);
                Respuesta resultadoB = obtenerRespuesta(respuestaB, "cliente B", ejecucion, incumplimientos);
                List<ReservaServicio> reservas = reservaRepository.findAll();
                long confirmadasDelRecurso = reservas.stream()
                        .filter(reserva -> reserva.getEstado() == EstadoReserva.confirmada)
                        .filter(reserva -> recursoId.equals(reserva.getRecursoId()))
                        .filter(reserva -> horarioId.equals(reserva.getHorarioId())
                                || segundoHorarioId.equals(reserva.getHorarioId()))
                        .count();
                boolean unaReservaPersistida = reservas.size() == 1 && confirmadasDelRecurso == 1;
                boolean unaRespuestaExitosa = (resultadoA.status() == 201 && resultadoB.status() == 409)
                        || (resultadoA.status() == 409 && resultadoB.status() == 201);
                boolean respuestaAConsistente = resultadoA.status() != 201
                        || resultadoA.reservaConfirmada(horarioId, servicioId);
                boolean respuestaBConsistente = resultadoB.status() != 201
                        || resultadoB.reservaConfirmada(segundoHorarioId, segundoServicioId);
                boolean horariosConsistentes = reservas.size() == 1
                        && (horario.getEstado() == EstadoHorario.reservado
                                ^ segundoHorario.getEstado() == EstadoHorario.reservado);
                boolean respuestasDeConflictoControladas = (resultadoA.status() != 409
                        || resultadoA.conflictoControladoPorRecurso())
                        && (resultadoB.status() != 409 || resultadoB.conflictoControladoPorRecurso());

                if (!listos) {
                    incumplimientos.add("Ejecución " + ejecucion + ": ambos clientes no llegaron a la barrera");
                }
                if (!unaRespuestaExitosa || !respuestaAConsistente || !respuestaBConsistente
                        || !respuestasDeConflictoControladas) {
                    incumplimientos.add("Ejecución " + ejecucion + ": respuestas HTTP inválidas; A="
                            + resultadoA.status() + ", B=" + resultadoB.status());
                }
                if (!unaReservaPersistida || !horariosConsistentes) {
                    incumplimientos.add("Ejecución " + ejecucion + ": estado persistido inconsistente; reservas="
                            + reservas.size() + ", confirmadas con el recurso compartido="
                            + confirmadasDelRecurso + ", horarioA=" + horario.getEstado()
                            + ", horarioB=" + segundoHorario.getEstado());
                }

                System.out.printf("Ejecución recurso %02d: clienteA=%d, clienteB=%d, reservas=%d, recursoConfirmado=%d, API=%s, persistencia=%s%n",
                        ejecucion, resultadoA.status(), resultadoB.status(), reservas.size(),
                        confirmadasDelRecurso,
                        unaRespuestaExitosa && respuestasDeConflictoControladas ? "OK" : "FALLÓ",
                        unaReservaPersistida && horariosConsistentes ? "OK" : "FALLÓ");
            }
        } finally {
            clientesConcurrentes.shutdownNow();
            clientesConcurrentes.awaitTermination(5, TimeUnit.SECONDS);
        }

        assertTrue(incumplimientos.isEmpty(),
                () -> "Fallaron criterios de reserva concurrente del recurso:%n"
                        + String.join("%n", incumplimientos));
    }

    private Respuesta reservarConSincronizacion(CountDownLatch clientesListos,
                                                CyclicBarrier inicioConcurrente,
                                                String token) throws Exception {
        return reservarConSincronizacion(clientesListos, inicioConcurrente, token, servicioId, horarioId);
    }

    private Respuesta reservarConSincronizacion(CountDownLatch clientesListos,
                                                CyclicBarrier inicioConcurrente,
                                                String token,
                                                UUID idServicio,
                                                UUID idHorario) throws Exception {
        clientesListos.countDown();
        inicioConcurrente.await(5, TimeUnit.SECONDS);
        try {
            MockHttpServletRequestBuilder solicitud = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .post("/api/reservas/reservar")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"servicioId\":\"" + idServicio + "\",\"horarioId\":\"" + idHorario + "\"}");
            MvcResult resultado = mockMvc.perform(solicitud).andReturn();
            return new Respuesta(resultado.getResponse().getStatus(),
                    resultado.getResponse().getContentAsString());
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            liberarBloqueosDelHilo();
        }
    }

    private String iniciarSesion(String email, String contrasena) throws Exception {
        MvcResult resultado = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"contrasena\":\"" + contrasena + "\"}"))
                .andReturn();
        if (resultado.getResponse().getStatus() != 200) {
            throw new AssertionError("No se pudo iniciar sesión para el cliente de prueba: HTTP "
                    + resultado.getResponse().getStatus());
        }
        return extraerCampoJson(resultado.getResponse().getContentAsString(), "token");
    }

    private String extraerCampoJson(String json, String campo) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(campo) + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (!matcher.find()) {
            throw new AssertionError("La respuesta de autenticación no contiene el campo " + campo);
        }
        return matcher.group(1);
    }

    private Respuesta obtenerRespuesta(Future<Respuesta> futuro,
                                      String cliente,
                                      int ejecucion,
                                      List<String> incumplimientos) {
        try {
            return futuro.get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            incumplimientos.add("Ejecución " + ejecucion + ": error en " + cliente + ": "
                    + exception.getClass().getSimpleName() + " - " + exception.getMessage());
            futuro.cancel(true);
            return new Respuesta(0, "");
        }
    }

    private long contarEstados(List<Respuesta> respuestas, int status) {
        return respuestas.stream().filter(respuesta -> respuesta.status() == status).count();
    }

    private void resetearDatosDePrueba() throws InterruptedException {
        ReentrantLock bloqueo = bloqueosHorario.computeIfAbsent(horarioId, id -> new ReentrantLock());
        if (!bloqueo.tryLock(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("No se pudo obtener el bloqueo del horario para reiniciar el escenario");
        }
        try {
            reservasPersistidas.clear();
            horario.setEstado(EstadoHorario.disponible);
            horariosPersistidos.clear();
            horariosPersistidos.put(horarioId, horario);
        } finally {
            bloqueo.unlock();
        }
    }

    private void liberarBloqueosDelHilo() {
        liberarBloqueoHorarioDelHilo();
        ReentrantLock bloqueoRecursoDelHilo = bloqueosRecursoPorHilo.remove(Thread.currentThread());
        if (bloqueoRecursoDelHilo != null && bloqueoRecursoDelHilo.isHeldByCurrentThread()) {
            bloqueoRecursoDelHilo.unlock();
        }
    }

    private void liberarBloqueoHorarioDelHilo() {
        ReentrantLock bloqueo = bloqueosHorarioPorHilo.remove(Thread.currentThread());
        if (bloqueo != null && bloqueo.isHeldByCurrentThread()) {
            bloqueo.unlock();
        }
    }

    private Usuario crearCliente(String email) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNombre("Cliente de prueba");
        usuario.setEmail(email);
        usuario.setRol(Rol.cliente);
        usuario.setActivo(true);
        return usuario;
    }

    private record Respuesta(int status, String body) {
        boolean reservaConfirmada(UUID horarioId, UUID servicioId) {
            return status == 201
                    && body.contains("\"estado\":\"confirmada\"")
                    && body.contains("\"horarioId\":\"" + horarioId + "\"")
                    && body.contains("\"servicioId\":\"" + servicioId + "\"");
        }

        boolean conflictoControlado() {
            return status == 409
                    && body.contains("\"error\"")
                    && body.contains("El horario seleccionado no está disponible");
        }

        boolean conflictoControladoPorRecurso() {
            return status == 409
                    && body.contains("\"error\"")
                    && body.contains("El recurso requerido no está disponible");
        }
    }
}
