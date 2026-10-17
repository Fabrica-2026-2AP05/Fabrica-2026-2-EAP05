package Fabrica_EAP05.Reservas.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Fabrica_EAP05.Reservas.DTO.DisponibilidadRecursoResponse;
import Fabrica_EAP05.Reservas.DTO.RecursoDisponibilidadResponse;
import Fabrica_EAP05.Reservas.DTO.RegistrarRecursoRequest;
import Fabrica_EAP05.Reservas.DTO.RegistroRecursoResponse;
import Fabrica_EAP05.Reservas.Entities.DisponibilidadRecurso;
import Fabrica_EAP05.Reservas.Entities.Recurso;
import Fabrica_EAP05.Reservas.Exception.ConflictoDisponibilidadException;
import Fabrica_EAP05.Reservas.Exception.RecursoComprometidoException;
import Fabrica_EAP05.Reservas.Exception.RecursoDuplicadoException;
import Fabrica_EAP05.Reservas.Exception.RecursoNoEncontradoException;
import Fabrica_EAP05.Reservas.Repository.DisponibilidadRecursoRepository;
import Fabrica_EAP05.Reservas.Repository.RecursoRepository;
import Fabrica_EAP05.Reservas.Repository.ReservaServicioRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;


@Service 
public class RecursoService{

    @Autowired 
    RecursoRepository recursoRepository;

    @Autowired 
    ReservaServicioRepository reservaServicioRepository;

    @Autowired 
    DisponibilidadRecursoRepository disponibilidadRecursoRepository;

    @Autowired
    private EntityManager entityManager;



    public RegistroRecursoResponse registrarRecurso(RegistrarRecursoRequest request) {
        if (recursoRepository.existsByNombre(request.getNombre())) {
            throw new RecursoDuplicadoException("El nombre de este recurso ya está registrado");
        }
        
        Recurso recurso = new Recurso();
        recurso.setId(UUID.randomUUID());
        recurso.setNombre(request.getNombre());
        recurso.setDescripcion(request.getDescripcion());
        recurso.setActivo(true);
        Recurso guardado = recursoRepository.saveAndFlush(recurso);
        entityManager.detach(guardado);
        Recurso recargado = recursoRepository.findById(guardado.getId())
            .orElseThrow(() -> new IllegalStateException("No se pudo recuperar el recurso recién creado"));
        return new RegistroRecursoResponse(
                recargado.getId(),
                recargado.getNombre(),
                recargado.getDescripcion(),
                recargado.getActivo(),
                recargado.getCreatedAt()
        );
    }
    @Transactional
    public RecursoDisponibilidadResponse cambiarDisponibilidad(UUID id, boolean activo) {
        Recurso recurso = recursoRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El recurso no existe"));

        // Solo al desactivar: no se puede si hay reservas confirmadas que aún no terminan
        if (!activo && Boolean.TRUE.equals(recurso.getActivo())) {
            long pendientes = reservaServicioRepository.contarReservasVigentesDeRecurso(id, LocalDateTime.now());
            if (pendientes > 0) {
                throw new RecursoComprometidoException(
                        "No se puede marcar como no disponible: el recurso tiene "
                                + pendientes + " reserva(s) confirmada(s) pendiente(s)");
            }
        }

        recurso.setActivo(activo);
        recursoRepository.save(recurso);

        return new RecursoDisponibilidadResponse(recurso.getId(), recurso.getNombre(), recurso.getActivo());
    }
    @Transactional
        public DisponibilidadRecursoResponse definirDisponibilidad(UUID recursoId,
                                                                OffsetDateTime inicio,
                                                                OffsetDateTime fin) {
            // El lock sobre el recurso serializa altas de periodos y cambios de 'activo'
            Recurso recurso = recursoRepository.findByIdParaActualizar(recursoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("El recurso no existe"));

            if (!Boolean.TRUE.equals(recurso.getActivo())) {
                throw new ConflictoDisponibilidadException(
                        "El recurso no está activo: actívalo antes de definir su disponibilidad");
            }

            LocalDateTime ini = aLocal(inicio);
            LocalDateTime fi = aLocal(fin);

            if (disponibilidadRecursoRepository.contarSolapadas(recursoId, ini, fi) > 0) {
                throw new ConflictoDisponibilidadException(
                        "El periodo se cruza con una disponibilidad ya definida para este recurso");
            }

            DisponibilidadRecurso periodo = new DisponibilidadRecurso();
            periodo.setId(UUID.randomUUID());
            periodo.setRecursoId(recursoId);
            periodo.setInicio(ini);
            periodo.setFin(fi);
            disponibilidadRecursoRepository.saveAndFlush(periodo);

            return new DisponibilidadRecursoResponse(periodo.getId(), recursoId, inicio, fin);
        }

        // Las entidades mapean timestamptz como LocalDateTime en la zona de la JVM
        private LocalDateTime aLocal(OffsetDateTime fecha) {
            return fecha.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        }
}
