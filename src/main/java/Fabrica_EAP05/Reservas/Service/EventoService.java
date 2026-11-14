package Fabrica_EAP05.Reservas.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Fabrica_EAP05.Reservas.DTO.RegistrarEventoRequest;
import Fabrica_EAP05.Reservas.DTO.RegistroEventoResponse;
import Fabrica_EAP05.Reservas.Entities.Evento;
import Fabrica_EAP05.Reservas.Exception.RecursoDuplicadoException;
import Fabrica_EAP05.Reservas.Repository.EventoRepository;

@Service
public class EventoService {

    @Autowired
    private EventoRepository eventoRepository;

    @Transactional
    public RegistroEventoResponse registrarEvento(RegistrarEventoRequest request) {
        if (eventoRepository.existsByNombre(request.getNombre())) {
            throw new RecursoDuplicadoException("El nombre de este evento ya está registrado");
        }

        Evento evento = new Evento();
        evento.setId(UUID.randomUUID());
        evento.setNombre(request.getNombre());
        evento.setDescripcion(request.getDescripcion());
        evento.setInicio(aLocal(request.getInicio()));
        evento.setFin(aLocal(request.getFin()));
        evento.setCapacidad(request.getCapacidad());
        evento.setActivo(true);

        Evento guardado = eventoRepository.saveAndFlush(evento);

        return new RegistroEventoResponse(
                guardado.getId(),
                guardado.getNombre(),
                guardado.getDescripcion(),
                aOffset(guardado.getInicio()),
                aOffset(guardado.getFin()),
                guardado.getCapacidad(),
                guardado.getActivo(),
                guardado.getCreatedAt()
        );
    }

    // Las entidades mapean timestamptz como LocalDateTime en la zona de la JVM
    private LocalDateTime aLocal(OffsetDateTime fecha) {
        return fecha.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private OffsetDateTime aOffset(LocalDateTime fecha) {
        return fecha != null ? fecha.atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
    }
}