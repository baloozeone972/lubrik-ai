import React, { useState } from 'react'
import { View, Text, FlatList, TextInput, TouchableOpacity, StyleSheet, KeyboardAvoidingView, Platform } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'

export const ChatScreen = ({ route }: any) => {
  const { companionId } = route.params || {}
  const [message, setMessage] = useState('')
  const [messages, setMessages] = useState([
    { id: '1', role: 'assistant', content: 'Bonjour! Comment puis-je vous aider?', timestamp: new Date() },
    { id: '2', role: 'user', content: 'Salut!', timestamp: new Date() }
  ])

  const handleSend = () => {
    if (message.trim()) {
      setMessages([...messages, {
        id: Date.now().toString(),
        role: 'user',
        content: message,
        timestamp: new Date()
      }])
      setMessage('')
    }
  }

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView 
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.keyboardView}
      >
        <View style={styles.header}>
          <Text style={styles.headerTitle}>Chat</Text>
        </View>

        <FlatList
          data={messages}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.messagesList}
          renderItem={({ item }) => (
            <View style={[
              styles.messageBubble,
              item.role === 'user' ? styles.userMessage : styles.assistantMessage
            ]}>
              <Text style={[
                styles.messageText,
                item.role === 'user' ? styles.userText : styles.assistantText
              ]}>
                {item.content}
              </Text>
            </View>
          )}
        />

        <View style={styles.inputContainer}>
          <TextInput
            style={styles.input}
            value={message}
            onChangeText={setMessage}
            placeholder="Écrivez votre message..."
            multiline
          />
          <TouchableOpacity 
            style={styles.sendButton}
            onPress={handleSend}
            disabled={!message.trim()}
          >
            <Text style={styles.sendButtonText}>➤</Text>
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#F9FAFB' },
  keyboardView: { flex: 1 },
  header: { backgroundColor: '#FFF', padding: 16, borderBottomWidth: 1, borderBottomColor: '#E5E7EB' },
  headerTitle: { fontSize: 18, fontWeight: '600', color: '#111827' },
  messagesList: { padding: 16 },
  messageBubble: { maxWidth: '80%', padding: 12, borderRadius: 16, marginBottom: 12 },
  userMessage: { alignSelf: 'flex-end', backgroundColor: '#3B82F6' },
  assistantMessage: { alignSelf: 'flex-start', backgroundColor: '#E5E7EB' },
  messageText: { fontSize: 16 },
  userText: { color: '#FFF' },
  assistantText: { color: '#111827' },
  inputContainer: { flexDirection: 'row', padding: 16, backgroundColor: '#FFF', borderTopWidth: 1, borderTopColor: '#E5E7EB', alignItems: 'center' },
  input: { flex: 1, backgroundColor: '#F9FAFB', borderRadius: 24, paddingHorizontal: 16, paddingVertical: 8, marginRight: 8, maxHeight: 100 },
  sendButton: { width: 40, height: 40, borderRadius: 20, backgroundColor: '#3B82F6', alignItems: 'center', justifyContent: 'center' },
  sendButtonText: { color: '#FFF', fontSize: 20 }
})
