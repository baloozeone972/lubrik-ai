import { ReactNode } from 'react'
import { cn } from '@/utils/helpers'

interface AlertProps {
  type?: 'info' | 'success' | 'warning' | 'error'
  children: ReactNode
  onClose?: () => void
  className?: string
}

export const Alert = ({ type = 'info', children, onClose, className }: AlertProps) => {
  const styles = {
    info: 'bg-blue-50 border-blue-200 text-blue-800',
    success: 'bg-green-50 border-green-200 text-green-800',
    warning: 'bg-yellow-50 border-yellow-200 text-yellow-800',
    error: 'bg-red-50 border-red-200 text-red-800'
  }

  const icons = {
    info: 'ℹ️',
    success: '✅',
    warning: '⚠️',
    error: '❌'
  }

  return (
    <div className={cn('border rounded-lg p-4 flex items-start gap-3', styles[type], className)}>
      <span className="text-xl">{icons[type]}</span>
      <div className="flex-1">{children}</div>
      {onClose && (
        <button onClick={onClose} className="text-xl hover:opacity-70">
          ×
        </button>
      )}
    </div>
  )
}
