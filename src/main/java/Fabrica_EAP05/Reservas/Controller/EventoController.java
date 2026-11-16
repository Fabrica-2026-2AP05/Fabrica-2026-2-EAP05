package Fabrica_EAP05.Reservas.Controller;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import Fabrica_EAP05.Reservas.DTO.CancelarReservaEventoResponse;
import Fabrica_EAP05.Reservas.DTO.DefinirCapacidadEventoRequest;
import Fabrica_EAP05.Reservas.DTO.ModificarEventoRequest;
import Fabrica_EAP05.Reservas.DTO.OcupacionEventoResponse;
import Fabrica_EAP05.Reservas.DTO.RegistrarEventoRequest;
import Fabrica_EAP05.Reservas.DTO.RegistroEventoResponse;
import Fabrica_EAP05.Reservas.DTO.ReservaEventoResponse;
import Fabrica_EAP05.Reservas.Service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos", description = "Gestión de eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService eventoService;

    // Declarada antes de las rutas /{id} para que "ocupacion" nunca se interprete como un id
    @Operation(summary = "Consultar ocupación de eventos",
            description = "Para cada evento que inicia en el periodo [desde, hasta) muestra la capacidad, "
                    + "los cupos reservados (reservas confirmadas) y el porcentaje de ocupación con 1 decimal. "
                    + "Fechas en formato ISO, por ejemplo 2026-12-01T00:00:00. Solo administradores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de ocupación por evento, o "
                    + "{\"mensaje\": \"No existen datos de ocupación para el periodo consultado.\"} si no hay reservas"),
            @ApiResponse(responseCode = "400", description = "Fechas faltantes, con formato inválido, "
                    + "o 'desde' no es anterior a 'hasta'"),
            @ApiResponse(responseCode = "401", description = "Sin autenticación"),
            @ApiResponse(responseCode = "403", description = "Sin permiso (requiere rol administrador)")
    })
    @GetMapping("/ocupacion")
    @PreAuthorize("hasRole('administrador')")
    public ResponseEntity<?> reporteOcupacion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        List<OcupacionEventoResponse> reporte = eventoService.reporteOcupacion(desde, hasta);
        if (reporte.isEmpty()) {
            return ResponseEntity.ok(Map.of("mensaje", "No existen datos de ocupación para el periodo consultado."));
        }
        return ResponseEntity.ok(reporte);
    }

    @Operation(summary = "Registrar evento",
            description = "Crea un nuevo evento. Solo administradores.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evento registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Sin autenticación"),
            @ApiResponse(responseCode = "403", description = "Sin permiso (requiere rol administrador)"),
            @ApiResponse(responseCode = "409", description = "Ya existe un evento con ese nombre")
    })
    @PostMapping("/registrar")
    @PreAuthorize("hasRole('administrador')")
    public ResponseEntity<RegistroEventoResponse> registrarEvento(@Valid @RequestBody RegistrarEventoRequest request) {
        RegistroEventoResponse response = eventoService.registrarEvento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Actualizar evento",
        description = "Modifica los datos generales del evento. Solo administradores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Sin autenticación"),
            @ApiResponse(responseCode = "403", description = "Sin permiso (requiere rol administrador)"),
            @ApiResponse(responseCode = "404", description = "El evento no existe"),
            @ApiResponse(responseCode = "409", description = "Ya existe otro evento con ese nombre")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('administrador')")
    public ResponseEntity<RegistroEventoResponse> actualizarEvento(
            @PathVariable UUID id,
            @Valid @RequestBody ModificarEventoRequest request) {
        return ResponseEntity.ok(eventoService.actualizarEvento(id, request));
    }
    @Operation(summary = "Definir capacidad del evento",
        description = "Cambia la capacidad máxima. Se rechaza si es menor a las reservas confirmadas existentes. Solo administradores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Capacidad actualizada"),
            @ApiResponse(responseCode = "400", description = "Capacidad inválida (debe ser mayor a 0)"),
            @ApiResponse(responseCode = "401", description = "Sin autenticación"),
            @ApiResponse(responseCode = "403", description = "Sin permiso (requiere rol administrador)"),
            @ApiResponse(responseCode = "404", description = "El evento no existe"),
            @ApiResponse(responseCode = "409", description = "La capacidad es menor a las reservas confirmadas existentes")
    })
    @PatchMapping("/{id}/capacidad")
    @PreAuthorize("hasRole('administrador')")
    public ResponseEntity<RegistroEventoResponse> definirCapacidad(
            @PathVariable UUID id,
            @Valid @RequestBody DefinirCapacidadEventoRequest request) {
        return ResponseEntity.ok(eventoService.definirCapacidad(id, request.getCapacidad()));
    }

    @Operation(summary = "Reservar cupo en evento",
        description = "Reserva un cupo del evento para el cliente autenticado (sin body; el cliente sale del token). "
                + "La reserva queda confirmada y los cupos disponibles disminuyen en uno.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva confirmada; incluye los cupos restantes"),
            @ApiResponse(responseCode = "400", description = "El id del evento no tiene formato UUID válido"),
            @ApiResponse(responseCode = "401", description = "Sin autenticación"),
            @ApiResponse(responseCode = "403", description = "Sin permiso (requiere rol cliente) o usuario inactivo"),
            @ApiResponse(responseCode = "404", description = "El evento no existe"),
            @ApiResponse(responseCode = "409", description = "No hay cupos disponibles, el evento no está activo "
                    + "o el cliente ya tiene una reserva confirmada en el evento")
    })
    @PostMapping("/{id}/reservas")
    @PreAuthorize("hasRole('cliente')")
    public ResponseEntity<ReservaEventoResponse> reservarCupo(@PathVariable UUID id,
                                                              Authentication authentication) {
        ReservaEventoResponse response = eventoService.reservarCupo(authentication.getName(), id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Cancelar reserva de evento",
        description = "Cancela una reserva confirmada del cliente autenticado y libera el cupo. "
                + "Solo se permite si faltan al menos 12 horas para el inicio del evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva cancelada; el cupo vuelve a estar disponible"),
            @ApiResponse(responseCode = "400", description = "Algún id no tiene formato UUID válido"),
            @ApiResponse(responseCode = "401", description = "Sin autenticación"),
            @ApiResponse(responseCode = "403", description = "Sin permiso (requiere rol cliente) o usuario inactivo"),
            @ApiResponse(responseCode = "404", description = "La reserva o el evento no existe, o la reserva es de otro cliente"),
            @ApiResponse(responseCode = "409", description = "La reserva no está confirmada o faltan menos de 12 horas "
                    + "para el inicio del evento")
    })
    @PostMapping("/{id}/reservas/{reservaId}/cancelar")
    @PreAuthorize("hasRole('cliente')")
    public ResponseEntity<CancelarReservaEventoResponse> cancelarReservaEvento(@PathVariable UUID id,
                                                                               @PathVariable UUID reservaId,
                                                                               Authentication authentication) {
        return ResponseEntity.ok(eventoService.cancelarReservaEvento(authentication.getName(), id, reservaId));
    }
}