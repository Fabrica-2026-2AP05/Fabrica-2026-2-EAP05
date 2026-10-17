package Fabrica_EAP05.Reservas.DTO;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * El recursoId no se envía: el servidor lo determina a partir del servicio.
 */
@Getter
@Setter
public class CrearReservaRequest {

    @NotNull(message = "El servicio es obligatorio")
    private UUID servicioId;

    @NotNull(message = "El horario es obligatorio")
    private UUID horarioId;
}
