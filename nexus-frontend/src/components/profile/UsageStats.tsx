import { Card } from '@/components/common/Card'

interface UsageStatsProps {
  messagesCount: number
  companionsCount: number
  conversationsCount: number
  imagesGenerated: number
}

export const UsageStats = ({ 
  messagesCount, 
  companionsCount, 
  conversationsCount, 
  imagesGenerated 
}: UsageStatsProps) => {
  const stats = [
    { label: 'Messages envoyés', value: messagesCount, icon: '💬' },
    { label: 'Compagnons créés', value: companionsCount, icon: '🤖' },
    { label: 'Conversations', value: conversationsCount, icon: '💭' },
    { label: 'Images générées', value: imagesGenerated, icon: '🎨' }
  ]

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
      {stats.map((stat) => (
        <Card key={stat.label} className="text-center">
          <div className="text-4xl mb-2">{stat.icon}</div>
          <p className="text-3xl font-bold text-gray-800 mb-1">{stat.value}</p>
          <p className="text-sm text-gray-600">{stat.label}</p>
        </Card>
      ))}
    </div>
  )
}
