package Fabrica_EAP05.Reservas.DTO;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// HU27: cantidad de reservas de un servicio en el periodo consultado
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsoServicioResponse {

    private UUID servicioId;
    private String nombre;
    private long reservas;
}
