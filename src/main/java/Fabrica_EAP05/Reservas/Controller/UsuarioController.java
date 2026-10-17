package Fabrica_EAP05.Reservas.Controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import Fabrica_EAP05.Reservas.DTO.UsuarioRegistroRequest;
import Fabrica_EAP05.Reservas.DTO.UsuarioRegistroResponse;
import Fabrica_EAP05.Reservas.Entities.Rol;
import Fabrica_EAP05.Reservas.Service.IUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/usuario")
@Tag(name = "Usuarios", description = "Registro de clientes y proveedores")
public class UsuarioController {

    @Autowired
    private IUsuarioService usuarioService;

    @Operation(summary = "Registrar cliente",
            description = "Registro público de un usuario con rol cliente. Se envía un correo para establecer la contraseña.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "El email ya está registrado")
    })
    @SecurityRequirements()
    @PostMapping("/registrar")
    public ResponseEntity<UsuarioRegistroResponse> registrarCliente(@Valid @RequestBody UsuarioRegistroRequest request) {
        UsuarioRegistroResponse response = usuarioService.registrarUsuario(request, Rol.cliente);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Registrar proveedor",
            description = "Registra un usuario con rol proveedor. Solo administradores.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proveedor registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Sin autenticación"),
            @ApiResponse(responseCode = "403", description = "Sin permiso (requiere rol administrador)"),
            @ApiResponse(responseCode = "409", description = "El email ya está registrado")
    })
    @PostMapping("/registrar/proveedor")
    @PreAuthorize("hasRole('administrador')")
    public ResponseEntity<UsuarioRegistroResponse> registrarProveedor(@Valid @RequestBody UsuarioRegistroRequest request) {
        UsuarioRegistroResponse response = usuarioService.registrarUsuario(request, Rol.proveedor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
