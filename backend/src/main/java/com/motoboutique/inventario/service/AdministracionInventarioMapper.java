package com.motoboutique.inventario.service;

import com.motoboutique.inventario.domain.AjusteInventario;
import com.motoboutique.inventario.domain.ConteoDetalle;
import com.motoboutique.inventario.domain.ConteoFisico;
import com.motoboutique.inventario.dto.AdministracionInventarioDto.AjusteDto;
import com.motoboutique.inventario.dto.AdministracionInventarioDto.ConteoDto;
import com.motoboutique.inventario.dto.AdministracionInventarioDto.ConteoLineaDto;
import com.motoboutique.inventario.repository.StockRepository;
import java.util.List;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdministracionInventarioMapper {
    private final StockRepository stockRepository;

    public ConteoDto conteo(ConteoFisico conteo, List<ConteoDetalle> detalles) {
        return new ConteoDto(conteo.getId(), conteo.getNumero(), conteo.getAlmacen().getId(),
                conteo.getAlmacen().getNombre(), conteo.getEstado(), conteo.getFechaApertura(),
                conteo.getFechaCierre(), detalles.stream().map(d -> new ConteoLineaDto(
                        d.getId(), d.getProducto().getId(), d.getProducto().getCodigo(),
                        d.getProducto().getNombre(), d.getStockSistema(), d.getStockContado(),
                        d.getDiferencia(), d.isAjustado())).toList());
    }

    public AjusteDto ajuste(AjusteInventario ajuste) {
        BigDecimal stockActual = stockRepository.findByProductoIdAndAlmacenIdAndEliminadoFalse(
                        ajuste.getProducto().getId(), ajuste.getAlmacen().getId())
                .map(stock -> stock.getCantidad()).orElse(BigDecimal.ZERO);
        BigDecimal stockResultante;
        if ("APROBADO".equals(ajuste.getEstado())) {
            stockResultante = stockActual;
            stockActual = stockActual.subtract(ajuste.getCantidad());
        } else {
            stockResultante = stockActual.add(ajuste.getCantidad());
        }
        return new AjusteDto(ajuste.getId(), ajuste.getNumero(), ajuste.getProducto().getId(),
                ajuste.getProducto().getCodigo(), ajuste.getProducto().getNombre(),
                ajuste.getAlmacen().getId(), ajuste.getAlmacen().getNombre(), ajuste.getCantidad(),
                ajuste.getMotivo(), ajuste.getOrigen(), ajuste.getEstado(),
                ajuste.getSolicitante().getId(), ajuste.getSolicitante().getNombre(),
                ajuste.getAprobador() == null ? null : ajuste.getAprobador().getNombre(),
                ajuste.getComentario(), ajuste.getFechaSolicitud(), ajuste.getFechaResolucion(),
                ajuste.getMovimiento() == null ? null : ajuste.getMovimiento().getId(),
                stockActual, stockResultante);
    }
}
