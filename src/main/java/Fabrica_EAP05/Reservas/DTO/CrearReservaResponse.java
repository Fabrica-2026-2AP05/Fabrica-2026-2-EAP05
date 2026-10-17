package Fabrica_EAP05.Reservas.DTO;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CrearReservaResponse {
    private UUID id;
    private UUID clienteId;
    private UUID servicioId;
    private String servicioNombre;
    private UUID horarioId;
    private OffsetDateTime horarioInicio;
    private OffsetDateTime horarioFin;
    private UUID recursoId;
    private String recursoNombre;
    private String estado;
    private OffsetDateTime creadaEn;
}
