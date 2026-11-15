package Fabrica_EAP05.Reservas.Repository;

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
}
