package Fabrica_EAP05.Reservas.DTO;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public class AsignarProveedorRequest {

    @NotNull(message = "El ID del servicio es obligatorio")
    private UUID servicioId;

    @NotNull(message = "El ID del proveedor es obligatorio")
    private UUID proveedorId;

    public UUID getServicioId() {
        return servicioId;
    }

    public void setServicioId(UUID servicioId) {
        this.servicioId = servicioId;
    }

    public UUID getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(UUID proveedorId) {
        this.proveedorId = proveedorId;
    }
}