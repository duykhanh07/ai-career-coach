import { useState, useEffect } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  AlertTriangle,
  Download,
  Edit,
  Loader2,
  Monitor,
  Save,
} from "lucide-react";
import { toast } from "sonner";
import MDEditor from "@uiw/react-md-editor";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";
import { EntryForm } from "./EntryForm";
import { entriesToMarkdown } from "@/lib/helper";
import { resumeSchema } from "@/lib/schema";
import html2pdf from "html2pdf.js";
import { apiClient } from "@/lib/api";

export default function ResumeBuilder({ initialContent }) {
  const [activeTab, setActiveTab] = useState("edit");
  const [previewContent, setPreviewContent] = useState(initialContent || "");
  const [resumeMode, setResumeMode] = useState("preview");
  const [isSaving, setIsSaving] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);

  // Lưu ý: userName nên được lấy từ context hoặc auth provider (Amplify/Cognito)
  const userName = "User Name";

  const {
    control,
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(resumeSchema),
    defaultValues: {
      contactInfo: {
        github: "", // Thay twitter thành github
      },
      summary: "",
      skills: "",
      experience: [],
      education: [],
      projects: [],
    },
  });

  const formValues = watch();

  useEffect(() => {
    if (initialContent) setPreviewContent(initialContent);
  }, [initialContent]);

  useEffect(() => {
    if (activeTab === "edit") {
      const newContent = getCombinedContent();
      setPreviewContent(newContent ? newContent : initialContent);
    }
  }, [formValues, activeTab]);

  const getContactMarkdown = () => {
    const { contactInfo } = formValues;
    const parts = [];
    if (contactInfo?.email) parts.push(`📧 ${contactInfo.email}`);
    if (contactInfo?.mobile) parts.push(`📱 ${contactInfo.mobile}`);
    if (contactInfo?.linkedin)
      parts.push(`💼 [LinkedIn](${contactInfo.linkedin})`);
    if (contactInfo?.github)
      parts.push(`🐙 [GitHub](${contactInfo.github})`); // Đổi icon sang GitHub

    return parts.length > 0
      ? `## <div align="center">${userName}</div>
        \n\n<div align="center">\n\n${parts.join(" | ")}\n\n</div>`
      : "";
  };

  const getCombinedContent = () => {
    const { summary, skills, experience, education, projects } = formValues;
    return [
      getContactMarkdown(),
      summary && `## Professional Summary\n\n${summary}`,
      skills && `## Skills\n\n${skills}`,
      entriesToMarkdown(experience, "Work Experience"),
      entriesToMarkdown(education, "Education"),
      entriesToMarkdown(projects, "Projects"),
    ]
      .filter(Boolean)
      .join("\n\n");
  };

  const generatePDF = async () => {
    setIsGenerating(true);
    try {
      const element = document.getElementById("resume-pdf");
      const opt = {
        margin: [15, 15],
        filename: "resume.pdf",
        image: { type: "jpeg", quality: 0.98 },
        html2canvas: { scale: 2 },
        jsPDF: { unit: "mm", format: "a4", orientation: "portrait" },
      };

      await html2pdf().set(opt).from(element).save();
    } catch (error) {
      console.error("PDF generation error:", error);
      toast.error("Failed to generate PDF");
    } finally {
      setIsGenerating(false);
    }
  };

  const onSubmit = async () => {
    setIsSaving(true);
    try {
      // Gọi API Java thông qua Lib API của bạn
      await apiClient("/resume", {
        method: "POST",
        body: JSON.stringify({
          content: previewContent
        }),
      });

      toast.success("Resume saved successfully!");
    } catch (error) {
      console.error("Save error:", error);
      toast.error("Failed to save resume: " + error.message);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div data-color-mode="light" className="space-y-4">
      <div className="flex flex-col md:flex-row justify-between items-center gap-2">
        <h1 className="font-bold gradient-title text-5xl md:text-6xl">
          Resume Builder
        </h1>
        <div className="space-x-2">
          <Button
            variant="destructive"
            onClick={handleSubmit(onSubmit)}
            disabled={isSaving}
          >
            {isSaving ? (
              <><Loader2 className="mr-2 h-4 w-4 animate-spin" />Saving...</>
            ) : (
              <><Save className="h-4 w-4 mr-2" />Save</>
            )}
          </Button>
          <Button onClick={generatePDF} disabled={isGenerating}>
            {isGenerating ? (
              <><Loader2 className="mr-2 h-4 w-4 animate-spin" />Generating PDF...</>
            ) : (
              <><Download className="h-4 w-4 mr-2" />Download PDF</>
            )}
          </Button>
        </div>
      </div>

      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          <TabsTrigger value="edit">Form</TabsTrigger>
          <TabsTrigger value="preview">Markdown Preview</TabsTrigger>
        </TabsList>

        <TabsContent value="edit">
          <form className="space-y-8">
            {/* Contact Information */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium">Contact Information</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 p-4 border rounded-lg bg-muted/20">
                <div className="space-y-2">
                  <label className="text-sm">Email</label>
                  <Input {...register("contactInfo.email")} type="email" placeholder="your@email.com" />
                </div>
                <div className="space-y-2">
                  <label className="text-sm">Mobile Number</label>
                  <Input {...register("contactInfo.mobile")} placeholder="+1 234 567 890" />
                </div>
                <div className="space-y-2">
                  <label className="text-sm">LinkedIn URL</label>
                  <Input {...register("contactInfo.linkedin")} placeholder="https://linkedin.com/in/username" />
                </div>
                <div className="space-y-2">
                  <label className="text-sm">GitHub Profile URL</label>
                  <Input {...register("contactInfo.github")} placeholder="https://github.com/username" />
                </div>
              </div>
            </div>

            {/* Summary */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium">Professional Summary</h3>
              <Controller
                name="summary"
                control={control}
                render={({ field }) => (
                  <Textarea {...field} className="h-32" placeholder="Write about yourself..." />
                )}
              />
            </div>

            {/* Skills */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium">Skills</h3>
              <Controller
                name="skills"
                control={control}
                render={({ field }) => (
                  <Textarea {...field} className="h-32" placeholder="Java, React, AWS..." />
                )}
              />
              {errors.skills && <p className="text-sm text-red-500">{errors.skills.message}</p>}
            </div>

            {/* Experiences */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium">Work Experience</h3>
              <Controller
                name="experience"
                control={control}
                render={({ field }) => (
                  <EntryForm type="Experience" entries={field.value} onChange={field.onChange} />
                )}
              />
            </div>

            {/* Education */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium">Education</h3>
              <Controller
                name="education"
                control={control}
                render={({ field }) => (
                  <EntryForm type="Education" entries={field.value} onChange={field.onChange} />
                )}
              />
            </div>

            {/* Projects */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium">Projects</h3>
              <Controller
                name="projects"
                control={control}
                render={({ field }) => (
                  <EntryForm type="Project" entries={field.value} onChange={field.onChange} />
                )}
              />
            </div>
          </form>
        </TabsContent>

        <TabsContent value="preview">
          {activeTab === "preview" && (
            <Button
              variant="link"
              className="mb-2"
              onClick={() => setResumeMode(resumeMode === "preview" ? "edit" : "preview")}
            >
              {resumeMode === "preview" ? <><Edit className="h-4 w-4 mr-2" />Edit Raw Markdown</> : <><Monitor className="h-4 w-4 mr-2" />Show Preview</>}
            </Button>
          )}

          <div className="border rounded-lg">
            <MDEditor
              value={previewContent}
              onChange={setPreviewContent}
              height={700}
              preview={resumeMode}
            />
          </div>

          <div className="hidden">
            <div id="resume-pdf">
              <MDEditor.Markdown
                source={previewContent}
                style={{ background: "white", color: "black", padding: '20px' }}
              />
            </div>
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}