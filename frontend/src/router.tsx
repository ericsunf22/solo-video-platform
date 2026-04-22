import { lazy, Suspense } from 'react'
import { Routes, Route } from 'react-router-dom'
import MainLayout from '@/components/layout/MainLayout'

const Home = lazy(() => import('@/pages/Home'))
const Player = lazy(() => import('@/pages/Player'))
const Favorites = lazy(() => import('@/pages/Favorites'))
const History = lazy(() => import('@/pages/History'))
const Tags = lazy(() => import('@/pages/Tags'))
const Settings = lazy(() => import('@/pages/Settings'))

function PageSkeleton() {
  return (
    <div className="animate-pulse">
      <div className="h-8 bg-gray-200 rounded w-1/4 mb-6"></div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="aspect-video bg-gray-200 rounded-lg"></div>
        ))}
      </div>
    </div>
  )
}

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<MainLayout />}>
        <Route index element={
          <Suspense fallback={<PageSkeleton />}>
            <Home />
          </Suspense>
        } />
        <Route path="player/:id" element={
          <Suspense fallback={<PageSkeleton />}>
            <Player />
          </Suspense>
        } />
        <Route path="favorites" element={
          <Suspense fallback={<PageSkeleton />}>
            <Favorites />
          </Suspense>
        } />
        <Route path="history" element={
          <Suspense fallback={<PageSkeleton />}>
            <History />
          </Suspense>
        } />
        <Route path="tags" element={
          <Suspense fallback={<PageSkeleton />}>
            <Tags />
          </Suspense>
        } />
        <Route path="settings" element={
          <Suspense fallback={<PageSkeleton />}>
            <Settings />
          </Suspense>
        } />
      </Route>
    </Routes>
  )
}
