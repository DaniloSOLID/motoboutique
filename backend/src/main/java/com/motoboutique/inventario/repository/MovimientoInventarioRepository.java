package com.motoboutique.inventario.repository;

import com.motoboutique.inventario.domain.MovimientoInventario;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long>,
        JpaSpecificationExecutor<MovimientoInventario> {
    interface SerieDiaria {
        LocalDate getDia();
        String getTipo();
        java.math.BigDecimal getCantidad();
    }

    boolean existsByAlmacenIdAndEliminadoFalse(Long almacenId);

    Optional<MovimientoInventario> findFirstByProductoIdAndAlmacenIdAndEliminadoFalseOrderByFechaDescIdDesc(
            Long productoId, Long almacenId);

    Optional<MovimientoInventario> findFirstByProductoIdAndAlmacenIdAndEliminadoFalseAndFechaBeforeOrderByFechaDescIdDesc(
            Long productoId, Long almacenId, Instant fecha);

    List<MovimientoInventario> findByProductoIdAndAlmacenIdAndEliminadoFalseAndFechaBeforeOrderByFechaAscIdAsc(
            Long productoId, Long almacenId, Instant hasta);

    List<MovimientoInventario> findByProductoIdAndAlmacenIdAndEliminadoFalseAndFechaLessThanOrderByFechaDescIdDesc(
            Long productoId, Long almacenId, Instant desde);

    List<MovimientoInventario> findByProductoIdAndAlmacenIdAndEliminadoFalseAndFechaBetweenOrderByFechaAscIdAsc(
            Long productoId, Long almacenId, Instant desde, Instant hasta);

    long countByEliminadoFalseAndTipoAndFechaGreaterThanEqualAndFechaLessThan(
            String tipo, Instant desde, Instant hasta);

    @Query(value = """
            select cast(fecha at time zone 'America/Lima' as date) as dia,
                   tipo as tipo, sum(cantidad) as cantidad
            from movimientos_inventario
            where eliminado = false and fecha >= :desde and fecha < :hasta
            group by cast(fecha at time zone 'America/Lima' as date), tipo
            order by dia
            """, nativeQuery = true)
    List<SerieDiaria> serieDiaria(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query(value = """
            select coalesce(sum(cantidad * coalesce(costo_unitario, costo_promedio, 0)), 0)
            from movimientos_inventario
            where eliminado = false and fecha >= :desde and fecha < :hasta
              and tipo in ('SALIDA', 'TRANSFERENCIA_SALIDA', 'AJUSTE_NEGATIVO')
            """, nativeQuery = true)
    java.math.BigDecimal costoSalidasEntre(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    @Query(value = """
            select coalesce(sum(case when tipo in
                     ('ENTRADA', 'TRANSFERENCIA_ENTRADA', 'AJUSTE_POSITIVO', 'RECEPCION')
                     then cantidad else 0 end), 0) as entradas,
                   coalesce(sum(case when tipo in
                     ('SALIDA', 'TRANSFERENCIA_SALIDA', 'AJUSTE_NEGATIVO')
                     then cantidad else 0 end), 0) as salidas
            from movimientos_inventario
            where eliminado = false and fecha >= :desde and fecha < :hasta
            """, nativeQuery = true)
    TotalesMovimiento totalesEntre(@Param("desde") Instant desde, @Param("hasta") Instant hasta);

    interface TotalesMovimiento {
        java.math.BigDecimal getEntradas();
        java.math.BigDecimal getSalidas();
    }

    interface ValorizacionProducto {
        Long getProductoId();
        String getCodigo();
        String getProducto();
        Long getCategoriaId();
        String getCategoria();
        java.math.BigDecimal getCantidad();
        java.math.BigDecimal getCostoPromedio();
        java.math.BigDecimal getValor();
        Long getTotalFilas();
    }

    interface ValorizacionCategoria {
        Long getCategoriaId();
        String getCategoria();
        java.math.BigDecimal getCantidad();
        java.math.BigDecimal getValor();
    }

    @Query(value = """
            with ultimo as (
                select distinct on (m.producto_id, m.almacen_id)
                       m.producto_id, m.almacen_id, m.saldo_cantidad, m.costo_promedio
                from movimientos_inventario m
                where m.eliminado = false and m.fecha < :corte
                  and (:almacenId is null or m.almacen_id = :almacenId)
                order by m.producto_id, m.almacen_id, m.fecha desc, m.id desc
            ), valor as (
                select p.id as "productoId", p.codigo, p.nombre as producto,
                       c.id as "categoriaId", c.nombre as categoria,
                       sum(u.saldo_cantidad) as cantidad,
                       case when sum(u.saldo_cantidad) = 0 then 0
                            else sum(u.saldo_cantidad * coalesce(u.costo_promedio, 0))
                                 / sum(u.saldo_cantidad) end as "costoPromedio",
                       sum(u.saldo_cantidad * coalesce(u.costo_promedio, 0)) as valor
                from ultimo u
                join productos p on p.id = u.producto_id and p.eliminado = false and p.activo = true
                join categorias c on c.id = p.categoria_id and c.eliminado = false
                where (:categoriaId is null or c.id = :categoriaId)
                  and (:q is null or lower(p.codigo) like lower(concat('%', :q, '%'))
                       or lower(p.nombre) like lower(concat('%', :q, '%')))
                group by p.id, p.codigo, p.nombre, c.id, c.nombre
            )
            select valor.*, count(*) over() as "totalFilas"
            from valor
            order by categoria, producto
            limit :limite offset :offset
            """, nativeQuery = true)
    List<ValorizacionProducto> valorizarAlCorte(
            @Param("corte") Instant corte, @Param("almacenId") Long almacenId,
            @Param("categoriaId") Long categoriaId, @Param("q") String q,
            @Param("limite") int limite, @Param("offset") long offset);

    @Query(value = """
            with ultimo as (
                select distinct on (m.producto_id, m.almacen_id)
                       m.producto_id, m.almacen_id, m.saldo_cantidad
                from movimientos_inventario m
                where m.eliminado = false and m.fecha < :corte
                  and (:almacenId is null or m.almacen_id = :almacenId)
                order by m.producto_id, m.almacen_id, m.fecha desc, m.id desc
            )
            select count(*) from (
                select p.id
                from ultimo u
                join productos p on p.id = u.producto_id and p.eliminado = false and p.activo = true
                join categorias c on c.id = p.categoria_id and c.eliminado = false
                where (:categoriaId is null or c.id = :categoriaId)
                  and (:q is null or lower(p.codigo) like lower(concat('%', :q, '%'))
                       or lower(p.nombre) like lower(concat('%', :q, '%')))
                group by p.id
            ) productos
            """, nativeQuery = true)
    long contarValorizacionAlCorte(
            @Param("corte") Instant corte, @Param("almacenId") Long almacenId,
            @Param("categoriaId") Long categoriaId, @Param("q") String q);

    @Query(value = """
            with ultimo as (
                select distinct on (m.producto_id, m.almacen_id)
                       m.producto_id, m.almacen_id, m.saldo_cantidad, m.costo_promedio
                from movimientos_inventario m
                where m.eliminado = false and m.fecha < :corte
                  and (:almacenId is null or m.almacen_id = :almacenId)
                order by m.producto_id, m.almacen_id, m.fecha desc, m.id desc
            )
            select c.id as "categoriaId", c.nombre as categoria,
                   coalesce(sum(u.saldo_cantidad), 0) as cantidad,
                   coalesce(sum(u.saldo_cantidad * coalesce(u.costo_promedio, 0)), 0) as valor
            from ultimo u
            join productos p on p.id = u.producto_id and p.eliminado = false and p.activo = true
            join categorias c on c.id = p.categoria_id and c.eliminado = false
            where (:categoriaId is null or c.id = :categoriaId)
              and (:q is null or lower(p.codigo) like lower(concat('%', :q, '%'))
                   or lower(p.nombre) like lower(concat('%', :q, '%')))
            group by c.id, c.nombre order by c.nombre
            """, nativeQuery = true)
    List<ValorizacionCategoria> valorizarPorCategoriaAlCorte(
            @Param("corte") Instant corte, @Param("almacenId") Long almacenId,
            @Param("categoriaId") Long categoriaId, @Param("q") String q);
}
