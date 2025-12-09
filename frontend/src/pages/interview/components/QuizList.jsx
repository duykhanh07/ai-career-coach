import { useState } from "react";
import { format } from "date-fns";
import { useNavigate } from "react-router-dom"; // Sửa router
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import QuizResult from "./QuizResult"; // Đảm bảo component này tồn tại

export default function QuizList({ assessments }) {
  const navigate = useNavigate();
  const [selectedQuiz, setSelectedQuiz] = useState(null);

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="gradient-title text-3xl md:text-4xl">
                Recent Quizzes
              </CardTitle>
              <CardDescription>
                Review your past quiz performance
              </CardDescription>
            </div>
            {/* Sửa đường dẫn điều hướng */}
            <Button onClick={() => navigate("/interview/mock")}>
              Start New Quiz
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {/* Kiểm tra mảng rỗng để tránh lỗi */}
            {assessments?.length === 0 && (
                <p className="text-center text-muted-foreground py-4">
                    No quizzes taken yet. Start one now!
                </p>
            )}

            {assessments?.map((assessment, i) => (
              <Card
                key={assessment.pk || i} // Sử dụng ID từ DynamoDB (pk/sk) hoặc index
                className="cursor-pointer hover:bg-muted/50 transition-colors"
                onClick={() => setSelectedQuiz(assessment)}
              >
                <CardHeader>
                  <CardTitle className="gradient-title text-2xl">
                    Quiz {i + 1}
                  </CardTitle>
                  <CardDescription className="flex justify-between w-full">
                    <div>Score: {assessment.quizScore.toFixed(1)}%</div>
                    <div>
                      {format(
                        new Date(assessment.createdAt),
                        "MMMM dd, yyyy HH:mm"
                      )}
                    </div>
                  </CardDescription>
                </CardHeader>
                {assessment.improvementTip && (
                  <CardContent>
                    <p className="text-sm text-muted-foreground">
                      {assessment.improvementTip}
                    </p>
                  </CardContent>
                )}
              </Card>
            ))}
          </div>
        </CardContent>
      </Card>

      <Dialog open={!!selectedQuiz} onOpenChange={() => setSelectedQuiz(null)}>
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Quiz Details</DialogTitle> {/* Thêm title cho đúng chuẩn */}
          </DialogHeader>
          <QuizResult
            result={selectedQuiz}
            hideStartNew
            onStartNew={() => navigate("/interview/mock")}
          />
        </DialogContent>
      </Dialog>
    </>
  );
}