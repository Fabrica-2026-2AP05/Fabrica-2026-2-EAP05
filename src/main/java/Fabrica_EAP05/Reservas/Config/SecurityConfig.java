package Fabrica_EAP05.Reservas.Config;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Sin esto, un frontend en otro origen (ej. localhost:5173) nunca llega
    // al filtro de Spring Security: el navegador corta la petición en el
    // preflight y algunas herramientas de dev lo reportan como 403.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Desactivar CSRF intencionalmente para API REST Stateless
            .csrf(csrf -> csrf
                    .ignoringRequestMatchers("/api/**", "/swagger-ui/**", "/v3/api-docs/**")
            )

            // 2. Habilitar CORS con la configuración de arriba
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 3. Configurar permisos de rutas
            .authorizeHttpRequests(auth -> auth
                // El preflight CORS (OPTIONS) no matchea contra requestMatchers(String...)
                // porque Spring MVC no expone OPTIONS como handler real de la ruta;
                // sin esta línea, cualquier request con Content-Type: application/json
                // (que dispara preflight) cae en anyRequest().authenticated() -> 403.
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/usuario/registrar").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/horarios/**").hasAnyRole("proveedor", "PROVEEDOR")
                .requestMatchers(HttpMethod.POST, "/api/proveedor-servicio/asignar").hasAnyRole("administrador", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/agenda/**").hasAnyRole("proveedor", "PROVEEDOR")
                .requestMatchers("/api/horarios/**").hasAnyRole("proveedor", "PROVEEDOR")
                .requestMatchers("/error").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )

            // 4. Desactivar el formulario HTML por defecto y la autenticación básica
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // 5. Configurar manejo de sesión como Stateless (sin sesión HTTP, ideal para JWT)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            // TEMPORAL: quitar una vez identificada la causa del 403 en Render.
            // Corre después de JwtFilter para mostrar exactamente lo que va a
            // evaluar el AuthorizationFilter (authorizeHttpRequests) al final de la cadena.
            .exceptionHandling(exceptions -> exceptions
                // Sin token válido / no autenticado -> 401 con mensaje claro
                .authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(401);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                Map<String, Object> body = new HashMap<>();
                body.put("timestamp", OffsetDateTime.now().toString());
                body.put("mensaje", "No autenticado: debes iniciar sesion para acceder a este recurso");
                response.getWriter().write(objectMapper.writeValueAsString(body));
            })
            .accessDeniedHandler((request, response, accessDeniedException) -> {
                response.setStatus(403);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                Map<String, Object> body = new HashMap<>();
                body.put("timestamp", OffsetDateTime.now().toString());
                body.put("mensaje", "No tienes permiso para acceder a este recurso");
                response.getWriter().write(objectMapper.writeValueAsString(body));
            }));
            
        return http.build();
    }
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Este proyecto no usa autenticación por UserDetailsService");
        };
    }
}
