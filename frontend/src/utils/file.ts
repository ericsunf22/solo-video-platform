export function getFileExtension(filename: string): string {
  const ext = filename.split('.').pop()
  return ext?.toLowerCase() || ''
}

export function isVideoFile(filename: string): boolean {
  const videoExtensions = ['mp4', 'avi', 'mkv', 'mov', 'flv', 'wmv', 'webm', 'm4v']
  return videoExtensions.includes(getFileExtension(filename))
}

export function getFileNameWithoutExtension(filename: string): string {
  const lastDot = filename.lastIndexOf('.')
  if (lastDot === -1) return filename
  return filename.slice(0, lastDot)
}
