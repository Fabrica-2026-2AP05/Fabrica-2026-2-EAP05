package Fabrica_EAP05.Reservas.Exception;

public class CuentaBloqueadaException extends RuntimeException {
    public CuentaBloqueadaException(String message) {
        super(message);
    }
}