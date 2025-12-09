package khanh.careercoach.backend.service;

import khanh.careercoach.backend.model.IndustryInsightEntity;
import khanh.careercoach.backend.model.UserEntity;
import khanh.careercoach.backend.repository.IndustryInsightRepository;
import khanh.careercoach.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class IndustryInsightService {

    private static final Logger logger = LoggerFactory.getLogger(IndustryInsightService.class);

    private final UserRepository userRepository;
    private final IndustryInsightRepository insightRepository;
    private final BedrockService bedrockService;

    public IndustryInsightService(UserRepository userRepository,
                                  IndustryInsightRepository insightRepository,
                                  BedrockService bedrockService) {
        this.userRepository = userRepository;
        this.insightRepository = insightRepository;
        this.bedrockService = bedrockService;
    }

    public IndustryInsightEntity getIndustryInsights(String userId) {
        // 1. Validate User ID
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }

        // 2. Lấy thông tin User để biết họ làm ngành gì
        // PK: USER#<id>, SK: METADATA
        UserEntity user = userRepository.findById("USER#" + userId, "METADATA");

        if (user == null) {
            logger.error("User not found: {}", userId);
            throw new RuntimeException("User not found");
        }

        // 3. Kiểm tra xem User đã chọn ngành chưa
        String industry = user.getIndustry();
        if (industry == null || industry.trim().isEmpty()) {
            logger.warn("User {} has not selected an industry yet", userId);
            // Có thể throw lỗi hoặc trả về null tùy logic frontend
            throw new RuntimeException("User has not selected an industry");
        }

        // 4. Tìm Insight trong DB
        logger.info("Fetching insights for industry: {}", industry);
        IndustryInsightEntity insight = insightRepository.findByIndustry(industry);

        // 5. Nếu chưa có hoặc (Optional: đã hết hạn cache), gọi AI tạo mới
        // Ở đây logic gốc là "If no insights exist", tôi giữ nguyên logic đó.
        if (insight == null) {
            logger.info("Insight not found for '{}'. Generating via Bedrock AI...", industry);

            // Gọi AI
            insight = bedrockService.generateIndustryInsights(industry);

            // Bổ sung các thông tin quản lý DB
            insight.setPk("INDUSTRY#" + industry);
            insight.setSk("METADATA");
            insight.setLastUpdated(Instant.now().toString());
            // Set next update = now + 7 days
            insight.setNextUpdate(Instant.now().plus(7, ChronoUnit.DAYS).toString());

            // Lưu vào DB
            insightRepository.save(insight);
            logger.info("Saved new insights for '{}'", industry);
        } else {
            logger.info("Found existing insights for '{}' in DB", industry);
        }

        return insight;
    }
}