import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { ApiError } from '../../api/client'
import { ErrorState } from '../../components/ErrorState'
import { FileDropzone } from '../../components/FileDropzone'
import { Skeleton } from '../../components/Skeleton'
import { descargar } from '../inventario/api'
import { importacionesApi, type ImportacionResultado } from './importacionesApi'

const camposImportables = [
  'codigo', 'nombre', 'descripcion', 'categoriaId', 'marcaId', 'precioCompra',
  'precioVenta', 'stockMinimo', 'unidad', 'imagenUrl', 'especificaciones',
]

export function ImportacionPage() {
  const [archivo, setArchivo] = useState<File | null>(null)
  const [resultado, setResultado] = useState<ImportacionResultado | null>(null)
  const [mapeo, setMapeo] = useState<Record<string, string>>({})
  const [paso, setPaso] = useState(1)
  const queryClient = useQueryClient()
  const historial = useQuery({ queryKey: ['importaciones-catalogo'], queryFn: importacionesApi.historial })
  const detectar = useMutation({
    mutationFn: importacionesApi.detectar,
    onSuccess: (data) => {
      setResultado(data)
      const propuesto: Record<string, string> = {}
      camposImportables.forEach((destino) => {
        const fuente = data.columnas.find((columna) => columna.toLocaleLowerCase('es-PE') === destino.toLocaleLowerCase('es-PE'))
        if (fuente) propuesto[destino] = fuente
      })
      setMapeo(propuesto)
      setPaso(2)
    },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo leer el archivo.'),
  })
  const validar = useMutation({
    mutationFn: () => importacionesApi.validar(archivo!, mapeo),
    onSuccess: (data) => { setResultado(data); setPaso(3) },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo validar la importación.'),
  })
  const confirmar = useMutation({
    mutationFn: () => importacionesApi.confirmar(archivo!, mapeo),
    onSuccess: async (data) => {
      setResultado(data)
      setPaso(4)
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['importaciones-catalogo'] }),
        queryClient.invalidateQueries({ queryKey: ['catalogo-productos'] }),
        queryClient.invalidateQueries({ queryKey: ['productos-opciones'] }),
      ])
      toast.success(`Importación confirmada: ${data.filasValidas.length} filas válidas.`)
    },
    onError: (error) => toast.error(error instanceof ApiError ? error.detalle : 'No se pudo confirmar la importación.'),
  })
  const seleccionar = (file: File) => {
    if (!/\.(csv|xlsx)$/i.test(file.name)) { toast.error('Selecciona un archivo CSV o XLSX.'); return }
    if (file.size > 10 * 1024 * 1024) { toast.error('El archivo supera el límite de 10 MB.'); return }
    setArchivo(file)
    setResultado(null)
    setMapeo({})
    setPaso(1)
  }
  const bajarPlantilla = async () => {
    try { descargar(await importacionesApi.plantilla(), 'plantilla_catalogo.csv') }
    catch (error) { toast.error(error instanceof ApiError ? error.detalle : 'No se pudo descargar la plantilla.') }
  }
  if (historial.isPending) return <Skeleton />
  if (historial.isError) return <ErrorState mensaje={historial.error instanceof ApiError
    ? historial.error.detalle : 'No se pudo cargar el historial.'} reintentar={() => void historial.refetch()} />
  return <section>
    <div className="card section-space">
      <div className="section-title"><div><h3>Importación de catálogo</h3><span>Los cambios se guardan únicamente al confirmar.</span></div>
        <button className="btn secondary" onClick={() => void bajarPlantilla()}>Descargar plantilla</button></div>
      <div className="steps"><span className={paso >= 1 ? 'active' : ''}>1. Archivo</span><span className={paso >= 2 ? 'active' : ''}>2. Mapeo</span>
        <span className={paso >= 3 ? 'active' : ''}>3. Validación</span><span className={paso >= 4 ? 'active' : ''}>4. Confirmación</span></div>
      {paso < 4 && <div className="import-workflow">
        <FileDropzone onFile={seleccionar} />
        {archivo && <p>Archivo seleccionado: <b>{archivo.name}</b> ({(archivo.size / 1024).toLocaleString('es-PE', { maximumFractionDigits: 1 })} KB)
          <button className="btn ghost" onClick={() => detectar.mutate(archivo)} disabled={detectar.isPending}>
            {detectar.isPending ? 'Leyendo…' : 'Detectar columnas'}</button></p>}
        {paso >= 2 && resultado && <div className="mapping-grid">
          <h4>Mapea columnas del archivo a los campos del catálogo</h4>
          {camposImportables.map((campo) => <label key={campo}>{campo}
            <select value={mapeo[campo] ?? ''} onChange={(event) => setMapeo((actual) => {
              const next = { ...actual }
              if (event.target.value) next[campo] = event.target.value
              else delete next[campo]
              return next
            })}>
              <option value="">No importar</option>{resultado.columnas.map((columna) =>
                <option key={columna} value={columna}>{columna}</option>)}
            </select></label>)}
          <button className="btn primary" disabled={!mapeo.codigo || !mapeo.nombre || validar.isPending}
            onClick={() => validar.mutate()}>{validar.isPending ? 'Validando…' : 'Validar archivo'}</button>
        </div>}
        {paso >= 3 && resultado && <div className="validation-results">
          <h4>Validación: {resultado.filasValidas.length} filas válidas · {resultado.errores.length} errores</h4>
          {resultado.errores.length > 0 && <div className="scroll"><table><thead><tr><th>Fila</th><th>Columna</th><th>Error</th></tr></thead>
            <tbody>{resultado.errores.map((error, index) => <tr key={`${error.fila}-${error.columna}-${index}`}>
              <td>{error.fila}</td><td>{error.columna}</td><td>{error.mensaje}</td></tr>)}</tbody></table></div>}
          {resultado.vistaPrevia.length > 0 && <div className="scroll"><table><thead><tr>{resultado.columnas.map((columna) =>
            <th key={columna}>{columna}</th>)}</tr></thead><tbody>{resultado.vistaPrevia.map((fila, index) =>
              <tr key={index}>{resultado.columnas.map((columna) => <td key={columna}>{fila[columna] ?? ''}</td>)}</tr>)}</tbody></table></div>}
          <button className="btn primary" disabled={confirmar.isPending || resultado.filasValidas.length === 0}
            onClick={() => confirmar.mutate()}>{confirmar.isPending ? 'Confirmando…' : `Confirmar ${resultado.filasValidas.length} filas válidas`}</button>
        </div>}
      </div>}
      {paso === 4 && resultado && <div role="status"><h4>Importación confirmada</h4>
        <p>Se guardaron {resultado.filasValidas.length} filas. Errores registrados: {resultado.errores.length}.</p>
        <button className="btn secondary" onClick={() => { setPaso(1); setArchivo(null); setResultado(null); setMapeo({}) }}>Importar otro archivo</button>
      </div>}
    </div>
    <div className="card"><h3>Historial de importaciones</h3>
      {historial.data.length === 0 ? <p className="empty-state">No hay importaciones registradas.</p> :
        <div className="scroll"><table><thead><tr><th>Archivo</th><th>Fecha</th><th>Filas válidas</th><th>Errores</th><th>Estado</th><th>Usuario ID</th></tr></thead>
          <tbody>{historial.data.map((item) => <tr key={item.id}><td>{item.archivo}</td>
            <td>{new Date(item.fecha).toLocaleString('es-PE')}</td><td>{item.filasValidas}</td><td>{item.errores}</td><td>{item.estado}</td><td>{item.usuarioId}</td></tr>)}</tbody></table></div>}
    </div>
  </section>
}
