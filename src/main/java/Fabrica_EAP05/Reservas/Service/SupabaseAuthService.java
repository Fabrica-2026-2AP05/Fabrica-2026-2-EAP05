package Fabrica_EAP05.Reservas.Service;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import Fabrica_EAP05.Reservas.Exception.RecursoDuplicadoException;

// Usa el Admin API de Supabase (requiere la service_role key, nunca la anon
// key) para crear/eliminar filas en auth.users. public.usuario tiene un FK
// hacia auth.users, así que cualquier id insertado ahí debe existir primero
// en Supabase Auth.
@Service
public class SupabaseAuthService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    @Value("${supabase.anon-key}")
    private String anonKey;

    public UUID crearUsuarioAuth(String email, String password) {
    HttpHeaders headers = headersAdmin();

    Map<String, Object> body = Map.of(
            "email", email,
            "password", password,
            "email_confirm", true
    );

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    try {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                supabaseUrl + "/auth/v1/admin/users",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || responseBody.get("id") == null) {
            throw new IllegalStateException("Supabase no devolvió un id de usuario al crear en Auth");
        }

        String id = (String) responseBody.get("id");
        return UUID.fromString(id);
    } catch (HttpClientErrorException e) {
        if (e.getStatusCode().value() == 422 || e.getStatusCode().value() == 400) {
            throw new RecursoDuplicadoException("El email ya está registrado en Supabase Auth");
        }
        throw new IllegalStateException("No se pudo crear el usuario en Supabase Auth: " + e.getStatusCode(), e);
    }
}

    // Genera un token de recovery vía el Admin API (no envía ningún correo:
    // Supabase solo lo genera y lo devuelve). Nosotros armamos y mandamos el
    // email nosotros mismos con MailService/Mailtrap.
    public String generarTokenRecovery(String email) {
    HttpHeaders headers = headersAdmin();

    Map<String, Object> body = Map.of(
            "type", "recovery",
            "email", email
    );

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    try {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                supabaseUrl + "/auth/v1/admin/generate_link",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new IllegalStateException("Supabase no devolvió body en generate_link");
        }

        Object token = responseBody.get("hashed_token");
        if (token == null) {
            throw new IllegalStateException("Supabase no devolvió hashed_token en generate_link");
        }
        return token.toString();
    } catch (HttpClientErrorException e) {
        throw new IllegalStateException("No se pudo generar el link de recovery: " + e.getStatusCode(), e);
    }
    }   

    // Intercambia el token de recovery por una sesión (access_token) del
    // usuario dueño de ese email. Si el token es inválido/expirado/ya usado,
    // Supabase responde 4xx y lo traducimos a IllegalArgumentException.
    public String verificarTokenRecovery(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("apikey", anonKey);

    Map<String, Object> body = Map.of(
        "type", "recovery",
        "token_hash", token.trim()
        );

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    try {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                supabaseUrl + "/auth/v1/verify",
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new IllegalArgumentException("Token de recuperación inválido o expirado");
        }

        Object accessToken = responseBody.get("access_token");
        if (accessToken == null) {
            throw new IllegalArgumentException("Token de recuperación inválido o expirado");
        }
        return accessToken.toString();
    } catch (HttpClientErrorException e) {
        throw new IllegalArgumentException("Token de recuperación inválido o expirado");
    }
    }

    // Requiere el access_token de la sesión obtenida en verificarTokenRecovery
    // (representa al usuario, no al admin) para cambiar su propia password.
    public void actualizarPassword(String accessToken, String nuevaPassword) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", anonKey);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = Map.of("password", nuevaPassword);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(
                    supabaseUrl + "/auth/v1/user",
                    HttpMethod.PUT,
                    request,
                    Void.class
            );
        } catch (HttpClientErrorException e) {
            throw new IllegalStateException("No se pudo actualizar la contraseña: " + e.getStatusCode(), e);
        }
    }

    // Compensación best-effort: si falla el insert local después de crear el
    // usuario en Supabase Auth, se intenta borrar para no dejar huérfanos.
    // Un fallo aquí no debe ocultar la excepción original.
    public void eliminarUsuarioAuth(UUID id) {
        HttpHeaders headers = headersAdmin();
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(
                    supabaseUrl + "/auth/v1/admin/users/" + id,
                    HttpMethod.DELETE,
                    request,
                    Void.class
            );
        } catch (Exception e) {
            System.err.println("[SupabaseAuthService] No se pudo limpiar auth.users id=" + id + ": " + e.getMessage());
        }
    }

    private HttpHeaders headersAdmin() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", serviceRoleKey);
        headers.setBearerAuth(serviceRoleKey);
        return headers;
    }
}
