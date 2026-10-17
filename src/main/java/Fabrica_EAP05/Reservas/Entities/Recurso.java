package Fabrica_EAP05.Reservas.Entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity 
@Table(name = "recurso")
public class Recurso {

    @Id
    private UUID id;

    @NotBlank(message ="El nombre es necesario")
    @Column(name = "nombre", unique = true)
    private String nombre;


    @NotBlank (message = "La descripción es necesaria")
    private String descripcion;


    private Boolean activo = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
    

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
    
    public String getDescripcion() {
        return descripcion;
    }

    public Boolean getActivo(){
        return activo;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(UUID id){
        this.id = id;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
    
}
