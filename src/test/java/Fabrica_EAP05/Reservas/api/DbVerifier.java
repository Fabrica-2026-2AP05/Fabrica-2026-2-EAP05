package Fabrica_EAP05.Reservas.api;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DbVerifier {

    public static boolean isConfigured() {
        return ApiTestConfig.getDbUrl() != null && !ApiTestConfig.getDbUrl().isEmpty()
                && ApiTestConfig.getDbUser() != null && !ApiTestConfig.getDbUser().isEmpty();
    }

    private static Connection getConnection() throws Exception {
        return DriverManager.getConnection(
                ApiTestConfig.getDbUrl(),
                ApiTestConfig.getDbUser(),
                ApiTestConfig.getDbPassword()
        );
    }

    public static int countUsuarios(String email) {
        if (!isConfigured()) return -1;
        String sql = "SELECT count(*) FROM public.usuario WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            System.err.println("[DbVerifier] Error al consultar usuario: " + e.getMessage());
        }
        return -1;
    }

    public static String getRol(String email) {
        if (!isConfigured()) return null;
        String sql = "SELECT rol FROM public.usuario WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (Exception e) {
            System.err.println("[DbVerifier] Error al consultar rol: " + e.getMessage());
        }
        return null;
    }

    public static Boolean isActivo(String email) {
        if (!isConfigured()) return null;
        String sql = "SELECT activo FROM public.usuario WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        } catch (Exception e) {
            System.err.println("[DbVerifier] Error al consultar activo: " + e.getMessage());
        }
        return null;
    }

    public static boolean marcarInactivo(String email) {
        if (!isConfigured()) return false;
        if (email == null || !email.startsWith("qa.auto.")) {
            throw new IllegalArgumentException("Solo se permite modificar usuarios qa.auto.% creados por la prueba");
        }
        String sql = "UPDATE public.usuario SET activo = false WHERE email = ? AND email LIKE 'qa.auto.%'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (Exception e) {
            System.err.println("[DbVerifier] Error al marcar inactivo: " + e.getMessage());
            return false;
        }
    }
}
