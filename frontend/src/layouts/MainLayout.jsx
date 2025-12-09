import React from "react";
import { Outlet } from "react-router-dom";
import Header from "@/components/Header";

const MainLayout = ({ user, signOut }) => {
  return (
    <div className="flex flex-col min-h-screen">
      {/* Header */}
      <Header user={user} signOut={signOut} />

      {/* Main Content */}
      <main className="container mx-auto mt-24 mb-20 px-4 flex-1">
        <Outlet />
      </main>

      {/* --- THÊM FOOTER VÀO ĐÂY (Lấy từ layout.js cũ) --- */}
      <footer className="bg-muted/50 py-12 mt-auto">
        <div className="container mx-auto px-4 text-center text-gray-500">
          <p>Made with 💗 by Duy Khanh</p>
        </div>
      </footer>

    </div>
  );
};

export default MainLayout;