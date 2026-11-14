package Fabrica_EAP05.Reservas.Controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Fabrica_EAP05.Reservas.DTO.CuposDisponiblesResponse;
import Fabrica_EAP05.Reservas.DTO.DefinirCapacidadEventoRequest;
import Fabrica_EAP05.Reservas.DTO.ModificarEventoRequest;
import Fabrica_EAP05.Reservas.DTO.RegistrarEventoRequest;
import Fabrica_EAP05.Reservas.DTO.RegistroEventoResponse;
import Fabrica_EAP05.Reservas.Service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/eventos")
@Tag(name = "Eventos", description = "Gestión de eventos")
public class EventoController {

    @Autowired
    private EventoService eventoService;

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
    @Operation(summary = "Consultar cupos disponibles",
        description = "Muestra cuántos cupos quedan disponibles para el evento, según su capacidad y las reservas confirmadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cupos disponibles consultados"),
            @ApiResponse(responseCode = "401", description = "Sin autenticación"),
            @ApiResponse(responseCode = "404", description = "El evento no existe")
    })
    @GetMapping("/{id}/cupos")
    public ResponseEntity<CuposDisponiblesResponse> consultarCupos(@PathVariable UUID id) {
        return ResponseEntity.ok(eventoService.consultarCupos(id));
    }
}