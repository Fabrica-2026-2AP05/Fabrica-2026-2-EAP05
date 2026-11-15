package Fabrica_EAP05.Reservas.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Fabrica_EAP05.Reservas.DTO.CancelarReservaResponse;
import Fabrica_EAP05.Reservas.DTO.CrearReservaRequest;
import Fabrica_EAP05.Reservas.DTO.CrearReservaResponse;
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
import java.util.List;
import Fabrica_EAP05.Reservas.DTO.HistorialProveedorResponse;
import java.util.ArrayList;
import java.util.Comparator;
import Fabrica_EAP05.Reservas.DTO.HistorialNegocioResponse;
import Fabrica_EAP05.Reservas.Entities.Evento;
import Fabrica_EAP05.Reservas.Entities.ReservaEvento;
import Fabrica_EAP05.Reservas.Repository.EventoRepository;
import Fabrica_EAP05.Reservas.Repository.ReservaEventoRepository;
import Fabrica_EAP05.Reservas.DTO.OcupacionServicioResponse;

@Service
public class ReservaServicioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private ReservaServicioRepository reservaServicioRepository;

    @Autowired
    private DisponibilidadRecursoRepository disponibilidadRecursoRepository;

    @Autowired
    private Fabrica_EAP05.Reservas.Repository.RecursoRepository recursoRepository;

    @Autowired
    private ReservaEventoRepository reservaEventoRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Transactional(readOnly = true)
    public java.util.List<Fabrica_EAP05.Reservas.DTO.HistorialReservaResponse> consultarHistorialCliente(String email) {
        Usuario cliente = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new UsuarioInactivoException("El usuario no está activo");
        }

        java.util.List<ReservaServicio> reservas = reservaServicioRepository.findByClienteIdOrderByCreadaEnDesc(cliente.getId());

        return reservas.stream().map(reserva -> {
            String nombreServicio = servicioRepository.findById(reserva.getServicioId())
                    .map(Servicio::getNombre)
                    .orElse("Servicio no disponible");

            Horario horario = horarioRepository.findById(reserva.getHorarioId()).orElse(null);
            OffsetDateTime inicio = horario != null ? aOffset(horario.getInicio()) : null;
            OffsetDateTime fin = horario != null ? aOffset(horario.getFin()) : null;

            String nombreRecurso = null;
            if (reserva.getRecursoId() != null) {
                nombreRecurso = recursoRepository.findById(reserva.getRecursoId())
                        .map(Recurso::getNombre)
                        .orElse(null);
            }

            return new Fabrica_EAP05.Reservas.DTO.HistorialReservaResponse(
                    reserva.getId(),
                    reserva.getServicioId(),
                    nombreServicio,
                    reserva.getHorarioId(),
                    inicio,
                    fin,
                    reserva.getRecursoId(),
                    nombreRecurso,
                    reserva.getEstado().name(),
                    aOffset(reserva.getCreadaEn())
            );
        }).toList();
    }

    @Autowired
    private EntityManager entityManager;

    @Transactional
    public CrearReservaResponse crearReserva(String email, CrearReservaRequest request) {
        if (request.getServicioId() == null) {
            throw new IllegalArgumentException("El servicio es obligatorio");
        }
        if (request.getHorarioId() == null) {
            throw new IllegalArgumentException("El horario es obligatorio");
        }

        // 0-1. Cliente autenticado y activo
        Usuario cliente = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new UsuarioInactivoException("El usuario no está activo");
        }

        // 2. Servicio existe y está activo
        Servicio servicio = servicioRepository.findById(request.getServicioId())
                .orElseThrow(() -> new IllegalArgumentException("El servicio no existe"));
        if (!Boolean.TRUE.equals(servicio.getActivo())) {
            throw new UsuarioInactivoException("El usuario no está activo");
        }

        // 3. Horario existe (bloqueado para escritura hasta el fin de la transacción)
        Horario horario = horarioRepository.findByIdParaActualizar(request.getHorarioId())
                .orElseThrow(() -> new IllegalArgumentException("El horario no existe"));

        // 4. Horario disponible
        if (horario.getEstado() != EstadoHorario.disponible) {
            throw new IllegalStateException("El horario seleccionado no está disponible");
        }

        // 5. Recurso requerido: debe tener disponibilidad que cubra el horario
        //    y no estar ocupado por otra reserva confirmada en ese rango
        Recurso recurso = servicio.getRecurso();
        if (recurso != null) {
            boolean cubierto = !disponibilidadRecursoRepository
                    .findByRecursoIdAndInicioLessThanEqualAndFinGreaterThanEqual(
                            recurso.getId(), horario.getInicio(), horario.getFin())
                    .isEmpty();
            boolean ocupado = reservaServicioRepository.contarReservasDeRecursoEnRango(
                    recurso.getId(), horario.getInicio(), horario.getFin()) > 0;

            if (!Boolean.TRUE.equals(recurso.getActivo()) || !cubierto || ocupado) {
                throw new IllegalStateException(
                        "El recurso requerido no está disponible en el horario seleccionado");
            }
        }

        // 6. Crear la reserva
        ReservaServicio reserva = new ReservaServicio();
        reserva.setId(UUID.randomUUID());
        reserva.setClienteId(cliente.getId());
        reserva.setServicioId(servicio.getId());
        reserva.setHorarioId(horario.getId());
        reserva.setRecursoId(recurso != null ? recurso.getId() : null);
        reserva.setEstado(EstadoReserva.confirmada);

        // 7. Marcar el horario como reservado
        horario.setEstado(EstadoHorario.reservado);
        horarioRepository.save(horario);

        ReservaServicio guardada = reservaServicioRepository.saveAndFlush(reserva);
        entityManager.detach(guardada);
        ReservaServicio recargada = reservaServicioRepository.findById(guardada.getId())
                .orElseThrow(() -> new IllegalStateException("No se pudo recuperar la reserva recién creada"));

        // 8. Respuesta
        return new CrearReservaResponse(
                recargada.getId(),
                recargada.getClienteId(),
                recargada.getServicioId(),
                servicio.getNombre(),
                horario.getId(),
                aOffset(horario.getInicio()),
                aOffset(horario.getFin()),
                recargada.getRecursoId(),
                recurso != null ? recurso.getNombre() : null,
                recargada.getEstado().name(),
                aOffset(recargada.getCreadaEn())
        );
    }

    // Las entidades mapean timestamptz como LocalDateTime en la zona de la JVM
    private OffsetDateTime aOffset(LocalDateTime fecha) {
        return fecha != null ? fecha.atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
    }
    @Transactional
    public CancelarReservaResponse cancelarReserva(String email, UUID reservaId) {
    Usuario cliente = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
    if (!Boolean.TRUE.equals(cliente.getActivo())) {
        throw new UsuarioInactivoException("El usuario no está activo");
    }

    // Mismo mensaje si no existe o es de otro cliente: no se revela cuál de las dos
    ReservaServicio reserva = reservaServicioRepository.findByIdParaActualizar(reservaId)
            .filter(r -> r.getClienteId().equals(cliente.getId()))
            .orElseThrow(() -> new NoSuchElementException("La reserva no existe"));

    if (reserva.getEstado() != EstadoReserva.confirmada) {
        throw new IllegalStateException("La reserva no está confirmada, no se puede cancelar");
    }

    // El plazo de 12 horas, cancelada_en y la liberación del horario los resuelven los triggers
    reserva.setEstado(EstadoReserva.cancelada);
    reservaServicioRepository.saveAndFlush(reserva);   // si el trigger rechaza, falla aquí
    entityManager.refresh(reserva);                    // lee cancelada_en que puso el trigger

    return new CancelarReservaResponse(
            reserva.getId(),
            reserva.getEstado().name(),
            aOffset(reserva.getCanceladaEn()));
    }

    @Transactional(readOnly = true)
    public List<HistorialProveedorResponse> consultarHistorialProveedor(String email) {
        Usuario proveedor = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));

        if (!Boolean.TRUE.equals(proveedor.getActivo())) {
            throw new UsuarioInactivoException("El usuario no está activo");
        }

        List<ReservaServicio> reservas = reservaServicioRepository.findHistorialByProveedorId(proveedor.getId());

        return reservas.stream().map(reserva -> {
            String nombreServicio = servicioRepository.findById(reserva.getServicioId())
                    .map(Servicio::getNombre)
                    .orElse("Servicio no disponible");

            String nombreCliente = usuarioRepository.findById(reserva.getClienteId())
                    .map(Usuario::getNombre)
                    .orElse("Cliente no disponible");

            Horario horario = horarioRepository.findById(reserva.getHorarioId()).orElse(null);
            OffsetDateTime inicio = horario != null ? aOffset(horario.getInicio()) : null;
            OffsetDateTime fin = horario != null ? aOffset(horario.getFin()) : null;

            String nombreRecurso = null;
            if (reserva.getRecursoId() != null) {
                nombreRecurso = recursoRepository.findById(reserva.getRecursoId())
                        .map(Recurso::getNombre)
                        .orElse(null);
            }

            return new HistorialProveedorResponse(
                    reserva.getId(),
                    reserva.getServicioId(),
                    nombreServicio,
                    reserva.getClienteId(),
                    nombreCliente,
                    reserva.getHorarioId(),
                    inicio,
                    fin,
                    reserva.getRecursoId(),
                    nombreRecurso,
                    reserva.getEstado().name(),
                    aOffset(reserva.getCreadaEn())
            );
        }).toList();


    }

    @Transactional(readOnly = true)
    public List<HistorialNegocioResponse> consultarHistorialNegocio(String email) {
        Usuario admin = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));

        if (!Boolean.TRUE.equals(admin.getActivo())) {
            throw new UsuarioInactivoException("El usuario no está activo");
        }

        List<HistorialNegocioResponse> historial = new ArrayList<>();

        // 1. Traer todas las reservas de servicios
        for (ReservaServicio r : reservaServicioRepository.findAll()) {
            String nombreServicio = servicioRepository.findById(r.getServicioId())
                    .map(Servicio::getNombre)
                    .orElse("Servicio no disponible");

            String nombreCliente = usuarioRepository.findById(r.getClienteId())
                    .map(Usuario::getNombre)
                    .orElse("Cliente no disponible");

            Horario horario = horarioRepository.findById(r.getHorarioId()).orElse(null);
            OffsetDateTime inicio = horario != null ? aOffset(horario.getInicio()) : null;
            OffsetDateTime fin = horario != null ? aOffset(horario.getFin()) : null;

            String nombreRecurso = null;
            if (r.getRecursoId() != null) {
                nombreRecurso = recursoRepository.findById(r.getRecursoId())
                        .map(Recurso::getNombre)
                        .orElse(null);
            }

            historial.add(new HistorialNegocioResponse(
                    r.getId(),
                    "SERVICIO",
                    r.getServicioId(),
                    nombreServicio,
                    r.getClienteId(),
                    nombreCliente,
                    inicio,
                    fin,
                    r.getRecursoId(),
                    nombreRecurso,
                    r.getEstado().name(),
                    aOffset(r.getCreadaEn())
            ));
        }

        // 2. Traer todas las reservas de eventos
        for (ReservaEvento re : reservaEventoRepository.findAll()) {
            Evento evento = eventoRepository.findById(re.getEventoId()).orElse(null);
            String nombreEvento = evento != null ? evento.getNombre() : "Evento no disponible";
            OffsetDateTime inicio = evento != null ? aOffset(evento.getInicio()) : null;
            OffsetDateTime fin = evento != null ? aOffset(evento.getFin()) : null;

            String nombreCliente = usuarioRepository.findById(re.getClienteId())
                    .map(Usuario::getNombre)
                    .orElse("Cliente no disponible");

            historial.add(new HistorialNegocioResponse(
                    re.getId(),
                    "EVENTO",
                    re.getEventoId(),
                    nombreEvento,
                    re.getClienteId(),
                    nombreCliente,
                    inicio,
                    fin,
                    null,
                    null,
                    re.getEstado().name(),
                    null
            ));
        }

        // 3. Ordenar todo por fecha de inicio (más recientes primero)
        historial.sort(Comparator.comparing(
                HistorialNegocioResponse::getInicio,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return historial;
    }

    @Transactional(readOnly = true)
    public List<OcupacionServicioResponse> consultarOcupacionServicios(
            String email, LocalDateTime desde, LocalDateTime hasta) {

        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas 'desde' y 'hasta' son obligatorias");
        }
        if (!desde.isBefore(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' debe ser anterior a 'hasta'");
        }

        Usuario admin = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));

        if (!Boolean.TRUE.equals(admin.getActivo())) {
            throw new UsuarioInactivoException("El usuario no está activo");
        }

        List<OcupacionServicioResponse> reporte = new ArrayList<>();

        for (Servicio servicio : servicioRepository.findAll()) {
            long totalHorarios = horarioRepository.contarHorariosOfertadosPorServicioEnPeriodo(
                    servicio.getId(), EstadoHorario.bloqueado, desde, hasta);

            long horariosReservados = reservaServicioRepository.contarReservasConfirmadasPorServicioEnPeriodo(
                    servicio.getId(), EstadoReserva.confirmada, desde, hasta);
            // Si este servicio no tiene horarios ni reservas en el periodo, no entra en el reporte
            if (totalHorarios == 0 && horariosReservados == 0) {
                continue;
            }

            double porcentaje = totalHorarios > 0
                    ? Math.round(((horariosReservados * 100.0) / totalHorarios) * 100.0) / 100.0
                    : 0.0;

            reporte.add(new OcupacionServicioResponse(
                    servicio.getId(),
                    servicio.getNombre(),
                    totalHorarios,
                    horariosReservados,
                    porcentaje
            ));
        }

        return reporte;
    }

}
