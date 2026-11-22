package Fabrica_EAP05.Reservas.DTO;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class HistorialReservaResponse {
    private UUID reservaId;
    private UUID servicioId;
    private String nombreServicio;
    private UUID horarioId;
    private OffsetDateTime inicio;
    private OffsetDateTime fin;
    private UUID recursoId;
    private String nombreRecurso;
    private String estado;
    private OffsetDateTime creadaEn;
}