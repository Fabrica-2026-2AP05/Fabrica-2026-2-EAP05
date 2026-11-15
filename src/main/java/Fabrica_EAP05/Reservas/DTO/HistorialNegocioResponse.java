package Fabrica_EAP05.Reservas.DTO;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class HistorialNegocioResponse {
    private UUID reservaId;
    private String tipo; // "SERVICIO" o "EVENTO"
    private UUID itemId; // servicioId o eventoId
    private String nombreItem; // nombre del servicio o del evento
    private UUID clienteId;
    private String nombreCliente;
    private OffsetDateTime inicio;
    private OffsetDateTime fin;
    private UUID recursoId; // null para eventos o servicios sin recurso
    private String nombreRecurso;
    private String estado;
    private OffsetDateTime creadaEn; // null en ReservaEvento si no maneja fecha de creación
}
