import { useNavigate } from "react-router-dom"; // Sửa router
import { format } from "date-fns";
import { Eye, Trash2 } from "lucide-react";
import { toast } from "sonner";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { apiClient } from "@/lib/api"; // Import API Client

// Thêm prop onDeleteSuccess để cập nhật lại danh sách sau khi xóa
export default function CoverLetterList({ coverLetters, onDeleteSuccess }) {
  const navigate = useNavigate();

  const handleDelete = async (id) => {
    try {
      // Gọi API Java: DELETE /cover-letters/{id}
      await apiClient(`/cover-letters/${id}`, {
        method: "DELETE",
      });

      toast.success("Cover letter deleted successfully!");

      // Gọi callback để cha load lại danh sách
      if (onDeleteSuccess) {
        onDeleteSuccess();
      }
    } catch (error) {
      console.error("Delete Error:", error);
      toast.error(error.message || "Failed to delete cover letter");
    }
  };

  if (!coverLetters?.length) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>No Cover Letters Yet</CardTitle>
          <CardDescription>
            Create your first cover letter to get started
          </CardDescription>
        </CardHeader>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      {coverLetters.map((letter) => {
        // Lấy ID từ PK/SK của DynamoDB (Format: LETTER#uuid -> lấy uuid)
        // Hoặc nếu Java đã trả về ID sạch thì dùng luôn
        const letterId = letter.sk ? letter.sk.replace("LETTER#", "") : letter.id;

        return (
          <Card key={letterId} className="group relative">
            <CardHeader>
              <div className="flex items-start justify-between">
                <div>
                  <CardTitle className="text-xl gradient-title">
                    {letter.jobTitle} at {letter.companyName}
                  </CardTitle>
                  <CardDescription>
                    Created {format(new Date(letter.createdAt), "PPP")}
                  </CardDescription>
                </div>
                <div className="flex space-x-2">
                  {/* Nút Xem (Mắt) */}
                  {/* Sửa onClick để không bị xung đột với AlertDialog */}
                  <Button
                    variant="outline"
                    size="icon"
                    onClick={() => navigate(`/cover-letter/${letterId}`)}
                  >
                    <Eye className="h-4 w-4" />
                  </Button>

                  {/* Nút Xóa (Thùng rác) */}
                  <AlertDialog>
                    <AlertDialogTrigger asChild>
                      <Button variant="outline" size="icon">
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </AlertDialogTrigger>
                    <AlertDialogContent>
                      <AlertDialogHeader>
                        <AlertDialogTitle>Delete Cover Letter?</AlertDialogTitle>
                        <AlertDialogDescription>
                          This action cannot be undone. This will permanently
                          delete your cover letter for {letter.jobTitle} at{" "}
                          {letter.companyName}.
                        </AlertDialogDescription>
                      </AlertDialogHeader>
                      <AlertDialogFooter>
                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                        <AlertDialogAction
                          onClick={() => handleDelete(letterId)}
                          className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                        >
                          Delete
                        </AlertDialogAction>
                      </AlertDialogFooter>
                    </AlertDialogContent>
                  </AlertDialog>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <div className="text-muted-foreground text-sm line-clamp-3">
                {letter.jobDescription}
              </div>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}