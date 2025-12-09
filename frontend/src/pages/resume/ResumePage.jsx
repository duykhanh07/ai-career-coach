import React, { useEffect, useState } from "react";
import { BarLoader } from "react-spinners";
import ResumeBuilder from "./components/ResumeBuilder"; // Nhớ sửa đường dẫn import
import { apiClient } from "@/lib/api";

const ResumePage = () => {
  const [resume, setResume] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchResume = async () => {
      try {
        // Gọi API Java: GET /resume
        // Backend sẽ trả về object ResumeEntity hoặc null
        const data = await apiClient("/resume");
        setResume(data);
      } catch (error) {
        console.error("Failed to load resume:", error);
        // Có thể hiện thông báo lỗi nếu cần
      } finally {
        setLoading(false);
      }
    };

    fetchResume();
  }, []);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <BarLoader width={"100%"} color="gray" />
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6">
      <ResumeBuilder initialContent={resume?.content} />
    </div>
  );
};

export default ResumePage;