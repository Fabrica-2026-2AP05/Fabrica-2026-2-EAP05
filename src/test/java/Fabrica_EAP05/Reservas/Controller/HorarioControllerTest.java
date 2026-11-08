package Fabrica_EAP05.Reservas.Controller;

import Fabrica_EAP05.Reservas.DTO.HorarioRequest;
import Fabrica_EAP05.Reservas.Entities.Horario;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;
import Fabrica_EAP05.Reservas.Service.HorarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HorarioControllerTest {
    @Mock private HorarioService horarioService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private Authentication authentication;
    @InjectMocks private HorarioController controller;
    private String email;
    private UUID proveedorId;
    private UUID horarioId;
    private Usuario proveedor;
    private HorarioRequest request;
    private Horario horario;

    @BeforeEach void setUp() {
        email = "proveedor@correo.com";
        proveedorId = UUID.randomUUID();
        horarioId = UUID.randomUUID();
        proveedor = new Usuario(); proveedor.setId(proveedorId);
        request = new HorarioRequest();
        request.setInicio(LocalDateTime.of(2026, 11, 1, 8, 0));
        request.setFin(LocalDateTime.of(2026, 11, 1, 9, 0));
        horario = new Horario(); horario.setId(horarioId); horario.setProveedorId(proveedorId);
        horario.setInicio(request.getInicio()); horario.setFin(request.getFin());
    }

    @Test void registrarHorario_datosValidos_retornaCreated() {
        // Arrange
        when(authentication.getName()).thenReturn(email);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.of(proveedor));
        when(horarioService.registrarHorario(request)).thenReturn(horario);
        // Act
        ResponseEntity<?> respuesta = controller.registrarHorario(request, authentication);
        // Assert
        assertEquals(HttpStatus.CREATED, respuesta.getStatusCode());
        assertEquals(proveedorId, request.getProveedorId());
    }

    @Test void registrarHorario_proveedorNoEncontrado_retornaBadRequest() {
        // Arrange
        when(authentication.getName()).thenReturn(email);
        when(usuarioRepository.findByEmail(email)).thenReturn(Optional.empty());
        // Act
        ResponseEntity<?> respuesta = controller.registrarHorario(request, authentication);
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        verifyNoInteractions(horarioService);
    }

    @Test void bloquearHorario_valido_retornaOk() {
        // Arrange
        when(authentication.getName()).thenReturn(email);
        when(horarioService.bloquearHorario(horarioId, email)).thenReturn(horario);
        // Act
        ResponseEntity<?> respuesta = controller.bloquearHorario(horarioId, authentication);
        // Assert
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
    }

    @Test void bloquearHorario_invalido_retornaBadRequest() {
        // Arrange
        when(authentication.getName()).thenReturn(email);
        when(horarioService.bloquearHorario(horarioId, email)).thenThrow(new IllegalArgumentException("Horario ajeno"));
        // Act
        ResponseEntity<?> respuesta = controller.bloquearHorario(horarioId, authentication);
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
    }

    @Test void bloquearHorario_conReserva_retornaConflict() {
        // Arrange
        when(authentication.getName()).thenReturn(email);
        when(horarioService.bloquearHorario(horarioId, email)).thenThrow(new IllegalStateException("Reserva confirmada"));
        // Act
        ResponseEntity<?> respuesta = controller.bloquearHorario(horarioId, authentication);
        // Assert
        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
    }
}
