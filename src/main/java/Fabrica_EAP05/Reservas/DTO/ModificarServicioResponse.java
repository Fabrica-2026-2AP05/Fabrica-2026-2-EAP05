package Fabrica_EAP05.Reservas.DTO;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ModificarServicioResponse {
    private UUID id;
    private String nombre;
    private String descripcion;
    private Integer duracionMin;
    private UUID recursoId;
    private String recursoNombre;
    private Boolean activo;
    private OffsetDateTime createdAt;
}
