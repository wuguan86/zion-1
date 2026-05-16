export function formatDateTime(value: string | null | undefined): string {
  const text = String(value ?? '').trim()
  if (!text) return '-'

  const normalizedText = text.replace('T', ' ')
  const dateTimeMatch = normalizedText.match(/^(\d{4}-\d{2}-\d{2})(?:\s+(\d{2}:\d{2}:\d{2}))?/)

  if (!dateTimeMatch) {
    return text
  }

  const [, datePart, timePart] = dateTimeMatch
  return timePart ? `${datePart} ${timePart}` : datePart
}
