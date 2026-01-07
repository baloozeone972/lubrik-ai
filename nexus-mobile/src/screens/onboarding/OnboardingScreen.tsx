import React, { useRef, useState } from 'react'
import { View, Text, FlatList, StyleSheet, Dimensions } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { Button } from '@/components/common/Button'

const { width } = Dimensions.get('window')

const slides = [
  {
    id: '1',
    title: 'Bienvenue sur NexusAI',
    description: 'Créez votre compagnon IA personnalisé',
    emoji: '🤖'
  },
  {
    id: '2',
    title: 'Conversations Illimitées',
    description: 'Discutez 24/7 avec votre assistant',
    emoji: '💬'
  },
  {
    id: '3',
    title: 'Personnalisation Avancée',
    description: 'Ajustez la personnalité de votre compagnon',
    emoji: '⚙️'
  }
]

export const OnboardingScreen = ({ navigation }: any) => {
  const [currentIndex, setCurrentIndex] = useState(0)
  const flatListRef = useRef<FlatList>(null)

  const handleNext = () => {
    if (currentIndex < slides.length - 1) {
      flatListRef.current?.scrollToIndex({ index: currentIndex + 1 })
      setCurrentIndex(currentIndex + 1)
    } else {
      navigation.navigate('Login')
    }
  }

  return (
    <SafeAreaView style={styles.container}>
      <FlatList
        ref={flatListRef}
        data={slides}
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        onMomentumScrollEnd={(e) => {
          const index = Math.round(e.nativeEvent.contentOffset.x / width)
          setCurrentIndex(index)
        }}
        renderItem={({ item }) => (
          <View style={styles.slide}>
            <Text style={styles.emoji}>{item.emoji}</Text>
            <Text style={styles.title}>{item.title}</Text>
            <Text style={styles.description}>{item.description}</Text>
          </View>
        )}
        keyExtractor={(item) => item.id}
      />

      <View style={styles.footer}>
        <View style={styles.pagination}>
          {slides.map((_, index) => (
            <View
              key={index}
              style={[styles.dot, currentIndex === index && styles.activeDot]}
            />
          ))}
        </View>

        <Button
          title={currentIndex === slides.length - 1 ? 'Commencer' : 'Suivant'}
          onPress={handleNext}
        />
      </View>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FFF' },
  slide: { width, alignItems: 'center', justifyContent: 'center', padding: 40 },
  emoji: { fontSize: 80, marginBottom: 32 },
  title: { fontSize: 28, fontWeight: 'bold', color: '#111827', textAlign: 'center', marginBottom: 16 },
  description: { fontSize: 16, color: '#6B7280', textAlign: 'center' },
  footer: { padding: 24 },
  pagination: { flexDirection: 'row', justifyContent: 'center', marginBottom: 24 },
  dot: { width: 8, height: 8, borderRadius: 4, backgroundColor: '#D1D5DB', marginHorizontal: 4 },
  activeDot: { backgroundColor: '#3B82F6', width: 24 }
})
