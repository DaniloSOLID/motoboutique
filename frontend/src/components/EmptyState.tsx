import type { ReactNode } from 'react'

export function EmptyState({ title, description, action }: {
  title: string
  description: string
  action?: ReactNode
}) {
  return <div className="state-preview"><div><span className="state-icon">i</span>
    <b>{title}</b><small>{description}</small>{action}</div></div>
}
