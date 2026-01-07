import { ReactNode, useEffect, useRef, useState } from 'react'
import { cn } from '@/utils/helpers'

interface DropdownProps {
  trigger: ReactNode
  children: ReactNode
  align?: 'left' | 'right'
}

export const Dropdown = ({ trigger, children, align = 'left' }: DropdownProps) => {
  const [isOpen, setIsOpen] = useState(false)
  const dropdownRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false)
      }
    }
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside)
    }
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [isOpen])

  return (
    <div ref={dropdownRef} className="relative">
      <div onClick={() => setIsOpen(!isOpen)} className="cursor-pointer">
        {trigger}
      </div>
      {isOpen && (
        <div className={cn(
          'absolute top-full mt-2 bg-white rounded-lg shadow-xl border min-w-48 py-2 z-50',
          align === 'right' ? 'right-0' : 'left-0'
        )}>
          {children}
        </div>
      )}
    </div>
  )
}

interface DropdownItemProps {
  children: ReactNode
  onClick?: () => void
  className?: string
}

export const DropdownItem = ({ children, onClick, className }: DropdownItemProps) => (
  <button
    onClick={onClick}
    className={cn('w-full text-left px-4 py-2 hover:bg-gray-50 transition-colors', className)}
  >
    {children}
  </button>
)
