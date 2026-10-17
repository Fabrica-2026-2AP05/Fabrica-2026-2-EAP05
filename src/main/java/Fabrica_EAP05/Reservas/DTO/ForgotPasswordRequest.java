package Fabrica_EAP05.Reservas.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {

    @Email(message = "El email no es válido")
    @NotBlank(message = "El email es obligatorio")
    private String email;
}