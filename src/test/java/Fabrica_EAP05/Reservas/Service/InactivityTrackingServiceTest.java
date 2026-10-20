package Fabrica_EAP05.Reservas.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InactivityTrackingServiceTest {

    private InactivityTrackingService inactivityTrackingService;
    private String correo;

    @BeforeEach
    void setUp() {
        inactivityTrackingService = new InactivityTrackingService();
        correo = "cliente@correo.com";
    }

    @Test
    @DisplayName("CP-03-A - La actividad recién registrada mantiene la sesión activa")
    void validarActividad_actividadReciente_retornaTrue() {
        // Arrange
        inactivityTrackingService.registrarActividad(correo);

        // Act
        boolean sesionActiva = inactivityTrackingService.validarActividad(correo);

        // Assert
        assertTrue(sesionActiva);
    }

    @Test
    @DisplayName("CP-03-C - Un usuario sin actividad registrada no tiene sesión activa")
    void validarActividad_sinRegistro_retornaFalse() {
        // Arrange: se usa un servicio nuevo sin actividad registrada.

        // Act
        boolean sesionActiva = inactivityTrackingService.validarActividad(correo);

        // Assert
        assertFalse(sesionActiva);
    }

    @Test
    @DisplayName("CP-03-C - Una sesión que supera treinta minutos vence")
    void validarActividad_actividadVencida_retornaFalseYEliminaRegistro() {
        // Arrange
        Map<String, Long> actividades = obtenerActividades();
        long haceTreintaYUnMinutos = System.currentTimeMillis() - (31L * 60 * 1000);
        actividades.put(correo, haceTreintaYUnMinutos);

        // Act
        boolean sesionActiva = inactivityTrackingService.validarActividad(correo);

        // Assert
        assertFalse(sesionActiva);
        assertFalse(actividades.containsKey(correo));
    }

    @Test
    @DisplayName("CP-03-A - Cerrar sesión elimina la actividad del usuario")
    void cerrarSesion_usuarioActivo_eliminaRegistro() {
        // Arrange
        inactivityTrackingService.registrarActividad(correo);
        assertTrue(inactivityTrackingService.validarActividad(correo));

        // Act
        inactivityTrackingService.cerrarSesion(correo);

        // Assert
        assertFalse(inactivityTrackingService.validarActividad(correo));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> obtenerActividades() {
        return (Map<String, Long>) ReflectionTestUtils.getField(
                inactivityTrackingService, "userLastActivity");
    }
}
