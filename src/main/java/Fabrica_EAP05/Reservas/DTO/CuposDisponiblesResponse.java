package Fabrica_EAP05.Reservas.DTO;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CuposDisponiblesResponse {
    private UUID eventoId;
    private Integer capacidad;
    private Integer cuposDisponibles;
    private Boolean hayCupos;
}