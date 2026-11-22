package Fabrica_EAP05.Reservas.Integracion;

import Fabrica_EAP05.Reservas.Config.JwtUtil;
import Fabrica_EAP05.Reservas.Entities.Rol;
import Fabrica_EAP05.Reservas.Entities.Usuario;
import Fabrica_EAP05.Reservas.Repository.UsuarioRepository;
import Fabrica_EAP05.Reservas.Service.InactivityTrackingService;
import Fabrica_EAP05.Reservas.Service.ReservaServicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservaIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private InactivityTrackingService inactivityTrackingService;

    @MockitoBean
    private ReservaServicioService reservaServicioService;

    @BeforeEach
    void setUp() {
        when(inactivityTrackingService.validarActividad(anyString())).thenReturn(true);
    }

    private Usuario crearUsuarioActivo(String email, Rol rol) {
        Usuario u = new Usuario();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setNombre("Usuario Test");
        u.setRol(rol);
        u.setActivo(true);
        return u;
    }

    @Test
    @DisplayName("PI-01: Rechaza acceso sin token JWT a endpoints protegidos (401)")
    void consultarHistorial_sinToken_retornaUnauthorized() throws Exception {
        mockMvc.perform(get("/api/reservas/historial"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    @DisplayName("PI-02: Impide que un cliente acceda a historial global del negocio (403)")
    void consultarHistorialNegocio_conRolCliente_retornaForbidden() throws Exception {
        String email = "cliente@test.com";
        when(usuarioRepository.findByEmail(email))
                .thenReturn(Optional.of(crearUsuarioActivo(email, Rol.cliente)));

        String tokenCliente = jwtUtil.generarToken(email, Rol.cliente);

        mockMvc.perform(get("/api/reservas/historial-negocio")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PI-03: Cliente autenticado consulta su historial exitosamente (200)")
    void consultarHistorial_conTokenCliente_retornaOk() throws Exception {
        String email = "cliente@test.com";
        when(usuarioRepository.findByEmail(email))
                .thenReturn(Optional.of(crearUsuarioActivo(email, Rol.cliente)));
        when(reservaServicioService.consultarHistorialCliente(email))
                .thenReturn(List.of());

        String tokenCliente = jwtUtil.generarToken(email, Rol.cliente);

        mockMvc.perform(get("/api/reservas/historial")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("No existen reservas registradas"));
    }

    @Test
    @DisplayName("PI-04: Administrador consulta ocupación de servicios con rango de fechas válido (200)")
    void consultarOcupacionServicios_conTokenAdmin_retornaOk() throws Exception {
        String emailAdmin = "admin@test.com";
        when(usuarioRepository.findByEmail(emailAdmin))
                .thenReturn(Optional.of(crearUsuarioActivo(emailAdmin, Rol.administrador)));
        when(reservaServicioService.consultarOcupacionServicios(
                eq(emailAdmin), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        String tokenAdmin = jwtUtil.generarToken(emailAdmin, Rol.administrador);

        mockMvc.perform(get("/api/reservas/ocupacion-servicios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .param("desde", "2026-11-01T00:00:00")
                        .param("hasta", "2026-11-30T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("No existen datos de ocupación para el periodo consultado"));
    }

    @Test
    @DisplayName("PI-05: Rechaza consulta de ocupación con formato de fecha inválido (400)")
    void consultarOcupacionServicios_fechaInvalida_retornaBadRequest() throws Exception {
        String emailAdmin = "admin@test.com";
        when(usuarioRepository.findByEmail(emailAdmin))
                .thenReturn(Optional.of(crearUsuarioActivo(emailAdmin, Rol.administrador)));

        String tokenAdmin = jwtUtil.generarToken(emailAdmin, Rol.administrador);

        mockMvc.perform(get("/api/reservas/ocupacion-servicios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin)
                        .param("desde", "fecha-mala")
                        .param("hasta", "2026-11-30T23:59:59"))
                .andExpect(status().isBadRequest());
    }
}