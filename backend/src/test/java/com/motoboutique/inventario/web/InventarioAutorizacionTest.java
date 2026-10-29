package com.motoboutique.inventario.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.motoboutique.inventario.dto.MovimientoDto;
import com.motoboutique.inventario.service.AdministracionInventarioService;
import com.motoboutique.inventario.service.AlmacenService;
import com.motoboutique.inventario.service.InventarioConsultaService;
import com.motoboutique.inventario.service.MovimientoInventarioService;
import com.motoboutique.seguridad.dto.UsuarioAutenticado;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:inventario-auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seed.enabled=false",
        "app.cors.allowed-origins=http://localhost:5173"
})
@AutoConfigureMockMvc
class InventarioAutorizacionTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private InventarioConsultaService consultaService;
    @MockitoBean private MovimientoInventarioService movimientoService;
    @MockitoBean private AlmacenService almacenService;
    @MockitoBean private AdministracionInventarioService administracionService;

    @Test
    void usuarioConPermisoPuedeConsultarStock() throws Exception {
        mockMvc.perform(get("/api/inventario/stock")
                        .with(user("almacenero").roles("ALMACENERO")
                                .authorities(() -> "INVENTARIO_VER")))
                .andExpect(status().isOk());
    }

    @Test
    void usuarioSinPermisoRecibe403() throws Exception {
        mockMvc.perform(get("/api/inventario/stock")
                        .with(user("consulta").roles("CONSULTA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void contadorPuedeConsultarPeroNoRegistrarMovimientos() throws Exception {
        mockMvc.perform(post("/api/inventario/entradas")
                        .with(user("contador").roles("CONTADOR")
                                .authorities(() -> "INVENTARIO_VER", () -> "KARDEX_VER",
                                        () -> "REPORTE_VER", () -> "REPORTE_EXPORTAR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productoId":1,"almacenId":1,"cantidad":1.000,
                                 "costoUnitario":10.00}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioSinSesionRecibe401() throws Exception {
        mockMvc.perform(get("/api/inventario/stock"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void solicitantePuedeListarSoloSusAjustes() throws Exception {
        var usuario = new UsuarioAutenticado(44L, "Almacenero", "almacen@example.test",
                "", true, List.of(new SimpleGrantedAuthority("AJUSTE_SOLICITAR")));

        mockMvc.perform(get("/api/inventario/ajustes").with(user(usuario)))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(administracionService).listarAjustes(
                null, null, null, null, null, 44L, 0, 20);
    }

    @Test
    void usuarioSinPermisoNoPuedeListarAjustes() throws Exception {
        mockMvc.perform(get("/api/inventario/ajustes")
                        .with(user("consulta").roles("CONSULTA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void almacenistaConPermisoPuedeRegistrarEntrada() throws Exception {
        org.mockito.Mockito.when(movimientoService.registrarEntrada(
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MovimientoDto(1L, "ENT-000001", java.time.Instant.now(),
                        "ENTRADA", 1L, "P-1", "Producto", 1L, "Principal",
                        new BigDecimal("1.000"), new BigDecimal("10.00"),
                        new BigDecimal("1.000"), new BigDecimal("10.00"),
                        "OC-1", null, "almacenero"));

        mockMvc.perform(post("/api/inventario/entradas")
                        .with(user("almacenero").roles("ALMACENERO")
                                .authorities(() -> "INVENTARIO_EDITAR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productoId":1,"almacenId":1,"cantidad":1.000,
                                 "costoUnitario":10.00,"documentoReferencia":"OC-1"}
                                """))
                .andExpect(status().isOk());
    }
}
