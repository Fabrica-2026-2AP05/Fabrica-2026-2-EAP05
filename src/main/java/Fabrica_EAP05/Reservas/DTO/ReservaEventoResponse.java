package Fabrica_EAP05.Reservas.DTO;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Reserva de cupo en un evento
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservaEventoResponse {

    private UUID id;
    private UUID eventoId;
    private UUID clienteId;
    private String estado;
    private long cuposRestantes;
}
