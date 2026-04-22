import { create } from 'zustand'

export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface Toast {
  id: string
  message: string
  type: ToastType
  duration?: number
}

interface ToastState {
  toasts: Toast[]

  addToast: (message: string, type?: ToastType, duration?: number) => string
  removeToast: (id: string) => void
  clearAll: () => void

  success: (message: string, duration?: number) => void
  error: (message: string, duration?: number) => void
  warning: (message: string, duration?: number) => void
  info: (message: string, duration?: number) => void
}

const generateId = () => `toast-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`

export const useToastStore = create<ToastState>((set, get) => ({
  toasts: [],

  addToast: (message, type = 'info', duration = 3000) => {
    const id = generateId()
    set((state) => ({
      toasts: [...state.toasts, { id, message, type, duration }],
    }))
    return id
  },

  removeToast: (id) => {
    set((state) => ({
      toasts: state.toasts.filter((toast) => toast.id !== id),
    }))
  },

  clearAll: () => {
    set({ toasts: [] })
  },

  success: (message, duration = 3000) => {
    get().addToast(message, 'success', duration)
  },

  error: (message, duration = 5000) => {
    get().addToast(message, 'error', duration)
  },

  warning: (message, duration = 4000) => {
    get().addToast(message, 'warning', duration)
  },

  info: (message, duration = 3000) => {
    get().addToast(message, 'info', duration)
  },
}))
