export function Pagination({ page, totalPages, onPage }: {
  page: number; totalPages: number; onPage: (page: number) => void
}) {
  return <div className="pagination"><span>Página {totalPages === 0 ? 0 : page + 1} de {totalPages}</span><div>
    <button disabled={page <= 0} onClick={() => onPage(page - 1)}>Anterior</button>
    <button disabled={totalPages === 0 || page + 1 >= totalPages} onClick={() => onPage(page + 1)}>Siguiente</button>
  </div></div>
}
