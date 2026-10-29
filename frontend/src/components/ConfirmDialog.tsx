import type { ReactNode } from 'react'
export function ConfirmDialog({ title, children, confirmar, cancelar }: {
  title: string; children: ReactNode; confirmar: () => void; cancelar: () => void
}) {
  return <div className="overlay" role="alertdialog" aria-modal="true"><div className="modal">
    <h2>{title}</h2><p>{children}</p><div className="modal-actions">
      <button className="btn secondary" onClick={cancelar}>Cancelar</button>
      <button className="btn danger" onClick={confirmar}>Confirmar</button></div>
  </div></div>
}
