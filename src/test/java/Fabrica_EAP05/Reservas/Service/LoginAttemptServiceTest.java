package Fabrica_EAP05.Reservas.Service;

import Fabrica_EAP05.Reservas.Exception.CuentaBloqueadaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {
    private LoginAttemptService service;
    private String email;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
        email = "cliente@correo.com";
    }

    @Test @DisplayName("Bloquea la cuenta después de cinco fallos")
    void verificarBloqueo_cincoFallos_lanzaExcepcion() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            service.registrarFallo(email);
        }

        // Act
        CuentaBloqueadaException error = assertThrows(CuentaBloqueadaException.class,
                () -> service.verificarBloqueo(email));

        // Assert
        assertTrue(error.getMessage().contains("bloqueada temporalmente"));
    }

    @Test @DisplayName("Un inicio exitoso reinicia los fallos")
    void registrarExito_despuesDeFallos_eliminaBloqueo() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            service.registrarFallo(email);
        }

        // Act
        service.registrarExito(email);

        // Assert
        assertDoesNotThrow(() -> service.verificarBloqueo(email));
    }
}
