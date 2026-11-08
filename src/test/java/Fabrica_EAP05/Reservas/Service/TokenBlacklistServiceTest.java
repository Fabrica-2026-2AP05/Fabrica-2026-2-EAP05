package Fabrica_EAP05.Reservas.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBlacklistServiceTest {

    private TokenBlacklistService tokenBlacklistService;
    private String token;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService();
        token = "jwt-token-de-prueba";
    }

    @Test
    @DisplayName("CP-03-C - Agregar un token lo marca como revocado")
    void agregarToken_tokenValido_quedaEnListaNegra() {
        // Arrange: el servicio y el token se preparan en setUp().

        // Act
        tokenBlacklistService.agregarTokenALista(token);

        // Assert
        assertTrue(tokenBlacklistService.estaEnLista(token));
    }

    @Test
    @DisplayName("CP-03-A - Eliminar un token permite dejar de considerarlo revocado")
    void limpiarToken_tokenRevocado_saleDeListaNegra() {
        // Arrange
        tokenBlacklistService.agregarTokenALista(token);
        assertTrue(tokenBlacklistService.estaEnLista(token));

        // Act
        tokenBlacklistService.limpiarToken(token);

        // Assert
        assertFalse(tokenBlacklistService.estaEnLista(token));
    }
}
