package Fabrica_EAP05.Reservas.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("api")
@ExtendWith(ResultadosExtension.class)
public class HU04RegistroClienteApiTest {

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
    @CasoPrueba(id = "CP-04-A", hu = "HU04", titulo = "Registro exitoso")
    public void cp04A_registroExitoso_responde201YPermiteLogin() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("201; id UUID; campos enviados iguales; rol = 'cliente'; createdAt presente; sin contrasena/password; login 200 rol = 'cliente'; si BD: activo = true");
        }

        String email = DatosPrueba.generarEmail("cp04a", 1);
        String nombre = "Ana Pérez";
        String telefono = "3009876543";
        String direccion = "Calle 10 #20-30";

        String body = DatosPrueba.bodyRegistro(nombre, email, DatosPrueba.DEFAULT_PASSWORD, telefono, direccion);
        Response reg = ApiClient.registrarCliente("Registrar cliente nuevo con body completo", body);

        String id = reg.jsonPath().getString("id");
        String respNombre = reg.jsonPath().getString("nombre");
        String respEmail = reg.jsonPath().getString("email");
        String respTelefono = reg.jsonPath().getString("telefono");
        String respDireccion = reg.jsonPath().getString("direccion");
        String respRol = reg.jsonPath().getString("rol");
        Object respCreatedAt = reg.jsonPath().get("createdAt");
        Object respContrasena = reg.jsonPath().get("contrasena");
        Object respPassword = reg.jsonPath().get("password");

        // Login con credenciales recién creadas
        Response login = ApiClient.login("Login con cliente recién registrado", email, DatosPrueba.DEFAULT_PASSWORD);

        // Verificación BD si está configurada
        Boolean activoBd = DbVerifier.isConfigured() ? DbVerifier.isActivo(email) : null;

        EvidenciaRecorder.principal(reg, login);
        assertAll("Aserciones CP-04-A",
                () -> assertEquals(201, reg.getStatusCode(), "Código HTTP esperado 201"),
                () -> assertNotNull(id, "El id no debe ser nulo"),
                () -> assertDoesNotThrow(() -> UUID.fromString(id), "El id debe ser un UUID válido"),
                () -> assertEquals(nombre, respNombre, "El nombre debe coincidir"),
                () -> assertEquals(email, respEmail, "El email debe coincidir"),
                () -> assertEquals(telefono, respTelefono, "El teléfono debe coincidir"),
                () -> assertEquals(direccion, respDireccion, "La dirección debe coincidir"),
                () -> assertEquals("cliente", respRol, "El rol debe ser 'cliente'"),
                () -> assertNotNull(respCreatedAt, "El campo createdAt debe estar presente"),
                () -> assertNull(respContrasena, "La respuesta no debe contener el campo contrasena"),
                () -> assertNull(respPassword, "La respuesta no debe contener el campo password"),
                () -> assertEquals(200, login.getStatusCode(), "El login debe responder 200"),
                () -> assertEquals("cliente", login.jsonPath().getString("rol"), "El rol en login debe ser 'cliente'"),
                () -> {
                    if (activoBd != null) {
                        assertTrue(activoBd, "En base de datos activo debe ser true");
                    }
                }
        );
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-04-B", hu = "HU04", titulo = "Información incompleta")
    public void cp04B_informacionIncompleta_responde400YNoRegistra() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("Cada una de las 6 variantes responde 400 con su respectivo mensaje en errores.<campo>; la cuenta no queda registrada (§5.3)");
        }

        // Variante 1: omitir nombre
        String emailV1 = DatosPrueba.generarEmail("cp04b", 1);
        Response res1 = ApiClient.registrarCliente("Variante 1: sin nombre",
                DatosPrueba.bodyRegistro(null, emailV1, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));

        // Variante 2: omitir email
        Response res2 = ApiClient.registrarCliente("Variante 2: sin email",
                DatosPrueba.bodyRegistro("Nombre V2", null, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));

        // Variante 3: omitir contrasena
        String emailV3 = DatosPrueba.generarEmail("cp04b", 3);
        Response res3 = ApiClient.registrarCliente("Variante 3: sin contrasena",
                DatosPrueba.bodyRegistro("Nombre V3", emailV3, null, "3009876543", "Calle 1 # 2-3"));

        // Variante 4: omitir telefono
        String emailV4 = DatosPrueba.generarEmail("cp04b", 4);
        Response res4 = ApiClient.registrarCliente("Variante 4: sin telefono",
                DatosPrueba.bodyRegistro("Nombre V4", emailV4, DatosPrueba.DEFAULT_PASSWORD, null, "Calle 1 # 2-3"));

        // Variante 5: omitir direccion
        String emailV5 = DatosPrueba.generarEmail("cp04b", 5);
        Response res5 = ApiClient.registrarCliente("Variante 5: sin direccion",
                DatosPrueba.bodyRegistro("Nombre V5", emailV5, DatosPrueba.DEFAULT_PASSWORD, "3009876543", null));

        // Variante 6: email inválido
        Response res6 = ApiClient.registrarCliente("Variante 6: email invalido",
                DatosPrueba.bodyRegistro("Nombre V6", "correo-invalido", DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));

        // Verificaciones §5.3 de que los correos válidos no quedaron creados
        Response verifV1 = ApiClient.registrarCliente("Verificación §5.3 para Variante 1",
                DatosPrueba.bodyRegistro("Nombre Completo V1", emailV1, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));
        Response verifV3 = ApiClient.registrarCliente("Verificación §5.3 para Variante 3",
                DatosPrueba.bodyRegistro("Nombre Completo V3", emailV3, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));
        Response verifV4 = ApiClient.registrarCliente("Verificación §5.3 para Variante 4",
                DatosPrueba.bodyRegistro("Nombre Completo V4", emailV4, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));
        Response verifV5 = ApiClient.registrarCliente("Verificación §5.3 para Variante 5",
                DatosPrueba.bodyRegistro("Nombre Completo V5", emailV5, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));

        EvidenciaRecorder.principal(res1, res2, res3, res4, res5, res6);
        assertAll("Aserciones CP-04-B (6 variantes)",
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
                () -> assertEquals(201, verifV5.getStatusCode(), "Verificación §5.3 Variante 5: registrar después debe dar 201"),

                () -> assertEquals(400, res6.getStatusCode(), "Variante 6 debe responder 400"),
                () -> assertEquals("El email no es válido", res6.jsonPath().getString("errores.email"),
                        "Variante 6 errores.email debe ser 'El email no es válido'")
        );
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-04-C", hu = "HU04", titulo = "Correo existente")
    public void cp04C_correoExistente_responde409() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("La segunda petición responde 409; mensaje = 'El email ya está registrado' (o 'El email ya está registrado en Supabase Auth')");
        }

        String email = DatosPrueba.generarEmail("cp04c", 1);
        String body = DatosPrueba.bodyRegistro("Cliente CP04C", email, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3");

        // Primer registro exitoso
        Response reg1 = ApiClient.registrarCliente("Primer registro de cliente", body);
        assertEquals(201, reg1.getStatusCode(), "El primer registro debe responder 201");

        // Segundo registro con mismo email
        Response reg2 = ApiClient.registrarCliente("Segundo registro con el mismo correo", body);

        String mensaje = reg2.jsonPath().getString("mensaje");
        boolean mensajeValido = "El email ya está registrado".equals(mensaje)
                || "El email ya está registrado en Supabase Auth".equals(mensaje);

        EvidenciaRecorder.principal(reg2);
        assertAll("Aserciones CP-04-C",
                () -> assertEquals(409, reg2.getStatusCode(), "La segunda petición debe responder 409 Conflict"),
                () -> assertTrue(mensajeValido, "El mensaje debe ser 'El email ya está registrado' o 'El email ya está registrado en Supabase Auth'. Recibido: " + mensaje)
        );
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-04-D", hu = "HU04", titulo = "Contraseña < 6")
    public void cp04D_contrasenaMenorA6_responde400Y201() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("(a) 400 con errores.contrasena = 'La contraseña debe tener como mínimo 6 caracteres' y la cuenta no queda creada; (b) 201");
        }

        // Variante (a): contraseña de 5 caracteres "Qa123"
        String emailA = DatosPrueba.generarEmail("cp04d", 1);
        Response resA = ApiClient.registrarCliente("Variante (a): contraseña de 5 caracteres Qa123",
                DatosPrueba.bodyRegistro("Cliente CP04D A", emailA, "Qa123", "3009876543", "Calle 1 # 2-3"));

        // Verificación §5.3: la cuenta no queda creada
        Response verifA = ApiClient.registrarCliente("Verificación §5.3: registrar después con clave de 6 caracteres",
                DatosPrueba.bodyRegistro("Cliente CP04D A", emailA, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));

        // Variante (b): contraseña de 6 caracteres con otro correo nuevo
        String emailB = DatosPrueba.generarEmail("cp04d", 2);
        Response resB = ApiClient.registrarCliente("Variante (b): contraseña de 6 caracteres Qa1234",
                DatosPrueba.bodyRegistro("Cliente CP04D B", emailB, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"));

        EvidenciaRecorder.principal(resA, resB);
        assertAll("Aserciones CP-04-D",
                () -> assertEquals(400, resA.getStatusCode(), "Variante (a) debe responder 400"),
                () -> assertEquals("La contraseña debe tener como mínimo 6 caracteres", resA.jsonPath().getString("errores.contrasena"),
                        "Variante (a) errores.contrasena debe ser 'La contraseña debe tener como mínimo 6 caracteres'"),
                () -> assertEquals(201, verifA.getStatusCode(),
                        "Verificación §5.3 Variante (a): registrar el correo con contraseña válida debe dar 201"),

                () -> assertEquals(201, resB.getStatusCode(), "Variante (b) debe responder 201")
        );
    }

    @Test
    @Tag("api")
    @CasoPrueba(id = "CP-04-E", hu = "HU04", titulo = "Rol enviado en el body")
    public void cp04E_rolEnviadoEnBody_ignoraRolYAsignaCliente() {
        ResultadoCP cp = EvidenciaRecorder.getActiveCP();
        if (cp != null) {
            cp.setEsperado("Registro 201 con rol = 'cliente'; login con rol = 'cliente'; registrar proveedor con ese token responde 403 con mensaje = 'No tienes permiso para acceder a este recurso'");
        }

        String email = DatosPrueba.generarEmail("cp04e", 1);
        String bodyConRolAdmin = DatosPrueba.bodyRegistroConRol("Cliente CP04E", email, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3", "administrador");

        // 1. Registro con rol administrador en el body
        Response reg = ApiClient.registrarCliente("Registro enviando rol administrador en el body", bodyConRolAdmin);

        // 2. Login con ese usuario
        Response login = ApiClient.login("Login con usuario creado", email, DatosPrueba.DEFAULT_PASSWORD);
        String tokenCliente = login.jsonPath().getString("token");

        // 3. Intentar registrar un proveedor con ese token
        String emailProv = DatosPrueba.generarEmail("cp04e", 2);
        Response resProv = tokenCliente != null
                ? ApiClient.registrarProveedor("Intentar registrar proveedor con token de usuario cliente", tokenCliente,
                DatosPrueba.bodyRegistro("Prov", emailProv, DatosPrueba.DEFAULT_PASSWORD, "3009876543", "Calle 1 # 2-3"))
                : null;

        EvidenciaRecorder.principal(reg, login, resProv);
        assertAll("Aserciones CP-04-E",
                () -> assertEquals(201, reg.getStatusCode(), "Registro debe responder 201"),
                () -> assertEquals("cliente", reg.jsonPath().getString("rol"), "El rol devuelto debe ser 'cliente'"),
                () -> assertEquals(200, login.getStatusCode(), "El login debe responder 200"),
                () -> assertEquals("cliente", login.jsonPath().getString("rol"), "El rol en login debe ser 'cliente'"),
                () -> assertNotNull(resProv, "Se debe haber intentado registrar proveedor con el token obtenido"),
                () -> assertEquals(403, resProv.getStatusCode(), "Registrar proveedor con token de cliente debe responder 403 Forbidden"),
                () -> assertEquals("No tienes permiso para acceder a este recurso", resProv.jsonPath().getString("mensaje"),
                        "Mensaje esperado 'No tienes permiso para acceder a este recurso'")
        );
    }
}
