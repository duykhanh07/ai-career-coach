package khanh.careercoach.backend.service;

import khanh.careercoach.backend.model.ResumeEntity;
import khanh.careercoach.backend.model.UserEntity;
import khanh.careercoach.backend.repository.ResumeRepository;
import khanh.careercoach.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ResumeService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final BedrockService bedrockService;

    public ResumeService(ResumeRepository resumeRepository, UserRepository userRepository, BedrockService bedrockService) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.bedrockService = bedrockService;
    }

    // 1. Save Resume (Upsert)
    public ResumeEntity saveResume(String userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Resume content cannot be empty");
        }

        // Kiểm tra user có tồn tại không
        // (Tùy chọn: nếu tin tưởng token thì có thể bỏ qua bước này để tiết kiệm 1 RCU)

        ResumeEntity resume = resumeRepository.findByUserId(userId);

        if (resume == null) {
            logger.info("Creating new resume for user: {}", userId);
            resume = new ResumeEntity();
            resume.setPk("USER#" + userId);
            resume.setSk("RESUME");
            resume.setCreatedAt(Instant.now().toString());
        } else {
            logger.info("Updating existing resume for user: {}", userId);
        }

        resume.setContent(content);
        resume.setUpdatedAt(Instant.now().toString());

        resumeRepository.save(resume);
        return resume;
    }

    // 2. Get Resume
    public ResumeEntity getResume(String userId) {
        logger.debug("Fetching resume for user: {}", userId);
        return resumeRepository.findByUserId(userId);
    }

    // 3. Improve Content with AI
    public String improveWithAI(String userId, String currentContent, String type) {
        // Validation
        if (currentContent == null || currentContent.isEmpty()) throw new IllegalArgumentException("Current content is required");
        if (type == null || type.isEmpty()) throw new IllegalArgumentException("Type is required");

        // Lấy thông tin User để biết Industry
        UserEntity user = userRepository.findById("USER#" + userId, "METADATA");
        if (user == null) throw new RuntimeException("User not found");

        String industry = user.getIndustry();
        if (industry == null) industry = "General Professional"; // Fallback nếu chưa có ngành

        logger.info("Improving resume section '{}' for industry '{}'", type, industry);

        // Tạo Prompt cho Bedrock (Claude 3)
        String prompt = String.format("""
            As an expert resume writer, improve the following %s description for a %s professional.
            Make it more impactful, quantifiable, and aligned with industry standards.
            Current content: "%s"

            Requirements:
            1. Use action verbs
            2. Include metrics and results where possible
            3. Highlight relevant technical skills
            4. Keep it concise but detailed
            5. Focus on achievements over responsibilities
            6. Use industry-specific keywords
            
            Format the response as a single paragraph without any additional text or explanations.
            """, type, industry, currentContent);

        // Gọi AI
        return bedrockService.generateTextCorrection(prompt);
    }
}