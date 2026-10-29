import { useRef } from 'react'
export function FileDropzone({ onFile, accept = '.csv,.xlsx' }: {
  onFile: (file: File) => void; accept?: string
}) {
  const input = useRef<HTMLInputElement>(null)
  return <div className="drop" role="button" tabIndex={0} onClick={() => input.current?.click()}
    onKeyDown={(event) => { if (event.key === 'Enter' || event.key === ' ') input.current?.click() }}>
    <b>Seleccionar archivo</b><span>CSV o XLSX · máximo 10 MB</span>
    <input ref={input} hidden type="file" accept={accept}
      onChange={(event) => { const file = event.target.files?.[0]; if (file) onFile(file) }} />
  </div>
}
