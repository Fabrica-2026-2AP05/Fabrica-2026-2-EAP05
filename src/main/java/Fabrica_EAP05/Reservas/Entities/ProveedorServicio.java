package Fabrica_EAP05.Reservas.Entities;

import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "proveedor_servicio")
public class ProveedorServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Usuario proveedor;

    @ManyToOne
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    public ProveedorServicio() {
    }

    public ProveedorServicio(Usuario proveedor, Servicio servicio) {
        this.proveedor = proveedor;
        this.servicio = servicio;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Usuario getProveedor() {
        return proveedor;
    }

    public void setProveedor(Usuario proveedor) {
        this.proveedor = proveedor;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public void setServicio(Servicio servicio) {
        this.servicio = servicio;
    }
}