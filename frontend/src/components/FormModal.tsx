import type { ReactNode } from 'react'
export function FormModal({ title, children, onClose }: { title: string; children: ReactNode; onClose: () => void }) {
  return <div className="overlay" role="dialog" aria-modal="true"><div className="modal">
    <div className="modal-title"><h2>{title}</h2><button className="icon-btn" onClick={onClose}>Cerrar</button></div>{children}
  </div></div>
}
