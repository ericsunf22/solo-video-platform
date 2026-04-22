import { create } from 'zustand'
import { Video } from '@/types'

interface PlayerState {
  isPlaying: boolean
  currentTime: number
  duration: number
  volume: number
  isMuted: boolean
  playbackRate: number
  isFullscreen: boolean
  currentVideo: Video | null

  setIsPlaying: (playing: boolean) => void
  setCurrentTime: (time: number) => void
  setDuration: (duration: number) => void
  setVolume: (volume: number) => void
  setIsMuted: (muted: boolean) => void
  setPlaybackRate: (rate: number) => void
  setIsFullscreen: (fullscreen: boolean) => void
  setCurrentVideo: (video: Video | null) => void
  reset: () => void
}

export const usePlayerStore = create<PlayerState>()((set) => ({
  isPlaying: false,
  currentTime: 0,
  duration: 0,
  volume: 1,
  isMuted: false,
  playbackRate: 1,
  isFullscreen: false,
  currentVideo: null,

  setIsPlaying: (playing) => set({ isPlaying: playing }),
  setCurrentTime: (time) => set({ currentTime: time }),
  setDuration: (duration) => set({ duration }),
  setVolume: (volume) => set({ volume }),
  setIsMuted: (muted) => set({ isMuted: muted }),
  setPlaybackRate: (rate) => set({ playbackRate: rate }),
  setIsFullscreen: (fullscreen) => set({ isFullscreen: fullscreen }),
  setCurrentVideo: (video) => set({ currentVideo: video }),
  reset: () =>
    set({
      isPlaying: false,
      currentTime: 0,
      duration: 0,
      volume: 1,
      isMuted: false,
      playbackRate: 1,
      isFullscreen: false,
    }),
}))
