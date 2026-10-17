package Fabrica_EAP05.Reservas.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import Fabrica_EAP05.Reservas.DTO.HorarioRequest;
import Fabrica_EAP05.Reservas.DTO.HorarioResponse;
import Fabrica_EAP05.Reservas.Entities.Horario;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;
import Fabrica_EAP05.Reservas.Service.HorarioService;
import jakarta.validation.Valid;

import java.util.Collections;
import java.util.UUID;

@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    @Autowired
    private HorarioService horarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('proveedor', 'PROVEEDOR')")
    public ResponseEntity<?> registrarHorario(@Valid @RequestBody HorarioRequest request, Authentication authentication) {
        try {
            // 1. Obtenemos el email del token JWT
            String emailProveedor = authentication.getName();

            // 2. Buscamos el usuario en la BD para sacar su UUID real
            Usuario proveedor = usuarioRepository.findByEmail(emailProveedor)
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró el proveedor logueado"));

            // 3. Asignamos el ID directamente al request
            request.setProveedorId(proveedor.getId());

            // 4. Invocamos tu servicio original
            Horario nuevoHorario = horarioService.registrarHorario(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(HorarioResponse.fromEntity(nuevoHorario));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/bloquear")
    @PreAuthorize("hasAnyRole('proveedor', 'PROVEEDOR')")
    public ResponseEntity<?> bloquearHorario(@PathVariable("id") UUID id, Authentication authentication) {
        try {
            String emailProveedor = authentication.getName();
            Horario horarioBloqueado = horarioService.bloquearHorario(id, emailProveedor);
            return ResponseEntity.ok(HorarioResponse.fromEntity(horarioBloqueado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}