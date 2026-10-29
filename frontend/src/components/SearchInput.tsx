import { useEffect, useState } from 'react'

export function SearchInput({ value, onChange, placeholder = 'Buscar...' }: {
  value: string; onChange: (value: string) => void; placeholder?: string
}) {
  const [local, setLocal] = useState(value)
  useEffect(() => setLocal(value), [value])
  useEffect(() => {
    const timer = window.setTimeout(() => { if (local !== value) onChange(local) }, 300)
    return () => window.clearTimeout(timer)
  }, [local, onChange, value])
  return <div className="search"><input aria-label="Buscar" value={local} placeholder={placeholder}
    onChange={(event) => setLocal(event.target.value)} /></div>
}
