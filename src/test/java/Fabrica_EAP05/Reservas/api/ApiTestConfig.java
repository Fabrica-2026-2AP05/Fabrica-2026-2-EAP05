package Fabrica_EAP05.Reservas.api;

import io.restassured.response.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class ApiTestConfig {
    private static final String runId = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    private static final Properties fileProps = new Properties();
    private static boolean propsLoaded = false;

    private static String baseUrl;
    private static String adminEmail;
    private static String adminPassword;
    private static String ejecutor;
    private static String ramaDesplegada;
    private static long timeoutMs;

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    private static String inactiveEmail;
    private static String inactivePassword;

    private static boolean warmupDone = false;
    private static boolean apiAvailable = false;
    private static String apiDocsInfo = "No disponible";
    private static String warmupError = null;

    private static Boolean adminValid = null;
    private static String adminCheckMessage = null;

    static {
        loadConfig();
    }

    private static synchronized void loadConfig() {
        if (!propsLoaded) {
            File propFile = new File("src/test/resources/api-tests.properties");
            if (propFile.exists()) {
                try (FileInputStream fis = new FileInputStream(propFile);
                     InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                    fileProps.load(isr);
                } catch (Exception e) {
                    System.err.println("[ApiTestConfig] No se pudo leer api-tests.properties: " + e.getMessage());
                }
            }
            propsLoaded = true;
        }

        baseUrl = getProp("QA_BASE_URL", "https://fabrica-2026-2-eap05-1.onrender.com");
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        adminEmail = getProp("QA_ADMIN_EMAIL", "");
        adminPassword = getProp("QA_ADMIN_PASSWORD", "");
        ejecutor = getProp("QA_EJECUTOR", "Garzón Mejía Juan José");
        ramaDesplegada = getProp("QA_RAMA_DESPLEGADA", "desconocida");

        String timeoutStr = getProp("QA_TIMEOUT_MS", "90000");
        try {
            timeoutMs = Long.parseLong(timeoutStr);
        } catch (NumberFormatException e) {
            timeoutMs = 90000L;
        }

        dbUrl = getProp("QA_DB_URL", null);
        dbUser = getProp("QA_DB_USER", null);
        dbPassword = getProp("QA_DB_PASSWORD", null);

        inactiveEmail = getProp("QA_INACTIVE_EMAIL", null);
        inactivePassword = getProp("QA_INACTIVE_PASSWORD", null);
    }

    private static String getProp(String key, String defaultValue) {
        String env = System.getenv(key);
        if (env != null && !env.trim().isEmpty()) {
            return env.trim();
        }
        String sys = System.getProperty(key);
        if (sys != null && !sys.trim().isEmpty()) {
            return sys.trim();
        }
        String prop = fileProps.getProperty(key);
        if (prop != null && !prop.trim().isEmpty()) {
            return prop.trim();
        }
        return defaultValue;
    }

    public static synchronized void ensureWarmup() {
        if (warmupDone) {
            return;
        }
        warmupDone = true;

        long start = System.currentTimeMillis();
        long deadline = start + timeoutMs;
        int intento = 0;

        while (System.currentTimeMillis() < deadline) {
            intento++;
            try {
                Response res = ApiClient.baseRequest()
                        .when()
                        .get("/v3/api-docs");

                int sc = res.getStatusCode();
                System.out.println("[Warmup] Intento " + intento + " -> HTTP " + sc);

                if (sc == 200) {
                    apiAvailable = true;
                    try {
                        String title = res.jsonPath().getString("info.title");
                        String version = res.jsonPath().getString("info.version");
                        apiDocsInfo = (title != null ? title : "API") + " v" + (version != null ? version : "desconocida");
                    } catch (Exception e) {
                        apiDocsInfo = "OpenAPI 200 OK";
                    }
                    System.out.println("[Warmup] API disponible: " + apiDocsInfo);
                    return;
                } else {
                    warmupError = "HTTP " + sc + " en /v3/api-docs";
                }
            } catch (Exception e) {
                warmupError = e.getClass().getSimpleName() + ": " + e.getMessage();
                System.out.println("[Warmup] Intento " + intento + " -> Excepcion: " + warmupError);
            }

            try {
                Thread.sleep(Math.min(3000, deadline - System.currentTimeMillis() > 0 ? 3000 : 500));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        apiAvailable = false;
        if (warmupError == null) {
            warmupError = "Timeout de calentamiento alcanzado (" + timeoutMs + " ms)";
        }
    }

    public static synchronized boolean isAdminValid() {
        if (adminValid != null) {
            return adminValid;
        }

        if (adminEmail == null || adminEmail.isEmpty() || adminPassword == null || adminPassword.isEmpty()) {
            adminValid = false;
            adminCheckMessage = "sin credenciales de administrador";
            return false;
        }

        try {
            Response res = ApiClient.baseRequest()
                    .contentType("application/json")
                    .body(DatosPrueba.bodyLogin(adminEmail, adminPassword))
                    .post("/api/auth/login");

            if (res.getStatusCode() == 200) {
                String rol = res.jsonPath().getString("rol");
                if ("administrador".equalsIgnoreCase(rol)) {
                    adminValid = true;
                    return true;
                } else {
                    adminValid = false;
                    adminCheckMessage = "Usuario no tiene rol administrador (rol=" + rol + ")";
                    return false;
                }
            } else {
                adminValid = false;
                adminCheckMessage = "Login admin falló con HTTP " + res.getStatusCode();
                return false;
            }
        } catch (Exception e) {
            adminValid = false;
            adminCheckMessage = "Error al verificar admin: " + e.getMessage();
            return false;
        }
    }

    public static String getRunId() { return runId; }
    public static String getBaseUrl() { return baseUrl; }
    public static String getAdminEmail() { return adminEmail; }
    public static String getAdminPassword() { return adminPassword; }
    public static String getEjecutor() { return ejecutor; }
    public static String getRamaDesplegada() { return ramaDesplegada; }
    public static long getTimeoutMs() { return timeoutMs; }
    public static String getDbUrl() { return dbUrl; }
    public static String getDbUser() { return dbUser; }
    public static String getDbPassword() { return dbPassword; }
    public static String getInactiveEmail() { return inactiveEmail; }
    public static String getInactivePassword() { return inactivePassword; }
    public static boolean isApiAvailable() { return apiAvailable; }
    public static String getApiDocsInfo() { return apiDocsInfo; }
    public static String getWarmupError() { return warmupError; }
    public static String getAdminCheckMessage() { return adminCheckMessage; }
}
