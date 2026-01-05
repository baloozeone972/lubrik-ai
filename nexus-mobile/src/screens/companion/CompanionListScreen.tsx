import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  RefreshControl,
  Alert,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { companionService } from '@/services/companionService';
import type { Companion, CompanionStackParamList } from '@/types';

type NavigationProp = NativeStackNavigationProp<CompanionStackParamList, 'CompanionList'>;

export function CompanionListScreen() {
  const navigation = useNavigation<NavigationProp>();
  const [companions, setCompanions] = useState<Companion[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const loadCompanions = async () => {
    try {
      const response = await companionService.getAll(0, 50);
      setCompanions(response.content);
    } catch (error) {
      console.error('Failed to load companions:', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadCompanions();
  }, []);

  useEffect(() => {
    const unsubscribe = navigation.addListener('focus', () => {
      loadCompanions();
    });
    return unsubscribe;
  }, [navigation]);

  const onRefresh = async () => {
    setIsRefreshing(true);
    await loadCompanions();
    setIsRefreshing(false);
  };

  const handleDelete = (id: string) => {
    Alert.alert(
      'Supprimer le compagnon',
      'Êtes-vous sûr de vouloir supprimer ce compagnon ?',
      [
        { text: 'Annuler', style: 'cancel' },
        {
          text: 'Supprimer',
          style: 'destructive',
          onPress: async () => {
            try {
              await companionService.delete(id);
              setCompanions(companions.filter((c) => c.id !== id));
            } catch (error) {
              Alert.alert('Erreur', 'Impossible de supprimer le compagnon');
            }
          },
        },
      ]
    );
  };

  const getStyleLabel = (style: string) => {
    const labels: Record<string, string> = {
      FRIENDLY: 'Amical',
      PROFESSIONAL: 'Pro',
      PLAYFUL: 'Joueur',
      WISE: 'Sage',
      CREATIVE: 'Créatif',
    };
    return labels[style] || style;
  };

  const renderCompanion = ({ item }: { item: Companion }) => (
    <TouchableOpacity
      style={styles.card}
      onPress={() => navigation.navigate('CompanionForm', { id: item.id })}
      onLongPress={() => handleDelete(item.id)}
    >
      <View style={styles.avatar}>
        <Text style={styles.avatarText}>
          {item.name.charAt(0).toUpperCase()}
        </Text>
      </View>
      <View style={styles.cardContent}>
        <View style={styles.cardHeader}>
          <Text style={styles.cardTitle}>{item.name}</Text>
          <View style={styles.badge}>
            <Text style={styles.badgeText}>{getStyleLabel(item.style)}</Text>
          </View>
        </View>
        <Text style={styles.cardDescription} numberOfLines={2}>
          {item.description}
        </Text>
        {item.personality?.traits && item.personality.traits.length > 0 && (
          <View style={styles.traits}>
            {item.personality.traits.slice(0, 3).map((trait, index) => (
              <View key={index} style={styles.trait}>
                <Text style={styles.traitText}>{trait}</Text>
              </View>
            ))}
          </View>
        )}
      </View>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <FlatList
        data={companions}
        renderItem={renderCompanion}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        refreshControl={
          <RefreshControl refreshing={isRefreshing} onRefresh={onRefresh} />
        }
        ListEmptyComponent={
          !isLoading ? (
            <View style={styles.emptyState}>
              <Text style={styles.emptyIcon}>🤖</Text>
              <Text style={styles.emptyTitle}>Aucun compagnon</Text>
              <Text style={styles.emptyText}>
                Créez votre premier compagnon IA
              </Text>
            </View>
          ) : null
        }
      />

      <TouchableOpacity
        style={styles.fab}
        onPress={() => navigation.navigate('CompanionForm', {})}
      >
        <Text style={styles.fabText}>+</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f9fafb',
  },
  list: {
    padding: 16,
    paddingBottom: 100,
  },
  card: {
    flexDirection: 'row',
    backgroundColor: 'white',
    borderRadius: 16,
    padding: 16,
    marginBottom: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
  },
  avatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#dbeafe',
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarText: {
    fontSize: 24,
    fontWeight: '600',
    color: '#2563eb',
  },
  cardContent: {
    flex: 1,
    marginLeft: 16,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 4,
  },
  cardTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#111827',
    flex: 1,
  },
  badge: {
    backgroundColor: '#dbeafe',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
  },
  badgeText: {
    fontSize: 12,
    color: '#2563eb',
    fontWeight: '500',
  },
  cardDescription: {
    fontSize: 14,
    color: '#6b7280',
    lineHeight: 20,
  },
  traits: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginTop: 8,
    gap: 6,
  },
  trait: {
    backgroundColor: '#f3f4f6',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
  },
  traitText: {
    fontSize: 12,
    color: '#4b5563',
  },
  emptyState: {
    alignItems: 'center',
    paddingVertical: 60,
  },
  emptyIcon: {
    fontSize: 60,
    marginBottom: 16,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#111827',
    marginBottom: 8,
  },
  emptyText: {
    fontSize: 14,
    color: '#6b7280',
  },
  fab: {
    position: 'absolute',
    right: 24,
    bottom: 24,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#2563eb',
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#2563eb',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 5,
  },
  fabText: {
    fontSize: 32,
    color: 'white',
    lineHeight: 36,
  },
});
