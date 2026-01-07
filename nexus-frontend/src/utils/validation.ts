export const validation = {
  email: (value: string) => {
    if (!value) return 'Email requis'
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return 'Email invalide'
    return undefined
  },
  password: (value: string) => {
    if (!value) return 'Mot de passe requis'
    if (value.length < 8) return 'Minimum 8 caractères'
    if (!/[A-Z]/.test(value)) return 'Une majuscule requise'
    if (!/[a-z]/.test(value)) return 'Une minuscule requise'
    if (!/[0-9]/.test(value)) return 'Un chiffre requis'
    return undefined
  },
  required: (value: string) => {
    if (!value?.trim()) return 'Ce champ est requis'
    return undefined
  }
}
