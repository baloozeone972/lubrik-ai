import React from 'react'
import { TouchableOpacity, Text, StyleSheet, ActivityIndicator } from 'react-native'

interface ButtonProps {
  title: string
  onPress: () => void
  variant?: 'primary' | 'secondary' | 'outline'
  loading?: boolean
  disabled?: boolean
}

export const Button = ({ 
  title, 
  onPress, 
  variant = 'primary', 
  loading = false, 
  disabled = false 
}: ButtonProps) => {
  const styles = StyleSheet.create({
    button: {
      padding: 16,
      borderRadius: 8,
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: variant === 'primary' ? '#3B82F6' : variant === 'secondary' ? '#6B7280' : 'transparent',
      borderWidth: variant === 'outline' ? 1 : 0,
      borderColor: '#3B82F6',
      opacity: disabled ? 0.5 : 1
    },
    text: {
      color: variant === 'outline' ? '#3B82F6' : '#FFF',
      fontSize: 16,
      fontWeight: '600'
    }
  })

  return (
    <TouchableOpacity
      style={styles.button}
      onPress={onPress}
      disabled={disabled || loading}
    >
      {loading ? (
        <ActivityIndicator color="#FFF" />
      ) : (
        <Text style={styles.text}>{title}</Text>
      )}
    </TouchableOpacity>
  )
}
