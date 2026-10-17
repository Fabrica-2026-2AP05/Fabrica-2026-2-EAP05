package Fabrica_EAP05.Reservas.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Gestor de Reservas API")
                .version("1.0.0")
                .description("API para gestionar reservas, servicios y proveedores"))
            // Requisito global: todos los endpoints llevan candado salvo los que
            // lo anulan con @SecurityRequirements() (login, registro, reset-password).
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    // Swagger UI antepone "Bearer " automáticamente
                    .description("Pega solo el JWT obtenido en /api/auth/login, sin el prefijo \"Bearer\".")));
    }
}
