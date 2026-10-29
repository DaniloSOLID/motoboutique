package com.motoboutique.compras.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.motoboutique.compras.domain.Proveedor;
import com.motoboutique.compras.dto.ProveedorGuardarDto;
import com.motoboutique.compras.repository.CodigoProveedorRepository;
import com.motoboutique.compras.repository.OrdenCompraRepository;
import com.motoboutique.compras.repository.ProveedorRepository;
import com.motoboutique.shared.exception.NegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProveedorServiceTest {
    @Mock private ProveedorRepository proveedores;
    @Mock private CodigoProveedorRepository codigos;
    @Mock private OrdenCompraRepository ordenes;
    @InjectMocks private ProveedorService servicio;

    @Test
    void rechazaUnRucDuplicado() {
        when(proveedores.existsByRuc("20123456789")).thenReturn(true);

        assertThrows(NegocioException.class, () -> servicio.crear(new ProveedorGuardarDto(
                "Proveedor", "20123456789", null, null, null, true)));
    }

    @Test
    void rechazaRucDuplicadoAlEditarOtroProveedor() {
        when(proveedores.findById(5L)).thenReturn(Optional.of(new Proveedor()));
        when(proveedores.existsByRucAndIdNot("20123456789", 5L)).thenReturn(true);

        assertThrows(NegocioException.class, () -> servicio.editar(5L, new ProveedorGuardarDto(
                "Proveedor", "20123456789", null, null, null, true)));
    }
}
