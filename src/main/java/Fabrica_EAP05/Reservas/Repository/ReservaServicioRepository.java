package Fabrica_EAP05.Reservas.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import Fabrica_EAP05.Reservas.DTO.ConteoReservasServicio;
import Fabrica_EAP05.Reservas.Entities.ReservaServicio;
import Fabrica_EAP05.Reservas.Enums.EstadoReserva;
import jakarta.persistence.LockModeType;

public interface ReservaServicioRepository extends JpaRepository<ReservaServicio, UUID> {

    long countByServicioId(UUID servicioId);

    List<ReservaServicio> findByServicioId(UUID servicioId);

    List<ReservaServicio> findByClienteIdOrderByCreadaEnDesc(UUID clienteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReservaServicio r WHERE r.id = :id")
    Optional<ReservaServicio> findByIdParaActualizar(@Param("id") UUID id);

    // Consulta de agenda para la HU11: reservas asociadas a los horarios de un proveedor específico
    @Query("SELECT r FROM ReservaServicio r, Horario h " +
            "WHERE r.horarioId = h.id " +
            "AND h.proveedorId = :proveedorId")
    List<ReservaServicio> findByProveedorId(@Param("proveedorId") UUID proveedorId);

    // Consulta de historial para la HU24 ordenada por fecha de creación
    @Query("SELECT r FROM ReservaServicio r, Horario h " +
            "WHERE r.horarioId = h.id " +
            "AND h.proveedorId = :proveedorId " +
            "ORDER BY r.creadaEn DESC")
    List<ReservaServicio> findHistorialByProveedorId(@Param("proveedorId") UUID proveedorId);

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

    // HU27: reservas por servicio creadas en [desde, hasta), sin filtrar por estado.
    // Solo devuelve servicios con al menos una reserva en el periodo.
    @Query("SELECT new Fabrica_EAP05.Reservas.DTO.ConteoReservasServicio(r.servicioId, COUNT(r)) "
            + "FROM ReservaServicio r "
            + "WHERE r.creadaEn >= :desde AND r.creadaEn < :hasta "
            + "GROUP BY r.servicioId")
    List<ConteoReservasServicio> contarReservasPorServicioEnPeriodo(@Param("desde") LocalDateTime desde,

                                                                   @Param("hasta") LocalDateTime hasta);

    // HU26: reservas confirmadas de un servicio cuyo horario cae en [desde, hasta)
    @Query("SELECT COUNT(r) FROM ReservaServicio r, Horario h " +
            "WHERE r.horarioId = h.id " +
            "AND r.servicioId = :servicioId " +
            "AND r.estado = :estado " +
            "AND h.inicio >= :desde " +
            "AND h.inicio < :hasta")
    long contarReservasConfirmadasPorServicioEnPeriodo(@Param("servicioId") UUID servicioId,
                                                       @Param("estado") EstadoReserva estado,
                                                       @Param("desde") LocalDateTime desde,
                                                       @Param("hasta") LocalDateTime hasta); 
}


