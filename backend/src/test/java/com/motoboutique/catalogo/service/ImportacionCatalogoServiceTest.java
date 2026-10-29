package com.motoboutique.catalogo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.motoboutique.catalogo.domain.Producto;
import com.motoboutique.catalogo.dto.ImportacionMapeoDto;
import com.motoboutique.catalogo.domain.Categoria;
import com.motoboutique.catalogo.repository.CategoriaRepository;
import com.motoboutique.catalogo.repository.ImportacionErrorRepository;
import com.motoboutique.catalogo.repository.ImportacionRepository;
import com.motoboutique.catalogo.repository.MarcaRepository;
import com.motoboutique.catalogo.repository.ProductoRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ImportacionCatalogoServiceTest {
    @Mock private ImportacionRepository importaciones;
    @Mock private ImportacionErrorRepository errores;
    @Mock private ProductoRepository productos;
    @Mock private CategoriaRepository categorias;
    @Mock private MarcaRepository marcas;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private ImportacionCatalogoService servicio;

    @Test
    void validarNoPersisteDatosAntesDeConfirmar() {
        MockMultipartFile csv = csv("codigo,nombre\nABC-1,Producto\n");
        when(productos.findByCodigoIgnoreCase("ABC-1")).thenReturn(Optional.of(new Producto()));
        ImportacionMapeoDto mapeo = new ImportacionMapeoDto(Map.of(
                "codigo", "codigo", "nombre", "nombre"));

        var resultado = servicio.validar(csv, mapeo);

        assertEquals(1, resultado.filasValidas().size());
        assertNull(resultado.importacionId());
        verify(importaciones, never()).save(any());
        verify(errores, never()).saveAll(any());
        verify(productos, never()).save(any());
    }

    @Test
    void detectarDevuelveEncabezadosYVistaPreviaSinPersistir() {
        var resultado = servicio.detectar(csv("sku,descripcion\nABC-2,Repuesto\n"));

        assertEquals("sku", resultado.columnas().getFirst());
        assertEquals("ABC-2", resultado.vistaPrevia().getFirst().get("sku"));
        verify(importaciones, never()).save(any());
        verify(productos, never()).save(any());
    }

    @Test
    void confirmarPersisteProductosYElHistorialDeLaOperacion() {
        Categoria categoria = new Categoria();
        when(productos.findByCodigoIgnoreCase("ABC-3")).thenReturn(Optional.empty());
        when(categorias.findByIdAndEliminadoFalse(9L)).thenReturn(Optional.of(categoria));
        when(importaciones.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(productos.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        ImportacionMapeoDto mapeo = new ImportacionMapeoDto(Map.of(
                "codigo", "sku",
                "nombre", "producto",
                "categoriaId", "categoria",
                "precioCompra", "costo",
                "precioVenta", "precio"));

        servicio.confirmar(csv("sku,producto,categoria,costo,precio\nABC-3,Cadena,9,10,15\n"),
                mapeo, 17L);

        verify(productos).save(any(Producto.class));
        verify(importaciones).save(any());
        verify(errores, times(1)).saveAll(any());
    }

    private MockMultipartFile csv(String contenido) {
        return new MockMultipartFile("archivo", "catalogo.csv", "text/csv",
                contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
