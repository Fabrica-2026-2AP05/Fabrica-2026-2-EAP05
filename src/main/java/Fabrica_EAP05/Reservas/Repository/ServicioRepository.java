package Fabrica_EAP05.Reservas.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import Fabrica_EAP05.Reservas.Entities.Servicio;

public interface ServicioRepository extends JpaRepository<Servicio, UUID> {
    boolean existsByNombre(String nombre);
    Optional<Servicio> findByNombre(String nombre);
    List<Servicio> findByRecursoId(UUID recursoId);
    List<Servicio> findByActivoTrue();
}
