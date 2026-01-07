import React, { ReactNode } from 'react'
import { View, StyleSheet } from 'react-native'

interface CardProps {
  children: ReactNode
}

export const Card = ({ children }: CardProps) => {
  return <View style={styles.card}>{children}</View>
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#FFF',
    borderRadius: 12,
    padding: 16,
    shadowColor: '#000',
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
    marginBottom: 12
  }
})
