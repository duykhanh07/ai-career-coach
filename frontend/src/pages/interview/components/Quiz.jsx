import { useState, useEffect } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Label } from "@/components/ui/label";
import QuizResult from "./QuizResult";
import { BarLoader } from "react-spinners";
import { apiClient } from "@/lib/api";

export default function Quiz() {
  const [currentQuestion, setCurrentQuestion] = useState(0);
  const [answers, setAnswers] = useState([]);
  const [showExplanation, setShowExplanation] = useState(false);
  const [quizData, setQuizData] = useState(null);
  const [resultData, setResultData] = useState(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  // --- HÀM 1: GỌI API TẠO CÂU HỎI ---
  const generateQuizFn = async () => {
    setIsGenerating(true);
    setQuizData(null);
    setResultData(null);
    setAnswers([]);
    setCurrentQuestion(0);

    try {
      const data = await apiClient("/interview/generate", {
        method: "POST",
        body: JSON.stringify({})
      });

      if (data.questions && data.questions.length > 0) {
        setQuizData(data.questions);
        setAnswers(new Array(data.questions.length).fill(null));
      } else {
        toast.error("Failed to generate quiz questions");
      }
    } catch (error) {
      console.error("Generate Quiz Error:", error);
      toast.error(error.message || "Failed to generate quiz");
    } finally {
      setIsGenerating(false);
    }
  };

  // --- TÍNH ĐIỂM (SỬA LOGIC SO SÁNH TẠI ĐÂY) ---
  const calculateScore = () => {
    let correct = 0;
    answers.forEach((answer, index) => {
      // Kiểm tra nếu người dùng đã chọn đáp án
      if (answer) {
        // Cắt lấy ký tự đầu tiên: "B. Text..." -> "B"
        const userOption = answer.split(".")[0].trim();
        // So sánh với đáp án đúng từ Backend (ví dụ "B")
        if (userOption === quizData[index].correctAnswer) {
          correct++;
        }
      }
    });
    return (correct / quizData.length) * 100;
  };

  // --- HÀM 2: GỌI API NỘP BÀI ---
  const finishQuiz = async () => {
    const score = calculateScore(); // Tính điểm chuẩn trước khi gửi
    setIsSaving(true);

    try {
      const payload = {
        questions: quizData,
        userAnswers: answers,
        score: score
      };

      const result = await apiClient("/interview/save", {
        method: "POST",
        body: JSON.stringify(payload)
      });

      setResultData(result);
      toast.success("Quiz completed!");
    } catch (error) {
      console.error("Save Quiz Error:", error);
      toast.error(error.message || "Failed to save quiz results");
    } finally {
      setIsSaving(false);
    }
  };

  const handleAnswer = (answer) => {
    const newAnswers = [...answers];
    newAnswers[currentQuestion] = answer;
    setAnswers(newAnswers);
  };

  const handleNext = () => {
    if (currentQuestion < quizData.length - 1) {
      setCurrentQuestion(currentQuestion + 1);
      setShowExplanation(false);
    } else {
      finishQuiz();
    }
  };

  const startNewQuiz = () => {
    setCurrentQuestion(0);
    setAnswers([]);
    setShowExplanation(false);
    setResultData(null);
    generateQuizFn();
  };

  if (isGenerating) {
    return <BarLoader className="mt-4" width={"100%"} color="gray" />;
  }

  // Hiển thị kết quả (Truyền thêm quizData và answers để hiển thị chi tiết nếu cần)
  if (resultData) {
    return (
      <div className="mx-2">
        <QuizResult
            result={resultData}
            onStartNew={startNewQuiz}
        />
      </div>
    );
  }

  if (!quizData) {
    return (
      <Card className="mx-2">
        <CardHeader>
          <CardTitle>Ready to test your knowledge?</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground">
            This quiz contains 10 questions specific to your industry and
            skills. Take your time and choose the best answer for each question.
          </p>
        </CardContent>
        <CardFooter>
          <Button onClick={generateQuizFn} className="w-full">
            Start Quiz
          </Button>
        </CardFooter>
      </Card>
    );
  }

  const question = quizData[currentQuestion];

  return (
    <Card className="mx-2">
      <CardHeader>
        <CardTitle>
          Question {currentQuestion + 1} of {quizData.length}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <p className="text-lg font-medium">{question.question}</p>
        <RadioGroup
          onValueChange={handleAnswer}
          value={answers[currentQuestion]}
          className="space-y-2"
        >
          {question.options.map((option, index) => (
            <div key={index} className="flex items-center space-x-2">
              <RadioGroupItem value={option} id={`option-${index}`} />
              <Label htmlFor={`option-${index}`}>{option}</Label>
            </div>
          ))}
        </RadioGroup>

        {showExplanation && (
          <div className="mt-4 p-4 bg-muted rounded-lg">
            <p className="font-medium">Explanation:</p>
            <p className="text-muted-foreground">{question.explanation}</p>
          </div>
        )}
      </CardContent>
      <CardFooter className="flex justify-between">
        {!showExplanation && (
          <Button
            onClick={() => setShowExplanation(true)}
            variant="outline"
            disabled={!answers[currentQuestion]}
          >
            Show Explanation
          </Button>
        )}
        <Button
          onClick={handleNext}
          disabled={!answers[currentQuestion] || isSaving}
          className="ml-auto"
        >
          {isSaving && (
            <BarLoader className="mt-4" width={"100%"} color="gray" />
          )}
          {currentQuestion < quizData.length - 1
            ? "Next Question"
            : "Finish Quiz"}
        </Button>
      </CardFooter>
    </Card>
  );
}