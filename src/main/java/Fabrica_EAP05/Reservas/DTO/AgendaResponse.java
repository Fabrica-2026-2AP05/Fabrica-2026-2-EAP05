package Fabrica_EAP05.Reservas.DTO;

import java.time.LocalDateTime;
import java.util.UUID;

public class AgendaResponse {
    private UUID id;
    private UUID clienteId;
    private UUID servicioId;
    private UUID horarioId;
    private String estado;
    private LocalDateTime creadaEn;

    public AgendaResponse() {}

    public AgendaResponse(UUID id, UUID clienteId, UUID servicioId, UUID horarioId, String estado, LocalDateTime creadaEn) {
        this.id = id;
        this.clienteId = clienteId;
        this.servicioId = servicioId;
        this.horarioId = horarioId;
        this.estado = estado;
        this.creadaEn = creadaEn;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getClienteId() { return clienteId; }
    public void setClienteId(UUID clienteId) { this.clienteId = clienteId; }

    public UUID getServicioId() { return servicioId; }
    public void setServicioId(UUID servicioId) { this.servicioId = servicioId; }

    public UUID getHorarioId() { return horarioId; }
    public void setHorarioId(UUID horarioId) { this.horarioId = horarioId; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getCreadaEn() { return creadaEn; }
    public void setCreadaEn(LocalDateTime creadaEn) { this.creadaEn = creadaEn; }
}