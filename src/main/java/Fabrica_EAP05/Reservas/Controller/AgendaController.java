package Fabrica_EAP05.Reservas.Controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import Fabrica_EAP05.Reservas.DTO.AgendaResponse;
import Fabrica_EAP05.Reservas.Service.AgendaService;

@RestController
@RequestMapping("/api/agenda")
public class AgendaController {

    @Autowired
    private AgendaService agendaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('proveedor', 'PROVEEDOR')")
    public ResponseEntity<?> consultarAgenda(Authentication authentication) {
        try {
            String email = authentication.getName();
            List<AgendaResponse> agenda = agendaService.consultarAgendaProveedor(email);

            if (agenda.isEmpty()) {
                return ResponseEntity.ok(Collections.singletonMap("mensaje", "No existen reservas programadas."));
            }

            return ResponseEntity.ok(agenda);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}