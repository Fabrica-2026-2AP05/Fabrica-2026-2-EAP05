package Fabrica_EAP05.Reservas.DTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioRegistroRequestValidationTest {

    private Validator validator;
    private UsuarioRegistroRequest request;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        request = new UsuarioRegistroRequest();
        request.setNombre("Ana Pérez");
        request.setEmail("ana@correo.com");
        request.setContrasena("Password123!");
        request.setTelefono("3001234567");
        request.setDireccion("Calle 10 # 20-30");
    }

    @Test
    @DisplayName("CP-04-A y CP-05-A - Acepta datos completos")
    void datosValidos_noGeneranErrores() {
        // Arrange: los datos válidos se preparan en setUp().

        // Act
        Set<ConstraintViolation<UsuarioRegistroRequest>> resultado = validator.validate(request);

        // Assert
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("CP-04-B y CP-05-B - Rechaza nombre vacío")
    void nombreVacio_generaError() {
        // Arrange
        request.setNombre(" ");
        // Act
        Set<ConstraintViolation<UsuarioRegistroRequest>> resultado = validator.validate(request);
        // Assert
        assertCampoInvalido(resultado, "nombre");
    }

    @Test
    @DisplayName("CP-04-B y CP-05-B - Rechaza correo vacío")
    void correoVacio_generaError() {
        // Arrange
        request.setEmail("");
        // Act
        Set<ConstraintViolation<UsuarioRegistroRequest>> resultado = validator.validate(request);
        // Assert
        assertCampoInvalido(resultado, "email");
    }

    @Test
    @DisplayName("CP-04-B y CP-05-B - Rechaza formato de correo inválido")
    void correoInvalido_generaError() {
        // Arrange
        request.setEmail("correo-invalido");
        // Act
        Set<ConstraintViolation<UsuarioRegistroRequest>> resultado = validator.validate(request);
        // Assert
        assertCampoInvalido(resultado, "email");
    }

    @Test
    @DisplayName("CP-04-B y CP-05-B - Rechaza teléfono vacío")
    void telefonoVacio_generaError() {
        // Arrange
        request.setTelefono("");
        // Act
        Set<ConstraintViolation<UsuarioRegistroRequest>> resultado = validator.validate(request);
        // Assert
        assertCampoInvalido(resultado, "telefono");
    }

    @Test
    @DisplayName("CP-04-B y CP-05-B - Rechaza dirección vacía")
    void direccionVacia_generaError() {
        // Arrange
        request.setDireccion("");
        // Act
        Set<ConstraintViolation<UsuarioRegistroRequest>> resultado = validator.validate(request);
        // Assert
        assertCampoInvalido(resultado, "direccion");
    }

    @Test
    @DisplayName("CP-04-B y CP-05-B - Detecta todos los campos vacíos")
    void todosLosCamposVacios_reportanLosCincoCampos() {
        // Arrange
        request.setNombre("");
        request.setEmail("");
        request.setContrasena("");
        request.setTelefono("");
        request.setDireccion("");
        // Act
        Set<ConstraintViolation<UsuarioRegistroRequest>> resultado = validator.validate(request);

        // Assert
        assertAll(
                () -> assertCampoInvalido(resultado, "nombre"),
                () -> assertCampoInvalido(resultado, "email"),
                () -> assertCampoInvalido(resultado, "contrasena"),
                () -> assertCampoInvalido(resultado, "telefono"),
                () -> assertCampoInvalido(resultado, "direccion")
        );
    }

    private void assertCampoInvalido(Set<ConstraintViolation<UsuarioRegistroRequest>> resultado, String campo) {
        assertTrue(resultado.stream().anyMatch(v -> v.getPropertyPath().toString().equals(campo)));
    }
}
