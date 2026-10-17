package Fabrica_EAP05.Reservas.Repository;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import Fabrica_EAP05.Reservas.Entities.Horario;
import jakarta.persistence.LockModeType;

public interface HorarioRepository extends JpaRepository<Horario, UUID> {

    // SELECT ... FOR UPDATE: evita que dos clientes reserven el mismo horario a la vez
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Horario h WHERE h.id = :id")
    Optional<Horario> findByIdParaActualizar(@Param("id") UUID id);

    // Método nuevo para la HU09 (Validación de solapamiento al crear horario)
    @Query("SELECT h FROM Horario h WHERE h.proveedorId = :proveedorId " +
            "AND (:inicio < h.fin AND :fin > h.inicio)")
    List<Horario> findSolapados(
            @Param("proveedorId") UUID proveedorId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    // NUEVO MÉTODO PARA HU10 (Bloquear Horario)
    Optional<Horario> findByIdAndProveedorId(UUID id, UUID proveedorId);
}
