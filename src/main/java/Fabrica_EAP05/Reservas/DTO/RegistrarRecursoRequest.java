package Fabrica_EAP05.Reservas.DTO;



import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter


public class RegistrarRecursoRequest {

    @NotBlank(message ="El nombre es necesario")    
    private String nombre;


    @NotBlank (message = "La descripción es necesaria")
    private String descripcion;

}
