package Fabrica_EAP05.Reservas.Controller;

import lombok.RequiredArgsConstructor;

import Fabrica_EAP05.Reservas.DTO.AsignarProveedorRequest;
import Fabrica_EAP05.Reservas.DTO.AsignarProveedorResponse;
import Fabrica_EAP05.Reservas.Service.ProveedorServicioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proveedor-servicio")
@RequiredArgsConstructor
public class ProveedorServicioController {

    private final ProveedorServicioService proveedorServicioService;

    @PostMapping("/asignar")
    @PreAuthorize("hasAnyRole('administrador', 'ADMIN')")
    public ResponseEntity<?> asignarProveedor(@RequestBody AsignarProveedorRequest request) {
        try {
            AsignarProveedorResponse response = proveedorServicioService.asignarProveedorAServicio(
                    request.getServicioId(),
                    request.getProveedorId()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}