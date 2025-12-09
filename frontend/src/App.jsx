import React from "react";
import { BrowserRouter, Routes, Route, Outlet } from "react-router-dom";
import { Authenticator, useAuthenticator } from "@aws-amplify/ui-react";
import "@aws-amplify/ui-react/styles.css";

// --- PROVIDERS & UI ---
import { ThemeProvider } from "@/components/theme-provider";
import { Toaster } from "@/components/ui/sonner";

// --- LAYOUTS ---
import MainLayout from "@/layouts/MainLayout";
import DashboardLayout from "@/pages/dashboard/DashboardLayout";
import InterviewLayout from "@/pages/interview/InterviewLayout";

// --- PAGES: LANDING & ONBOARDING ---
import LandingPage from "@/pages/LandingPage";
import OnboardingPage from "@/pages/onboarding/OnboardingPage";

// --- PAGES: DASHBOARD ---
import DashboardPage from "@/pages/dashboard/DashboardPage";

// --- PAGES: RESUME ---
import ResumePage from "@/pages/resume/ResumePage";

// --- PAGES: INTERVIEW ---
import InterviewPage from "@/pages/interview/InterviewPage";
import MockInterviewPage from "@/pages/interview/MockInterviewPage";

// --- PAGES: COVER LETTER ---
import CoverLetterPage from "@/pages/cover-letter/CoverLetterPage";
import NewCoverLetterPage from "@/pages/cover-letter/NewCoverLetterPage";
import CoverLetterDetailPage from "@/pages/cover-letter/CoverLetterDetailPage";

import NotFoundPage from "@/pages/NotFoundPage";
/**
 * Component bảo vệ: Nếu chưa đăng nhập thì hiện Form Login.
 * Nếu đã đăng nhập thì hiện nội dung con (Outlet).
 */
const RequireAuth = () => {
  const { route } = useAuthenticator((context) => [context.route]);

  if (route !== "authenticated") {
    // Chưa đăng nhập -> Hiện Form Amplify
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <Authenticator />
      </div>
    );
  }
  // Đã đăng nhập -> Cho phép truy cập
  return <Outlet />;
};

/**
 * Wrapper cho MainLayout để truyền props user/signOut từ Amplify
 */
const MainLayoutWrapper = () => {
  const { user, signOut } = useAuthenticator((context) => [context.user]);
  return <MainLayout user={user} signOut={signOut} />;
};

export default function App() {
  return (
    <ThemeProvider defaultTheme="light" storageKey="vite-ui-theme">
      {/* Provider này giúp quản lý Auth state toàn cục */}
      <Authenticator.Provider>
        <BrowserRouter>
          {/* Toaster thông báo đặt ở ngoài cùng */}
          <Toaster position="bottom-right" richColors />

          <Routes>
            {/* --- ROUTE GỐC: MAIN LAYOUT (Có Header/Footer) --- */}
            <Route element={<MainLayoutWrapper />}>

              {/* 1. Public Route: Trang chủ */}
              <Route path="/" element={<LandingPage />} />

              {/* 2. Protected Routes: Các trang cần đăng nhập */}
              <Route element={<RequireAuth />}>

                {/* Onboarding */}
                <Route path="/onboarding" element={<OnboardingPage />} />

                {/* Dashboard (Có Layout con riêng) */}
                <Route element={<DashboardLayout />}>
                  <Route path="/dashboard" element={<DashboardPage />} />
                </Route>

                {/* Resume Module */}
                <Route path="/resume" element={<ResumePage />} />

                {/* Interview Module (Có Layout con riêng) */}
                <Route element={<InterviewLayout />}>
                  <Route path="/interview" element={<InterviewPage />} />
                  <Route path="/interview/mock" element={<MockInterviewPage />} />
                </Route>

                {/* Cover Letter Module */}
                <Route path="/cover-letter" element={<CoverLetterPage />} />
                <Route path="/cover-letter/new" element={<NewCoverLetterPage />} />
                <Route path="/cover-letter/:id" element={<CoverLetterDetailPage />} />

              </Route>
            </Route>
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </BrowserRouter>
      </Authenticator.Provider>
    </ThemeProvider>
  );
}