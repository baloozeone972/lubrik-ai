import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Card } from '@/components/common/Card'
import { Input } from '@/components/common/Input'
import { Button } from '@/components/common/Button'

export const ChatScreen = () => {
  const { companionId } = useParams()
  const [message, setMessage] = useState('')

  return (
    <div className="h-[calc(100vh-12rem)]">
      <Card padding="none" className="h-full flex flex-col">
        <div className="p-4 border-b">
          <h2 className="text-lg font-semibold">Chat avec Compagnon {companionId}</h2>
        </div>
        
        <div className="flex-1 p-4 overflow-y-auto">
          <div className="space-y-4">
            <div className="flex justify-start">
              <div className="bg-gray-100 rounded-lg p-3 max-w-[70%]">
                <p className="text-sm">Bonjour! Comment puis-je vous aider?</p>
              </div>
            </div>
            <div className="flex justify-end">
              <div className="bg-blue-600 text-white rounded-lg p-3 max-w-[70%]">
                <p className="text-sm">Salut!</p>
              </div>
            </div>
          </div>
        </div>
        
        <div className="p-4 border-t">
          <div className="flex gap-2">
            <Input
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="Écrivez votre message..."
              className="flex-1"
            />
            <Button>Envoyer</Button>
          </div>
        </div>
      </Card>
    </div>
  )
}
