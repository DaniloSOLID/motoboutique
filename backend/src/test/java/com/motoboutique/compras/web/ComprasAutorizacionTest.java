package com.motoboutique.compras.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.motoboutique.compras.service.OrdenCompraService;
import com.motoboutique.compras.service.ProveedorService;
import com.motoboutique.compras.service.RecepcionCompraService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:compras-auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seed.enabled=false",
        "app.cors.allowed-origins=http://localhost:5173"
})
@AutoConfigureMockMvc
class ComprasAutorizacionTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private ProveedorService proveedorService;
    @MockitoBean private OrdenCompraService ordenService;
    @MockitoBean private RecepcionCompraService recepcionService;

    @Test
    void contadorPuedeConsultarProveedores() throws Exception {
        mockMvc.perform(get("/api/compras/proveedores")
                        .with(user("contador").roles("CONTADOR")
                                .authorities(() -> "COMPRA_VER")))
                .andExpect(status().isOk());
    }

    @Test
    void usuarioSinPermisoNoPuedeConsultarProveedores() throws Exception {
        mockMvc.perform(get("/api/compras/proveedores")
                        .with(user("consulta").roles("CONSULTA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioSinSesionRecibe401() throws Exception {
        mockMvc.perform(get("/api/compras/proveedores"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void almacenistaConPermisoPuedeRegistrarProveedor() throws Exception {
        mockMvc.perform(post("/api/compras/proveedores")
                        .with(user("almacenero").roles("ALMACENERO")
                                .authorities(() -> "COMPRA_EDITAR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"razonSocial":"Proveedor de prueba","ruc":"20123456789"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void contadorNoPuedeCrearProveedores() throws Exception {
        mockMvc.perform(post("/api/compras/proveedores")
                        .with(user("contador").roles("CONTADOR")
                                .authorities(() -> "COMPRA_VER", () -> "REPORTE_VER",
                                        () -> "REPORTE_EXPORTAR", () -> "KARDEX_VER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"razonSocial":"Proveedor de prueba","ruc":"20123456789"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void proveedorRequiereRucDeOnceDigitos() throws Exception {
        mockMvc.perform(post("/api/compras/proveedores")
                        .with(user("almacenero").roles("ALMACENERO")
                                .authorities(() -> "COMPRA_EDITAR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"razonSocial":"Proveedor de prueba","ruc":"123"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
