package com.motoboutique.catalogo.web;

import com.motoboutique.catalogo.dto.*;
import com.motoboutique.catalogo.service.CatalogoService;
import com.motoboutique.shared.web.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
class CatalogoController {
    private final CatalogoService servicio;

    @GetMapping("/categorias") @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    PageResponse<CategoriaDto> categorias(String q, Boolean activo, @PageableDefault(size=20, sort="nombre") Pageable pageable) { return servicio.listarCategorias(q, activo, pageable); }
    @PostMapping("/categorias") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')") CategoriaDto crearCategoria(@Valid @RequestBody CategoriaGuardarDto dto) { return servicio.crearCategoria(dto); }
    @PutMapping("/categorias/{id}") @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')") CategoriaDto actualizarCategoria(@PathVariable Long id, @Valid @RequestBody CategoriaGuardarDto dto) { return servicio.actualizarCategoria(id, dto); }
    @DeleteMapping("/categorias/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')") void eliminarCategoria(@PathVariable Long id) { servicio.eliminarCategoria(id); }

    @GetMapping("/marcas") @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    PageResponse<MarcaDto> marcas(String q, Boolean activo, @PageableDefault(size=20, sort="nombre") Pageable pageable) { return servicio.listarMarcas(q, activo, pageable); }
    @PostMapping("/marcas") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')") MarcaDto crearMarca(@Valid @RequestBody MarcaGuardarDto dto) { return servicio.crearMarca(dto); }
    @PutMapping("/marcas/{id}") @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')") MarcaDto actualizarMarca(@PathVariable Long id, @Valid @RequestBody MarcaGuardarDto dto) { return servicio.actualizarMarca(id, dto); }
    @DeleteMapping("/marcas/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')") void eliminarMarca(@PathVariable Long id) { servicio.eliminarMarca(id); }

    @GetMapping("/productos") @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    PageResponse<ProductoDto> productos(String q, Long categoriaId, Long marcaId, Boolean activo, Boolean stockBajo, Long modeloMotoId, @PageableDefault(size=20, sort="nombre") Pageable pageable) { return servicio.listarProductos(q, categoriaId, marcaId, activo, stockBajo, modeloMotoId, pageable); }
    @GetMapping("/productos/{id}") @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')") ProductoDto producto(@PathVariable Long id) { return servicio.obtenerProducto(id); }
    @PostMapping("/productos") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')") ProductoDto crearProducto(@Valid @RequestBody ProductoGuardarDto dto) { return servicio.crearProducto(dto); }
    @PutMapping("/productos/{id}") @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')") ProductoDto actualizarProducto(@PathVariable Long id, @Valid @RequestBody ProductoGuardarDto dto) { return servicio.actualizarProducto(id, dto); }
    @DeleteMapping("/productos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')") void eliminarProducto(@PathVariable Long id) { servicio.eliminarProducto(id); }

    @GetMapping("/modelos") @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')") PageResponse<ModeloMotoDto> modelos(String q, Long marcaId, Integer anio, @PageableDefault(size=20, sort="nombre") Pageable pageable) { return servicio.listarModelos(q, marcaId, anio, pageable); }
    @GetMapping("/modelos/{id}") @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')") ModeloMotoDto modelo(@PathVariable Long id) { return servicio.obtenerModelo(id); }
    @PostMapping("/modelos") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')") ModeloMotoDto crearModelo(@Valid @RequestBody ModeloMotoGuardarDto dto) { return servicio.crearModelo(dto); }
    @PutMapping("/modelos/{id}") @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')") ModeloMotoDto actualizarModelo(@PathVariable Long id, @Valid @RequestBody ModeloMotoGuardarDto dto) { return servicio.actualizarModelo(id, dto); }
    @DeleteMapping("/modelos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')") void eliminarModelo(@PathVariable Long id) { servicio.eliminarModelo(id); }
    @PutMapping("/productos/{id}/compatibilidades") @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')") void compatibilidades(@PathVariable Long id, @RequestBody List<Long> modeloIds) { servicio.actualizarCompatibilidades(id, modeloIds); }
    @GetMapping("/modelos/{id}/productos") @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')") List<ProductoDto> productosModelo(@PathVariable Long id) { return servicio.productosDeModelo(id); }
    @GetMapping("/marcas-con-modelos") @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')") List<MarcaModeloArbolDto> marcasConModelos() { return servicio.marcasConModelos(); }
    @GetMapping("/modelos/{id}/anios") @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')") List<Integer> aniosModelo(@PathVariable Long id) { return servicio.aniosModelo(id); }
}