import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";
import { useNavigate } from "react-router-dom"; // Sửa router
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { coverLetterSchema } from "@/lib/schema"; // Đảm bảo đường dẫn đúng
import { apiClient } from "@/lib/api"; // Import API Client

export default function CoverLetterGenerator() {
  const navigate = useNavigate();
  const [generating, setGenerating] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm({
    resolver: zodResolver(coverLetterSchema),
  });

  const onSubmit = async (data) => {
    setGenerating(true);
    try {
      // Gọi API: POST /cover-letters (Tạo mới bằng AI)
      // Backend (Java Lambda 4) sẽ nhận Job Info, lấy Profile User, gọi Bedrock, lưu DB, trả về Entity

      const generatedLetter = await apiClient("/cover-letters", {
        method: "POST",
        body: JSON.stringify(data),
      });

      if (generatedLetter && (generatedLetter.sk || generatedLetter.SK)) {
        toast.success("Cover letter generated successfully!");

        // Điều hướng sang trang chi tiết (Sử dụng ID từ SK: LETTER#uuid -> lấy uuid)
        const letterId = generatedLetter.sk.replace("LETTER#", "");
        navigate(`/cover-letter/${letterId}`);

        reset();
      } else {
        throw new Error("Invalid response from server");
      }

    } catch (error) {
      console.error("Generate Error:", error);
      toast.error(error.message || "Failed to generate cover letter");
    } finally {
      setGenerating(false);
    }
  };

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Job Details</CardTitle>
          <CardDescription>
            Provide information about the position you're applying for
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="companyName">Company Name</Label>
                <Input
                  id="companyName"
                  placeholder="Enter company name"
                  {...register("companyName")}
                />
                {errors.companyName && (
                  <p className="text-sm text-red-500">
                    {errors.companyName.message}
                  </p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="jobTitle">Job Title</Label>
                <Input
                  id="jobTitle"
                  placeholder="Enter job title"
                  {...register("jobTitle")}
                />
                {errors.jobTitle && (
                  <p className="text-sm text-red-500">
                    {errors.jobTitle.message}
                  </p>
                )}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="jobDescription">Job Description</Label>
              <Textarea
                id="jobDescription"
                placeholder="Paste the job description here"
                className="h-32"
                {...register("jobDescription")}
              />
              {errors.jobDescription && (
                <p className="text-sm text-red-500">
                  {errors.jobDescription.message}
                </p>
              )}
            </div>

            <div className="flex justify-end">
              <Button type="submit" disabled={generating}>
                {generating ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Generating...
                  </>
                ) : (
                  "Generate Cover Letter"
                )}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}