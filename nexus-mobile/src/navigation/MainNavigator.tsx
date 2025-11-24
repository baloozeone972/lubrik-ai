import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { Ionicons } from '@expo/vector-icons';
import { DashboardScreen } from '@/screens/home/DashboardScreen';
import { CompanionListScreen } from '@/screens/companion/CompanionListScreen';
import { CompanionFormScreen } from '@/screens/companion/CompanionFormScreen';
import { ConversationListScreen } from '@/screens/conversation/ConversationListScreen';
import { ChatScreen } from '@/screens/conversation/ChatScreen';
import { ProfileScreen } from '@/screens/profile/ProfileScreen';
import type {
  MainTabParamList,
  CompanionStackParamList,
  ConversationStackParamList,
} from '@/types';

const Tab = createBottomTabNavigator<MainTabParamList>();
const CompanionStack = createNativeStackNavigator<CompanionStackParamList>();
const ConversationStack = createNativeStackNavigator<ConversationStackParamList>();

function CompanionNavigator() {
  return (
    <CompanionStack.Navigator>
      <CompanionStack.Screen
        name="CompanionList"
        component={CompanionListScreen}
        options={{ title: 'Compagnons' }}
      />
      <CompanionStack.Screen
        name="CompanionForm"
        component={CompanionFormScreen}
        options={({ route }) => ({
          title: route.params?.id ? 'Modifier' : 'Nouveau compagnon',
        })}
      />
    </CompanionStack.Navigator>
  );
}

function ConversationNavigator() {
  return (
    <ConversationStack.Navigator>
      <ConversationStack.Screen
        name="ConversationList"
        component={ConversationListScreen}
        options={{ title: 'Conversations' }}
      />
      <ConversationStack.Screen
        name="Chat"
        component={ChatScreen}
        options={{ headerShown: false }}
      />
    </ConversationStack.Navigator>
  );
}

export function MainNavigator() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ focused, color, size }) => {
          let iconName: keyof typeof Ionicons.glyphMap = 'home';

          switch (route.name) {
            case 'Home':
              iconName = focused ? 'home' : 'home-outline';
              break;
            case 'Companions':
              iconName = focused ? 'people' : 'people-outline';
              break;
            case 'Conversations':
              iconName = focused ? 'chatbubbles' : 'chatbubbles-outline';
              break;
            case 'Profile':
              iconName = focused ? 'person' : 'person-outline';
              break;
          }

          return <Ionicons name={iconName} size={size} color={color} />;
        },
        tabBarActiveTintColor: '#2563eb',
        tabBarInactiveTintColor: 'gray',
        headerShown: route.name === 'Home' || route.name === 'Profile',
      })}
    >
      <Tab.Screen
        name="Home"
        component={DashboardScreen}
        options={{ title: 'Accueil' }}
      />
      <Tab.Screen
        name="Companions"
        component={CompanionNavigator}
        options={{ title: 'Compagnons' }}
      />
      <Tab.Screen
        name="Conversations"
        component={ConversationNavigator}
        options={{ title: 'Conversations' }}
      />
      <Tab.Screen
        name="Profile"
        component={ProfileScreen}
        options={{ title: 'Profil' }}
      />
    </Tab.Navigator>
  );
}
