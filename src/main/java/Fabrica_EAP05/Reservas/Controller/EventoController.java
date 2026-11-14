package Fabrica_EAP05.Reservas.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}