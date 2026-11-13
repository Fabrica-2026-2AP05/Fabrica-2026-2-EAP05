package Fabrica_EAP05.Reservas.DTO;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrarEventoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private OffsetDateTime inicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private OffsetDateTime fin;

    @NotNull(message = "La capacidad es obligatoria")
    @Positive(message = "La capacidad debe ser mayor a 0")
    private Integer capacidad;

    @AssertTrue(message = "La fecha de fin debe ser posterior a la de inicio")
    public boolean isRangoValido() {
        return inicio == null || fin == null || fin.isAfter(inicio);
    }
}