package Fabrica_EAP05.Reservas.DTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LoginDTOValidationTest {

    private Validator validator;
    private LoginDTO login;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        login = new LoginDTO();
        login.setEmail("cliente@correo.com");
        login.setContrasena("Clave123*");
    }

    @Test
    @DisplayName("CP-01-A - Acepta credenciales completas y válidas")
    void credencialesValidas_noGeneranErrores() {
        // Arrange: los datos válidos se preparan en setUp().

        // Act
        Set<ConstraintViolation<LoginDTO>> resultado = validator.validate(login);

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("CP-01-D - Rechaza un correo vacío")
    void correoVacio_generaError() {
        // Arrange
        login.setEmail("");

        // Act
        Set<ConstraintViolation<LoginDTO>> resultado = validator.validate(login);
        // Assert
        assertCampoInvalido(resultado, "email");
    }

    @Test
    @DisplayName("CP-01-D - Rechaza un correo con formato incorrecto")
    void correoInvalido_generaError() {
        // Arrange
        login.setEmail("correo-invalido");

        // Act
        Set<ConstraintViolation<LoginDTO>> resultado = validator.validate(login);
        // Assert
        assertCampoInvalido(resultado, "email");
    }

    @Test
    @DisplayName("CP-01-D - Rechaza una contraseña vacía")
    void contrasenaVacia_generaError() {
        // Arrange
        login.setContrasena(" ");

        // Act
        Set<ConstraintViolation<LoginDTO>> resultado = validator.validate(login);
        // Assert
        assertCampoInvalido(resultado, "contrasena");
    }

    @Test
    @DisplayName("CP-01-D - Detecta ambos campos requeridos vacíos")
    void camposVacios_generanDosErrores() {
        // Arrange
        login.setEmail("");
        login.setContrasena("");

        // Act
        Set<ConstraintViolation<LoginDTO>> resultado = validator.validate(login);

        // Assert
        assertEquals(2, resultado.size());
    }

    private void assertCampoInvalido(Set<ConstraintViolation<LoginDTO>> resultado, String campo) {
        assertTrue(resultado.stream().anyMatch(v -> v.getPropertyPath().toString().equals(campo)));
    }
}
