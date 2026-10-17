package Fabrica_EAP05.Reservas.Enums;

// Los nombres deben coincidir exactamente con las etiquetas del enum
// estado_horario de PostgreSQL (en minúscula), porque Hibernate persiste name().
public enum EstadoHorario {
    disponible,
    bloqueado,
    reservado
}
