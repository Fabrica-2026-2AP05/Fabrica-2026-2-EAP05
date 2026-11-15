package Fabrica_EAP05.Reservas.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Fabrica_EAP05.Reservas.DTO.AgendaResponse;
import Fabrica_EAP05.Reservas.Entities.Horario;
import Fabrica_EAP05.Reservas.Entities.ReservaServicio;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Repository.HorarioRepository;
import Fabrica_EAP05.Reservas.Repository.ReservaServicioRepository;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;

@Service
public class AgendaService {

    @Autowired
    private ReservaServicioRepository reservaServicioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HorarioRepository horarioRepository;

    public List<AgendaResponse> consultarAgendaProveedor(String emailProveedor) {
        Usuario proveedor = usuarioRepository.findByEmail(emailProveedor)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));

        List<ReservaServicio> reservas = reservaServicioRepository.findByProveedorId(proveedor.getId());

        return reservas.stream().map(r -> {
            // BUG18: fecha y hora de la cita desde el horario reservado; null si no existe
            Horario horario = r.getHorarioId() != null
                    ? horarioRepository.findById(r.getHorarioId()).orElse(null)
                    : null;
            return new AgendaResponse(
                    r.getId(),
                    r.getClienteId(),
                    r.getServicioId(),
                    r.getHorarioId(),
                    r.getEstado() != null ? r.getEstado().name() : "confirmada",
                    r.getCreadaEn(),
                    horario != null ? aOffset(horario.getInicio()) : null,
                    horario != null ? aOffset(horario.getFin()) : null);
        }).collect(Collectors.toList());
    }

    // Misma conversión que ReservaServicioService.aOffset: las entidades mapean
    // timestamptz como LocalDateTime en la zona de la JVM
    private OffsetDateTime aOffset(LocalDateTime fecha) {
        return fecha != null ? fecha.atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
    }
}
