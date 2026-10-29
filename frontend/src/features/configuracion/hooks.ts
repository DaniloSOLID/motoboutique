import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { configuracionApi } from './api'

export function useConfiguracionEmpresa() {
  const queryClient = useQueryClient()
  const empresa = useQuery({ queryKey: ['configuracion-empresa'], queryFn: configuracionApi.empresa })
  const almacenes = useQuery({ queryKey: ['almacenes'], queryFn: configuracionApi.almacenes })
  const guardar = useMutation({
    mutationFn: configuracionApi.guardar,
    onSuccess: (datos) => queryClient.setQueryData(['configuracion-empresa'], datos),
  })
  return { empresa, almacenes, guardar }
}
