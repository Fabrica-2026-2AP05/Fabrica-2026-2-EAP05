package Fabrica_EAP05.Reservas.Entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import Fabrica_EAP05.Reservas.Enums.EstadoHorario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "horario")
public class Horario {

    @Id
    private UUID id;

    @NotNull(message = "El proveedor es obligatorio")
    @Column(name = "proveedor_id", nullable = false)
    private UUID proveedorId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Column(name = "inicio", nullable = false)
    private LocalDateTime inicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    @Column(name = "fin", nullable = false)
    private LocalDateTime fin;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado", columnDefinition = "estado_horario")
    private EstadoHorario estado = EstadoHorario.disponible;

    public Horario() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(UUID proveedorId) {
        this.proveedorId = proveedorId;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public void setFin(LocalDateTime fin) {
        this.fin = fin;
    }

    public EstadoHorario getEstado() {
        return estado;
    }

    public void setEstado(EstadoHorario estado) {
        this.estado = estado;
    }
}
