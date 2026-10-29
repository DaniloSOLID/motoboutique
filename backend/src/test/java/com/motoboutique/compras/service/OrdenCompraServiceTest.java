package com.motoboutique.compras.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.motoboutique.compras.domain.OrdenCompra;
import com.motoboutique.compras.repository.ConfiguracionEmpresaRepository;
import com.motoboutique.compras.repository.OrdenCompraDetalleRepository;
import com.motoboutique.compras.repository.OrdenCompraRepository;
import com.motoboutique.compras.repository.ProveedorRepository;
import com.motoboutique.catalogo.repository.ProductoRepository;
import com.motoboutique.inventario.repository.AlmacenRepository;
import com.motoboutique.inventario.repository.NumeracionDocumentoRepository;
import com.motoboutique.shared.exception.NegocioException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrdenCompraServiceTest {
    @Mock private OrdenCompraRepository ordenes;
    @Mock private OrdenCompraDetalleRepository detalles;
    @Mock private ProveedorRepository proveedores;
    @Mock private ProductoRepository productos;
    @Mock private AlmacenRepository almacenes;
    @Mock private ConfiguracionEmpresaRepository configuracion;
    @Mock private NumeracionDocumentoRepository numeracion;
    @InjectMocks private OrdenCompraService servicio;

    @Test
    void noPermiteConfirmarUnaOrdenQueNoEstaEnBorrador() {
        OrdenCompra orden = new OrdenCompra();
        orden.setEstado("CONFIRMADA");
        when(ordenes.bloquearActiva(15L)).thenReturn(Optional.of(orden));

        assertThrows(NegocioException.class, () -> servicio.confirmar(15L));
    }

    @Test
    void noPermiteAnularUnaOrdenRecibida() {
        OrdenCompra orden = new OrdenCompra();
        orden.setEstado("RECIBIDA");
        when(ordenes.bloquearActiva(15L)).thenReturn(Optional.of(orden));

        assertThrows(NegocioException.class, () -> servicio.anular(15L));
    }
}
