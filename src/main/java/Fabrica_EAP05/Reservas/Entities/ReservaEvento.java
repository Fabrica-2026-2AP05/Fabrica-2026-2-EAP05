package Fabrica_EAP05.Reservas.Entities;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import Fabrica_EAP05.Reservas.Enums.EstadoReserva;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reserva_evento")
public class ReservaEvento {

    @Id
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "evento_id", nullable = false)
    private UUID eventoId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", columnDefinition = "estado_reserva")
    private EstadoReserva estado = EstadoReserva.confirmada;

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public UUID getEventoId() {
        return eventoId;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setClienteId(UUID clienteId) {
        this.clienteId = clienteId;
    }

    public void setEventoId(UUID eventoId) {
        this.eventoId = eventoId;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }
}