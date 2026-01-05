import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  RefreshControl,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useNavigation } from '@react-navigation/native';
import { useAuthStore } from '@/store/authStore';
import { companionService } from '@/services/companionService';
import { conversationService } from '@/services/conversationService';
import type { Companion, Conversation } from '@/types';

export function DashboardScreen() {
  const navigation = useNavigation<any>();
  const { user } = useAuthStore();
  const [companions, setCompanions] = useState<Companion[]>([]);
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const loadData = async () => {
    try {
      const [companionsRes, conversationsRes] = await Promise.all([
        companionService.getAll(0, 5),
        conversationService.getAll(0, 5),
      ]);
      setCompanions(companionsRes.content);
      setConversations(conversationsRes.content);
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const onRefresh = async () => {
    setIsRefreshing(true);
    await loadData();
    setIsRefreshing(false);
  };

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <ScrollView
        style={styles.scrollView}
        refreshControl={
          <RefreshControl refreshing={isRefreshing} onRefresh={onRefresh} />
        }
      >
        {/* Welcome */}
        <View style={styles.header}>
          <Text style={styles.greeting}>Bonjour, {user?.username} !</Text>
          <Text style={styles.subtitle}>
            Que voulez-vous faire aujourd'hui ?
          </Text>
        </View>

        {/* Quick Actions */}
        <View style={styles.actionsContainer}>
          <TouchableOpacity
            style={[styles.actionCard, { backgroundColor: '#dbeafe' }]}
            onPress={() => navigation.navigate('Companions', { screen: 'CompanionForm' })}
          >
            <Text style={styles.actionIcon}>✨</Text>
            <Text style={styles.actionTitle}>Créer un compagnon</Text>
            <Text style={styles.actionSubtitle}>Personnalisez votre IA</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.actionCard, { backgroundColor: '#dcfce7' }]}
            onPress={() => navigation.navigate('Conversations')}
          >
            <Text style={styles.actionIcon}>💬</Text>
            <Text style={styles.actionTitle}>Nouvelle conversation</Text>
            <Text style={styles.actionSubtitle}>Démarrez un chat</Text>
          </TouchableOpacity>
        </View>

        {/* Recent Companions */}
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>Mes compagnons</Text>
            <TouchableOpacity onPress={() => navigation.navigate('Companions')}>
              <Text style={styles.seeAll}>Voir tout</Text>
            </TouchableOpacity>
          </View>

          {companions.length === 0 ? (
            <View style={styles.emptyState}>
              <Text style={styles.emptyIcon}>🤖</Text>
              <Text style={styles.emptyText}>Aucun compagnon créé</Text>
            </View>
          ) : (
            companions.map((companion) => (
              <TouchableOpacity
                key={companion.id}
                style={styles.listItem}
                onPress={() =>
                  navigation.navigate('Conversations', {
                    screen: 'ConversationList',
                    params: { companionId: companion.id },
                  })
                }
              >
                <View style={styles.avatar}>
                  <Text style={styles.avatarText}>
                    {companion.name.charAt(0).toUpperCase()}
                  </Text>
                </View>
                <View style={styles.listItemContent}>
                  <Text style={styles.listItemTitle}>{companion.name}</Text>
                  <Text style={styles.listItemSubtitle} numberOfLines={1}>
                    {companion.description}
                  </Text>
                </View>
                <Text style={styles.arrow}>→</Text>
              </TouchableOpacity>
            ))
          )}
        </View>

        {/* Recent Conversations */}
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>Conversations récentes</Text>
            <TouchableOpacity onPress={() => navigation.navigate('Conversations')}>
              <Text style={styles.seeAll}>Voir tout</Text>
            </TouchableOpacity>
          </View>

          {conversations.length === 0 ? (
            <View style={styles.emptyState}>
              <Text style={styles.emptyIcon}>💬</Text>
              <Text style={styles.emptyText}>Aucune conversation</Text>
            </View>
          ) : (
            conversations.map((conversation) => (
              <TouchableOpacity
                key={conversation.id}
                style={styles.listItem}
                onPress={() =>
                  navigation.navigate('Conversations', {
                    screen: 'Chat',
                    params: { conversationId: conversation.id },
                  })
                }
              >
                <View style={styles.avatar}>
                  <Text style={styles.avatarText}>
                    {conversation.companion?.name?.charAt(0)?.toUpperCase() || 'C'}
                  </Text>
                </View>
                <View style={styles.listItemContent}>
                  <Text style={styles.listItemTitle}>
                    {conversation.companion?.name || 'Conversation'}
                  </Text>
                  <Text style={styles.listItemSubtitle}>
                    {conversation.title || 'Sans titre'}
                  </Text>
                </View>
                <Text style={styles.arrow}>→</Text>
              </TouchableOpacity>
            ))
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f9fafb',
  },
  scrollView: {
    flex: 1,
  },
  header: {
    padding: 24,
    paddingBottom: 16,
  },
  greeting: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#111827',
  },
  subtitle: {
    fontSize: 16,
    color: '#6b7280',
    marginTop: 4,
  },
  actionsContainer: {
    flexDirection: 'row',
    paddingHorizontal: 24,
    gap: 12,
  },
  actionCard: {
    flex: 1,
    padding: 16,
    borderRadius: 16,
  },
  actionIcon: {
    fontSize: 24,
    marginBottom: 8,
  },
  actionTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#111827',
  },
  actionSubtitle: {
    fontSize: 12,
    color: '#6b7280',
    marginTop: 2,
  },
  section: {
    marginTop: 24,
    paddingHorizontal: 24,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#111827',
  },
  seeAll: {
    fontSize: 14,
    color: '#2563eb',
    fontWeight: '500',
  },
  emptyState: {
    alignItems: 'center',
    padding: 32,
    backgroundColor: 'white',
    borderRadius: 16,
  },
  emptyIcon: {
    fontSize: 40,
    marginBottom: 8,
  },
  emptyText: {
    fontSize: 14,
    color: '#6b7280',
  },
  listItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'white',
    padding: 16,
    borderRadius: 12,
    marginBottom: 8,
  },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: '#e5e7eb',
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#374151',
  },
  listItemContent: {
    flex: 1,
    marginLeft: 12,
  },
  listItemTitle: {
    fontSize: 16,
    fontWeight: '500',
    color: '#111827',
  },
  listItemSubtitle: {
    fontSize: 14,
    color: '#6b7280',
    marginTop: 2,
  },
  arrow: {
    fontSize: 18,
    color: '#9ca3af',
  },
});
