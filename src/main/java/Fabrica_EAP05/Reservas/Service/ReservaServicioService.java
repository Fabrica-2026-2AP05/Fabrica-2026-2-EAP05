package Fabrica_EAP05.Reservas.Service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Fabrica_EAP05.Reservas.DTO.CancelarReservaResponse;
import Fabrica_EAP05.Reservas.DTO.CrearReservaRequest;
import Fabrica_EAP05.Reservas.DTO.CrearReservaResponse;
import Fabrica_EAP05.Reservas.DTO.HistorialReservaResponse;
import Fabrica_EAP05.Reservas.DTO.HorarioDisponibleResponse;
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
@RequiredArgsConstructor
public class ReservaServicioService {

    private final UsuarioRepository usuarioRepository;

    private final ServicioRepository servicioRepository;

    private final HorarioRepository horarioRepository;

    private final ReservaServicioRepository reservaServicioRepository;

    private final DisponibilidadRecursoRepository disponibilidadRecursoRepository;

    private final Fabrica_EAP05.Reservas.Repository.RecursoRepository recursoRepository;

    private final ReservaEventoRepository reservaEventoRepository;

    private final EventoRepository eventoRepository;

    @Transactional(readOnly = true)
    public java.util.List<Fabrica_EAP05.Reservas.DTO.HistorialReservaResponse> consultarHistorialCliente(String email) {
        Usuario cliente = obtenerUsuarioActivo(email);

        java.util.List<ReservaServicio> reservas = reservaServicioRepository.findByClienteIdOrderByCreadaEnDesc(cliente.getId());

        return reservas.stream().map(this::mapHistorialReserva).toList();
    }

    private HistorialReservaResponse mapHistorialReserva(ReservaServicio reserva) {
        Optional<Horario> horario = horarioRepository.findById(reserva.getHorarioId());

        return new HistorialReservaResponse(
                reserva.getId(),
                reserva.getServicioId(),
                nombreServicio(reserva.getServicioId()),
                reserva.getHorarioId(),
                horario.map(Horario::getInicio).map(this::aOffset).orElse(null),
                horario.map(Horario::getFin).map(this::aOffset).orElse(null),
                reserva.getRecursoId(),
                nombreRecurso(reserva.getRecursoId()),
                reserva.getEstado().name(),
                aOffset(reserva.getCreadaEn())
        );
    }

    private final EntityManager entityManager;

    @Transactional
    public CrearReservaResponse crearReserva(String email, CrearReservaRequest request) {
        if (request.getServicioId() == null) {
            throw new IllegalArgumentException("El servicio es obligatorio");
        }
        if (request.getHorarioId() == null) {
            throw new IllegalArgumentException("El horario es obligatorio");
        }

        // 0-1. Cliente autenticado y activo
        Usuario cliente = obtenerUsuarioActivo(email);

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
        if (recurso != null && !recursoDisponibleEn(recurso, horario)) {
            throw new IllegalStateException(
                    "El recurso requerido no está disponible en el horario seleccionado");
        }

        // 6. Crear la reserva
        ReservaServicio reserva = new ReservaServicio();
        reserva.setId(UUID.randomUUID());
        reserva.setClienteId(cliente.getId());
        reserva.setServicioId(servicio.getId());
        reserva.setHorarioId(horario.getId());
        reserva.setRecursoId(Optional.ofNullable(recurso).map(Recurso::getId).orElse(null));
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
                Optional.ofNullable(recurso).map(Recurso::getNombre).orElse(null),
                recargada.getEstado().name(),
                aOffset(recargada.getCreadaEn())
        );
    }

    // HU14: horarios que el cliente puede reservar para un servicio
    @Transactional(readOnly = true)
    public List<HorarioDisponibleResponse> consultarDisponibilidad(UUID servicioId) {
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new NoSuchElementException(
                        "El servicio con ID " + servicioId + " no existe en el sistema"));
        if (!Boolean.TRUE.equals(servicio.getActivo())) {
            throw new IllegalStateException("El servicio no está activo");
        }

        Recurso recurso = servicio.getRecurso();
        return horarioRepository.findDisponiblesPorServicio(servicioId, LocalDateTime.now())
                .stream()
                .filter(h -> recurso == null || recursoDisponibleEn(recurso, h))
                .map(h -> new HorarioDisponibleResponse(
                        h.getId(), aOffset(h.getInicio()), aOffset(h.getFin()), h.getProveedorId()))
                .toList();
    }

    // Misma regla para HU14 y HU15: recurso activo, con disponibilidad que cubra
    // el rango del horario y sin reservas confirmadas que se solapen
    private boolean recursoDisponibleEn(Recurso recurso, Horario horario) {
        boolean cubierto = !disponibilidadRecursoRepository
                .findByRecursoIdAndInicioLessThanEqualAndFinGreaterThanEqual(
                        recurso.getId(), horario.getInicio(), horario.getFin())
                .isEmpty();
        boolean ocupado = reservaServicioRepository.contarReservasDeRecursoEnRango(
                recurso.getId(), horario.getInicio(), horario.getFin()) > 0;

        return Boolean.TRUE.equals(recurso.getActivo()) && cubierto && !ocupado;
    }

    // Las entidades mapean timestamptz como LocalDateTime en la zona de la JVM
    private OffsetDateTime aOffset(LocalDateTime fecha) {
        return fecha != null ? fecha.atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
    }

    // Usuario autenticado y activo: mismas excepciones y mensajes en todos los casos de uso
    private Usuario obtenerUsuarioActivo(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new UsuarioInactivoException("El usuario no está activo");
        }
        return usuario;
    }

    private String nombreServicio(UUID servicioId) {
        return servicioRepository.findById(servicioId)
                .map(Servicio::getNombre)
                .orElse("Servicio no disponible");
    }

    private String nombreCliente(UUID clienteId) {
        return usuarioRepository.findById(clienteId)
                .map(Usuario::getNombre)
                .orElse("Cliente no disponible");
    }

    // Sin recurso asignado no se consulta el repositorio
    private String nombreRecurso(UUID recursoId) {
        return Optional.ofNullable(recursoId)
                .flatMap(recursoRepository::findById)
                .map(Recurso::getNombre)
                .orElse(null);
    }

    @Transactional
    public CancelarReservaResponse cancelarReserva(String email, UUID reservaId) {
    Usuario cliente = obtenerUsuarioActivo(email);

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
        Usuario proveedor = obtenerUsuarioActivo(email);

        List<ReservaServicio> reservas = reservaServicioRepository.findHistorialByProveedorId(proveedor.getId());

        return reservas.stream().map(this::mapHistorialProveedor).toList();
    }

    private HistorialProveedorResponse mapHistorialProveedor(ReservaServicio reserva) {
        Optional<Horario> horario = horarioRepository.findById(reserva.getHorarioId());

        return new HistorialProveedorResponse(
                reserva.getId(),
                reserva.getServicioId(),
                nombreServicio(reserva.getServicioId()),
                reserva.getClienteId(),
                nombreCliente(reserva.getClienteId()),
                reserva.getHorarioId(),
                horario.map(Horario::getInicio).map(this::aOffset).orElse(null),
                horario.map(Horario::getFin).map(this::aOffset).orElse(null),
                reserva.getRecursoId(),
                nombreRecurso(reserva.getRecursoId()),
                reserva.getEstado().name(),
                aOffset(reserva.getCreadaEn())
        );
    }

    @Transactional(readOnly = true)
    public List<HistorialNegocioResponse> consultarHistorialNegocio(String email) {
        obtenerUsuarioActivo(email);

        // 1. Reservas de servicios, 2. reservas de eventos (mismo orden de carga que antes)
        List<HistorialNegocioResponse> historial = new ArrayList<>(historialNegocioServicios());
        historial.addAll(historialNegocioEventos());

        // 3. Ordenar todo por fecha de inicio (más recientes primero)
        historial.sort(Comparator.comparing(
                HistorialNegocioResponse::getInicio,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return historial;
    }

    private List<HistorialNegocioResponse> historialNegocioServicios() {
        return reservaServicioRepository.findAll().stream().map(this::mapHistorialNegocio).toList();
    }

    private List<HistorialNegocioResponse> historialNegocioEventos() {
        return reservaEventoRepository.findAll().stream().map(this::mapReservaEvento).toList();
    }

    private HistorialNegocioResponse mapHistorialNegocio(ReservaServicio r) {
        Optional<Horario> horario = horarioRepository.findById(r.getHorarioId());

        return new HistorialNegocioResponse(
                r.getId(),
                "SERVICIO",
                r.getServicioId(),
                nombreServicio(r.getServicioId()),
                r.getClienteId(),
                nombreCliente(r.getClienteId()),
                horario.map(Horario::getInicio).map(this::aOffset).orElse(null),
                horario.map(Horario::getFin).map(this::aOffset).orElse(null),
                r.getRecursoId(),
                nombreRecurso(r.getRecursoId()),
                r.getEstado().name(),
                aOffset(r.getCreadaEn())
        );
    }

    private HistorialNegocioResponse mapReservaEvento(ReservaEvento re) {
        Evento evento = eventoRepository.findById(re.getEventoId()).orElse(null);
        // Ternario conservado: si el evento existe pero su nombre es null, se devuelve null (no el texto por defecto)
        String nombreEvento = evento != null ? evento.getNombre() : "Evento no disponible";

        return new HistorialNegocioResponse(
                re.getId(),
                "EVENTO",
                re.getEventoId(),
                nombreEvento,
                re.getClienteId(),
                nombreCliente(re.getClienteId()),
                Optional.ofNullable(evento).map(Evento::getInicio).map(this::aOffset).orElse(null),
                Optional.ofNullable(evento).map(Evento::getFin).map(this::aOffset).orElse(null),
                null,
                null,
                re.getEstado().name(),
                null
        );
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

        obtenerUsuarioActivo(email);

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
