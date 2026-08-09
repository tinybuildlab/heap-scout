const BYTE_UNITS = ['B', 'KB', 'MB', 'GB', 'TB'] as const;

export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes === 0) return '0 B';
  const magnitude = Math.abs(bytes);
  const unitIndex = Math.min(Math.floor(Math.log(magnitude) / Math.log(1024)), BYTE_UNITS.length - 1);
  const value = magnitude / 1024 ** unitIndex;
  const sign = bytes < 0 ? '-' : '';
  return `${sign}${value >= 10 || unitIndex === 0 ? value.toFixed(0) : value.toFixed(1)} ${BYTE_UNITS[unitIndex]}`;
}

export function formatCount(value: number): string {
  return new Intl.NumberFormat('en-US').format(value);
}

export function formatDuration(milliseconds: number): string {
  if (milliseconds < 1_000) return `${milliseconds} ms`;
  return `${(milliseconds / 1_000).toFixed(1)} s`;
}
