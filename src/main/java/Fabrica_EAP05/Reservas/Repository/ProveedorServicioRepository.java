package Fabrica_EAP05.Reservas.Repository;
import Fabrica_EAP05.Reservas.Entities.ProveedorServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProveedorServicioRepository extends JpaRepository<ProveedorServicio, UUID> {

    // Método para verificar si ya existe la asociación
    boolean existsByProveedorIdAndServicioId(UUID proveedorId, UUID servicioId);

    Optional<ProveedorServicio> findByProveedorIdAndServicioId(UUID proveedorId, UUID servicioId);

    // Método nuevo para TU historia HU09 (Validar que el proveedor tenga al menos un servicio)
    boolean existsByProveedorId(UUID proveedorId);
}