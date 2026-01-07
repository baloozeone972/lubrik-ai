import { useMutation } from '@tanstack/react-query'
import { mediaService } from '@/services/mediaService'

export const useMedia = () => {
  const uploadMutation = useMutation({
    mutationFn: (file: File) => mediaService.uploadImage(file)
  })

  const generateMutation = useMutation({
    mutationFn: (prompt: string) => mediaService.generateImage(prompt)
  })

  return {
    upload: uploadMutation.mutateAsync,
    generate: generateMutation.mutateAsync,
    isUploading: uploadMutation.isPending,
    isGenerating: generateMutation.isPending
  }
}
