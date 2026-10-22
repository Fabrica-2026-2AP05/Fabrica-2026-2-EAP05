package Fabrica_EAP05.Reservas.api;

import io.restassured.response.Response;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

public class EvidenciaRecorder {
    private static final ThreadLocal<ResultadoCP> activeCP = new ThreadLocal<>();
    // No enmascara los mensajes de validación ("La contraseña es obligatoria", etc.): son evidencia del CP
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(\"(?:contrasena|password)\"\\s*:\\s*\")(?!La contraseña )[^\"]*(\")", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\"(?:token)\"\\s*:\\s*\")([^\"]{12})[^\"]*(\")", Pattern.CASE_INSENSITIVE);
    // JWT sueltos, por ejemplo dentro del mensaje de una aserción fallida
    private static final Pattern JWT_PATTERN = Pattern.compile("(eyJ[A-Za-z0-9_-]{9})[A-Za-z0-9_-]*\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*");
    private static final Pattern BEARER_PATTERN = Pattern.compile("(Bearer\\s+)([A-Za-z0-9_-]{12})[A-Za-z0-9_.-]*", Pattern.CASE_INSENSITIVE);

    public static void setActiveCP(ResultadoCP cp) {
        activeCP.set(cp);
    }

    public static ResultadoCP getActiveCP() {
        return activeCP.get();
    }

    public static void clear() {
        activeCP.remove();
    }

    public static void registrarPaso(String paso, String metodo, String ruta, String reqBody, int status, String respBody, long ms) {
        ResultadoCP cp = activeCP.get();
        if (cp != null) {
            String maskedReq = enmascarar(reqBody);
            String maskedResp = enmascarar(respBody);
            if (maskedResp != null && maskedResp.length() > 2000) {
                maskedResp = maskedResp.substring(0, 1997) + "...";
            }
            ResultadoCP.PasoEvidencia p = new ResultadoCP.PasoEvidencia(paso, metodo, ruta, maskedReq, status, maskedResp, ms);
            cp.addEvidencia(p);
            if (!cp.isHttpPrincipalFijado()) {
                cp.setHttpObtenido(String.valueOf(status));
            }
        }
    }

    /**
     * Fija el "HTTP obtenido" del CP con las respuestas que se comparan con el oráculo (no las de preparación
     * ni las de verificación), en el orden de la columna "Esperado". Con variantes queda, p. ej., "400 / 201".
     */
    public static void principal(Response... respuestas) {
        ResultadoCP cp = activeCP.get();
        if (cp != null) {
            cp.fijarHttpPrincipal(Arrays.stream(respuestas).filter(Objects::nonNull)
                    .map(r -> String.valueOf(r.getStatusCode())).collect(Collectors.joining(" / ")));
        }
    }

    public static String enmascarar(String input) {
        if (input == null) return null;
        String adminPass = ApiTestConfig.getAdminPassword();
        String result = input;
        if (adminPass != null && !adminPass.isEmpty()) {
            result = result.replace(adminPass, "***");
        }
        result = PASSWORD_PATTERN.matcher(result).replaceAll("$1***$2");
        result = TOKEN_PATTERN.matcher(result).replaceAll("$1$2...$3");
        result = BEARER_PATTERN.matcher(result).replaceAll("$1$2...");
        result = JWT_PATTERN.matcher(result).replaceAll("$1...");
        return result;
    }
}
