package Fabrica_EAP05.Reservas.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@Tag("api")
@ExtendWith(ResultadosExtension.class)
public class HU05RegistroProveedorApiTest {

    @BeforeAll
    public static void setup() {
        ApiTestConfig.ensureWarmup();
    }

    @BeforeEach
    public void checkApiAvailable() {
        Assumptions.assumeTrue(ApiTestConfig.isApiAvailable(), "API no disponible: " + ApiTestConfig.getWarmupError());
    }

    private String requireAdminToken() {
        Assumptions.assumeTrue(ApiTestConfig.isAdminValid(), "sin token de administrador: " + ApiTestConfig.getAdminCheckMessage());
        String token = ApiClient.obtenerTokenAdmin();
        assertNotNull(token, "No se pudo obtener el token de administrador");
        return token;
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-05-A", hu = "HU05", titulo = "Registro exitoso")
    public void cp05A_registroExitoso_responde201YPermiteLogin() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("Con Bearer <admin>: 201 con rol = 'proveedor', campos iguales a lo enviado y sin contraseña; login del proveedor 200 con rol = 'proveedor'");
        }

        String adminToken = requireAdminToken();

        String email = DatosPrueba.generarEmail("cp05a", 1);
        String nombre = "Carlos Proveedor";
        String telefono = "3017654321";
        String direccion = "Avenida 30 # 45-67";

        String body = DatosPrueba.bodyRegistro(nombre, email, DatosPrueba.DEFAULT_PASSWORD, telefono, direccion);
        Response reg = ApiClient.registrarProveedor("Registrar proveedor con token admin", adminToken, body);

        String id = reg.jsonPath().getString("id");
        String respNombre = reg.jsonPath().getString("nombre");
        String respEmail = reg.jsonPath().getString("email");
        String respTelefono = reg.jsonPath().getString("telefono");
        String respDireccion = reg.jsonPath().getString("direccion");
        String respRol = reg.jsonPath().getString("rol");
        Object respContrasena = reg.jsonPath().get("contrasena");
        Object respPassword = reg.jsonPath().get("password");

        // Login del proveedor recién registrado
        Response login = ApiClient.login("Login del proveedor recién registrado", email, DatosPrueba.DEFAULT_PASSWORD);

        EvidenciaRecorder.principal(reg, login);
        assertAll("Aserciones CP-05-A",
                () -> assertEquals(201, reg.getStatusCode(), "Código HTTP esperado 201"),
                () -> assertNotNull(id, "El id no debe ser nulo"),
                () -> assertEquals("proveedor", respRol, "El rol debe ser 'proveedor'"),
                () -> assertEquals(nombre, respNombre, "El nombre debe coincidir"),
                () -> assertEquals(email, respEmail, "El email debe coincidir"),
                () -> assertEquals(telefono, respTelefono, "El teléfono debe coincidir"),
                () -> assertEquals(direccion, respDireccion, "La dirección debe coincidir"),
                () -> assertNull(respContrasena, "La respuesta no debe contener el campo contrasena"),
                () -> assertNull(respPassword, "La respuesta no debe contener el campo password"),
                () -> assertEquals(200, login.getStatusCode(), "El login del proveedor debe responder 200"),
                () -> assertEquals("proveedor", login.jsonPath().getString("rol"), "El rol en login debe ser 'proveedor'")
        );
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-05-B", hu = "HU05", titulo = "Información incompleta")
    public void cp05B_informacionIncompleta_responde400YNoRegistra() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("Con token admin, 5 variantes omitiendo cada campo: cada una 400 con el mensaje del campo; el proveedor no queda registrado");
        }

        String adminToken = requireAdminToken();

        // 1. Omitir nombre
        String emailV1 = DatosPrueba.generarEmail("cp05b", 1);
        Response res1 = ApiClient.registrarProveedor("Variante 1: sin nombre", adminToken,
                DatosPrueba.bodyRegistro(null, emailV1, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));

        // 2. Omitir email
        Response res2 = ApiClient.registrarProveedor("Variante 2: sin email", adminToken,
                DatosPrueba.bodyRegistro("Prov V2", null, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));

        // 3. Omitir contrasena
        String emailV3 = DatosPrueba.generarEmail("cp05b", 3);
        Response res3 = ApiClient.registrarProveedor("Variante 3: sin contrasena", adminToken,
                DatosPrueba.bodyRegistro("Prov V3", emailV3, null, "3009876543", "Calle 1 # 2-3"));

        // 4. Omitir telefono
        String emailV4 = DatosPrueba.generarEmail("cp05b", 4);
        Response res4 = ApiClient.registrarProveedor("Variante 4: sin telefono", adminToken,
                DatosPrueba.bodyRegistro("Prov V4", emailV4, DatosPrueba.DEFAULT_PASSWORD, null, "Calle 1 # 2-3"));

        // 5. Omitir direccion
        String emailV5 = DatosPrueba.generarEmail("cp05b", 5);
        Response res5 = ApiClient.registrarProveedor("Variante 5: sin direccion", adminToken,
                DatosPrueba.bodyRegistro("Prov V5", emailV5, DatosPrueba.DEFAULT_PASSWORD, "3009876543", null));

        // Verificaciones §5.3 de que no quedaron registrados
        Response verifV1 = ApiClient.registrarProveedor("Verificación §5.3 Variante 1", adminToken,
                DatosPrueba.bodyRegistro("Prov Completo V1", emailV1, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));
        Response verifV3 = ApiClient.registrarProveedor("Verificación §5.3 Variante 3", adminToken,
                DatosPrueba.bodyRegistro("Prov Completo V3", emailV3, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));
        Response verifV4 = ApiClient.registrarProveedor("Verificación §5.3 Variante 4", adminToken,
                DatosPrueba.bodyRegistro("Prov Completo V4", emailV4, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));
        Response verifV5 = ApiClient.registrarProveedor("Verificación §5.3 Variante 5", adminToken,
                DatosPrueba.bodyRegistro("Prov Completo V5", emailV5, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));

        EvidenciaRecorder.principal(res1, res2, res3, res4, res5);
        assertAll("Aserciones CP-05-B (5 variantes)",
                () -> assertEquals(400, res1.getStatusCode(), "Variante 1 debe responder 400"),
                () -> assertEquals("El nombre es obligatorio", res1.jsonPath().getString("errores.nombre"),
                        "Variante 1 errores.nombre debe ser 'El nombre es obligatorio'"),
                () -> assertEquals(201, verifV1.getStatusCode(), "Verificación §5.3 Variante 1: registrar después debe dar 201"),

                () -> assertEquals(400, res2.getStatusCode(), "Variante 2 debe responder 400"),
                () -> assertEquals("El email es obligatorio", res2.jsonPath().getString("errores.email"),
                        "Variante 2 errores.email debe ser 'El email es obligatorio'"),

                () -> assertEquals(400, res3.getStatusCode(), "Variante 3 debe responder 400"),
                () -> assertEquals("La contraseña es obligatoria", res3.jsonPath().getString("errores.contrasena"),
                        "Variante 3 errores.contrasena debe ser 'La contraseña es obligatoria'"),
                () -> assertEquals(201, verifV3.getStatusCode(), "Verificación §5.3 Variante 3: registrar después debe dar 201"),

                () -> assertEquals(400, res4.getStatusCode(), "Variante 4 debe responder 400"),
                () -> assertEquals("El teléfono es obligatorio", res4.jsonPath().getString("errores.telefono"),
                        "Variante 4 errores.telefono debe ser 'El teléfono es obligatorio'"),
                () -> assertEquals(201, verifV4.getStatusCode(), "Verificación §5.3 Variante 4: registrar después debe dar 201"),

                () -> assertEquals(400, res5.getStatusCode(), "Variante 5 debe responder 400"),
                () -> assertEquals("La dirección es obligatoria", res5.jsonPath().getString("errores.direccion"),
                        "Variante 5 errores.direccion debe ser 'La dirección es obligatoria'"),
                () -> assertEquals(201, verifV5.getStatusCode(), "Verificación §5.3 Variante 5: registrar después debe dar 201")
        );
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-05-C", hu = "HU05", titulo = "Proveedor duplicado")
    public void cp05C_proveedorDuplicado_responde409() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("Registrar un proveedor (201) y repetir: 409; mensaje = 'El email ya está registrado' (o '... en Supabase Auth')");
        }

        String adminToken = requireAdminToken();

        String email = DatosPrueba.generarEmail("cp05c", 1);
        String body = DatosPrueba.bodyRegistro("Proveedor CP05C", email, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3");

        // 1. Primer registro de proveedor
        Response reg1 = ApiClient.registrarProveedor("Primer registro de proveedor", adminToken, body);
        assertEquals(201, reg1.getStatusCode(), "El primer registro de proveedor debe responder 201");

        // 2. Segundo registro con mismo body
        Response reg2 = ApiClient.registrarProveedor("Segundo registro con mismo body de proveedor", adminToken, body);

        String mensaje = reg2.jsonPath().getString("mensaje");
        boolean mensajeValido = "El email ya está registrado".equals(mensaje)
                || "El email ya está registrado en Supabase Auth".equals(mensaje);

        EvidenciaRecorder.principal(reg2);
        assertAll("Aserciones CP-05-C",
                () -> assertEquals(409, reg2.getStatusCode(), "La segunda petición debe responder 409 Conflict"),
                () -> assertTrue(mensajeValido, "El mensaje debe ser 'El email ya está registrado' o 'El email ya está registrado en Supabase Auth'. Recibido: " + mensaje)
        );
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-05-D", hu = "HU05", titulo = "Sin token")
    public void cp05D_sinToken_responde401YNoRegistra() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("Registrar un proveedor sin Authorization: 401; mensaje = 'No autenticado: debes iniciar sesion para acceder a este recurso'; el proveedor no queda registrado (§5.3)");
        }

        // Esta prueba NO necesita adminToken para ejecutarse
        String email = DatosPrueba.generarEmail("cp05d", 1);
        String body = DatosPrueba.bodyRegistro("Prov Sin Token", email, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3");

        Response res = ApiClient.registrarProveedor("Registrar proveedor sin header Authorization", null, body);

        // Verificación §5.3 con adminToken si está disponible
        Response verif = null;
        if (ApiTestConfig.isAdminValid()) {
            String adminToken = ApiClient.obtenerTokenAdmin();
            verif = ApiClient.registrarProveedor("Verificación §5.3 para CP-05-D", adminToken, body);
        }

        final Response finalVerif = verif;
        EvidenciaRecorder.principal(res);
        assertAll("Aserciones CP-05-D",
                () -> assertEquals(401, res.getStatusCode(), "Debe responder 401 Unauthorized sin token"),
                () -> assertEquals("No autenticado: debes iniciar sesion para acceder a este recurso", res.jsonPath().getString("mensaje"),
                        "Mensaje esperado 'No autenticado: debes iniciar sesion para acceder a este recurso'"),
                () -> {
                    if (finalVerif != null) {
                        assertEquals(201, finalVerif.getStatusCode(), "Verificación §5.3: Registrar después con token admin debe dar 201");
                    }
                }
        );
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-05-E", hu = "HU05", titulo = "Rol no autorizado")
    public void cp05E_rolNoAutorizado_responde403YNoRegistra() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("(a) Token de cliente nuevo; (b) token de proveedor creado por admin: ambas 403; mensaje = 'No tienes permiso para acceder a este recurso'; el proveedor no queda registrado");
        }

        String adminToken = requireAdminToken();

        // Variante (a): token de un cliente nuevo
        String emailCliente = DatosPrueba.generarEmail("cp05e_cli", 1);
        Response regCli = ApiClient.registrarCliente("Registrar cliente para prueba de rol no autorizado",
                DatosPrueba.bodyRegistro("Cliente CP05E", emailCliente, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));
        assertEquals(201, regCli.getStatusCode(), "El registro de cliente debe responder 201");

        Response loginCli = ApiClient.login("Login de cliente para obtener su token", emailCliente, DatosPrueba.DEFAULT_PASSWORD);
        assertEquals(200, loginCli.getStatusCode(), "Login de cliente debe responder 200");
        String tokenCliente = loginCli.jsonPath().getString("token");

        String emailTargetA = DatosPrueba.generarEmail("cp05e_ta", 1);
        String bodyTargetA = DatosPrueba.bodyRegistro("Prov Target A", emailTargetA, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3");
        Response resA = ApiClient.registrarProveedor("Variante (a): registrar proveedor con token cliente", tokenCliente, bodyTargetA);

        // Verificación §5.3 Variante (a)
        Response verifA = ApiClient.registrarProveedor("Verificación §5.3 Variante (a)", adminToken, bodyTargetA);

        // Variante (b): token de un proveedor creado por admin
        String emailProvBase = DatosPrueba.generarEmail("cp05e_prv", 1);
        Response regProvBase = ApiClient.registrarProveedor("Registrar proveedor base con admin", adminToken,
                DatosPrueba.bodyRegistro("Prov Base", emailProvBase, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));
        assertEquals(201, regProvBase.getStatusCode(), "El registro de proveedor base debe responder 201");

        Response loginProvBase = ApiClient.login("Login de proveedor base para obtener su token", emailProvBase, DatosPrueba.DEFAULT_PASSWORD);
        assertEquals(200, loginProvBase.getStatusCode(), "Login de proveedor base debe responder 200");
        String tokenProvBase = loginProvBase.jsonPath().getString("token");

        String emailTargetB = DatosPrueba.generarEmail("cp05e_tb", 1);
        String bodyTargetB = DatosPrueba.bodyRegistro("Prov Target B", emailTargetB, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3");
        Response resB = ApiClient.registrarProveedor("Variante (b): registrar proveedor con token de otro proveedor", tokenProvBase, bodyTargetB);

        // Verificación §5.3 Variante (b)
        Response verifB = ApiClient.registrarProveedor("Verificación §5.3 Variante (b)", adminToken, bodyTargetB);

        EvidenciaRecorder.principal(resA, resB);
        assertAll("Aserciones CP-05-E",
                () -> assertEquals(403, resA.getStatusCode(), "Variante (a) con token de cliente debe responder 403 Forbidden"),
                () -> assertEquals("No tienes permiso para acceder a este recurso", resA.jsonPath().getString("mensaje"),
                        "Variante (a) mensaje debe ser 'No tienes permiso para acceder a este recurso'"),
                () -> assertEquals(201, verifA.getStatusCode(), "Verificación §5.3 Variante (a): registrar después debe dar 201"),

                () -> assertEquals(403, resB.getStatusCode(), "Variante (b) con token de proveedor debe responder 403 Forbidden"),
                () -> assertEquals("No tienes permiso para acceder a este recurso", resB.jsonPath().getString("mensaje"),
                        "Variante (b) mensaje debe ser 'No tienes permiso para acceder a este recurso'"),
                () -> assertEquals(201, verifB.getStatusCode(), "Verificación §5.3 Variante (b): registrar después debe dar 201")
        );
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-05-F", hu = "HU05", titulo = "Contraseña < 6")
    public void cp05F_contrasenaMenorA6_responde400Y201() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("Con token admin: (a) Qa123 responde 400 con errores.contrasena = 'La contraseña debe tener como mínimo 6 caracteres'; (b) 201");
        }

        String adminToken = requireAdminToken();

        // Variante (a): contraseña de 5 caracteres "Qa123"
        String emailA = DatosPrueba.generarEmail("cp05f", 1);
        Response resA = ApiClient.registrarProveedor("Variante (a): proveedor con contraseña Qa123", adminToken,
                DatosPrueba.bodyRegistro("Prov CP05F A", emailA, "Qa123", "3009876543", "Calle 1 # 2-3"));

        // Verificación §5.3 Variante (a): registrar después con contraseña válida de 6 caracteres
        Response verifA = ApiClient.registrarProveedor("Verificación §5.3 Variante (a): registrar después con 6 caracteres", adminToken,
                DatosPrueba.bodyRegistro("Prov CP05F A", emailA, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));

        // Variante (b): contraseña de 6 caracteres con otro correo nuevo
        String emailB = DatosPrueba.generarEmail("cp05f", 2);
        Response resB = ApiClient.registrarProveedor("Variante (b): proveedor con contraseña Qa1234", adminToken,
                DatosPrueba.bodyRegistro("Prov CP05F B", emailB, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));

        EvidenciaRecorder.principal(resA, resB);
        assertAll("Aserciones CP-05-F",
                () -> assertEquals(400, resA.getStatusCode(), "Variante (a) debe responder 400"),
                () -> assertEquals("La contraseña debe tener como mínimo 6 caracteres", resA.jsonPath().getString("errores.contrasena"),
                        "Variante (a) errores.contrasena debe ser 'La contraseña debe tener como mínimo 6 caracteres'"),
                () -> assertEquals(201, verifA.getStatusCode(), "Verificación §5.3 Variante (a): registrar después debe dar 201"),

                () -> assertEquals(201, resB.getStatusCode(), "Variante (b) debe responder 201")
        );
    }
}
