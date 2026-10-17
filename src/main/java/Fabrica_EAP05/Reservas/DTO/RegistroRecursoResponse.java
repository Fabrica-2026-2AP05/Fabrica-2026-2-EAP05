package Fabrica_EAP05.Reservas.DTO;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistroRecursoResponse {
    private UUID id;
    private String nombre;
    private String descripcion;
    private boolean activo;
    private OffsetDateTime createdAt;

    public RegistroRecursoResponse(UUID id, String nombre, String descripcion, boolean activo,
                                     OffsetDateTime createdAt) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = activo;
        this.createdAt = createdAt;
    }
    
}
