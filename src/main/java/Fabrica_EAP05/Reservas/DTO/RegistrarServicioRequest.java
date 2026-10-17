package Fabrica_EAP05.Reservas.DTO;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrarServicioRequest {

    @NotBlank(message = "El nombre del servicio es obligatorio")
    private String nombre;

    private String descripcion;

    private Integer duracionMin;

    private UUID recursoId;
}
