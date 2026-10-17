package Fabrica_EAP05.Reservas.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import Fabrica_EAP05.Reservas.Exception.CuentaBloqueadaException;

import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private static final int MAX_INTENTOS = 5;

    private static final int minutosBloqueo = 15;

    private final ConcurrentHashMap<String, Intentos> registro = new ConcurrentHashMap<>();

    private static class Intentos {
        int fallos = 0;
        Instant bloqueadoHasta = null;
    }

    public void registrarFallo(String email) {
        Intentos intentos = registro.computeIfAbsent(email, k -> new Intentos());
        synchronized (intentos) {
            intentos.fallos++;
            if (intentos.fallos >= MAX_INTENTOS) {
                intentos.bloqueadoHasta = Instant.now().plusSeconds(minutosBloqueo * 60);
            }
        }
    }

    public void registrarExito(String email) {
        registro.remove(email);
    }

    public void verificarBloqueo(String email) {
        Intentos intentos = registro.get(email);
        if (intentos == null || intentos.bloqueadoHasta == null) {
            return;
        }

        synchronized (intentos) {
            if (intentos.bloqueadoHasta == null) {
                return;
            }
            if (Instant.now().isBefore(intentos.bloqueadoHasta)) {
                long minutosRestantes = Math.max(1,
                        (intentos.bloqueadoHasta.getEpochSecond() - Instant.now().getEpochSecond()) / 60);
                throw new CuentaBloqueadaException(
                        "Cuenta bloqueada temporalmente por múltiples intentos fallidos. Intenta de nuevo en " + minutosRestantes + " minuto(s).");
            } else {
                intentos.fallos = 0;
                intentos.bloqueadoHasta = null;
            }
        }
    }
}
