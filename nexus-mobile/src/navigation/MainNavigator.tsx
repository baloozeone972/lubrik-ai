import React from 'react'
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs'
import { Ionicons } from '@expo/vector-icons'
import { DashboardScreen } from '@/screens/home/DashboardScreen'
import { CompanionNavigator } from './CompanionNavigator'
import { ChatNavigator } from './ChatNavigator'
import { ProfileScreen } from '@/screens/profile/ProfileScreen'

const Tab = createBottomTabNavigator()

export const MainNavigator = () => {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ focused, color, size }) => {
          let iconName: keyof typeof Ionicons.glyphMap = 'home'
          
          if (route.name === 'Home') iconName = focused ? 'home' : 'home-outline'
          else if (route.name === 'Companions') iconName = focused ? 'people' : 'people-outline'
          else if (route.name === 'Chat') iconName = focused ? 'chatbubbles' : 'chatbubbles-outline'
          else if (route.name === 'Profile') iconName = focused ? 'person' : 'person-outline'

          return <Ionicons name={iconName} size={size} color={color} />
        },
        tabBarActiveTintColor: '#3B82F6',
        tabBarInactiveTintColor: 'gray',
        headerShown: false,
      })}
    >
      <Tab.Screen name="Home" component={DashboardScreen} />
      <Tab.Screen name="Companions" component={CompanionNavigator} />
      <Tab.Screen name="Chat" component={ChatNavigator} />
      <Tab.Screen name="Profile" component={ProfileScreen} />
    </Tab.Navigator>
  )
}
