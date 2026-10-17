package Fabrica_EAP05.Reservas.DTO;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DisponibilidadRecursoResponse {
    private UUID id;
    private UUID recursoId;
    private OffsetDateTime inicio;
    private OffsetDateTime fin;
}