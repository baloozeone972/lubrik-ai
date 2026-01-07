import { createBottomTabNavigator } from '@react-navigation/bottom-tabs'
import { DashboardScreen } from '@/screens/home/DashboardScreen'
import { CompanionListScreen } from '@/screens/companion/CompanionListScreen'
import { ChatScreen } from '@/screens/chat/ChatScreen'
import { ProfileScreen } from '@/screens/profile/ProfileScreen'

const Tab = createBottomTabNavigator()

export const MainNavigator = () => {
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: '#3B82F6',
        tabBarInactiveTintColor: '#9CA3AF'
      }}
    >
      <Tab.Screen 
        name="Dashboard" 
        component={DashboardScreen}
        options={{ tabBarLabel: 'Accueil', tabBarIcon: () => '🏠' }}
      />
      <Tab.Screen 
        name="Companions" 
        component={CompanionListScreen}
        options={{ tabBarLabel: 'Compagnons', tabBarIcon: () => '🤖' }}
      />
      <Tab.Screen 
        name="Chat" 
        component={ChatScreen}
        options={{ tabBarLabel: 'Chat', tabBarIcon: () => '💬' }}
      />
      <Tab.Screen 
        name="Profile" 
        component={ProfileScreen}
        options={{ tabBarLabel: 'Profil', tabBarIcon: () => '👤' }}
      />
    </Tab.Navigator>
  )
}
