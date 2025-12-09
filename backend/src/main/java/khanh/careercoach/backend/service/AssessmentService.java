package khanh.careercoach.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import khanh.careercoach.backend.dto.QuizQuestion;
import khanh.careercoach.backend.dto.SaveAssessmentRequest;
import khanh.careercoach.backend.model.AssessmentEntity;
import khanh.careercoach.backend.model.AssessmentEntity.QuestionItem; // Inner class
import khanh.careercoach.backend.model.UserEntity;
import khanh.careercoach.backend.repository.AssessmentRepository;
import khanh.careercoach.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentService.class);

    private final AssessmentRepository assessmentRepository;
    private final UserRepository userRepository;
    private final BedrockService bedrockService;
    private final ObjectMapper objectMapper;

    public AssessmentService(AssessmentRepository assessmentRepository, UserRepository userRepository,
                             BedrockService bedrockService, ObjectMapper objectMapper) {
        this.assessmentRepository = assessmentRepository;
        this.userRepository = userRepository;
        this.bedrockService = bedrockService;
        this.objectMapper = objectMapper;
    }

    // 1. Generate Quiz
    public List<QuizQuestion> generateQuiz(String userId) {
        // Lấy thông tin User
        UserEntity user = userRepository.findById("USER#" + userId, "METADATA");
        if (user == null) throw new RuntimeException("User not found");

        String industry = user.getIndustry();
        String skills = user.getSkills() != null ? String.join(", ", user.getSkills()) : "";

        // Gọi AI
        String jsonResponse = bedrockService.generateQuizJson(industry, skills);

        // Parse JSON trả về List Questions
        try {
            // Claude có thể trả về text kèm markdown, cần clean
            String cleanedJson = jsonResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            JsonNode root = objectMapper.readTree(cleanedJson);
            JsonNode questionsNode = root.get("questions");

            return Arrays.asList(objectMapper.treeToValue(questionsNode, QuizQuestion[].class));
        } catch (Exception e) {
            logger.error("Failed to parse Quiz JSON", e);
            throw new RuntimeException("Failed to parse AI response");
        }
    }

    // 2. Save Result & Generate Tip
    public AssessmentEntity saveQuizResult(String userId, SaveAssessmentRequest request) {
        UserEntity user = userRepository.findById("USER#" + userId, "METADATA");
        if (user == null) throw new RuntimeException("User not found");

        List<QuestionItem> questionResults = new ArrayList<>();
        List<String> wrongAnswersText = new ArrayList<>();

        // Map DTO sang Entity và tìm câu sai
        for (int i = 0; i < request.getQuestions().size(); i++) {
            QuizQuestion q = request.getQuestions().get(i);
            String userAnswer = request.getUserAnswers().get(i);
            boolean isCorrect = q.getCorrectAnswer().equals(userAnswer);

            QuestionItem item = new QuestionItem();
            item.setQuestion(q.getQuestion());
            item.setAnswer(q.getCorrectAnswer());
            item.setUserAnswer(userAnswer);
            item.setIsCorrect(isCorrect);
            item.setExplanation(q.getExplanation());

            questionResults.add(item);

            if (!isCorrect) {
                wrongAnswersText.add(String.format("Question: \"%s\"\nCorrect: \"%s\"\nUser Answer: \"%s\"",
                        q.getQuestion(), q.getCorrectAnswer(), userAnswer));
            }
        }

        // Tạo Improvement Tip nếu có câu sai
        String improvementTip = null;
        if (!wrongAnswersText.isEmpty()) {
            String prompt = String.format("""
                The user got the following %s technical interview questions wrong:
                %s
                
                Based on these mistakes, provide a concise, specific improvement tip.
                Focus on knowledge gaps. Keep it under 2 sentences. Encouraging tone.
                """, user.getIndustry(), String.join("\n\n", wrongAnswersText));

            improvementTip = bedrockService.generateTextCorrection(prompt); // Tái sử dụng hàm sinh text
        }

        // Lưu DB
        AssessmentEntity entity = new AssessmentEntity();
        entity.setPk("USER#" + userId);
        entity.setSk("ASSESS#" + UUID.randomUUID().toString());
        entity.setQuizScore(request.getScore());
        entity.setCategory("Technical");
        entity.setImprovementTip(improvementTip);
        entity.setQuestions(questionResults); // DynamoDB Enhanced tự convert List sang JSON
        entity.setCreatedAt(Instant.now().toString());
        entity.setUpdatedAt(Instant.now().toString());

        assessmentRepository.save(entity);
        return entity;
    }

    // 3. Get History
    public List<AssessmentEntity> getAssessments(String userId) {
        return assessmentRepository.findAllByUserId(userId);
    }
}