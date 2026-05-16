const FILE_API_PREFIX = '/api/files'

export function normalizeFileUrl(url?: string | null): string {
  if (!url) {
    return ''
  }

  const trimmedUrl = url.trim()
  if (!trimmedUrl) {
    return ''
  }

  if (/^(https?:)?\/\//i.test(trimmedUrl) || /^(data|blob):/i.test(trimmedUrl)) {
    return trimmedUrl
  }

  if (trimmedUrl.startsWith(`${FILE_API_PREFIX}/`) || trimmedUrl.startsWith('/api/sys/file/')) {
    return trimmedUrl
  }

  if (trimmedUrl.startsWith('/')) {
    return `${FILE_API_PREFIX}${trimmedUrl}`
  }

  return `${FILE_API_PREFIX}/${trimmedUrl}`
}
