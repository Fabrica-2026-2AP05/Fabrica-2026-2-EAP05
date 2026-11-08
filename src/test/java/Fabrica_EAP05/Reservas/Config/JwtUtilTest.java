package Fabrica_EAP05.Reservas.Config;

import Fabrica_EAP05.Reservas.Entities.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRETO = "clave-super-segura-para-pruebas-unitarias-1234567890";
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRETO);
    }

    @Test
    @DisplayName("CP-01-A - Genera JWT válido con correo y rol")
    void generarToken_datosValidos_conservaDatos() {
        // Arrange: JwtUtil se crea en setUp().

        // Act
        String token = jwtUtil.generarToken("cliente@correo.com", Rol.cliente);

        // Assert
        assertAll(
                () -> assertTrue(jwtUtil.validarToken(token)),
                () -> assertEquals("cliente@correo.com", jwtUtil.extraerEmail(token)),
                () -> assertEquals("cliente", jwtUtil.extraerRol(token))
        );
    }

    @Test
    @DisplayName("CP-01-A - Conserva el rol proveedor en el JWT")
    void generarToken_proveedor_conservaRol() {
        // Arrange: JwtUtil se crea en setUp().
        // Act
        String token = jwtUtil.generarToken("proveedor@correo.com", Rol.proveedor);
        // Assert
        assertEquals("proveedor", jwtUtil.extraerRol(token));
    }

    @Test
    @DisplayName("CP-01-A - Usa rol cliente cuando el rol es nulo")
    void generarToken_rolNulo_usaCliente() {
        // Arrange: JwtUtil se crea en setUp().
        // Act
        String token = jwtUtil.generarToken("cliente@correo.com", null);
        // Assert
        assertEquals("cliente", jwtUtil.extraerRol(token));
    }

    @Test
    @DisplayName("CP-03-B - Rechaza token inválido")
    void validarToken_tokenInvalido_retornaFalse() {
        // Arrange
        String tokenInvalido = "esto-no-es-un-jwt";
        // Act
        boolean esValido = jwtUtil.validarToken(tokenInvalido);
        // Assert
        assertFalse(esValido);
    }

    @Test
    @DisplayName("CP-03-B - Rechaza token alterado")
    void validarToken_tokenAlterado_retornaFalse() {
        // Arrange
        String token = jwtUtil.generarToken("cliente@correo.com", Rol.cliente);
        // Act
        boolean esValido = jwtUtil.validarToken(token + "alterado");
        // Assert
        assertFalse(esValido);
    }
}
