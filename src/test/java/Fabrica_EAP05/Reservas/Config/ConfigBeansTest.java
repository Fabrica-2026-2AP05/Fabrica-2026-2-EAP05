package Fabrica_EAP05.Reservas.Config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ConfigBeansTest {

    @Test @DisplayName("OpenAPI publica la información y el esquema JWT")
    void openApi_contieneInformacionYSeguridadBearer() {
        OpenAPI api = new OpenApiConfig().customOpenAPI();
        SecurityScheme esquema = api.getComponents().getSecuritySchemes().get("bearerAuth");

        assertAll(
                () -> assertEquals("Gestor de Reservas API", api.getInfo().getTitle()),
                () -> assertEquals("1.0.0", api.getInfo().getVersion()),
                () -> assertTrue(api.getInfo().getDescription().contains("reservas")),
                () -> assertEquals(SecurityScheme.Type.HTTP, esquema.getType()),
                () -> assertEquals("bearer", esquema.getScheme()),
                () -> assertEquals("JWT", esquema.getBearerFormat()),
                () -> assertTrue(api.getSecurity().get(0).containsKey("bearerAuth"))
        );
    }

    @Test @DisplayName("AppConfig crea un RestTemplate utilizable")
    void appConfig_creaRestTemplate() {
        assertNotNull(new AppConfig().restTemplate());
    }

    @Test @DisplayName("SecurityConfig configura los orígenes, métodos y encabezados CORS")
    void corsConfigurationSource_contienePoliticaEsperada() {
        SecurityConfig config = new SecurityConfig(mock(JwtFilter.class));
        ReflectionTestUtils.setField(config, "corsAllowedOrigins",
                "https://frontend.test,http://localhost:5173");

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/test"));

        assertNotNull(cors);
        assertAll(
                () -> assertEquals(List.of("https://frontend.test", "http://localhost:5173"),
                        cors.getAllowedOrigins()),
                () -> assertTrue(cors.getAllowedMethods().containsAll(
                        List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"))),
                () -> assertEquals(List.of("*"), cors.getAllowedHeaders())
        );
    }

    @Test @DisplayName("SecurityConfig recorta espacios alrededor de los orígenes CORS")
    void corsConfigurationSource_recortaEspaciosEnOrigenes() {
        SecurityConfig config = new SecurityConfig(mock(JwtFilter.class));
        ReflectionTestUtils.setField(config, "corsAllowedOrigins",
                "https://frontend.test , http://localhost:5173");

        CorsConfiguration cors = config.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/test"));

        assertNotNull(cors);
        assertEquals(List.of("https://frontend.test", "http://localhost:5173"),
                cors.getAllowedOrigins());
        assertTrue(cors.getAllowedOrigins().stream().noneMatch(o -> o.contains(" ")));
    }

    @Test @DisplayName("El codificador BCrypt verifica la contraseña original")
    void passwordEncoder_codificaYVerificaPassword() {
        SecurityConfig config = new SecurityConfig(mock(JwtFilter.class));
        PasswordEncoder encoder = config.passwordEncoder();

        String hash = encoder.encode("ClaveSegura123");

        assertAll(
                () -> assertNotEquals("ClaveSegura123", hash),
                () -> assertTrue(encoder.matches("ClaveSegura123", hash)),
                () -> assertFalse(encoder.matches("OtraClave", hash))
        );
    }

    @Test @DisplayName("UserDetailsService rechaza autenticación basada en usuario y contraseña")
    void userDetailsService_siempreLanzaExcepcion() {
        SecurityConfig config = new SecurityConfig(mock(JwtFilter.class));

        UsernameNotFoundException error = assertThrows(UsernameNotFoundException.class,
                () -> config.userDetailsService().loadUserByUsername("usuario@test.com"));

        assertTrue(error.getMessage().contains("no usa autenticación"));
    }
}