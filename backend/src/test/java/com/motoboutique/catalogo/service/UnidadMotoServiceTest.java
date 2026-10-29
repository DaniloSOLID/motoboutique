package com.motoboutique.catalogo.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.motoboutique.catalogo.domain.UnidadMoto;
import com.motoboutique.catalogo.dto.UnidadMotoGuardarDto;
import com.motoboutique.catalogo.repository.ModeloMotoRepository;
import com.motoboutique.catalogo.repository.UnidadMotoRepository;
import com.motoboutique.shared.exception.NegocioException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnidadMotoServiceTest {
    @Mock private UnidadMotoRepository unidades;
    @Mock private ModeloMotoRepository modelos;
    @InjectMocks private UnidadMotoService servicio;

    @Test
    void rechazaChasisDuplicadoAlCrear() {
        when(unidades.existsByChasisIgnoreCase("CH-100")).thenReturn(true);

        assertThrows(NegocioException.class, () -> servicio.crear(new UnidadMotoGuardarDto(
                1L, "CH-100", "MO-100", "Negro", null, null)));
    }

    @Test
    void noPermiteReactivarUnaUnidadVendida() {
        UnidadMoto unidad = new UnidadMoto();
        unidad.setEstado("VENDIDA");
        when(unidades.findByIdAndEliminadoFalse(5L)).thenReturn(Optional.of(unidad));
        when(unidades.existsByChasisIgnoreCaseAndIdNot("CH-100", 5L)).thenReturn(false);
        when(unidades.existsByMotorIgnoreCaseAndIdNot("MO-100", 5L)).thenReturn(false);

        assertThrows(NegocioException.class, () -> servicio.actualizar(5L,
                new UnidadMotoGuardarDto(1L, "CH-100", "MO-100", "Negro",
                        "DISPONIBLE", null)));
    }
}
