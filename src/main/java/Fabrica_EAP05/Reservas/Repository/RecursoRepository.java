package Fabrica_EAP05.Reservas.Repository;


import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import Fabrica_EAP05.Reservas.Entities.Recurso;
import jakarta.persistence.LockModeType;

public interface RecursoRepository extends JpaRepository<Recurso, UUID> {
    boolean existsByNombre(String nombre);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Recurso r WHERE r.id = :id")
    Optional<Recurso> findByIdParaActualizar(@Param("id") UUID id);
}
 