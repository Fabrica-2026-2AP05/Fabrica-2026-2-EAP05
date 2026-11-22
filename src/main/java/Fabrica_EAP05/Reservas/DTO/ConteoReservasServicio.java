package Fabrica_EAP05.Reservas.DTO;

import java.util.UUID;

// HU27: fila de la consulta agrupada (SELECT new ...) de reservas por servicio.
// Uso interno entre repositorio y servicio; no se expone en la API.
public record ConteoReservasServicio(UUID servicioId, Long reservas) {
}
