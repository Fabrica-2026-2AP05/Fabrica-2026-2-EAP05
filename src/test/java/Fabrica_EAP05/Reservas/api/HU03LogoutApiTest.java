package Fabrica_EAP05.Reservas.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@Tag("api")
@ExtendWith(ResultadosExtension.class)
public class HU03LogoutApiTest {

    @BeforeAll
    public static void setup() {
        ApiTestConfig.ensureWarmup();
    }

    @BeforeEach
    public void checkApiAvailable() {
        Assumptions.assumeTrue(ApiTestConfig.isApiAvailable(), "API no disponible: " + ApiTestConfig.getWarmupError());
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-03-A", hu = "HU03", titulo = "Cierre exitoso")
    public void cp03A_cierreExitoso_responde200YSonda401() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("Logout 200 con texto plano 'Sesión cerrada exitosamente'; sonda antes del cierre != 401; sonda después del cierre responde 401");
        }

        String email = DatosPrueba.generarEmail("cp03a", 1);
        Response reg = ApiClient.registrarCliente("Registrar cliente para prueba de logout",
                DatosPrueba.bodyRegistro("Cliente CP03A", email, DatosPrueba.DEFAULT_PASSWORD, "3001234567", "Calle 1 # 2-3"));
        assertEquals(201, reg.getStatusCode(), "El prerrequisito de registro debe responder 201");

        Response login = ApiClient.login("Login para obtener token", email, DatosPrueba.DEFAULT_PASSWORD);
        assertEquals(200, login.getStatusCode(), "El login debe responder 200");
        String token = login.jsonPath().getString("token");
        assertNotNull(token, "El token no debe ser nulo");

        // Sonda antes del cierre
        Response sondaAntes = ApiClient.sondaSesionCliente("Sonda con sesión activa antes de logout", token);

        // Logout
        Response logout = ApiClient.logout("Logout con token válido", token);

        // Sonda después del cierre
        Response sondaDespues = ApiClient.sondaSesionCliente("Sonda con sesión cerrada después de logout", token);

        assertAll("Aserciones CP-03-A",
                () -> assertNotEquals(401, sondaAntes.getStatusCode(), "La sonda antes del cierre no debe responder 401"),
                () -> assertEquals(200, logout.getStatusCode(), "El logout debe responder 200"),
                () -> assertEquals("Sesión cerrada exitosamente", logout.getBody().asString().trim(),
                        "El cuerpo de logout debe ser 'Sesión cerrada exitosamente'"),
                () -> assertEquals(401, sondaDespues.getStatusCode(), "La sonda después de logout debe responder 401")
        );
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-03-B", hu = "HU03", titulo = "Token inválido o expirado")
    public void cp03B_tokenInvalidoOExpirado_responde401() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("(a) y (b) 401 con 'No hay sesión activa'; (c) 401 con 'Sesión expirada. Por favor, inicie sesión nuevamente'; ninguna 5xx");
        }

        // Variante (a): token inválido abc
        Response resA = ApiClient.logout("Logout con token inválido abc", "abc");

        // Variante (b): sin header Authorization
        Response resB = ApiClient.logout("Logout sin header Authorization", null);

        // Variante (c): token de sesión ya cerrada
        String emailC = DatosPrueba.generarEmail("cp03b", 1);
        Response regC = ApiClient.registrarCliente("Registrar cliente para variante (c)",
                DatosPrueba.bodyRegistro("Cliente CP03B", emailC, DatosPrueba.DEFAULT_PASSWORD, "3001234567", "Calle 1 # 2-3"));
        assertEquals(201, regC.getStatusCode(), "El registro previo debe responder 201");

        Response loginC = ApiClient.login("Login para variante (c)", emailC, DatosPrueba.DEFAULT_PASSWORD);
        assertEquals(200, loginC.getStatusCode(), "El login previo debe responder 200");
        String tokenC = loginC.jsonPath().getString("token");

        Response logoutPrevio = ApiClient.logout("Primer logout exitoso para cerrar sesión", tokenC);
        assertEquals(200, logoutPrevio.getStatusCode(), "El primer logout debe responder 200");

        // Segundo logout con token ya revocado
        Response resC = ApiClient.logout("Segundo logout con token de sesión ya cerrada", tokenC);

        assertAll("Aserciones CP-03-B",
                () -> assertEquals(401, resA.getStatusCode(), "Variante (a) debe responder 401"),
                () -> assertEquals("No hay sesión activa", resA.getBody().asString().trim(),
                        "Variante (a) debe responder 'No hay sesión activa'"),
                () -> assertTrue(resA.getStatusCode() < 500, "Variante (a) no debe ser 5xx"),

                () -> assertEquals(401, resB.getStatusCode(), "Variante (b) debe responder 401"),
                () -> assertEquals("No hay sesión activa", resB.getBody().asString().trim(),
                        "Variante (b) debe responder 'No hay sesión activa'"),
                () -> assertTrue(resB.getStatusCode() < 500, "Variante (b) no debe ser 5xx"),

                () -> assertEquals(401, resC.getStatusCode(), "Variante (c) debe responder 401"),
                () -> assertEquals("Sesión expirada. Por favor, inicie sesión nuevamente", resC.getBody().asString().trim(),
                        "Variante (c) debe responder 'Sesión expirada. Por favor, inicie sesión nuevamente'"),
                () -> assertTrue(resC.getStatusCode() < 500, "Variante (c) no debe ser 5xx")
        );
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-03-C", hu = "HU03", titulo = "Token revocado")
    public void cp03C_tokenRevocado_responde401YNoRegistraProveedor() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("401 con 'Sesión expirada. Por favor, inicie sesión nuevamente'; el proveedor no queda registrado (verificación §5.3)");
        }

        Assumptions.assumeTrue(ApiTestConfig.isAdminValid(), "sin token de administrador: " + ApiTestConfig.getAdminCheckMessage());

        // 1. Login admin
        Response loginAdmin = ApiClient.login("Login admin para revocar sesión", ApiTestConfig.getAdminEmail(), ApiTestConfig.getAdminPassword());
        assertEquals(200, loginAdmin.getStatusCode(), "Login admin debe responder 200");
        String tokenAdminRevocado = loginAdmin.jsonPath().getString("token");
        assertNotNull(tokenAdminRevocado, "Token admin no debe ser nulo");

        // 2. Logout con ese token admin
        Response logoutAdmin = ApiClient.logout("Logout de token admin", tokenAdminRevocado);
        assertEquals(200, logoutAdmin.getStatusCode(), "Logout admin debe responder 200");

        // 3. Intentar registrar proveedor con token revocado
        String emailProveedor = DatosPrueba.generarEmail("cp03c", 1);
        String bodyProveedor = DatosPrueba.bodyRegistro("Proveedor CP03C", emailProveedor, DatosPrueba.DEFAULT_PASSWORD, "3001234567", "Carrera 5 # 6-7");
        Response resProveedor = ApiClient.registrarProveedor("Registrar proveedor con token admin revocado", tokenAdminRevocado, bodyProveedor);

        // 4. Verificación de no queda registrado (§5.3)
        // Por BD si está configurada
        if (DbVerifier.isConfigured()) {
            int countBd = DbVerifier.countUsuarios(emailProveedor);
            assertEquals(0, countBd, "En base de datos no debe existir el proveedor con token revocado");
        }

        // Por API: obtener token admin fresco y registrar con datos válidos -> debe responder 201
        String tokenAdminFresco = ApiClient.obtenerTokenAdmin();
        assertNotNull(tokenAdminFresco, "Se requiere token admin fresco para verificar que la cuenta no quedó creada");
        Response verificacionApi = ApiClient.registrarProveedor("Verificación §5.3: registrar mismo correo con token fresco", tokenAdminFresco, bodyProveedor);

        assertAll("Aserciones CP-03-C",
                () -> assertEquals(401, resProveedor.getStatusCode(), "Debe responder 401 con token revocado"),
                () -> assertEquals("Sesión expirada. Por favor, inicie sesión nuevamente", resProveedor.getBody().asString().trim(),
                        "Mensaje esperado 'Sesión expirada. Por favor, inicie sesión nuevamente'"),
                () -> assertEquals(201, verificacionApi.getStatusCode(),
                        "Verificación §5.3: Registrar el correo después debe dar 201 (si da 409 quedó registrado erróneamente)")
        );
    }
}
