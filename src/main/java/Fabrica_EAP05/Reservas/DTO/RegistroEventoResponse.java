package Fabrica_EAP05.Reservas.DTO;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RegistroEventoResponse {
    private UUID id;
    private String nombre;
    private String descripcion;
    private OffsetDateTime inicio;
    private OffsetDateTime fin;
    private Integer capacidad;
    private Boolean activo;
    private OffsetDateTime createdAt;
}