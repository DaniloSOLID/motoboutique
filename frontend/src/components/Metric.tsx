import type { ReactNode } from 'react'

export function Metric({ label, value, icon }: { label: string; value: string; icon: ReactNode }) {
  return <article className="metric"><div className="metric-head"><span>{label}</span>
    <b className="icon-box">{icon}</b></div><strong>{value}</strong><small>Datos actualizados desde la API</small></article>
}
