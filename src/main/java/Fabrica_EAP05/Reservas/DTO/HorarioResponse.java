package Fabrica_EAP05.Reservas.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

import Fabrica_EAP05.Reservas.Entities.Horario;
import Fabrica_EAP05.Reservas.Enums.EstadoHorario;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HorarioResponse {

    private UUID id;
    private UUID proveedorId;
    private LocalDateTime inicio;
    private LocalDateTime fin;
    private EstadoHorario estado;

    public static HorarioResponse fromEntity(Horario horario) {
        return new HorarioResponse(
                horario.getId(),
                horario.getProveedorId(),
                horario.getInicio(),
                horario.getFin(),
                horario.getEstado()
        );
    }
}