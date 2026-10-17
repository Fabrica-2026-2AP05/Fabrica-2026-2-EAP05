package Fabrica_EAP05.Reservas.Entities;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "disponibilidad_recurso")
public class DisponibilidadRecurso {

    @Id
    private UUID id;

    @Column(name = "recurso_id", nullable = false)
    private UUID recursoId;

    @Column(name = "inicio", nullable = false)
    private LocalDateTime inicio;

    @Column(name = "fin", nullable = false)
    private LocalDateTime fin;

    public DisponibilidadRecurso() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRecursoId() {
        return recursoId;
    }

    public void setRecursoId(UUID recursoId) {
        this.recursoId = recursoId;
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
}
