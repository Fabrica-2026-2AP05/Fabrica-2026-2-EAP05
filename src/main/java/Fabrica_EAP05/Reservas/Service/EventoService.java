package Fabrica_EAP05.Reservas.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Fabrica_EAP05.Reservas.DTO.ModificarEventoRequest;
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
import Fabrica_EAP05.Reservas.Exception.RecursoDuplicadoException;
import Fabrica_EAP05.Reservas.Exception.UsuarioInactivoException;
import Fabrica_EAP05.Reservas.Repository.EventoRepository;
import Fabrica_EAP05.Reservas.Repository.ReservaEventoRepository;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;

@Service
public class EventoService {

    @Autowired
    private ReservaEventoRepository reservaEventoRepository;
    @Autowired
    private EventoRepository eventoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

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

    // Las entidades mapean timestamptz como LocalDateTime en la zona de la JVM
    private LocalDateTime aLocal(OffsetDateTime fecha) {
        return fecha.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private OffsetDateTime aOffset(LocalDateTime fecha) {
        return fecha != null ? fecha.atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
    }
}