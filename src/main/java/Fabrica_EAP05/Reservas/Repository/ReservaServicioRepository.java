package Fabrica_EAP05.Reservas.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import Fabrica_EAP05.Reservas.Entities.ReservaServicio;
import Fabrica_EAP05.Reservas.Enums.EstadoReserva;
import jakarta.persistence.LockModeType;

public interface ReservaServicioRepository extends JpaRepository<ReservaServicio, UUID> {

    long countByServicioId(UUID servicioId);

    List<ReservaServicio> findByServicioId(UUID servicioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReservaServicio r WHERE r.id = :id")
    Optional<ReservaServicio> findByIdParaActualizar(@Param("id") UUID id);

    // Consulta de agenda para la HU11: reservas asociadas a los horarios de un proveedor específico
    @Query("SELECT r FROM ReservaServicio r, Horario h " +
            "WHERE r.horarioId = h.id " +
            "AND h.proveedorId = :proveedorId")
    List<ReservaServicio> findByProveedorId(@Param("proveedorId") UUID proveedorId);

    @Query("SELECT COUNT(r) FROM ReservaServicio r, Horario h "
            + "WHERE r.horarioId = h.id "
            + "AND r.recursoId = :recursoId "
            + "AND r.estado = :estado "
            + "AND h.inicio < :fin AND h.fin > :inicio")
    long contarReservasEnRango(@Param("recursoId") UUID recursoId,
                               @Param("estado") EstadoReserva estado,
                               @Param("inicio") LocalDateTime inicio,
                               @Param("fin") LocalDateTime fin);

    default long contarReservasDeRecursoEnRango(UUID recursoId, LocalDateTime inicio, LocalDateTime fin) {
        return contarReservasEnRango(recursoId, EstadoReserva.confirmada, inicio, fin);
    }

    @Query("SELECT COUNT(r) FROM ReservaServicio r, Horario h "
            + "WHERE r.horarioId = h.id "
            + "AND r.recursoId = :recursoId "
            + "AND r.estado = :estado "
            + "AND h.fin > :ahora")
    long contarReservasPendientesDeRecurso(@Param("recursoId") UUID recursoId,
                                           @Param("estado") EstadoReserva estado,
                                           @Param("ahora") LocalDateTime ahora);

    default long contarReservasVigentesDeRecurso(UUID recursoId, LocalDateTime ahora) {
        return contarReservasPendientesDeRecurso(recursoId, EstadoReserva.confirmada, ahora);
    }

    // AQUÍ SE AGREGA: Consulta para comprobar si un horario tiene una reserva confirmada (HU10)
    boolean existsByHorarioIdAndEstado(UUID horarioId, EstadoReserva estado);
}