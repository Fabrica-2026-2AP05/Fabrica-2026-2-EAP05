package Fabrica_EAP05.Reservas.DTO;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class OcupacionServicioResponse {
    private UUID servicioId;
    private String nombreServicio;
    private long totalHorarios;
    private long horariosReservados;
    private double porcentajeOcupacion;
}