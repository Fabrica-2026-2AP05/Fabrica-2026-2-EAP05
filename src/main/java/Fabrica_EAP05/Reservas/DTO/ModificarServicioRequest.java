package Fabrica_EAP05.Reservas.DTO;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/**
 * Todos los campos son opcionales: solo se actualizan los que vengan con valor.
 * Las validaciones (nombre no vacío, duración > 0) se hacen en el servicio,
 * después de verificar que el servicio no tenga reservas asociadas.
 */
@Getter
@Setter
public class ModificarServicioRequest {

    private String nombre;

    private String descripcion;

    private Integer duracionMin;

    private UUID recursoId;
}
