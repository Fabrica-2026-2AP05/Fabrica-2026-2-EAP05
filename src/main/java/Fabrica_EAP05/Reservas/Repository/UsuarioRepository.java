package Fabrica_EAP05.Reservas.Repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import Fabrica_EAP05.Reservas.Entities.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    boolean existsByEmail(String email);
    Optional<Usuario> findByEmail(String email);
}

