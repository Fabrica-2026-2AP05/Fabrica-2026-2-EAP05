package Fabrica_EAP05.Reservas.DTO;

import java.util.UUID;

public class AsignarProveedorResponse {

    private UUID id;
    private UUID servicioId;
    private String nombreServicio;
    private UUID proveedorId;
    private String nombreProveedor;
    private String mensaje;

    public AsignarProveedorResponse() {
    }

    public AsignarProveedorResponse(UUID id, UUID servicioId, String nombreServicio, UUID proveedorId, String nombreProveedor, String mensaje) {
        this.id = id;
        this.servicioId = servicioId;
        this.nombreServicio = nombreServicio;
        this.proveedorId = proveedorId;
        this.nombreProveedor = nombreProveedor;
        this.mensaje = mensaje;
    }

    // Getters y Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getServicioId() {
        return servicioId;
    }

    public void setServicioId(UUID servicioId) {
        this.servicioId = servicioId;
    }

    public String getNombreServicio() {
        return nombreServicio;
    }

    public void setNombreServicio(String nombreServicio) {
        this.nombreServicio = nombreServicio;
    }

    public UUID getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(UUID proveedorId) {
        this.proveedorId = proveedorId;
    }

    public String getNombreProveedor() {
        return nombreProveedor;
    }

    public void setNombreProveedor(String nombreProveedor) {
        this.nombreProveedor = nombreProveedor;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}