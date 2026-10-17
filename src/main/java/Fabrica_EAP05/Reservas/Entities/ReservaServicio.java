package Fabrica_EAP05.Reservas.Entities;

import java.time.LocalDateTime;
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
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "reserva_servicio")
public class ReservaServicio {

    @Id
    private UUID id;

    @NotNull(message = "El cliente es obligatorio")
    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @NotNull(message = "El servicio es obligatorio")
    @Column(name = "servicio_id", nullable = false)
    private UUID servicioId;

    @NotNull(message = "El horario es obligatorio")
    @Column(name = "horario_id", nullable = false)
    private UUID horarioId;

    @Column(name = "recurso_id")
    private UUID recursoId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", columnDefinition = "estado_reserva")
    private EstadoReserva estado = EstadoReserva.confirmada;

    @Column(name = "creada_en", insertable = false, updatable = false)
    private LocalDateTime creadaEn;

    @Column(name = "cancelada_en")
    private LocalDateTime canceladaEn;

    public ReservaServicio() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public void setClienteId(UUID clienteId) {
        this.clienteId = clienteId;
    }

    public UUID getServicioId() {
        return servicioId;
    }

    public void setServicioId(UUID servicioId) {
        this.servicioId = servicioId;
    }

    public UUID getHorarioId() {
        return horarioId;
    }

    public void setHorarioId(UUID horarioId) {
        this.horarioId = horarioId;
    }

    public UUID getRecursoId() {
        return recursoId;
    }

    public void setRecursoId(UUID recursoId) {
        this.recursoId = recursoId;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public LocalDateTime getCanceladaEn() {
        return canceladaEn;
    }

    public void setCanceladaEn(LocalDateTime canceladaEn) {
        this.canceladaEn = canceladaEn;
    }
}
