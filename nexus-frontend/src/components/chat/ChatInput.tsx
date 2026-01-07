import { useState, KeyboardEvent } from 'react'
import { Button } from '@/components/common/Button'

interface ChatInputProps {
  onSend: (message: string) => void
  disabled?: boolean
}

export const ChatInput = ({ onSend, disabled }: ChatInputProps) => {
  const [message, setMessage] = useState('')

  const handleSend = () => {
    if (message.trim()) {
      onSend(message)
      setMessage('')
    }
  }

  const handleKeyPress = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div className="border-t p-4">
      <div className="flex gap-2">
        <textarea
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          onKeyPress={handleKeyPress}
          placeholder="Écrivez votre message..."
          disabled={disabled}
          className="flex-1 px-4 py-3 border rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-primary-500"
          rows={1}
        />
        <Button onClick={handleSend} disabled={disabled || !message.trim()}>
          Envoyer
        </Button>
      </div>
    </div>
  )
}
