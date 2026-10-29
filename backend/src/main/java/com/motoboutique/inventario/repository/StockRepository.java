package com.motoboutique.inventario.repository;

import com.motoboutique.inventario.domain.Stock;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long>, JpaSpecificationExecutor<Stock> {
    List<Stock> findByProductoIdAndEliminadoFalse(Long productoId);
    List<Stock> findByAlmacenIdAndEliminadoFalse(Long almacenId);

    Optional<Stock> findByProductoIdAndAlmacenIdAndEliminadoFalse(Long productoId, Long almacenId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.producto.id = :productoId and s.almacen.id = :almacenId and s.eliminado = false")
    Optional<Stock> bloquearPorProductoYAlmacen(@Param("productoId") Long productoId,
                                                 @Param("almacenId") Long almacenId);

    long countByAlmacenIdAndEliminadoFalse(Long almacenId);

    @Query("select coalesce(sum(s.cantidad), 0) from Stock s where s.producto.id = :productoId and s.eliminado = false")
    BigDecimal cantidadTotalProducto(@Param("productoId") Long productoId);

    @Query("select coalesce(sum(s.cantidad * coalesce((select m.costoPromedio "
            + "from MovimientoInventario m where m.producto.id = s.producto.id "
            + "and m.almacen.id = s.almacen.id and m.eliminado = false "
            + "and m.id = (select max(m2.id) from MovimientoInventario m2 "
            + "where m2.producto.id = s.producto.id and m2.almacen.id = s.almacen.id "
            + "and m2.eliminado = false)), 0)), 0) "
            + "from Stock s where s.eliminado = false")
    BigDecimal valorizarInventario();

    @Query("select count(p) from Producto p where p.activo = true and p.eliminado = false "
            + "and coalesce((select sum(s2.cantidad) from Stock s2 "
            + "where s2.producto.id = p.id and s2.eliminado = false), 0) <= p.stockMinimo")
    long contarProductosConStockBajo();
}
