package Fabrica_EAP05.Reservas.Controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Fabrica_EAP05.Reservas.DTO.CrearReservaRequest;
import Fabrica_EAP05.Reservas.DTO.CrearReservaResponse;
import Fabrica_EAP05.Reservas.Service.ReservaServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservas", description = "Reservas de servicios")
public class ReservaServicioController {

    @Autowired
    private ReservaServicioService reservaServicioService;

    @Operation(summary = "Crear reserva",
            description = "Reserva un servicio en un horario disponible para el cliente autenticado. "
                    + "Si el servicio requiere recurso, se asigna automáticamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva confirmada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, o servicio/horario inexistente o inactivo"),
            @ApiResponse(responseCode = "401", description = "Sin autenticación"),
            @ApiResponse(responseCode = "403", description = "Sin permiso (requiere rol cliente)"),
            @ApiResponse(responseCode = "409", description = "El horario o el recurso requerido no está disponible")
    })
    @PostMapping("/reservar")
    @PreAuthorize("hasRole('cliente')")
    public ResponseEntity<?> crearReserva(@Valid @RequestBody CrearReservaRequest request,
                                          @AuthenticationPrincipal String email) {
        try {
            CrearReservaResponse response = reservaServicioService.crearReserva(email, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (DataAccessException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("[crearReserva] " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno del servidor"));
        }
    }
    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('cliente')")
    public ResponseEntity<?> cancelarReserva(@PathVariable UUID id,
                                            @AuthenticationPrincipal String email) {
        try {
            return ResponseEntity.ok(reservaServicioService.cancelarReserva(email, id));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", e.getMessage()));
        }
    }
}
