import { cn } from '@/utils/helpers'

interface LoaderProps {
  size?: 'sm' | 'md' | 'lg'
  fullScreen?: boolean
  text?: string
}

export const Loader = ({ size = 'md', fullScreen = false, text }: LoaderProps) => {
  const sizes = {
    sm: 'w-6 h-6 border-2',
    md: 'w-12 h-12 border-4',
    lg: 'w-16 h-16 border-4'
  }

  const spinner = (
    <div className="flex flex-col items-center gap-4">
      <div className={cn('rounded-full border-primary-200 border-t-primary-500 animate-spin', sizes[size])} />
      {text && <p className="text-gray-600">{text}</p>}
    </div>
  )

  if (fullScreen) {
    return (
      <div className="fixed inset-0 bg-white bg-opacity-90 flex items-center justify-center z-50">
        {spinner}
      </div>
    )
  }

  return spinner
}
