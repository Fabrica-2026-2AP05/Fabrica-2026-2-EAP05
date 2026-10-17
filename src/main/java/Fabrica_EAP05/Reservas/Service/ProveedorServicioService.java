package Fabrica_EAP05.Reservas.Service;

import Fabrica_EAP05.Reservas.DTO.AsignarProveedorResponse;
import Fabrica_EAP05.Reservas.Entities.ProveedorServicio;
import Fabrica_EAP05.Reservas.Entities.Servicio;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Repository.ProveedorServicioRepository;
import Fabrica_EAP05.Reservas.Repository.ServicioRepository;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProveedorServicioService {

    @Autowired
    private ProveedorServicioRepository proveedorServicioRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional
    public AsignarProveedorResponse asignarProveedorAServicio(UUID servicioId, UUID proveedorId) {
        // 1. Validar que el servicio existe
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RuntimeException("El servicio especificado no existe."));

        // 2. Validar que el usuario/proveedor existe
        Usuario proveedor = usuarioRepository.findById(proveedorId)
                .orElseThrow(() -> new RuntimeException("El proveedor especificado no existe."));

        // 3. Criterio de Aceptación 2: Validar si la asociación ya existe
        boolean yaAsignado = proveedorServicioRepository.existsByProveedorIdAndServicioId(proveedorId, servicioId);
        if (yaAsignado) {
            throw new RuntimeException("El proveedor ya se encuentra asignado a este servicio.");
        }

        // 4. Criterio de Aceptación 1: Asignación exitosa
        ProveedorServicio nuevaAsignacion = new ProveedorServicio(proveedor, servicio);
        ProveedorServicio guardado = proveedorServicioRepository.save(nuevaAsignacion);

        // 5. Retornar DTO de respuesta estructurado
        return new AsignarProveedorResponse(
                guardado.getId(),
                servicio.getId(),
                servicio.getNombre(),
                proveedor.getId(),
                proveedor.getNombre(),
                "Proveedor asignado al servicio exitosamente."
        );
    }
}