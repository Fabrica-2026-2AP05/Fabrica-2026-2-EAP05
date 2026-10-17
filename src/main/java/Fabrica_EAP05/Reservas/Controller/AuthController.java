package Fabrica_EAP05.Reservas.Controller;


import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import Fabrica_EAP05.Reservas.DTO.ForgotPasswordRequest;
import Fabrica_EAP05.Reservas.DTO.LoginDTO;
import Fabrica_EAP05.Reservas.DTO.LoginResponseDTO;
import Fabrica_EAP05.Reservas.DTO.ResetPasswordRequest;
import Fabrica_EAP05.Reservas.Service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión, cierre de sesión y restablecimiento de contraseña")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "Iniciar sesión",
            description = "Valida las credenciales y devuelve un JWT para usar en el header Authorization: Bearer <token>.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso, devuelve el token"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (email o contraseña vacíos o con formato incorrecto)"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas o usuario inactivo"),
            @ApiResponse(responseCode = "429", description = "Cuenta bloqueada temporalmente por intentos fallidos")
    })
    @SecurityRequirements()
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginDTO dto) {
        LoginResponseDTO response = authService.login(dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest dto) {
        authService.solicitarRecuperacion(dto.getEmail());
        Map<String, String> body = new HashMap<>();
        body.put("mensaje", "Si el correo está registrado, recibirás instrucciones para restablecer tu contraseña.");
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Restablecer contraseña",
            description = "Cambia la contraseña usando el token de recuperación enviado por correo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Token de recuperación inválido o expirado")
    })
    @SecurityRequirements()
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest dto) {
        authService.resetPassword(dto);
        return ResponseEntity.ok("Contraseña actualizada correctamente");
    }

    @Operation(summary = "Cerrar sesión",
            description = "Invalida el JWT actual: queda en lista negra y no puede volver a usarse.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión cerrada exitosamente"),
            @ApiResponse(responseCode = "401", description = "No hay sesión activa")
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No hay sesión activa");
    }

    String token = null;
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        token = authHeader.replace("Bearer ", "");
    }

    String correo = (String) auth.getPrincipal();
    authService.logout(correo, token);

    SecurityContextHolder.clearContext();

    return ResponseEntity.ok("Sesión cerrada exitosamente");
}
}