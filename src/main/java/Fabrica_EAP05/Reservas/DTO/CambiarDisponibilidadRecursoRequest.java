package Fabrica_EAP05.Reservas.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class CambiarDisponibilidadRecursoRequest {
    @NotNull(message = "El campo 'activo' es obligatorio")
    private Boolean activo;
}