package com.motoboutique.inventario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.motoboutique.catalogo.domain.Categoria;
import com.motoboutique.catalogo.domain.Producto;
import com.motoboutique.catalogo.repository.CategoriaRepository;
import com.motoboutique.catalogo.repository.ProductoRepository;
import com.motoboutique.inventario.domain.Almacen;
import com.motoboutique.inventario.domain.Stock;
import com.motoboutique.inventario.dto.MovimientoSolicitudDto;
import com.motoboutique.inventario.dto.TransferenciaDetalleSolicitudDto;
import com.motoboutique.inventario.dto.TransferenciaSolicitudDto;
import com.motoboutique.inventario.domain.MotivoSalida;
import com.motoboutique.inventario.repository.AlmacenRepository;
import com.motoboutique.inventario.repository.MovimientoInventarioRepository;
import com.motoboutique.inventario.repository.StockRepository;
import com.motoboutique.inventario.repository.TransferenciaRepository;
import com.motoboutique.shared.exception.NegocioException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.seed.enabled=false",
        "app.cors.allowed-origins=http://localhost:5173"
})
class MovimientoInventarioPostgresTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configurarDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private MovimientoInventarioService servicio;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private AlmacenRepository almacenRepository;
    @Autowired private StockRepository stockRepository;
    @Autowired private MovimientoInventarioRepository movimientoRepository;
    @Autowired private TransferenciaRepository transferenciaRepository;

    private Almacen origen;
    private Almacen destino;
    private Producto producto;

    @BeforeEach
    void crearDatosPersistentes() {
        String sufijo = Long.toString(System.nanoTime());
        Categoria categoria = new Categoria();
        categoria.setNombre("Pruebas " + sufijo);
        categoria.setActivo(true);
        categoria = categoriaRepository.saveAndFlush(categoria);
        origen = crearAlmacen("O-" + sufijo, "Origen " + sufijo);
        destino = crearAlmacen("D-" + sufijo, "Destino " + sufijo);
        producto = crearProducto(categoria, "P-" + sufijo);
    }

    @Test
    void dosSalidasSimultaneasNoDejanStockNegativoEnPostgres() throws Exception {
        servicio.registrarEntrada(entrada(producto, origen, "5.000", "10.00"));
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> una = executor.submit(() -> salidaConcurrente(inicio));
            Future<Boolean> dos = executor.submit(() -> salidaConcurrente(inicio));
            inicio.countDown();
            assertEquals(1, (una.get() ? 1 : 0) + (dos.get() ? 1 : 0));
        } finally {
            executor.shutdownNow();
        }
        Stock saldo = stockRepository.findByProductoIdAndAlmacenIdAndEliminadoFalse(
                producto.getId(), origen.getId()).orElseThrow();
        assertEquals(new BigDecimal("1.000"), saldo.getCantidad());
    }

    @Test
    void transferenciaCompletaRegistraAmbosMovimientos() {
        servicio.registrarEntrada(entrada(producto, origen, "5.000", "10.00"));
        var transferencia = servicio.crearTransferencia(new TransferenciaSolicitudDto(
                origen.getId(), destino.getId(),
                List.of(new TransferenciaDetalleSolicitudDto(producto.getId(),
                        new BigDecimal("2.000"))), "Prueba de transferencia"));
        servicio.despachar(transferencia.id());
        var completada = servicio.recibir(transferencia.id());

        assertEquals("COMPLETADA", completada.estado());
        assertEquals(new BigDecimal("3.000"), saldo(producto, origen).getCantidad());
        assertEquals(new BigDecimal("2.000"), saldo(producto, destino).getCantidad());
        assertEquals(2, movimientoRepository.findAll().stream()
                .filter(m -> m.getReferencia().equals(transferencia.numero())).count());
    }

    @Test
    void fallaALaMitadRevierteTodaLaRecepcion() {
        Categoria categoria = producto.getCategoria();
        Producto segundoProducto = crearProducto(categoria, "P2-" + System.nanoTime());
        servicio.registrarEntrada(entrada(producto, origen, "4.000", "10.00"));
        servicio.registrarEntrada(entrada(segundoProducto, origen, "4.000", "8.00"));
        var transferencia = servicio.crearTransferencia(new TransferenciaSolicitudDto(
                origen.getId(), destino.getId(),
                List.of(new TransferenciaDetalleSolicitudDto(producto.getId(),
                                new BigDecimal("1.000")),
                        new TransferenciaDetalleSolicitudDto(segundoProducto.getId(),
                                new BigDecimal("1.000"))), null));
        servicio.despachar(transferencia.id());
        Producto inactivo = productoRepository.findByIdAndEliminadoFalse(segundoProducto.getId())
                .orElseThrow();
        inactivo.setActivo(false);
        productoRepository.saveAndFlush(inactivo);

        assertThrows(NegocioException.class, () -> servicio.recibir(transferencia.id()));

        assertFalse(stockRepository.findByProductoIdAndAlmacenIdAndEliminadoFalse(
                producto.getId(), destino.getId()).isPresent());
        assertEquals("EN_TRANSITO", transferenciaRepository.findById(transferencia.id())
                .orElseThrow().getEstado());
        assertTrue(movimientoRepository.findAll().stream()
                .noneMatch(m -> m.getReferencia().equals(transferencia.numero())
                        && m.getTipo().equals("TRANSFERENCIA_ENTRADA")));
    }

    private boolean salidaConcurrente(CountDownLatch inicio) throws Exception {
        inicio.await();
        try {
            servicio.registrarSalida(new MovimientoSolicitudDto(producto.getId(), origen.getId(),
                    new BigDecimal("4.000"), null, null, MotivoSalida.MERMA, null));
            return true;
        } catch (NegocioException e) {
            return false;
        }
    }

    private MovimientoSolicitudDto entrada(Producto producto, Almacen almacen,
                                           String cantidad, String costo) {
        return new MovimientoSolicitudDto(producto.getId(), almacen.getId(),
                new BigDecimal(cantidad), new BigDecimal(costo), null, null, null);
    }

    private Almacen crearAlmacen(String codigo, String nombre) {
        Almacen almacen = new Almacen();
        almacen.setCodigo(codigo);
        almacen.setNombre(nombre);
        almacen.setActivo(true);
        return almacenRepository.saveAndFlush(almacen);
    }

    private Producto crearProducto(Categoria categoria, String codigo) {
        Producto nuevo = new Producto();
        nuevo.setCategoria(categoria);
        nuevo.setCodigo(codigo);
        nuevo.setNombre("Producto " + codigo);
        nuevo.setPrecioCompra(new BigDecimal("10.00"));
        nuevo.setPrecioVenta(new BigDecimal("15.00"));
        nuevo.setStockMinimo(BigDecimal.ZERO.setScale(3));
        nuevo.setActivo(true);
        return productoRepository.saveAndFlush(nuevo);
    }

    private Stock saldo(Producto producto, Almacen almacen) {
        return stockRepository.findByProductoIdAndAlmacenIdAndEliminadoFalse(
                producto.getId(), almacen.getId()).orElseThrow();
    }
}
