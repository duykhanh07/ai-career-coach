import React, { useEffect, useState } from "react";
import { BarLoader } from "react-spinners";
import { apiClient } from "@/lib/api";

// Import các component con (Lưu ý: folder _components đổi thành components)
import StatsCards from "./components/StatsCards";
import PerformanceChart from "./components/PerformanceChart"; // Sửa lỗi chính tả tên file gốc performace -> Performance
import QuizList from "./components/QuizList";

const InterviewPage = () => {
  const [assessments, setAssessments] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAssessments = async () => {
      try {
        // Gọi API Java: GET /interview/history
        // API này trả về List<AssessmentEntity>
        const data = await apiClient("/interview/history");
        setAssessments(data || []);
      } catch (error) {
        console.error("Failed to load assessments:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchAssessments();
  }, []);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-[50vh]">
        <BarLoader width={"100%"} color="gray" />
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-5">
        <h1 className="text-6xl font-bold gradient-title">
          Interview Preparation
        </h1>
      </div>
      <div className="space-y-6">
        {/* Truyền dữ liệu xuống các component con */}
        <StatsCards assessments={assessments} />
        <PerformanceChart assessments={assessments} />
        <QuizList assessments={assessments} />
      </div>
    </div>
  );
};

export default InterviewPage;