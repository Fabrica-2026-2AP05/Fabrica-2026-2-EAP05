package Fabrica_EAP05.Reservas.DTO;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// HU22: resultado de cancelar una reserva de evento
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancelarReservaEventoResponse {

    private UUID id;
    private UUID eventoId;
    private UUID clienteId;
    private String estado;
}
