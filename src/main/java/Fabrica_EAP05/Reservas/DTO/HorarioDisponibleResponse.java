package Fabrica_EAP05.Reservas.DTO;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// HU14: horario que un cliente puede reservar para un servicio
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HorarioDisponibleResponse {

    private UUID id;
    private OffsetDateTime inicio;
    private OffsetDateTime fin;
    private UUID proveedorId;
}
