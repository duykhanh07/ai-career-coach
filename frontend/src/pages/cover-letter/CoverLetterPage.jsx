import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom"; // Sửa import Link
import { Plus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { BarLoader } from "react-spinners";
import CoverLetterList from "./components/CoverLetterList"; // Sửa đường dẫn import
import { apiClient } from "@/lib/api";

const CoverLetterPage = () => {
  const [coverLetters, setCoverLetters] = useState([]);
  const [loading, setLoading] = useState(true);

  // Hàm gọi API lấy danh sách
  const fetchCoverLetters = async () => {
    try {
      // Gọi API Java: GET /cover-letters (Lambda thứ 4)
      const data = await apiClient("/cover-letters");
      setCoverLetters(data || []);
    } catch (error) {
      console.error("Failed to load cover letters:", error);
    } finally {
      setLoading(false);
    }
  };

  // Gọi API khi trang vừa tải
  useEffect(() => {
    fetchCoverLetters();
  }, []);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <BarLoader width={"100%"} color="gray" />
      </div>
    );
  }

  return (
    <div>
      <div className="flex flex-col md:flex-row gap-2 items-center justify-between mb-5">
        <h1 className="text-6xl font-bold gradient-title">My Cover Letters</h1>

        {/* Nút tạo mới dẫn sang trang /cover-letter/new */}
        <Link to="/cover-letter/new">
          <Button>
            <Plus className="h-4 w-4 mr-2" />
            Create New
          </Button>
        </Link>
      </div>

      {/* Truyền hàm fetchCoverLetters xuống để component con gọi lại khi xóa xong */}
      <CoverLetterList
        coverLetters={coverLetters}
        onDeleteSuccess={fetchCoverLetters}
      />
    </div>
  );
};

export default CoverLetterPage;