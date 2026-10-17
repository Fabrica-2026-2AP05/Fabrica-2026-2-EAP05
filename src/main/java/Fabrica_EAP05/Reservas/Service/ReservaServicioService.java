package Fabrica_EAP05.Reservas.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
            .orElseThrow(() -> new IllegalArgumentException("La reserva no existe"));

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
}
