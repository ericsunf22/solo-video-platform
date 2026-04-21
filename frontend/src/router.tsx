import { Routes, Route } from 'react-router-dom'
import MainLayout from '@/components/layout/MainLayout'
import Home from '@/pages/Home'
import Player from '@/pages/Player'
import Favorites from '@/pages/Favorites'
import History from '@/pages/History'
import Tags from '@/pages/Tags'
import Settings from '@/pages/Settings'

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<MainLayout />}>
        <Route index element={<Home />} />
        <Route path="player/:id" element={<Player />} />
        <Route path="favorites" element={<Favorites />} />
        <Route path="history" element={<History />} />
        <Route path="tags" element={<Tags />} />
        <Route path="settings" element={<Settings />} />
      </Route>
    </Routes>
  )
}
