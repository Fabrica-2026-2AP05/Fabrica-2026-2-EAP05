package Fabrica_EAP05.Reservas.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@Tag("api")
@ExtendWith(ResultadosExtension.class)
public class HU01LoginApiTest {

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
    @CasoPrueba(id = "CP-01-A", hu = "HU01", titulo = "Inicio exitoso")
    public void cp01A_inicioExitoso_responde200ConTokenYAccesoRol() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("200; token JWT con 3 partes; mensaje = 'Inicio de sesión exitoso'; usuario = email; rol = 'cliente'; sonda POST /api/reservas/reservar {} responde != 401/403");
        }

        String email = DatosPrueba.generarEmail("cp01a", 1);
        Response reg = ApiClient.registrarCliente("Registrar cliente para login",
                DatosPrueba.bodyRegistro("Cliente CP01A", email, DatosPrueba.DEFAULT_PASSWORD, "3001234567", "Calle 1 # 2-3"));
        assertEquals(201, reg.getStatusCode(), "El prerrequisito de registro de cliente debe responder 201");

        Response login = ApiClient.login("POST /api/auth/login credenciales correctas", email, DatosPrueba.DEFAULT_PASSWORD);

        String token = login.jsonPath().getString("token");
        String mensaje = login.jsonPath().getString("mensaje");
        String usuario = login.jsonPath().getString("usuario");
        String rol = login.jsonPath().getString("rol");

        Response sonda = token != null ? ApiClient.sondaSesionCliente("Sonda con sesión cliente", token) : null;

        assertAll("Aserciones CP-01-A",
                () -> assertEquals(200, login.getStatusCode(), "Código HTTP esperado 200"),
                () -> assertNotNull(token, "El token no debe ser nulo"),
                () -> assertTrue(token != null && token.split("\\.").length == 3, "El token debe tener 3 partes separadas por punto"),
                () -> assertEquals("Inicio de sesión exitoso", mensaje, "Mensaje de inicio exitoso exacto"),
                () -> assertEquals(email, usuario, "El campo usuario debe ser el email registrado"),
                () -> assertEquals("cliente", rol, "El campo rol debe ser 'cliente'"),
                () -> assertNotNull(sonda, "La sonda de sesión debe haberse ejecutado"),
                () -> assertNotEquals(401, sonda.getStatusCode(), "La sonda no debe responder 401 con sesión activa"),
                () -> assertNotEquals(403, sonda.getStatusCode(), "La sonda no debe responder 403 con sesión activa de cliente")
        );
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-01-B", hu = "HU01", titulo = "Credenciales incorrectas")
    public void cp01B_credencialesIncorrectas_responde401() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("401; JSON con timestamp; mensaje = 'Credenciales inválidas'; sin token");
        }

        String email = DatosPrueba.generarEmail("cp01b", 1);
        Response reg = ApiClient.registrarCliente("Registrar cliente para clave errada",
                DatosPrueba.bodyRegistro("Cliente CP01B", email, DatosPrueba.DEFAULT_PASSWORD, "3001234567", "Calle 1 # 2-3"));
        assertEquals(201, reg.getStatusCode(), "El prerrequisito de registro debe responder 201");

        Response login = ApiClient.login("Login con contraseña incorrecta", email, "Incorrecta99");

        assertAll("Aserciones CP-01-B",
                () -> assertEquals(401, login.getStatusCode(), "Código HTTP esperado 401"),
                () -> assertNotNull(login.jsonPath().get("timestamp"), "Debe contener timestamp"),
                () -> assertEquals("Credenciales inválidas", login.jsonPath().getString("mensaje"), "Mensaje exacto 'Credenciales inválidas'"),
                () -> assertNull(login.jsonPath().get("token"), "No debe retornar token")
        );
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-01-C", hu = "HU01", titulo = "Bloqueo por intentos")
    public void cp01C_bloqueoPorIntentos_responde429EnSextoIntento() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("6.º intento responde 429; mensaje empieza por 'Cuenta bloqueada temporalmente por múltiples intentos fallidos' y contiene 'minuto'; sin token; evidencia registra los 6 intentos");
        }

        String email = DatosPrueba.generarEmail("cp01c", 1);
        Response reg = ApiClient.registrarCliente("Registrar cuenta nueva dedicada",
                DatosPrueba.bodyRegistro("Cliente CP01C", email, DatosPrueba.DEFAULT_PASSWORD, "3001234567", "Calle 1 # 2-3"));
        assertEquals(201, reg.getStatusCode(), "El prerrequisito de registro debe responder 201");

        // 5 intentos fallidos
        for (int i = 1; i <= 5; i++) {
            ApiClient.login("Intento fallido " + i + "/5 con clave incorrecta", email, "ClaveErronea" + i);
        }

        // 6to intento con la contraseña correcta
        Response sexto = ApiClient.login("6to intento con contraseña correcta", email, DatosPrueba.DEFAULT_PASSWORD);

        String mensaje = sexto.jsonPath().getString("mensaje");

        assertAll("Aserciones CP-01-C",
                () -> assertEquals(429, sexto.getStatusCode(), "El 6to intento debe responder 429 Too Many Requests"),
                () -> assertNotNull(mensaje, "Debe contener mensaje de bloqueo"),
                () -> assertTrue(mensaje != null && mensaje.startsWith("Cuenta bloqueada temporalmente por múltiples intentos fallidos"),
                        "El mensaje debe empezar por 'Cuenta bloqueada temporalmente por múltiples intentos fallidos'"),
                () -> assertTrue(mensaje != null && mensaje.contains("minuto"),
                        "El mensaje debe contener la palabra 'minuto'"),
                () -> assertNull(sexto.jsonPath().get("token"), "No debe retornar token en cuenta bloqueada")
        );
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-01-D", hu = "HU01", titulo = "Campos ausentes")
    public void cp01D_camposAusentes_responde400() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("Ambas 400; errores.contrasena = 'La contraseña es obligatoria' en (a); errores.email = 'El correo es obligatorio' en (b); sin token");
        }

        // Variante (a): sin contraseña
        Response resA = ApiClient.login("Login sin contraseña", "usuario.valido@example.com", null);
        // Variante (b): sin email
        Response resB = ApiClient.login("Login sin email", null, DatosPrueba.DEFAULT_PASSWORD);

        assertAll("Aserciones CP-01-D",
                () -> assertEquals(400, resA.getStatusCode(), "Variante (a) debe responder 400"),
                () -> assertEquals("La contraseña es obligatoria", resA.jsonPath().getString("errores.contrasena"),
                        "Variante (a) errores.contrasena debe ser 'La contraseña es obligatoria'"),
                () -> assertNull(resA.jsonPath().get("token"), "Variante (a) no debe retornar token"),

                () -> assertEquals(400, resB.getStatusCode(), "Variante (b) debe responder 400"),
                () -> assertEquals("El correo es obligatorio", resB.jsonPath().getString("errores.email"),
                        "Variante (b) errores.email debe ser 'El correo es obligatorio'"),
                () -> assertNull(resB.jsonPath().get("token"), "Variante (b) no debe retornar token")
        );
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-01-E", hu = "HU01", titulo = "Correo no registrado")
    public void cp01E_correoNoRegistrado_responde401IdenticoAClaveErrada() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("401; mensaje = 'Credenciales inválidas'; código y mensaje idénticos a clave errada; sin token");
        }

        // 1. Ejecutar petición de CP-01-B (cuenta existente + clave errada) para comparar
        String emailExistente = DatosPrueba.generarEmail("cp01e", 1);
        Response reg = ApiClient.registrarCliente("Registrar cliente para referencia de clave errada",
                DatosPrueba.bodyRegistro("Cliente CP01E", emailExistente, DatosPrueba.DEFAULT_PASSWORD, "3001234567", "Calle 1 # 2-3"));
        assertEquals(201, reg.getStatusCode(), "El prerrequisito de registro debe responder 201");

        Response resClaveErrada = ApiClient.login("Referencia clave errada cuenta existente", emailExistente, "ClaveErrada99");
        int statusRef = resClaveErrada.getStatusCode();
        String mensajeRef = resClaveErrada.jsonPath().getString("mensaje");

        // 2. Login con correo no registrado
        String emailNoExiste = "noexiste." + ApiTestConfig.getRunId() + "@example.com";
        Response resNoExiste = ApiClient.login("Login con correo no registrado", emailNoExiste, "MiPassword123");

        assertAll("Aserciones CP-01-E",
                () -> assertEquals(401, resNoExiste.getStatusCode(), "Código HTTP esperado 401"),
                () -> assertEquals("Credenciales inválidas", resNoExiste.jsonPath().getString("mensaje"),
                        "Mensaje esperado 'Credenciales inválidas'"),
                () -> assertEquals(statusRef, resNoExiste.getStatusCode(),
                        "El código HTTP debe ser idéntico al de cuenta existente con clave errada"),
                () -> assertEquals(mensajeRef, resNoExiste.jsonPath().getString("mensaje"),
                        "El mensaje debe ser idéntico al de cuenta existente con clave errada"),
                () -> assertNull(resNoExiste.jsonPath().get("token"), "No debe retornar token")
        );
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-01-F", hu = "HU01", titulo = "Usuario inactivo")
    public void cp01F_usuarioInactivo_responde401() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("401; mensaje = 'El usuario se encuentra inactivo'; sin token");
        }

        String emailInactivo = null;
        String passInactivo = null;

        if (DbVerifier.isConfigured()) {
            emailInactivo = DatosPrueba.generarEmail("cp01f", 1);
            passInactivo = DatosPrueba.DEFAULT_PASSWORD;
            Response reg = ApiClient.registrarCliente("Registrar cliente para marcarlo inactivo en BD",
                    DatosPrueba.bodyRegistro("Cliente CP01F", emailInactivo, passInactivo, "3001234567", "Calle 1 # 2-3"));
            assertEquals(201, reg.getStatusCode(), "El registro de cliente debe responder 201");

            boolean marcado = DbVerifier.marcarInactivo(emailInactivo);
            Assumptions.assumeTrue(marcado, "No se pudo marcar como inactivo en la base de datos");
        } else if (ApiTestConfig.getInactiveEmail() != null && !ApiTestConfig.getInactiveEmail().isEmpty()) {
            emailInactivo = ApiTestConfig.getInactiveEmail();
            passInactivo = ApiTestConfig.getInactivePassword() != null ? ApiTestConfig.getInactivePassword() : DatosPrueba.DEFAULT_PASSWORD;
        } else {
            Assumptions.assumeTrue(false, "No hay cuenta inactiva disponible (se requiere BD o QA_INACTIVE_*)");
        }

        Response login = ApiClient.login("Login con usuario inactivo y clave correcta", emailInactivo, passInactivo);

        assertAll("Aserciones CP-01-F",
                () -> assertEquals(401, login.getStatusCode(), "Código HTTP esperado 401"),
                () -> assertEquals("El usuario se encuentra inactivo", login.jsonPath().getString("mensaje"),
                        "Mensaje esperado 'El usuario se encuentra inactivo'"),
                () -> assertNull(login.jsonPath().get("token"), "No debe retornar token")
        );
    }
}
