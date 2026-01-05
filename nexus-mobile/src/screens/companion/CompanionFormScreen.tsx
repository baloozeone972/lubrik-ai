import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { useRoute, useNavigation, RouteProp } from '@react-navigation/native';
import { companionService } from '@/services/companionService';
import type { CompanionStackParamList, CompanionStyle } from '@/types';

type RouteProps = RouteProp<CompanionStackParamList, 'CompanionForm'>;

const STYLES: { value: CompanionStyle; label: string; emoji: string }[] = [
  { value: 'FRIENDLY', label: 'Amical', emoji: '😊' },
  { value: 'PROFESSIONAL', label: 'Pro', emoji: '💼' },
  { value: 'PLAYFUL', label: 'Joueur', emoji: '🎮' },
  { value: 'WISE', label: 'Sage', emoji: '🦉' },
  { value: 'CREATIVE', label: 'Créatif', emoji: '🎨' },
];

export function CompanionFormScreen() {
  const route = useRoute<RouteProps>();
  const navigation = useNavigation();
  const isEditing = Boolean(route.params?.id);

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [style, setStyle] = useState<CompanionStyle>('FRIENDLY');
  const [traits, setTraits] = useState('');
  const [specialties, setSpecialties] = useState('');
  const [customPrompt, setCustomPrompt] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (route.params?.id) {
      loadCompanion(route.params.id);
    }
  }, [route.params?.id]);

  const loadCompanion = async (id: string) => {
    try {
      const companion = await companionService.getById(id);
      setName(companion.name);
      setDescription(companion.description);
      setStyle(companion.style);
      setTraits(companion.personality?.traits?.join(', ') || '');
      setSpecialties(companion.personality?.specialties?.join(', ') || '');
      setCustomPrompt(companion.personality?.customPrompt || '');
    } catch (error) {
      Alert.alert('Erreur', 'Impossible de charger le compagnon');
      navigation.goBack();
    }
  };

  const handleSubmit = async () => {
    if (!name.trim()) {
      Alert.alert('Erreur', 'Le nom est requis');
      return;
    }

    setIsLoading(true);
    try {
      const payload = {
        name: name.trim(),
        description: description.trim(),
        style,
        personality: {
          traits: traits.split(',').map((t) => t.trim()).filter(Boolean),
          specialties: specialties.split(',').map((s) => s.trim()).filter(Boolean),
          tone: style.toLowerCase(),
          customPrompt: customPrompt.trim(),
        },
      };

      if (isEditing && route.params?.id) {
        await companionService.update(route.params.id, payload);
      } else {
        await companionService.create(payload);
      }

      navigation.goBack();
    } catch (error: any) {
      Alert.alert('Erreur', error.response?.data?.message || 'Une erreur est survenue');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <ScrollView style={styles.container}>
      <View style={styles.form}>
        {/* Name */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>Nom du compagnon *</Text>
          <TextInput
            style={styles.input}
            value={name}
            onChangeText={setName}
            placeholder="Ex: Assistant Tech"
            maxLength={50}
          />
        </View>

        {/* Description */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>Description</Text>
          <TextInput
            style={[styles.input, styles.textArea]}
            value={description}
            onChangeText={setDescription}
            placeholder="Décrivez votre compagnon..."
            multiline
            numberOfLines={3}
            maxLength={500}
          />
        </View>

        {/* Style */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>Style de personnalité</Text>
          <View style={styles.styleGrid}>
            {STYLES.map((s) => (
              <TouchableOpacity
                key={s.value}
                style={[
                  styles.styleOption,
                  style === s.value && styles.styleOptionSelected,
                ]}
                onPress={() => setStyle(s.value)}
              >
                <Text style={styles.styleEmoji}>{s.emoji}</Text>
                <Text
                  style={[
                    styles.styleLabel,
                    style === s.value && styles.styleLabelSelected,
                  ]}
                >
                  {s.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* Traits */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>Traits de caractère</Text>
          <TextInput
            style={styles.input}
            value={traits}
            onChangeText={setTraits}
            placeholder="patient, curieux, empathique"
          />
          <Text style={styles.hint}>Séparez par des virgules</Text>
        </View>

        {/* Specialties */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>Spécialités</Text>
          <TextInput
            style={styles.input}
            value={specialties}
            onChangeText={setSpecialties}
            placeholder="technologie, musique, cuisine"
          />
          <Text style={styles.hint}>Séparez par des virgules</Text>
        </View>

        {/* Custom Prompt */}
        <View style={styles.inputGroup}>
          <Text style={styles.label}>Instructions personnalisées</Text>
          <TextInput
            style={[styles.input, styles.textArea]}
            value={customPrompt}
            onChangeText={setCustomPrompt}
            placeholder="Instructions spécifiques pour personnaliser le comportement..."
            multiline
            numberOfLines={4}
            maxLength={2000}
          />
        </View>

        {/* Submit */}
        <TouchableOpacity
          style={[styles.button, isLoading && styles.buttonDisabled]}
          onPress={handleSubmit}
          disabled={isLoading}
        >
          <Text style={styles.buttonText}>
            {isLoading
              ? 'Enregistrement...'
              : isEditing
              ? 'Enregistrer'
              : 'Créer le compagnon'}
          </Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f9fafb',
  },
  form: {
    padding: 24,
  },
  inputGroup: {
    marginBottom: 20,
  },
  label: {
    fontSize: 14,
    fontWeight: '500',
    color: '#374151',
    marginBottom: 8,
  },
  input: {
    backgroundColor: 'white',
    borderWidth: 1,
    borderColor: '#d1d5db',
    borderRadius: 12,
    padding: 16,
    fontSize: 16,
  },
  textArea: {
    minHeight: 100,
    textAlignVertical: 'top',
  },
  hint: {
    fontSize: 12,
    color: '#9ca3af',
    marginTop: 4,
  },
  styleGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
  },
  styleOption: {
    width: '30%',
    backgroundColor: 'white',
    borderWidth: 2,
    borderColor: '#e5e7eb',
    borderRadius: 12,
    padding: 12,
    alignItems: 'center',
  },
  styleOptionSelected: {
    borderColor: '#2563eb',
    backgroundColor: '#eff6ff',
  },
  styleEmoji: {
    fontSize: 24,
    marginBottom: 4,
  },
  styleLabel: {
    fontSize: 12,
    color: '#6b7280',
    fontWeight: '500',
  },
  styleLabelSelected: {
    color: '#2563eb',
  },
  button: {
    backgroundColor: '#2563eb',
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    marginTop: 12,
  },
  buttonDisabled: {
    opacity: 0.7,
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
});
