package Fabrica_EAP05.Reservas.Service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import Fabrica_EAP05.Reservas.Config.JwtUtil;
import Fabrica_EAP05.Reservas.DTO.LoginDTO;
import Fabrica_EAP05.Reservas.DTO.LoginResponseDTO;
import Fabrica_EAP05.Reservas.DTO.ResetPasswordRequest;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Exception.CredencialesInvalidasException;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private InactivityTrackingService inactivityTrackingService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SupabaseAuthService supabaseAuthService;

     @Autowired
    private MailService mailService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.anon-key}")
    private String supabaseAnonKey;

    @Value("${app.frontend.reset-password-url}")
    private String resetPasswordUrl;

    public LoginResponseDTO login(LoginDTO dto) {

        // 0. Verificar si el usuario está bloqueado ANTES de gastar una llamada a Supabase
        loginAttemptService.verificarBloqueo(dto.getEmail());

        // 1. Validar credenciales contra Supabase Auth
        try {
            validarCredencialesSupabase(dto.getEmail(), dto.getContrasena());
        } catch (CredencialesInvalidasException e) {
            loginAttemptService.registrarFallo(dto.getEmail());
            throw e;
        }

        // Login exitoso: resetear contador
        loginAttemptService.registrarExito(dto.getEmail());

        // 2. Buscar perfil local por email
        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail())
        .orElseThrow(() -> new CredencialesInvalidasException("Usuario no encontrado"));        
        // 3. Verificar estado del usuario en tu base de datos
        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new CredencialesInvalidasException("El usuario se encuentra inactivo");
        }

        // 4. Generar token y registrar actividad
        String token = jwtUtil.generarToken(usuario.getEmail(), usuario.getRol());
        inactivityTrackingService.registrarActividad(usuario.getEmail());

        return new LoginResponseDTO(token, usuario.getEmail(), usuario.getRol());
    }

    private void validarCredencialesSupabase(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", supabaseAnonKey);

        Map<String, String> body = Map.of(
                "email", email,
                "password", password
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(
                    supabaseUrl + "/auth/v1/token?grant_type=password",
                    request,
                    String.class
            );
        } catch (HttpClientErrorException e) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
    }

   public void solicitarRecuperacion(String email) {
    usuarioRepository.findByEmail(email).ifPresent(usuario -> {
        try {
            String token = supabaseAuthService.generarTokenRecovery(email);
            mailService.enviarEmailResetPassword(email, usuario.getNombre(), token);
        } catch (Exception e) {
            System.err.println("[AuthService] No se pudo enviar email de recuperación a "
                    + email + ": " + e.getMessage());
        }
    });
}
    
    public void resetPassword(ResetPasswordRequest dto) {
        String accessToken = supabaseAuthService.verificarTokenRecovery(dto.getToken());
        supabaseAuthService.actualizarPassword(accessToken, dto.getPassword());
    }

    public void logout(String email, String token) {
        inactivityTrackingService.cerrarSesion(email);

        if (token != null && !token.isEmpty()) {
            tokenBlacklistService.agregarTokenALista(token);
        }
    }
}
