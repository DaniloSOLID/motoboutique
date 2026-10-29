export function ErrorState({ mensaje, reintentar }: { mensaje: string; reintentar?: () => void }) {
  return <div className="state-preview" role="alert"><div><b>No se pudo cargar</b>
    <small>{mensaje}</small>{reintentar && <button className="btn secondary" onClick={reintentar}>Reintentar</button>}
  </div></div>
}
