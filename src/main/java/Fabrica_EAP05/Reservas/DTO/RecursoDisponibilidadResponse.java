package Fabrica_EAP05.Reservas.DTO;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RecursoDisponibilidadResponse {
    private UUID id;
    private String nombre;
    private Boolean activo;
}