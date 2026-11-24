package Fabrica_EAP05.Reservas.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import Fabrica_EAP05.Reservas.DTO.HorarioDisponibleResponse;
import Fabrica_EAP05.Reservas.DTO.ModificarServicioRequest;
import Fabrica_EAP05.Reservas.DTO.ModificarServicioResponse;
import Fabrica_EAP05.Reservas.DTO.RegistrarServicioRequest;
import Fabrica_EAP05.Reservas.DTO.RegistrarServicioResponse;
import Fabrica_EAP05.Reservas.DTO.UsoServicioResponse;
import Fabrica_EAP05.Reservas.Exception.RecursoDuplicadoException;
import Fabrica_EAP05.Reservas.Service.ReservaServicioService;
import Fabrica_EAP05.Reservas.Service.ServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RestController
@RequestMapping("/api/servicios")
@Tag(name = "Servicios", description = "Registro, modificación y reporte de uso de servicios (administradores) "
        + "y consulta de disponibilidad (clientes).")
@RequiredArgsConstructor
public class ServicioController {

    private static final String CLAVE_ERROR = "error";
    private static final String PREFIJO_ERROR_INTERNO = "Error interno: ";

    private final ServicioService servicioService;

    private final ReservaServicioService reservaServicioService;

    // Declarada antes de las rutas /{servicioId} para que "uso" nunca se interprete como un id.
    // Los errores 400 (fechas inválidas o desde >= hasta) los arma GlobalExceptionHandler.
    @Operation(summary = "Consultar uso de servicios",
            description = "Cantidad de reservas por servicio creadas en el periodo [desde, hasta), "
                    + "incluidas las canceladas. Incluye los servicios sin reservas (0), ordenados de más "
                    + "a menos usado. Fechas en formato ISO, por ejemplo 2026-12-01T00:00:00. Solo administradores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de reservas por servicio, o "
                    + "{\"mensaje\": \"No existen datos de uso para el periodo consultado.\"} si no hay reservas"),
            @ApiResponse(responseCode = "400", description = "Fechas faltantes, con formato inválido, "
                    + "o 'desde' no es anterior a 'hasta'"),
            @ApiResponse(responseCode = "401", description = "Sin autenticación"),
            @ApiResponse(responseCode = "403", description = "Sin permiso (requiere rol administrador)")
    })
    @GetMapping("/uso")
    @PreAuthorize("hasRole('administrador')")
    public ResponseEntity<?> reporteUso(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        List<UsoServicioResponse> reporte = servicioService.reporteUso(desde, hasta);
        if (reporte.isEmpty()) {
            return ResponseEntity.ok(Map.of("mensaje", "No existen datos de uso para el periodo consultado."));
        }
        return ResponseEntity.ok(reporte);
    }

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
            return errorResponse(HttpStatus.BAD_REQUEST, CLAVE_ERROR, e.getMessage());
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
            return errorResponse(HttpStatus.CONFLICT, CLAVE_ERROR, e.getMessage());
        } catch (IllegalArgumentException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, CLAVE_ERROR, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, CLAVE_ERROR, PREFIJO_ERROR_INTERNO + e.getMessage());
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
            return errorResponse(HttpStatus.NOT_FOUND, CLAVE_ERROR, e.getMessage());
        } catch (IllegalStateException | RecursoDuplicadoException e) {
            // Servicio con reservas asociadas o nombre duplicado
            return errorResponse(HttpStatus.CONFLICT, CLAVE_ERROR, e.getMessage());
        } catch (IllegalArgumentException e) {
            return errorResponse(HttpStatus.BAD_REQUEST, CLAVE_ERROR, e.getMessage());
        } catch (Exception e) {
            return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, CLAVE_ERROR, PREFIJO_ERROR_INTERNO + e.getMessage());
        }
    }

    @Operation(summary = "Consultar disponibilidad de un servicio",
            description = "Lista los horarios futuros en estado disponible de los proveedores asignados "
                    + "al servicio. Si el servicio requiere recurso, solo se incluyen los horarios en que "
                    + "el recurso está activo, tiene disponibilidad que cubre el rango y no tiene reservas "
                    + "confirmadas. Si no hay horarios se retorna una lista vacía.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de horarios disponibles (puede estar vacía)"),
            @ApiResponse(responseCode = "400", description = "El servicioId no tiene formato UUID válido"),
            @ApiResponse(responseCode = "401", description = "Sin autenticación"),
            @ApiResponse(responseCode = "403", description = "Sin permiso (requiere rol cliente)"),
            @ApiResponse(responseCode = "404", description = "El servicio no existe"),
            @ApiResponse(responseCode = "409", description = "El servicio no está activo")
    })
    @GetMapping("/{servicioId}/disponibilidad")
    @PreAuthorize("hasRole('cliente')")
    public ResponseEntity<?> consultarDisponibilidad(@PathVariable UUID servicioId) {
        try {
            List<HorarioDisponibleResponse> horarios = reservaServicioService.consultarDisponibilidad(servicioId);
            return ResponseEntity.ok(horarios);
        } catch (NoSuchElementException e) {
            return errorResponse(HttpStatus.NOT_FOUND, CLAVE_ERROR, e.getMessage());
        } catch (IllegalStateException e) {
            return errorResponse(HttpStatus.CONFLICT, CLAVE_ERROR, e.getMessage());
        }
    }

    // Mismo cuerpo que antes: Map.of(clave, mensaje) con el status indicado
    private static ResponseEntity<Map<String, String>> errorResponse(HttpStatus status, String clave, String mensaje) {
        return ResponseEntity.status(status).body(Map.of(clave, mensaje));
    }
}
