package Fabrica_EAP05.Reservas.Service;

import java.util.List;
import java.util.UUID;

import Fabrica_EAP05.Reservas.Enums.EstadoReserva;
import Fabrica_EAP05.Reservas.Repository.ReservaServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import Fabrica_EAP05.Reservas.DTO.HorarioRequest;
import Fabrica_EAP05.Reservas.Entities.Horario;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Entities.Rol;
import Fabrica_EAP05.Reservas.Enums.EstadoHorario;
import Fabrica_EAP05.Reservas.Repository.HorarioRepository;
import Fabrica_EAP05.Reservas.Repository.ProveedorServicioRepository;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;

@Service
public class HorarioService {

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private ProveedorServicioRepository proveedorServicioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ReservaServicioRepository reservaServicioRepository;

    public Horario registrarHorario(HorarioRequest request) {

        // 1. Validar que la hora de inicio sea estrictamente anterior a la hora de fin
        if (!request.getInicio().isBefore(request.getFin())) {
            throw new IllegalArgumentException("La fecha y hora de inicio debe ser anterior a la fecha y hora de fin.");
        }

        // 2. Validar existencia del usuario y que tenga rol de proveedor
        Usuario proveedor = usuarioRepository.findById(request.getProveedorId())
                .orElseThrow(() -> new IllegalArgumentException("El proveedor especificado no existe."));

// Como Rol es un Enum, comparamos directamente con Rol.proveedor o usamos .name()
        if (proveedor.getRol() == null || proveedor.getRol() != Rol.proveedor) {
            throw new IllegalArgumentException("El usuario especificado no tiene rol de proveedor.");
        }

        // 3. Criterio de Aceptación: Validar que el proveedor esté asociado a al menos un servicio
        boolean asociado = proveedorServicioRepository.existsByProveedorId(request.getProveedorId());
        if (!asociado) {
            throw new IllegalArgumentException("El proveedor debe estar asociado a al menos un servicio para registrar un horario.");
        }

        // 4. Criterio de Aceptación: Validar solapamientos
        List<Horario> solapados = horarioRepository.findSolapados(
                request.getProveedorId(),
                request.getInicio(),
                request.getFin()
        );

        if (!solapados.isEmpty()) {
            Horario conflicto = solapados.get(0);
            throw new IllegalArgumentException("El horario ingresado se solapa con un periodo existente ("
                    + conflicto.getInicio() + " a " + conflicto.getFin() + ").");
        }

        // 5. Crear y guardar el registro
        Horario nuevoHorario = new Horario();
        nuevoHorario.setId(java.util.UUID.randomUUID());
        nuevoHorario.setProveedorId(request.getProveedorId());
        nuevoHorario.setInicio(request.getInicio());
        nuevoHorario.setFin(request.getFin());
        nuevoHorario.setEstado(EstadoHorario.disponible);

        return horarioRepository.save(nuevoHorario);

    }

    // NUEVA LÓGICA HU10: BLOQUEAR HORARIO
    // ==========================================
    public Horario bloquearHorario(UUID horarioId, String emailProveedor) {
        // 1. Buscar al usuario proveedor por su email (extraído del token de autenticación)
        Usuario proveedor = usuarioRepository.findByEmail(emailProveedor)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con el email: " + emailProveedor));

        // 2. Verificar que el horario exista y pertenezca a este proveedor
        Horario horario = horarioRepository.findByIdAndProveedorId(horarioId, proveedor.getId())
                .orElseThrow(() -> new IllegalArgumentException("El horario no existe o no pertenece al proveedor autenticado."));

        // 3. Validar si existe alguna reserva con estado 'confirmada' para este horario (Task 95)
        boolean tieneReservaConfirmada = reservaServicioRepository.existsByHorarioIdAndEstado(horarioId, EstadoReserva.confirmada);
        if (tieneReservaConfirmada) {
            throw new IllegalStateException("El bloqueo es rechazado porque existe una reserva confirmada asociada a este horario.");
        }

        // 4. Cambiar el estado del horario a 'bloqueado' (Task 94)
        horario.setEstado(EstadoHorario.bloqueado);
        return horarioRepository.save(horario);
    }
}