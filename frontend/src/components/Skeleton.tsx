export function Skeleton() {
  return <div className="state-preview" role="status" aria-label="Cargando">
    <div><span className="skeleton wide" /><span className="skeleton" />
      <span className="skeleton short" /><small>Cargando información…</small></div>
  </div>
}
