import React, { useState } from 'react'
import { View, Text, TextInput, TouchableOpacity, StyleSheet, ScrollView } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { useAuthStore } from '@/store/authStore'

export const RegisterScreen = ({ navigation }: any) => {
  const { register } = useAuthStore()
  const [formData, setFormData] = useState({
    email: '',
    username: '',
    password: '',
    confirmPassword: ''
  })

  const handleRegister = () => {
    if (formData.password !== formData.confirmPassword) {
      alert('Les mots de passe ne correspondent pas')
      return
    }
    register(formData.email, formData.username, formData.password)
  }

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title}>Inscription</Text>
        <Text style={styles.subtitle}>Créer un compte NexusAI</Text>

        <TextInput
          style={styles.input}
          placeholder="Email"
          value={formData.email}
          onChangeText={(email) => setFormData({ ...formData, email })}
          keyboardType="email-address"
          autoCapitalize="none"
        />

        <TextInput
          style={styles.input}
          placeholder="Nom d'utilisateur"
          value={formData.username}
          onChangeText={(username) => setFormData({ ...formData, username })}
          autoCapitalize="none"
        />

        <TextInput
          style={styles.input}
          placeholder="Mot de passe"
          value={formData.password}
          onChangeText={(password) => setFormData({ ...formData, password })}
          secureTextEntry
        />

        <TextInput
          style={styles.input}
          placeholder="Confirmer le mot de passe"
          value={formData.confirmPassword}
          onChangeText={(confirmPassword) => setFormData({ ...formData, confirmPassword })}
          secureTextEntry
        />

        <TouchableOpacity style={styles.button} onPress={handleRegister}>
          <Text style={styles.buttonText}>S'inscrire</Text>
        </TouchableOpacity>

        <TouchableOpacity onPress={() => navigation.navigate('Login')}>
          <Text style={styles.link}>Déjà un compte ? Se connecter</Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F9FAFB' },
  content: { flexGrow: 1, padding: 24, justifyContent: 'center' },
  title: { fontSize: 32, fontWeight: 'bold', color: '#111827', textAlign: 'center', marginBottom: 8 },
  subtitle: { fontSize: 16, color: '#6B7280', textAlign: 'center', marginBottom: 32 },
  input: { backgroundColor: '#FFF', padding: 16, borderRadius: 8, marginBottom: 16, borderWidth: 1, borderColor: '#D1D5DB' },
  button: { backgroundColor: '#3B82F6', padding: 16, borderRadius: 8, marginTop: 16 },
  buttonText: { color: '#FFF', textAlign: 'center', fontSize: 16, fontWeight: '600' },
  link: { color: '#3B82F6', textAlign: 'center', marginTop: 16 }
})
