package Fabrica_EAP05.Reservas.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EvidenciaRecorder {
    private static final ThreadLocal<ResultadoCP> activeCP = new ThreadLocal<>();
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(\"(?:contrasena|password)\"\\s*:\\s*\")[^\"]*(\")", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\"(?:token)\"\\s*:\\s*\")([^\"]{12})[^\"]*(\")", Pattern.CASE_INSENSITIVE);
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
            cp.setHttpObtenido(String.valueOf(status));
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
        return result;
    }
}
