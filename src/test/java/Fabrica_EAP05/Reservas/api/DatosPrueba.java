package Fabrica_EAP05.Reservas.api;

public class DatosPrueba {
    public static final String DEFAULT_PASSWORD = "Qa1234";

    public static String generarEmail(String cp, int n) {
        String cleanCp = cp.toLowerCase().replace("-", "").trim();
        return "qa.auto." + ApiTestConfig.getRunId() + "." + cleanCp + "." + n + "@example.com";
    }

    public static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static String bodyLogin(String email, String contrasena) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        if (email != null) {
            sb.append("\"email\":\"").append(escapeJson(email)).append("\"");
            first = false;
        }
        if (contrasena != null) {
            if (!first) sb.append(",");
            sb.append("\"contrasena\":\"").append(escapeJson(contrasena)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    public static String bodyRegistro(String nombre, String email, String contrasena, String telefono, String direccion) {
        return bodyRegistroConRol(nombre, email, contrasena, telefono, direccion, null);
    }

    public static String bodyRegistroConRol(String nombre, String email, String contrasena, String telefono, String direccion, String rol) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        if (nombre != null) {
            sb.append("\"nombre\":\"").append(escapeJson(nombre)).append("\"");
            first = false;
        }
        if (email != null) {
            if (!first) sb.append(",");
            sb.append("\"email\":\"").append(escapeJson(email)).append("\"");
            first = false;
        }
        if (contrasena != null) {
            if (!first) sb.append(",");
            sb.append("\"contrasena\":\"").append(escapeJson(contrasena)).append("\"");
            first = false;
        }
        if (telefono != null) {
            if (!first) sb.append(",");
            sb.append("\"telefono\":\"").append(escapeJson(telefono)).append("\"");
            first = false;
        }
        if (direccion != null) {
            if (!first) sb.append(",");
            sb.append("\"direccion\":\"").append(escapeJson(direccion)).append("\"");
            first = false;
        }
        if (rol != null) {
            if (!first) sb.append(",");
            sb.append("\"rol\":\"").append(escapeJson(rol)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }
}
