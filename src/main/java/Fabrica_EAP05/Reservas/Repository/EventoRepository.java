package Fabrica_EAP05.Reservas.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import Fabrica_EAP05.Reservas.Entities.Evento;

public interface EventoRepository extends JpaRepository<Evento, UUID> {
    boolean existsByNombre(String nombre);
}