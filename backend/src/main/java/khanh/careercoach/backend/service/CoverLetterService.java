package khanh.careercoach.backend.service;

import khanh.careercoach.backend.dto.CoverLetterRequest;
import khanh.careercoach.backend.model.CoverLetterEntity;
import khanh.careercoach.backend.model.UserEntity;
import khanh.careercoach.backend.repository.CoverLetterRepository;
import khanh.careercoach.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CoverLetterService {

    private static final Logger logger = LoggerFactory.getLogger(CoverLetterService.class);

    private final CoverLetterRepository coverLetterRepository;
    private final UserRepository userRepository;
    private final BedrockService bedrockService;

    public CoverLetterService(CoverLetterRepository coverLetterRepository,
                              UserRepository userRepository,
                              BedrockService bedrockService) {
        this.coverLetterRepository = coverLetterRepository;
        this.userRepository = userRepository;
        this.bedrockService = bedrockService;
    }

    // 1. Generate Cover Letter (Create)
    public CoverLetterEntity generateCoverLetter(String userId, CoverLetterRequest request) {
        // Validate
        if (request.getJobTitle() == null || request.getCompanyName() == null || request.getJobDescription() == null) {
            throw new IllegalArgumentException("Missing required fields (jobTitle, companyName, jobDescription)");
        }

        // Lấy thông tin User để AI viết cho chuẩn
        UserEntity user = userRepository.findById("USER#" + userId, "METADATA");
        if (user == null) throw new RuntimeException("User not found");

        String skills = user.getSkills() != null ? String.join(", ", user.getSkills()) : "Not specified";

        // Prompt Engineering (Copy từ logic cũ của bạn)
        String prompt = String.format("""
            Write a professional cover letter for a %s position at %s.
            
            About the candidate:
            - Industry: %s
            - Years of Experience: %d
            - Skills: %s
            - Professional Background: %s
            
            Job Description:
            %s
            
            Requirements:
            1. Use a professional, enthusiastic tone
            2. Highlight relevant skills and experience
            3. Show understanding of the company's needs
            4. Keep it concise (max 400 words)
            5. Use proper business letter formatting in markdown
            6. Include specific examples of achievements
            7. Relate candidate's background to job requirements
            
            Format the letter in markdown. Do not include any preamble or postscript.
            """,
                request.getJobTitle(), request.getCompanyName(),
                user.getIndustry(), user.getExperience(), skills, user.getBio(),
                request.getJobDescription()
        );

        // Gọi Bedrock
        String aiContent = bedrockService.generateTextCorrection(prompt);

        // Tạo Entity lưu xuống DB
        CoverLetterEntity entity = new CoverLetterEntity();
        String letterId = UUID.randomUUID().toString();

        entity.setPk("USER#" + userId);
        entity.setSk("LETTER#" + letterId); // SK unique
        entity.setContent(aiContent);
        entity.setJobTitle(request.getJobTitle());
        entity.setCompanyName(request.getCompanyName());
        entity.setJobDescription(request.getJobDescription());
        entity.setStatus("completed");
        entity.setCreatedAt(Instant.now().toString());
        entity.setUpdatedAt(Instant.now().toString());

        coverLetterRepository.save(entity);
        logger.info("Generated cover letter {} for user {}", letterId, userId);

        return entity;
    }

    // 2. Get All
    public List<CoverLetterEntity> getAllCoverLetters(String userId) {
        // Lấy về tất cả item của user, sau đó lọc ra những cái là Cover Letter
        // (Trong môi trường production nên dùng Query SK begins_with "LETTER#" để tối ưu)
        List<CoverLetterEntity> allItems = coverLetterRepository.findAllByUserId(userId);

        return allItems.stream()
                .filter(item -> item.getSk().startsWith("LETTER#"))
                .collect(Collectors.toList());
    }

    // 3. Get One
    public CoverLetterEntity getCoverLetter(String userId, String letterId) {
        return coverLetterRepository.findById(userId, letterId);
    }

    // 4. Delete
    public void deleteCoverLetter(String userId, String letterId) {
        coverLetterRepository.deleteById(userId, letterId);
        logger.info("Deleted cover letter {} for user {}", letterId, userId);
    }
}