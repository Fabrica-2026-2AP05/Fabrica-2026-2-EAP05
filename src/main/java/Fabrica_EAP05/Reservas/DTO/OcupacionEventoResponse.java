package Fabrica_EAP05.Reservas.DTO;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// HU28: ocupación de un evento (cupos reservados y porcentaje respecto a su capacidad)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OcupacionEventoResponse {

    private UUID eventoId;
    private String nombre;
    private Integer capacidad;
    private long reservados;
    private double porcentajeOcupacion;
}
