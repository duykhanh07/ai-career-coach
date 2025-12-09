import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { BarLoader } from "react-spinners";
import DashboardView from "./components/DashboardView"; // Đổi đường dẫn cho đúng
import { apiClient } from "@/lib/api"; // Hàm gọi API tự viết

const DashboardPage = () => {
  const navigate = useNavigate();
  const [insights, setInsights] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        // 1. Kiểm tra Onboarding trước
        // Gọi API: GET /onboarding
        const onboardingData = await apiClient("/onboarding");

        if (!onboardingData.isOnboarded) {
          // Nếu chưa update profile -> Đá sang trang onboarding
          navigate("/onboarding");
          return;
        }

        // 2. Nếu đã Onboard -> Lấy Insights
        // Gọi API: GET /industry-insights
        const insightsData = await apiClient("/industry-insights");
        setInsights(insightsData);

      } catch (err) {
        console.error("Dashboard Load Error:", err);
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [navigate]);

  // Hiển thị Loading khi đang gọi API
  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <BarLoader width={"100%"} color="gray" />
      </div>
    );
  }

  // Hiển thị Lỗi nếu có
  if (error) {
    return (
      <div className="text-red-500 text-center mt-10">
        Error loading dashboard: {error}
      </div>
    );
  }

  return (
    <div className="container mx-auto">
      {/* Truyền dữ liệu insights vào View */}
      {insights && <DashboardView insights={insights} />}
    </div>
  );
};

export default DashboardPage;