import React from 'react'
import { View, StyleSheet, ViewStyle, TouchableOpacity } from 'react-native'

interface CardProps {
  children: React.ReactNode
  style?: ViewStyle
  padding?: 'none' | 'sm' | 'md' | 'lg'
  onPress?: () => void
}

export const Card: React.FC<CardProps> = ({
  children,
  style,
  padding = 'md',
  onPress,
}) => {
  const cardStyle: ViewStyle = {
    ...styles.base,
    ...styles[`padding_${padding}`],
    ...style,
  }

  if (onPress) {
    return (
      <TouchableOpacity style={cardStyle} onPress={onPress} activeOpacity={0.7}>
        {children}
      </TouchableOpacity>
    )
  }

  return <View style={cardStyle}>{children}</View>
}

const styles = StyleSheet.create({
  base: {
    backgroundColor: '#FFF',
    borderRadius: 12,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  padding_none: {
    padding: 0,
  },
  padding_sm: {
    padding: 12,
  },
  padding_md: {
    padding: 16,
  },
  padding_lg: {
    padding: 24,
  },
})
