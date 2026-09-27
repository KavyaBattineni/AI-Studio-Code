import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ToastProvider } from './context/ToastContext'
import ProtectedRoute from './components/routing/ProtectedRoute'
import AdminRoute from './components/routing/AdminRoute'
import AppLayout from './components/layout/AppLayout'
import AdminLayout from './components/layout/AdminLayout'

import Login from './pages/auth/Login'
import Register from './pages/auth/Register'
import VerifyOtp from './pages/auth/VerifyOtp'
import ForgotPassword from './pages/auth/ForgotPassword'

import Dashboard from './pages/Dashboard'
import Generate from './pages/Generate'
import MyVideos from './pages/MyVideos'
import Wallet from './pages/Wallet'
import BuyDiamonds from './pages/BuyDiamonds'
import DailyReward from './pages/DailyReward'
import Transactions from './pages/Transactions'
import Profile from './pages/Profile'
import NotFound from './pages/NotFound'

import AdminDashboard from './pages/admin/AdminDashboard'
import AdminUsers from './pages/admin/AdminUsers'
import AdminPayments from './pages/admin/AdminPayments'
import AdminVideos from './pages/admin/AdminVideos'
import AdminModeration from './pages/admin/AdminModeration'
import AdminTransactions from './pages/admin/AdminTransactions'
import AdminRevenue from './pages/admin/AdminRevenue'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />

            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/verify-otp" element={<VerifyOtp />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />

            <Route element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/generate" element={<Generate />} />
                <Route path="/my-videos" element={<MyVideos />} />
                <Route path="/wallet" element={<Wallet />} />
                <Route path="/buy-diamonds" element={<BuyDiamonds />} />
                <Route path="/daily-reward" element={<DailyReward />} />
                <Route path="/transactions" element={<Transactions />} />
                <Route path="/profile" element={<Profile />} />
              </Route>

              <Route element={<AdminRoute />}>
                <Route element={<AdminLayout />}>
                  <Route path="/admin" element={<AdminDashboard />} />
                  <Route path="/admin/users" element={<AdminUsers />} />
                  <Route path="/admin/payments" element={<AdminPayments />} />
                  <Route path="/admin/videos" element={<AdminVideos />} />
                  <Route path="/admin/moderation" element={<AdminModeration />} />
                  <Route path="/admin/transactions" element={<AdminTransactions />} />
                  <Route path="/admin/revenue" element={<AdminRevenue />} />
                </Route>
              </Route>
            </Route>

            <Route path="*" element={<NotFound />} />
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
