export function Badge({ children }: { children: string }) {
  const tone = ['Aprobado','Activo','Recibida','Completada','DISPONIBLE'].includes(children)
    ? 'success' : ['Pendiente','Enviada','En tránsito','PENDIENTE'].includes(children)
      ? 'warning' : ['Rechazado','Cancelada','Stock bajo','AGOTADO'].includes(children) ? 'danger' : 'neutral'
  return <span className={`badge ${tone}`}><i />{children}</span>
}
