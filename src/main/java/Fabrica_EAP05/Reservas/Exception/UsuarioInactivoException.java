package Fabrica_EAP05.Reservas.Exception;

public class UsuarioInactivoException extends RuntimeException {
    public UsuarioInactivoException(String message) {
        super(message);
    }
}