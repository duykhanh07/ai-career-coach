import React from "react";
import { Link } from "react-router-dom"; // Sửa import
import { ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import CoverLetterGenerator from "./components/CoverLetterGenerator"; // Sửa đường dẫn import

const NewCoverLetterPage = () => {
  return (
    <div className="container mx-auto py-6">
      <div className="flex flex-col space-y-2">
        <Link to="/cover-letter"> {/* Sửa href thành to */}
          <Button variant="link" className="gap-2 pl-0">
            <ArrowLeft className="h-4 w-4" />
            Back to Cover Letters
          </Button>
        </Link>

        <div className="pb-6">
          <h1 className="text-6xl font-bold gradient-title">
            Create Cover Letter
          </h1>
          <p className="text-muted-foreground">
            Generate a tailored cover letter for your job application
          </p>
        </div>
      </div>

      <CoverLetterGenerator />
    </div>
  );
};

export default NewCoverLetterPage;