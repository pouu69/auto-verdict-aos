// Aligned to DESIGN.md (Apple-style single blue accent, minimal chrome).
export const color = {
  background: '#f5f5f7', // parchment
  surface: '#ffffff',
  primary: '#0066CC',
  primaryFocus: '#0071E3',
  textPrimary: '#1d1d1f', // ink
  textSecondary: '#6e6e73',
  danger: '#E53935',
  dangerBg: '#FFEBEE',
  warning: '#F57C00',
  warningBg: '#FFF3E0',
  success: '#2E7D32',
  successBg: '#E8F5E9',
  unknownBg: '#F1F1F4',
  border: '#f0f0f0', // divider soft
  hairline: '#e0e0e0', // card outline
} as const;

export const radius = {
  card: '16px',
  button: '12px',
  pill: '9999px',
  badge: '6px',
} as const;

export const shadow = {
  card: '0 1px 4px rgba(0,0,0,0.04)',
} as const;
