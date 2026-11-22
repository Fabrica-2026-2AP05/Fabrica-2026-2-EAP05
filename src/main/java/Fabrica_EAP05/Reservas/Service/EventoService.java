package Fabrica_EAP05.Reservas.Service;

import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Fabrica_EAP05.Reservas.DTO.CancelarReservaEventoResponse;
import Fabrica_EAP05.Reservas.DTO.ModificarEventoRequest;
import Fabrica_EAP05.Reservas.DTO.OcupacionEventoResponse;
import Fabrica_EAP05.Reservas.DTO.RegistrarEventoRequest;
import Fabrica_EAP05.Reservas.DTO.RegistroEventoResponse;
import Fabrica_EAP05.Reservas.DTO.ReservaEventoResponse;
import Fabrica_EAP05.Reservas.Entities.Evento;
import Fabrica_EAP05.Reservas.Entities.ReservaEvento;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Enums.EstadoReserva;
import Fabrica_EAP05.Reservas.Exception.CapacidadInsuficienteException;
import Fabrica_EAP05.Reservas.Exception.ConflictoDisponibilidadException;
import Fabrica_EAP05.Reservas.Exception.CupoAgotadoException;
import Fabrica_EAP05.Reservas.Exception.EventoNoEncontradoException;
import Fabrica_EAP05.Reservas.Exception.PlazoCancelacionException;
import Fabrica_EAP05.Reservas.Exception.RecursoDuplicadoException;
import Fabrica_EAP05.Reservas.Exception.UsuarioInactivoException;
import Fabrica_EAP05.Reservas.Repository.EventoRepository;
import Fabrica_EAP05.Reservas.Repository.ReservaEventoRepository;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;
import jakarta.persistence.EntityManager;

@Service
@RequiredArgsConstructor
public class EventoService {

    private final ReservaEventoRepository reservaEventoRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EntityManager entityManager;

    // HU22: horas mínimas de anticipación para cancelar una reserva de evento
    static final long HORAS_MINIMAS_CANCELACION = 12;

    // Reloj del sistema en la zona de la JVM; los tests lo reemplazan por uno fijo
    private Clock clock = Clock.systemDefaultZone();

    @Transactional
    public RegistroEventoResponse registrarEvento(RegistrarEventoRequest request) {
        if (eventoRepository.existsByNombre(request.getNombre())) {
            throw new RecursoDuplicadoException("El nombre de este evento ya está registrado");
        }

        Evento evento = new Evento();
        evento.setId(UUID.randomUUID());
        evento.setNombre(request.getNombre());
        evento.setDescripcion(request.getDescripcion());
        evento.setInicio(aLocal(request.getInicio()));
        evento.setFin(aLocal(request.getFin()));
        evento.setCapacidad(request.getCapacidad());
        evento.setActivo(true);

        Evento guardado = eventoRepository.saveAndFlush(evento);

        return new RegistroEventoResponse(
                guardado.getId(),
                guardado.getNombre(),
                guardado.getDescripcion(),
                aOffset(guardado.getInicio()),
                aOffset(guardado.getFin()),
                guardado.getCapacidad(),
                guardado.getActivo(),
                guardado.getCreatedAt()
        );
    }

    @Transactional
    public RegistroEventoResponse actualizarEvento(UUID id, ModificarEventoRequest request) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new EventoNoEncontradoException("El evento no existe"));

        if (!evento.getNombre().equals(request.getNombre())
                && eventoRepository.existsByNombreAndIdNot(request.getNombre(), id)) {
            throw new RecursoDuplicadoException("El nombre de este evento ya está registrado");
        }

        evento.setNombre(request.getNombre());
        evento.setDescripcion(request.getDescripcion());
        evento.setInicio(aLocal(request.getInicio()));
        evento.setFin(aLocal(request.getFin()));

        Evento guardado = eventoRepository.saveAndFlush(evento);

        return new RegistroEventoResponse(
                guardado.getId(),
                guardado.getNombre(),
                guardado.getDescripcion(),
                aOffset(guardado.getInicio()),
                aOffset(guardado.getFin()),
                guardado.getCapacidad(),
                guardado.getActivo(),
                guardado.getCreatedAt()
        );
    }

    @Transactional
    public RegistroEventoResponse definirCapacidad(UUID id, Integer nuevaCapacidad) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new EventoNoEncontradoException("El evento no existe"));

        long confirmadas = reservaEventoRepository.countByEventoIdAndEstado(id, EstadoReserva.confirmada);

        if (nuevaCapacidad < confirmadas) {
            throw new CapacidadInsuficienteException(
                    "La capacidad mínima permitida es " + confirmadas
                            + " (cantidad de reservas confirmadas existentes)");
        }

        evento.setCapacidad(nuevaCapacidad);
        Evento guardado = eventoRepository.saveAndFlush(evento);

        return new RegistroEventoResponse(
                guardado.getId(),
                guardado.getNombre(),
                guardado.getDescripcion(),
                aOffset(guardado.getInicio()),
                aOffset(guardado.getFin()),
                guardado.getCapacidad(),
                guardado.getActivo(),
                guardado.getCreatedAt()
        );
    }

    // Reserva de cupo: el cliente toma un cupo de la capacidad del evento
    @Transactional
    public ReservaEventoResponse reservarCupo(String emailCliente, UUID eventoId) {
        // a. Cliente autenticado y activo
        Usuario cliente = usuarioRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new UsuarioInactivoException("El usuario no está activo");
        }

        // b-c. Evento existe y está activo (bloqueado hasta el fin de la transacción)
        Evento evento = eventoRepository.findByIdParaActualizar(eventoId)
                .orElseThrow(() -> new EventoNoEncontradoException("El evento no existe"));
        if (!Boolean.TRUE.equals(evento.getActivo())) {
            throw new ConflictoDisponibilidadException("El evento no está activo");
        }

        // Una sola reserva confirmada por cliente y evento
        if (reservaEventoRepository.existsByClienteIdAndEventoIdAndEstado(
                cliente.getId(), eventoId, EstadoReserva.confirmada)) {
            throw new ConflictoDisponibilidadException("Ya tienes una reserva confirmada en este evento");
        }

        // d. Cupos disponibles = capacidad - reservas confirmadas
        int capacidad = evento.getCapacidad() != null ? evento.getCapacidad() : 0;
        long confirmadas = reservaEventoRepository.countByEventoIdAndEstado(eventoId, EstadoReserva.confirmada);
        if (confirmadas >= capacidad) {
            throw new CupoAgotadoException("No hay cupos disponibles para este evento");
        }

        // e. Crear la reserva confirmada
        ReservaEvento reserva = new ReservaEvento();
        reserva.setId(UUID.randomUUID());
        reserva.setClienteId(cliente.getId());
        reserva.setEventoId(eventoId);
        reserva.setEstado(EstadoReserva.confirmada);
        ReservaEvento guardada = reservaEventoRepository.saveAndFlush(reserva);

        // f. Con el evento bloqueado, los cupos restantes descuentan solo la reserva recién creada
        long cuposRestantes = capacidad - (confirmadas + 1);

        return new ReservaEventoResponse(
                guardada.getId(),
                guardada.getEventoId(),
                guardada.getClienteId(),
                guardada.getEstado().name(),
                cuposRestantes);
    }

    // HU22: el cliente cancela su reserva de evento y el cupo vuelve a quedar disponible
    @Transactional
    public CancelarReservaEventoResponse cancelarReservaEvento(String emailCliente, UUID eventoId, UUID reservaId) {
        // a. Cliente autenticado y activo
        Usuario cliente = usuarioRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new UsuarioInactivoException("El usuario no está activo");
        }

        // b. Mismo mensaje si no existe, es de otro cliente o de otro evento: no se revela cuál
        ReservaEvento reserva = reservaEventoRepository.findById(reservaId)
                .filter(r -> cliente.getId().equals(r.getClienteId()) && eventoId.equals(r.getEventoId()))
                .orElseThrow(() -> new EventoNoEncontradoException("La reserva no existe"));

        // c. Solo se cancela una reserva confirmada
        if (reserva.getEstado() != EstadoReserva.confirmada) {
            throw new ConflictoDisponibilidadException("La reserva no está confirmada, no se puede cancelar");
        }

        // d. Plazo mínimo: deben faltar al menos HORAS_MINIMAS_CANCELACION para el inicio
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new EventoNoEncontradoException("El evento no existe"));
        if (ahora().plusHours(HORAS_MINIMAS_CANCELACION).isAfter(evento.getInicio())) {
            throw new PlazoCancelacionException("No se puede cancelar: faltan menos de "
                    + HORAS_MINIMAS_CANCELACION + " horas para el inicio del evento");
        }

        // e. Cancelar; el cupo se libera porque solo cuentan las reservas confirmadas.
        //    cancelada_en lo asigna la base; refresh lo lee (mismo patrón que ReservaServicioService)
        reserva.setEstado(EstadoReserva.cancelada);
        ReservaEvento guardada = reservaEventoRepository.saveAndFlush(reserva);
        entityManager.refresh(guardada);

        return new CancelarReservaEventoResponse(
                guardada.getId(),
                guardada.getEventoId(),
                guardada.getClienteId(),
                guardada.getEstado().name());
    }

    // HU28: ocupación de los eventos que inician en [desde, hasta).
    // Si ningún evento del periodo tiene reservas confirmadas, devuelve lista vacía.
    @Transactional(readOnly = true)
    public List<OcupacionEventoResponse> reporteOcupacion(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Los parámetros 'desde' y 'hasta' son obligatorios");
        }
        if (!desde.isBefore(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' debe ser anterior a la fecha 'hasta'");
        }

        List<OcupacionEventoResponse> reporte = eventoRepository.findByPeriodo(desde, hasta).stream()
                .map(e -> {
                    long reservados = reservaEventoRepository
                            .countByEventoIdAndEstado(e.getId(), EstadoReserva.confirmada);
                    return new OcupacionEventoResponse(e.getId(), e.getNombre(), e.getCapacidad(),
                            reservados, porcentajeOcupacion(reservados, e.getCapacidad()));
                })
                .toList();

        boolean hayReservas = reporte.stream().anyMatch(o -> o.getReservados() > 0);
        return hayReservas ? reporte : List.of();
    }

    // Porcentaje con 1 decimal; capacidad 0 o nula -> 0 (sin dividir por cero)
    private double porcentajeOcupacion(long reservados, Integer capacidad) {
        if (capacidad == null || capacidad <= 0) {
            return 0.0;
        }
        return Math.round(reservados * 1000.0 / capacidad) / 10.0;
    }

    // Único punto para obtener "ahora", en la misma zona en que se mapea Evento.inicio
    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }

    // Las entidades mapean timestamptz como LocalDateTime en la zona de la JVM
    private LocalDateTime aLocal(OffsetDateTime fecha) {
        return fecha.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private OffsetDateTime aOffset(LocalDateTime fecha) {
        return fecha != null ? fecha.atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
    }
}