import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { seguridadFeatureApi } from './api'

export function useUsuarios(filtros: {
  q?: string; page: number; size: number; rol?: string; activo?: boolean
  sort?: string; direction?: string
}) {
  return useQuery({
    queryKey: ['usuarios', filtros],
    queryFn: () => seguridadFeatureApi.usuarios(filtros),
  })
}

export function useRolesYPermisos() {
  const queryClient = useQueryClient()
  const roles = useQuery({ queryKey: ['roles'], queryFn: seguridadFeatureApi.roles })
  const permisos = useQuery({ queryKey: ['permisos'], queryFn: seguridadFeatureApi.permisos })
  const actualizar = useMutation({
    mutationFn: ({ id, permisos: codigos }: { id: number; permisos: string[]; codigo: string }) =>
      seguridadFeatureApi.actualizarPermisos(id, codigos),
    onMutate: async ({ id, permisos: codigos }) => {
      await queryClient.cancelQueries({ queryKey: ['roles'] })
      const anteriores = queryClient.getQueryData<Awaited<ReturnType<typeof seguridadFeatureApi.roles>>>(['roles'])
      queryClient.setQueryData(['roles'], (roles: typeof anteriores) =>
        roles?.map((rol) => rol.id === id ? { ...rol, permisos: codigos } : rol))
      return { anteriores }
    },
    onError: (_error, _variables, contexto) => {
      if (contexto?.anteriores) queryClient.setQueryData(['roles'], contexto.anteriores)
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['roles'] }),
  })
  return { roles, permisos, actualizar }
}
