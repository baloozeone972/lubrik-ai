import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { conversationService } from '@/services/conversationService'

export const useConversations = (companionId?: string) => {
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['conversations', companionId],
    queryFn: () => conversationService.getAll(companionId)
  })

  const createMutation = useMutation({
    mutationFn: (data: { companionId: string; title?: string }) =>
      conversationService.create(data.companionId, data.title),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['conversations'] })
    }
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => conversationService.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['conversations'] })
    }
  })

  return {
    conversations: data || [],
    isLoading,
    create: createMutation.mutateAsync,
    delete: deleteMutation.mutateAsync
  }
}
