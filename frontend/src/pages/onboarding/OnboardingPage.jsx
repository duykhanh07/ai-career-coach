import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { BarLoader } from "react-spinners";
import OnboardingForm from "./components/OnboardingForm"; // Nhớ sửa đường dẫn import
import { industries } from "@/data/industries";
import { apiClient } from "@/lib/api";

const OnboardingPage = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const checkStatus = async () => {
      try {
        // Gọi API kiểm tra trạng thái
        // GET /onboarding trả về { isOnboarded: true/false }
        const data = await apiClient("/onboarding");

        if (data.isOnboarded) {
          // Nếu đã onboard rồi thì đá về Dashboard ngay
          navigate("/dashboard");
        }
      } catch (error) {
        console.error("Check onboarding status failed:", error);
        // Nếu lỗi mạng, có thể cho phép user thử điền form hoặc hiện lỗi
      } finally {
        // Dù kết quả thế nào cũng tắt loading để hiện Form (nếu chưa onboard)
        setLoading(false);
      }
    };

    checkStatus();
  }, [navigate]);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <BarLoader width={"100%"} color="gray" />
      </div>
    );
  }

  return (
    <main>
      <OnboardingForm industries={industries} />
    </main>
  );
};

export default OnboardingPage;