import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { companionService } from '@/services/companionService'
import type { CreateCompanionRequest } from '@/types/companion.types'

export const useCompanions = () => {
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['companions'],
    queryFn: () => companionService.getAll()
  })

  const createMutation = useMutation({
    mutationFn: (data: CreateCompanionRequest) => companionService.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['companions'] })
    }
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => companionService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['companions'] })
    }
  })

  return {
    companions: data?.data || [],
    isLoading,
    create: createMutation.mutateAsync,
    delete: deleteMutation.mutateAsync
  }
}
