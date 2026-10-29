package com.motoboutique.catalogo.service;

import com.motoboutique.catalogo.dto.*;
import com.motoboutique.shared.web.PageResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface CatalogoService {
    PageResponse<CategoriaDto> listarCategorias(String q, Boolean activo, Pageable pageable);
    CategoriaDto crearCategoria(CategoriaGuardarDto datos);
    CategoriaDto actualizarCategoria(Long id, CategoriaGuardarDto datos);
    void eliminarCategoria(Long id);
    PageResponse<MarcaDto> listarMarcas(String q, Boolean activo, Pageable pageable);
    MarcaDto crearMarca(MarcaGuardarDto datos);
    MarcaDto actualizarMarca(Long id, MarcaGuardarDto datos);
    void eliminarMarca(Long id);
    PageResponse<ProductoDto> listarProductos(String q, Long categoriaId, Long marcaId, Boolean activo,
                                               Boolean stockBajo, Long modeloMotoId, Pageable pageable);
    ProductoDto obtenerProducto(Long id);
    ProductoDto crearProducto(ProductoGuardarDto datos);
    ProductoDto actualizarProducto(Long id, ProductoGuardarDto datos);
    void eliminarProducto(Long id);
    PageResponse<ModeloMotoDto> listarModelos(String q, Long marcaId, Integer anio, Pageable pageable);
    ModeloMotoDto obtenerModelo(Long id);
    ModeloMotoDto crearModelo(ModeloMotoGuardarDto datos);
    ModeloMotoDto actualizarModelo(Long id, ModeloMotoGuardarDto datos);
    void eliminarModelo(Long id);
    void actualizarCompatibilidades(Long productoId, List<Long> modeloIds);
    List<ProductoDto> productosDeModelo(Long modeloId);
    List<MarcaModeloArbolDto> marcasConModelos();
    List<Integer> aniosModelo(Long modeloId);
}
