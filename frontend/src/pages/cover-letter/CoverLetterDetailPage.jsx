import React, { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom"; // useParams để lấy ID
import { ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { BarLoader } from "react-spinners";
import CoverLetterPreview from "./components/CoverLetterPreview"; // Sửa đường dẫn import
import { apiClient } from "@/lib/api";

const CoverLetterDetailPage = () => {
  const { id } = useParams(); // Lấy ID từ URL: /cover-letter/:id
  const [coverLetter, setCoverLetter] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchCoverLetter = async () => {
      try {
        // Gọi API Java: GET /cover-letters/{id} (Lambda thứ 4)
        const data = await apiClient(`/cover-letters/${id}`);
        setCoverLetter(data);
      } catch (error) {
        console.error("Failed to load cover letter:", error);
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      fetchCoverLetter();
    }
  }, [id]);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <BarLoader width={"100%"} color="gray" />
      </div>
    );
  }

  if (!coverLetter) {
    return <div className="text-center mt-10">Cover Letter not found</div>;
  }

  return (
    <div className="container mx-auto py-6">
      <div className="flex flex-col space-y-2">
        {/* Link quay lại trang danh sách */}
        <Link to="/cover-letter">
          <Button variant="link" className="gap-2 pl-0">
            <ArrowLeft className="h-4 w-4" />
            Back to Cover Letters
          </Button>
        </Link>

        <h1 className="text-6xl font-bold gradient-title mb-6">
          {coverLetter?.jobTitle} at {coverLetter?.companyName}
        </h1>
      </div>

      <CoverLetterPreview content={coverLetter?.content} />
    </div>
  );
};

export default CoverLetterDetailPage;