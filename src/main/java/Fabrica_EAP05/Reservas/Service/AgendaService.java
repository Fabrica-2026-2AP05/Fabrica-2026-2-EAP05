package Fabrica_EAP05.Reservas.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Fabrica_EAP05.Reservas.DTO.AgendaResponse;
import Fabrica_EAP05.Reservas.Entities.ReservaServicio;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Repository.ReservaServicioRepository;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;

@Service
public class AgendaService {

    @Autowired
    private ReservaServicioRepository reservaServicioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public List<AgendaResponse> consultarAgendaProveedor(String emailProveedor) {
        Usuario proveedor = usuarioRepository.findByEmail(emailProveedor)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));

        List<ReservaServicio> reservas = reservaServicioRepository.findByProveedorId(proveedor.getId());

        return reservas.stream().map(r -> new AgendaResponse(
                r.getId(),
                r.getClienteId(),
                r.getServicioId(),
                r.getHorarioId(),
                r.getEstado() != null ? r.getEstado().name() : "confirmada",
                r.getCreadaEn()
        )).collect(Collectors.toList());
    }
}