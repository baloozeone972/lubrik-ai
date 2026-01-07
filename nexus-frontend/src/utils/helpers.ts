export const cn = (...classes: (string | boolean | undefined)[]) => {
  return classes.filter(Boolean).join(' ')
}

export const formatDate = (date: string | Date) => {
  return new Date(date).toLocaleDateString('fr-FR')
}

export const truncate = (str: string, length: number) => {
  if (str.length <= length) return str
  return str.substring(0, length) + '...'
}
