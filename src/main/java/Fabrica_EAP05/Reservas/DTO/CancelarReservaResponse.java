package Fabrica_EAP05.Reservas.DTO;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CancelarReservaResponse {
    private UUID id;
    private String estado;
    private OffsetDateTime canceladaEn;
}