package com.motoboutique.catalogo.service;

import com.motoboutique.catalogo.domain.Importacion;
import com.motoboutique.catalogo.domain.ImportacionError;
import com.motoboutique.catalogo.domain.Producto;
import com.motoboutique.catalogo.dto.ImportacionDto;
import com.motoboutique.catalogo.dto.ImportacionErrorDto;
import com.motoboutique.catalogo.dto.ImportacionMapeoDto;
import com.motoboutique.catalogo.dto.ImportacionResultadoDto;
import com.motoboutique.catalogo.repository.CategoriaRepository;
import com.motoboutique.catalogo.repository.ImportacionErrorRepository;
import com.motoboutique.catalogo.repository.ImportacionRepository;
import com.motoboutique.catalogo.repository.MarcaRepository;
import com.motoboutique.catalogo.repository.ProductoRepository;
import com.motoboutique.shared.exception.NegocioException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImportacionCatalogoService {
    private static final long TAMANIO_MAXIMO = 10L * 1024 * 1024;
    private static final Set<String> COLUMNAS_PERMITIDAS = Set.of(
            "codigo", "nombre", "descripcion", "categoriaId", "marcaId", "precioCompra",
            "precioVenta", "stockMinimo", "unidad", "imagenUrl", "especificaciones");

    private final ImportacionRepository importaciones;
    private final ImportacionErrorRepository erroresRepository;
    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final MarcaRepository marcas;
    private final ObjectMapper objectMapper;

    public ImportacionResultadoDto detectar(MultipartFile archivo) {
        List<List<String>> filas = leerArchivo(archivo);
        if (filas.isEmpty()) {
            throw new NegocioException("El archivo no contiene datos.");
        }
        List<String> columnas = limpiarEncabezados(filas.getFirst());
        List<Map<String, String>> preview = filas.stream()
                .skip(1)
                .limit(10)
                .map(fila -> filaComoMapa(columnas, fila))
                .toList();
        return new ImportacionResultadoDto(nombreArchivo(archivo), columnas, preview,
                List.of(), List.of(), null);
    }

    public ImportacionResultadoDto validar(MultipartFile archivo, ImportacionMapeoDto mapeo) {
        return analizar(archivo, mapeo).resultado();
    }

    @Transactional
    public ImportacionResultadoDto confirmar(MultipartFile archivo, ImportacionMapeoDto mapeo,
                                              Long usuarioId) {
        if (usuarioId == null) {
            throw new NegocioException("No se pudo identificar al usuario que confirma la importación.");
        }
        Analisis analisis = analizar(archivo, mapeo);
        if (analisis.filasValidas().isEmpty() && analisis.errores().isEmpty()) {
            throw new NegocioException("El archivo no contiene filas para importar.");
        }

        int procesadas = 0;
        for (Map<String, String> fila : analisis.filasValidas()) {
            guardarProducto(fila);
            procesadas++;
        }

        Importacion historial = new Importacion();
        historial.setArchivo(nombreArchivo(archivo));
        historial.setUsuarioId(usuarioId);
        historial.setFilasValidas(procesadas);
        historial.setErrores(analisis.errores().size());
        historial.setEstado(analisis.errores().isEmpty() ? "COMPLETADA" : "PARCIAL");
        Importacion historialGuardado = importaciones.save(historial);

        List<ImportacionError> errores = analisis.errores().stream()
                .map(error -> convertirError(historialGuardado, error))
                .toList();
        erroresRepository.saveAll(errores);
        return new ImportacionResultadoDto(nombreArchivo(archivo), analisis.columnas(),
                analisis.preview(), analisis.filasValidas(), analisis.errores(), historialGuardado.getId());
    }

    @Transactional(readOnly = true)
    public List<ImportacionDto> historial() {
        return importaciones.findAllByEliminadoFalseOrderByFechaDesc().stream()
                .map(importacion -> new ImportacionDto(importacion.getId(), importacion.getArchivo(),
                        importacion.getFecha(), importacion.getFilasValidas(), importacion.getErrores(),
                        importacion.getEstado(), importacion.getUsuarioId()))
                .toList();
    }

    private Analisis analizar(MultipartFile archivo, ImportacionMapeoDto mapeo) {
        List<List<String>> filas = leerArchivo(archivo);
        if (filas.isEmpty()) {
            throw new NegocioException("El archivo no contiene datos.");
        }
        List<String> encabezados = limpiarEncabezados(filas.getFirst());
        Map<String, String> mapeoValidado = validarMapeo(encabezados, mapeo);
        List<Map<String, String>> datos = new ArrayList<>();
        List<ImportacionErrorDto> errores = new ArrayList<>();
        List<Map<String, String>> preview = new ArrayList<>();
        Map<String, Integer> cantidadesPorCodigo = new LinkedHashMap<>();
        for (int indice = 1; indice < filas.size(); indice++) {
            Map<String, String> filaOriginal = filaComoMapa(encabezados, filas.get(indice));
            String codigo = filaOriginal.get(mapeoValidado.get("codigo"));
            if (!vacio(codigo)) {
                cantidadesPorCodigo.merge(codigo.trim().toLowerCase(Locale.ROOT), 1, Integer::sum);
            }
        }

        for (int indice = 1; indice < filas.size(); indice++) {
            int numeroFila = indice + 1;
            Map<String, String> origen = filaComoMapa(encabezados, filas.get(indice));
            if (origen.values().stream().allMatch(String::isBlank)) {
                continue;
            }
            Map<String, String> fila = new LinkedHashMap<>();
            mapeoValidado.forEach((destino, fuente) -> fila.put(destino, origen.get(fuente)));
            preview.add(origen);
            String codigo = fila.get("codigo");
            if (!vacio(codigo)
                    && cantidadesPorCodigo.getOrDefault(codigo.trim().toLowerCase(Locale.ROOT), 0) > 1) {
                errores.add(new ImportacionErrorDto(numeroFila, "codigo",
                        "El código está duplicado dentro del archivo."));
            }
            validarFila(numeroFila, fila, errores);
            if (errores.stream().noneMatch(error -> error.fila() == numeroFila)) {
                datos.add(fila);
            }
        }
        return new Analisis(encabezados, preview, datos, errores,
                new ImportacionResultadoDto(nombreArchivo(archivo), encabezados,
                        preview.stream().limit(10).toList(), datos, errores, null));
    }

    private void validarFila(int numeroFila, Map<String, String> fila,
                             List<ImportacionErrorDto> errores) {
        String codigo = fila.get("codigo");
        if (vacio(codigo)) {
            errores.add(new ImportacionErrorDto(numeroFila, "codigo", "El código es obligatorio."));
            return;
        }
        if (vacio(fila.get("nombre"))) {
            errores.add(new ImportacionErrorDto(numeroFila, "nombre", "El nombre es obligatorio."));
        }
        Producto existente = productos.findByCodigoIgnoreCase(codigo.trim()).orElse(null);
        if (existente == null && vacio(fila.get("categoriaId"))) {
            errores.add(new ImportacionErrorDto(numeroFila, "categoriaId",
                    "La categoría es obligatoria para crear un producto."));
        }
        validarReferencia(numeroFila, "categoriaId", fila.get("categoriaId"),
                categorias::findByIdAndEliminadoFalse, errores);
        validarReferencia(numeroFila, "marcaId", fila.get("marcaId"),
                marcas::findByIdAndEliminadoFalse, errores);
        validarDecimal(numeroFila, "precioCompra", fila.get("precioCompra"), errores);
        validarDecimal(numeroFila, "precioVenta", fila.get("precioVenta"), errores);
        validarDecimal(numeroFila, "stockMinimo", fila.get("stockMinimo"), errores);
        BigDecimal precioCompra = decimalParaValidacion(fila.get("precioCompra"),
                existente == null ? null : existente.getPrecioCompra());
        BigDecimal precioVenta = decimalParaValidacion(fila.get("precioVenta"),
                existente == null ? null : existente.getPrecioVenta());
        if (precioCompra != null && precioVenta != null
                && precioVenta.compareTo(precioCompra) < 0) {
            errores.add(new ImportacionErrorDto(numeroFila, "precioVenta",
                    "El precio de venta no puede ser menor que el precio de compra."));
        }
        if (existente == null && vacio(fila.get("precioCompra"))) {
            errores.add(new ImportacionErrorDto(numeroFila, "precioCompra",
                    "El precio de compra es obligatorio para crear un producto."));
        }
        if (existente == null && vacio(fila.get("precioVenta"))) {
            errores.add(new ImportacionErrorDto(numeroFila, "precioVenta",
                    "El precio de venta es obligatorio para crear un producto."));
        }
        if (!vacio(fila.get("especificaciones"))) {
            try {
                objectMapper.readValue(fila.get("especificaciones"),
                        new TypeReference<Map<String, Object>>() {});
            } catch (JacksonException ex) {
                errores.add(new ImportacionErrorDto(numeroFila, "especificaciones",
                        "Debe contener un objeto JSON válido."));
            }
        }
    }

    private void validarReferencia(int fila, String columna, String valor,
                                   java.util.function.Function<Long, java.util.Optional<?>> buscar,
                                   List<ImportacionErrorDto> errores) {
        if (vacio(valor)) {
            return;
        }
        try {
            if (buscar.apply(Long.valueOf(valor.trim())).isEmpty()) {
                errores.add(new ImportacionErrorDto(fila, columna,
                        "No existe el registro indicado."));
            }
        } catch (NumberFormatException ex) {
            errores.add(new ImportacionErrorDto(fila, columna,
                    "Debe ser un identificador numérico."));
        }
    }

    private void validarDecimal(int fila, String columna, String valor,
                                List<ImportacionErrorDto> errores) {
        if (vacio(valor)) {
            return;
        }
        try {
            if (new BigDecimal(valor.trim().replace(',', '.')).signum() < 0) {
                errores.add(new ImportacionErrorDto(fila, columna, "No puede ser negativo."));
            }
        } catch (NumberFormatException ex) {
            errores.add(new ImportacionErrorDto(fila, columna, "Debe ser un número válido."));
        }
    }

    private void guardarProducto(Map<String, String> fila) {
        String codigo = fila.get("codigo").trim();
        Producto producto = productos.findByCodigoIgnoreCase(codigo).orElseGet(Producto::new);
        if (producto.getId() == null) {
            producto.setCodigo(codigo);
            producto.setCategoria(categorias.findByIdAndEliminadoFalse(
                    Long.valueOf(fila.get("categoriaId"))).orElseThrow());
            producto.setPrecioCompra(decimal(fila.get("precioCompra"), BigDecimal.ZERO));
            producto.setPrecioVenta(decimal(fila.get("precioVenta"), BigDecimal.ZERO));
            producto.setStockMinimo(decimal(fila.get("stockMinimo"), BigDecimal.ZERO));
            producto.setUnidad(vacio(fila.get("unidad")) ? "UNIDAD" : fila.get("unidad").trim().toUpperCase(Locale.ROOT));
        } else {
            if (!vacio(fila.get("categoriaId"))) {
                producto.setCategoria(categorias.findByIdAndEliminadoFalse(
                        Long.valueOf(fila.get("categoriaId"))).orElseThrow());
            }
            if (!vacio(fila.get("precioCompra"))) producto.setPrecioCompra(decimal(fila.get("precioCompra"), producto.getPrecioCompra()));
            if (!vacio(fila.get("precioVenta"))) producto.setPrecioVenta(decimal(fila.get("precioVenta"), producto.getPrecioVenta()));
            if (!vacio(fila.get("stockMinimo"))) producto.setStockMinimo(decimal(fila.get("stockMinimo"), producto.getStockMinimo()));
            if (!vacio(fila.get("unidad"))) producto.setUnidad(fila.get("unidad").trim().toUpperCase(Locale.ROOT));
            producto.restaurar();
        }
        producto.setNombre(fila.get("nombre").trim());
        producto.setDescripcion(fila.get("descripcion"));
        producto.setImagenUrl(fila.get("imagenUrl"));
        if (!vacio(fila.get("marcaId"))) {
            producto.setMarca(marcas.findByIdAndEliminadoFalse(
                    Long.valueOf(fila.get("marcaId"))).orElseThrow());
        }
        if (!vacio(fila.get("especificaciones"))) {
            try {
                producto.setEspecificaciones(objectMapper.readValue(fila.get("especificaciones"),
                        new TypeReference<Map<String, Object>>() {}));
            } catch (JacksonException ex) {
                throw new NegocioException("Las especificaciones dejaron de ser JSON válido.");
            }
        }
        productos.save(producto);
    }

    private ImportacionError convertirError(Importacion importacion, ImportacionErrorDto dto) {
        ImportacionError error = new ImportacionError();
        error.setImportacion(importacion);
        error.setFila(dto.fila());
        error.setColumna(dto.columna());
        error.setMensaje(dto.mensaje());
        return error;
    }

    private Map<String, String> validarMapeo(List<String> encabezados,
                                              ImportacionMapeoDto mapeo) {
        if (mapeo == null || mapeo.columnas() == null || mapeo.columnas().isEmpty()) {
            throw new NegocioException("Debes indicar el mapeo de columnas.");
        }
        Map<String, String> resultado = new LinkedHashMap<>();
        mapeo.columnas().forEach((columnaDestino, columnaOrigen) -> {
            String destino = normalizarNombre(columnaDestino);
            if (!COLUMNAS_PERMITIDAS.contains(destino)) {
                throw new NegocioException("Columna de destino no admitida: " + columnaDestino);
            }
            if (columnaOrigen == null || !encabezados.contains(columnaOrigen)) {
                throw new NegocioException("No existe la columna de origen: " + columnaOrigen);
            }
            if (resultado.put(destino, columnaOrigen) != null) {
                throw new NegocioException("No se puede mapear dos veces la columna: " + destino);
            }
        });
        if (!resultado.containsKey("codigo") || !resultado.containsKey("nombre")) {
            throw new NegocioException("El mapeo debe incluir codigo y nombre.");
        }
        return resultado;
    }

    private List<List<String>> leerArchivo(MultipartFile archivo) {
        validarTipo(archivo);
        try {
            if (nombreArchivo(archivo).toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
                return leerXlsx(archivo.getBytes());
            }
            return leerCsv(archivo.getBytes());
        } catch (java.io.IOException | org.apache.poi.openxml4j.exceptions.InvalidFormatException ex) {
            throw new NegocioException("No se pudo leer el archivo de importación.");
        }
    }

    private void validarTipo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty() || archivo.getSize() > TAMANIO_MAXIMO) {
            throw new NegocioException("El archivo debe tener entre 1 byte y 10 MB.");
        }
        String nombre = nombreArchivo(archivo).toLowerCase(Locale.ROOT);
        byte[] bytes;
        try {
            bytes = archivo.getBytes();
        } catch (java.io.IOException ex) {
            throw new NegocioException("No se pudo leer el archivo de importación.");
        }
        if (nombre.endsWith(".xlsx")) {
            if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K') {
                throw new NegocioException("El archivo no tiene un contenido XLSX válido.");
            }
            return;
        }
        if (!nombre.endsWith(".csv")) {
            throw new NegocioException("Solo se permiten archivos CSV o XLSX.");
        }
        for (byte value : bytes) {
            if (value == 0) {
                throw new NegocioException("El archivo CSV contiene datos binarios no válidos.");
            }
        }
    }

    private List<List<String>> leerCsv(byte[] bytes) throws java.io.IOException {
        List<List<String>> filas = new ArrayList<>();
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            StringBuilder registro = new StringBuilder();
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (!registro.isEmpty()) registro.append('\n');
                registro.append(linea);
                if (comillasAbiertas(registro)) continue;
                List<String> valores = separarCsv(registro.toString());
                if (valores.stream().anyMatch(valor -> !valor.isBlank())) filas.add(valores);
                registro.setLength(0);
            }
            if (!registro.isEmpty()) throw new NegocioException("El CSV termina con comillas sin cerrar.");
        }
        return filas;
    }

    private List<List<String>> leerXlsx(byte[] bytes)
            throws java.io.IOException, org.apache.poi.openxml4j.exceptions.InvalidFormatException {
        List<List<String>> filas = new ArrayList<>();
        DataFormatter formato = new DataFormatter(Locale.ROOT);
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            if (libro.getNumberOfSheets() == 0) return filas;
            var evaluador = libro.getCreationHelper().createFormulaEvaluator();
            for (Row fila : libro.getSheetAt(0)) {
                List<String> valores = new ArrayList<>();
                int ultimaColumna = Math.max(0, fila.getLastCellNum());
                for (int columna = 0; columna < ultimaColumna; columna++) {
                    valores.add(formato.formatCellValue(fila.getCell(columna), evaluador));
                }
                filas.add(valores);
            }
        }
        return filas;
    }

    private List<String> limpiarEncabezados(List<String> encabezados) {
        return encabezados.stream().map(String::trim).map(valor ->
                valor.startsWith("\uFEFF") ? valor.substring(1) : valor).toList();
    }

    private Map<String, String> filaComoMapa(List<String> encabezados, List<String> valores) {
        Map<String, String> resultado = new LinkedHashMap<>();
        for (int columna = 0; columna < encabezados.size(); columna++) {
            resultado.put(encabezados.get(columna),
                    columna < valores.size() ? valores.get(columna).trim() : "");
        }
        return resultado;
    }

    private List<String> separarCsv(String linea) {
        List<String> valores = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreComillas = false;
        for (int indice = 0; indice < linea.length(); indice++) {
            char caracter = linea.charAt(indice);
            if (caracter == '"') {
                if (entreComillas && indice + 1 < linea.length() && linea.charAt(indice + 1) == '"') {
                    campo.append('"');
                    indice++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (caracter == ',' && !entreComillas) {
                valores.add(campo.toString());
                campo.setLength(0);
            } else {
                campo.append(caracter);
            }
        }
        if (entreComillas) throw new NegocioException("El CSV contiene comillas sin cerrar.");
        valores.add(campo.toString());
        return valores;
    }

    private boolean comillasAbiertas(CharSequence valor) {
        boolean comillas = false;
        for (int indice = 0; indice < valor.length(); indice++) {
            if (valor.charAt(indice) == '"') {
                if (comillas && indice + 1 < valor.length() && valor.charAt(indice + 1) == '"') {
                    indice++;
                } else {
                    comillas = !comillas;
                }
            }
        }
        return comillas;
    }

    private String normalizarNombre(String valor) {
        return switch (valor.trim().replace("_", "").toLowerCase(Locale.ROOT)) {
            case "codigo" -> "codigo";
            case "nombre" -> "nombre";
            case "descripcion" -> "descripcion";
            case "categoriaid" -> "categoriaId";
            case "marcaid" -> "marcaId";
            case "preciocompra" -> "precioCompra";
            case "precioventa" -> "precioVenta";
            case "stockminimo" -> "stockMinimo";
            case "unidad" -> "unidad";
            case "imagenurl" -> "imagenUrl";
            case "especificaciones" -> "especificaciones";
            default -> valor;
        };
    }

    private BigDecimal decimal(String valor, BigDecimal defecto) {
        return vacio(valor) ? defecto : new BigDecimal(valor.trim().replace(',', '.'));
    }

    private BigDecimal decimalParaValidacion(String valor, BigDecimal defecto) {
        if (vacio(valor)) return defecto;
        try {
            return new BigDecimal(valor.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String nombreArchivo(MultipartFile archivo) {
        return archivo == null || archivo.getOriginalFilename() == null
                ? "archivo" : archivo.getOriginalFilename();
    }

    private record Analisis(List<String> columnas, List<Map<String, String>> preview,
                            List<Map<String, String>> filasValidas,
                            List<ImportacionErrorDto> errores,
                            ImportacionResultadoDto resultado) {
    }
}
