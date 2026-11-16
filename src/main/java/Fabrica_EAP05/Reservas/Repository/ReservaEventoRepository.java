package Fabrica_EAP05.Reservas.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import Fabrica_EAP05.Reservas.Entities.ReservaEvento;
import Fabrica_EAP05.Reservas.Enums.EstadoReserva;

public interface ReservaEventoRepository extends JpaRepository<ReservaEvento, UUID> {
    long countByEventoIdAndEstado(UUID eventoId, EstadoReserva estado);

    // Reserva de cupo: ¿el cliente ya tiene una reserva en este estado para el evento?
    boolean existsByClienteIdAndEventoIdAndEstado(UUID clienteId, UUID eventoId, EstadoReserva estado);
}
