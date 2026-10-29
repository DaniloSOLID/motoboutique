package com.motoboutique.inventario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:inventario-query;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.seed.enabled=false"
})
class InventarioConsultaServiceTest {
    @Autowired private InventarioConsultaService servicio;

    @Test
    void resumenConsultaStockValorizadoYAlertasSinDatos() {
        InventarioConsultaService.ResumenInventario resumen = servicio.resumen();

        assertEquals(0, resumen.productosActivos());
        assertEquals(BigDecimal.ZERO, resumen.stockValorizado());
        assertEquals(0, resumen.alertasStock());
    }

    @Test
    void filtraStockAgotadoSinErroresDeCriteria() {
        var pagina = servicio.listarStock(null, null, null, "AGOTADO", 0, 20,
                "producto.nombre", "asc");

        assertEquals(0, pagina.getTotalElementos());
    }
}
