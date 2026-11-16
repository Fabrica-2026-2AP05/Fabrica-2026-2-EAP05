package Fabrica_EAP05.Reservas.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import Fabrica_EAP05.Reservas.Entities.Evento;
import jakarta.persistence.LockModeType;

public interface EventoRepository extends JpaRepository<Evento, UUID> {
    boolean existsByNombre(String nombre);
    boolean existsByNombreAndIdNot(String nombre, UUID id);

    // SELECT ... FOR UPDATE: serializa las reservas de cupo de un mismo evento
    // para que dos clientes no tomen el último cupo a la vez
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Evento e WHERE e.id = :id")
    Optional<Evento> findByIdParaActualizar(@Param("id") UUID id);

    // HU28: eventos cuyo inicio está en [desde, hasta), ordenados por inicio
    @Query("SELECT e FROM Evento e WHERE e.inicio >= :desde AND e.inicio < :hasta ORDER BY e.inicio")
    List<Evento> findByPeriodo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
