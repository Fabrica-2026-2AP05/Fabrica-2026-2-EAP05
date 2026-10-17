package Fabrica_EAP05.Reservas.Controller;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Fabrica_EAP05.Reservas.DTO.ModificarServicioRequest;
import Fabrica_EAP05.Reservas.DTO.ModificarServicioResponse;
import Fabrica_EAP05.Reservas.DTO.RegistrarServicioRequest;
import Fabrica_EAP05.Reservas.DTO.RegistrarServicioResponse;
import Fabrica_EAP05.Reservas.Exception.RecursoDuplicadoException;
import Fabrica_EAP05.Reservas.Service.ServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@RestController
@RequestMapping("/api/servicios")
@Tag(name = "Servicios", description = "Registro y modificación de servicios. Solo administradores.")
public class ServicioController {

    @Autowired
    private ServicioService servicioService;

    @Getter
    @Setter
    public static class ValidarRecursoRequest {
        private UUID recursoId;
    }

    @Operation(summary = "Validar recurso",
            description = "Verifica que un recurso exista y esté activo antes de asociarlo a un servicio. "
                    + "Un recursoId nulo se considera válido (servicio sin recurso).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "El recurso es válido y está activo"),
            @ApiResponse(responseCode = "400", description = "El recurso no existe o no está activo"),
            @ApiResponse(responseCode = "401", description = "Sin autenticación"),
            @ApiResponse(responseCode = "403", description = "Sin permiso (requiere rol administrador)")
    })
    @PostMapping("/validar-recurso")
    @PreAuthorize("hasRole('administrador')")
    public ResponseEntity<?> validarRecurso(@RequestBody ValidarRecursoRequest request) {
        try {
            servicioService.validarRecursoRequerido(request.getRecursoId());
            return ResponseEntity.ok("El recurso es válido y está activo");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Registrar servicio",
            description = "Crea un servicio, opcionalmente asociado a un recurso activo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Servicio registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o recurso inexistente/inactivo"),
            @ApiResponse(responseCode = "401", description = "Sin autenticación"),
            @ApiResponse(responseCode = "403", description = "Sin permiso (requiere rol administrador)"),
            @ApiResponse(responseCode = "409", description = "Ya existe un servicio con ese nombre")
    })
    @PostMapping("/registrar")
    @PreAuthorize("hasRole('administrador')")
    public ResponseEntity<?> registrarServicio(@Valid @RequestBody RegistrarServicioRequest request) {
        try {
            RegistrarServicioResponse response = servicioService.registrarServicio(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RecursoDuplicadoException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }

    @Operation(summary = "Modificar servicio",
            description = "Actualización parcial: solo se cambian los campos enviados. "
                    + "No se permite si el servicio tiene reservas asociadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servicio modificado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o recurso inexistente/inactivo"),
            @ApiResponse(responseCode = "401", description = "Sin autenticación"),
            @ApiResponse(responseCode = "403", description = "Sin permiso (requiere rol administrador)"),
            @ApiResponse(responseCode = "404", description = "El servicio no existe"),
            @ApiResponse(responseCode = "409", description = "El servicio tiene reservas asociadas o el nombre ya existe")
    })
    @PutMapping("/{servicioId}")
    @PreAuthorize("hasRole('administrador')")
    public ResponseEntity<?> modificarServicio(@PathVariable UUID servicioId,
                                               @Valid @RequestBody ModificarServicioRequest request) {
        try {
            ModificarServicioResponse response = servicioService.modificarServicio(servicioId, request);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException | RecursoDuplicadoException e) {
            // Servicio con reservas asociadas o nombre duplicado
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }
}
