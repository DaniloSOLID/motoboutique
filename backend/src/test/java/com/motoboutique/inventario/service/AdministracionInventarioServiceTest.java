package com.motoboutique.inventario.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.motoboutique.catalogo.repository.ProductoRepository;
import com.motoboutique.inventario.domain.AjusteInventario;
import com.motoboutique.inventario.repository.AjusteInventarioRepository;
import com.motoboutique.inventario.repository.AlmacenRepository;
import com.motoboutique.inventario.repository.ConteoDetalleRepository;
import com.motoboutique.inventario.repository.ConteoFisicoRepository;
import com.motoboutique.inventario.repository.MovimientoInventarioRepository;
import com.motoboutique.inventario.repository.NumeracionDocumentoRepository;
import com.motoboutique.inventario.repository.StockRepository;
import com.motoboutique.seguridad.domain.Usuario;
import com.motoboutique.seguridad.repository.UsuarioRepository;
import com.motoboutique.shared.audit.AuditoriaService;
import com.motoboutique.shared.exception.NegocioException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdministracionInventarioServiceTest {

    @Mock private ConteoFisicoRepository conteoRepository;
    @Mock private ConteoDetalleRepository detalleRepository;
    @Mock private AjusteInventarioRepository ajusteRepository;
    @Mock private StockRepository stockRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private AlmacenRepository almacenRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private NumeracionDocumentoRepository numeracionRepository;
    @Mock private MovimientoInventarioService movimientoService;
    @Mock private AuditoriaService auditoriaService;
    @Mock private MovimientoInventarioRepository movimientoRepository;
    @Mock private AdministracionInventarioMapper mapper;

    private AdministracionInventarioService service;

    @BeforeEach
    void preparar() {
        service = new AdministracionInventarioService(conteoRepository, detalleRepository,
                ajusteRepository, stockRepository, productoRepository, almacenRepository,
                usuarioRepository, numeracionRepository, movimientoRepository, movimientoService,
                auditoriaService, mapper);
    }

    @Test
    void solicitanteNoPuedeAprobarSuPropioAjuste() {
        Usuario solicitante = new Usuario();
        ReflectionTestUtils.setField(solicitante, "id", 18L);
        AjusteInventario ajuste = new AjusteInventario();
        ajuste.setSolicitante(solicitante);
        when(ajusteRepository.findById(7L)).thenReturn(Optional.of(ajuste));

        assertThrows(NegocioException.class, () -> service.aprobar(7L, 18L, false, null));

        verify(movimientoService, never()).ajustar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void comentarioEsObligatorioAlRechazarAjuste() {
        assertThrows(NegocioException.class, () -> service.rechazar(7L, 20L, " "));
        verify(ajusteRepository, never()).findById(7L);
    }
}
