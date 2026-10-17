package Fabrica_EAP05.Reservas.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import Fabrica_EAP05.Reservas.Entities.DisponibilidadRecurso;

public interface DisponibilidadRecursoRepository extends JpaRepository<DisponibilidadRecurso, UUID> {

    // inicio <= horarioInicio AND fin >= horarioFin:
    // períodos de disponibilidad que cubren por completo el rango del horario.
    List<DisponibilidadRecurso> findByRecursoIdAndInicioLessThanEqualAndFinGreaterThanEqual(
            UUID recursoId, LocalDateTime horarioInicio, LocalDateTime horarioFin);

    @Query("SELECT COUNT(d) FROM DisponibilidadRecurso d "
        + "WHERE d.recursoId = :recursoId AND d.inicio < :fin AND d.fin > :inicio")
    long contarSolapadas(@Param("recursoId") UUID recursoId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin);
}
