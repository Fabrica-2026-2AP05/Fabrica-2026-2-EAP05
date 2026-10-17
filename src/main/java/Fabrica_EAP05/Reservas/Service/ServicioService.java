package Fabrica_EAP05.Reservas.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Fabrica_EAP05.Reservas.DTO.ModificarServicioRequest;
import Fabrica_EAP05.Reservas.DTO.ModificarServicioResponse;
import Fabrica_EAP05.Reservas.DTO.RegistrarServicioRequest;
import Fabrica_EAP05.Reservas.DTO.RegistrarServicioResponse;
import Fabrica_EAP05.Reservas.Entities.Recurso;
import Fabrica_EAP05.Reservas.Entities.Servicio;
import Fabrica_EAP05.Reservas.Exception.RecursoDuplicadoException;
import Fabrica_EAP05.Reservas.Repository.RecursoRepository;
import Fabrica_EAP05.Reservas.Repository.ReservaServicioRepository;
import Fabrica_EAP05.Reservas.Repository.ServicioRepository;
import jakarta.persistence.EntityManager;

@Service
public class ServicioService {

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private RecursoRepository recursoRepository;

    @Autowired
    private ReservaServicioRepository reservaServicioRepository;

    @Autowired
    private EntityManager entityManager;

    public boolean validarRecursoRequerido(UUID recursoId) {
        if (recursoId == null) {
            return true;
        }

        Recurso recurso = recursoRepository.findById(recursoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El recurso con ID " + recursoId + " no existe en el sistema"));

        if (!Boolean.TRUE.equals(recurso.getActivo())) {
            throw new IllegalArgumentException("El recurso con ID " + recursoId + " no está activo");
        }

        return true;
    }

    public RegistrarServicioResponse registrarServicio(RegistrarServicioRequest request) {
        if (servicioRepository.existsByNombre(request.getNombre())) {
            throw new RecursoDuplicadoException(
                    "El nombre del servicio '" + request.getNombre() + "' ya está registrado");
        }

        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del servicio es obligatorio");
        }

        Recurso recurso = null;
        if (request.getRecursoId() != null) {
            validarRecursoRequerido(request.getRecursoId());
            recurso = recursoRepository.findById(request.getRecursoId()).orElse(null);
        }

        Servicio servicio = new Servicio();
        servicio.setId(UUID.randomUUID());
        servicio.setNombre(request.getNombre());
        servicio.setDescripcion(request.getDescripcion());
        servicio.setDuracionMin(request.getDuracionMin());
        servicio.setRecurso(recurso);
        servicio.setActivo(true);

        Servicio guardado = servicioRepository.saveAndFlush(servicio);
        entityManager.detach(guardado);
        Servicio recargado = servicioRepository.findById(guardado.getId())
                .orElseThrow(() -> new IllegalStateException("No se pudo recuperar el servicio recién creado"));

        return new RegistrarServicioResponse(
                recargado.getId(),
                recargado.getNombre(),
                recargado.getDescripcion(),
                recargado.getDuracionMin(),
                recargado.getRecurso() != null ? recargado.getRecurso().getId() : null,
                recargado.getRecurso() != null ? recargado.getRecurso().getNombre() : null,
                recargado.getActivo(),
                recargado.getCreatedAt()
        );
    }

    public ModificarServicioResponse modificarServicio(UUID servicioId, ModificarServicioRequest request) {
        // 1. El servicio debe existir
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new NoSuchElementException(
                        "El servicio con ID " + servicioId + " no existe en el sistema"));

        // 2. Regla crítica: no se modifica un servicio con reservas asociadas
        long reservas = reservaServicioRepository.countByServicioId(servicioId);
        if (reservas > 0) {
            throw new IllegalStateException(
                    "No se puede modificar el servicio. Existe(n) " + reservas
                            + " reserva(s) asociada(s) a este servicio.");
        }

        // 3. Validar campos enviados
        if (request.getNombre() != null) {
            if (request.getNombre().isBlank()) {
                throw new IllegalArgumentException("El nombre del servicio no puede estar vacío");
            }
            if (!request.getNombre().equals(servicio.getNombre())
                    && servicioRepository.existsByNombre(request.getNombre())) {
                throw new RecursoDuplicadoException(
                        "El nombre del servicio '" + request.getNombre() + "' ya está registrado");
            }
        }

        if (request.getDuracionMin() != null && request.getDuracionMin() <= 0) {
            throw new IllegalArgumentException("La duración debe ser mayor a 0");
        }

        // 4. Validar recurso si fue especificado
        Recurso recurso = null;
        if (request.getRecursoId() != null) {
            validarRecursoRequerido(request.getRecursoId());
            recurso = recursoRepository.findById(request.getRecursoId()).orElse(null);
        }

        // 5-6. Actualización parcial: solo los campos que vinieron en el request
        if (request.getNombre() != null) {
            servicio.setNombre(request.getNombre());
        }
        if (request.getDescripcion() != null) {
            servicio.setDescripcion(request.getDescripcion());
        }
        if (request.getDuracionMin() != null) {
            servicio.setDuracionMin(request.getDuracionMin());
        }
        if (recurso != null) {
            servicio.setRecurso(recurso);
        }

        // 7. Guardar, recargar y retornar
        Servicio guardado = servicioRepository.saveAndFlush(servicio);
        entityManager.detach(guardado);
        Servicio recargado = servicioRepository.findById(guardado.getId())
                .orElseThrow(() -> new IllegalStateException("No se pudo recuperar el servicio modificado"));

        return new ModificarServicioResponse(
                recargado.getId(),
                recargado.getNombre(),
                recargado.getDescripcion(),
                recargado.getDuracionMin(),
                recargado.getRecurso() != null ? recargado.getRecurso().getId() : null,
                recargado.getRecurso() != null ? recargado.getRecurso().getNombre() : null,
                recargado.getActivo(),
                recargado.getCreatedAt()
        );
    }
}
