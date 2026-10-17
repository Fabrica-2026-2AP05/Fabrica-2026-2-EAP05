package Fabrica_EAP05.Reservas.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HorarioRequest {


    private UUID proveedorId;

    @NotNull(message = "La fecha y hora de inicio es obligatoria")
    private LocalDateTime inicio;

    @NotNull(message = "La fecha y hora de fin es obligatoria")
    private LocalDateTime fin;
}