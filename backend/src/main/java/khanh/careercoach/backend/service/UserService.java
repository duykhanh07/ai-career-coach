package khanh.careercoach.backend.service;

import khanh.careercoach.backend.dto.UpdateUserRequest;
import khanh.careercoach.backend.model.UserEntity;
import khanh.careercoach.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    // Định nghĩa các hằng số Key Pattern để tránh Hardcode rải rác
    private static final String USER_PK_PREFIX = "USER#";
    private static final String METADATA_SK = "METADATA";

    private final UserRepository userRepository;

    // Constructor Injection
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Lấy thông tin User Profile
     */
    public UserEntity getUserProfile(String userId) {
        // 1. Validator: Kiểm tra đầu vào
        validateUserId(userId);

        // 2. Tạo Key chuẩn Single Table Design
        String pk = USER_PK_PREFIX + userId;

        logger.debug("Fetching profile for user: {}", userId);

        // 3. Gọi Repository (Đã có sẵn hàm findById)
        return userRepository.findById(pk, METADATA_SK);
    }

    /**
     * Cập nhật (hoặc tạo mới) User Profile
     */
    public UserEntity updateUserProfile(String userId, String email, UpdateUserRequest request) {
        // 1. Validator
        validateUserId(userId);
        if (request == null) {
            throw new IllegalArgumentException("Update request cannot be null");
        }

        logger.info("Processing profile update for user: {}", userId);

        // 2. Lấy user cũ để kiểm tra tồn tại
        UserEntity user = getUserProfile(userId);

        // 3. Logic: Create if not exists (Upsert)
        if (user == null) {
            logger.info("User not found via ID {}, creating new profile...", userId);
            if (email == null || email.isEmpty()) {
                logger.warn("Creating new user but Email is missing!");
            }

            user = new UserEntity();
            user.setPk(USER_PK_PREFIX + userId);
            user.setSk(METADATA_SK);
            user.setCreatedAt(Instant.now().toString());
            user.setEmail(email); // Chỉ set email khi tạo mới
        }

        // 4. Mapping dữ liệu từ DTO sang Entity (Chỉ update nếu có dữ liệu)
        boolean isUpdated = false;

        if (hasValue(request.getIndustry())) {
            user.setIndustry(request.getIndustry());
            isUpdated = true;
        }
        if (hasValue(request.getBio())) {
            user.setBio(request.getBio());
            isUpdated = true;
        }
        if (request.getExperience() != null) {
            user.setExperience(request.getExperience());
            isUpdated = true;
        }
        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            user.setSkills(request.getSkills());
            isUpdated = true;
        }

        // 5. Lưu xuống DB
        user.setUpdatedAt(Instant.now().toString());

        // Dùng hàm save của Repository (nó sẽ log debug bên trong)
        userRepository.save(user);

        logger.info("Profile updated successfully for user: {}", userId);
        return user;
    }

    // Thêm hàm này vào UserService.java

    public Map<String, Boolean> checkOnboardingStatus(String userId) {
        // 1. Validator input
        if (userId == null || userId.trim().isEmpty()) {
            logger.error("CheckOnboardingStatus failed: UserId is null/empty");
            throw new IllegalArgumentException("User ID required");
        }

        // 2. Tạo Key chuẩn
        String pk = "USER#" + userId;
        String sk = "METADATA";

        logger.info("Checking onboarding status for User: {}", userId);

        // 3. Gọi Repo lấy User
        UserEntity user = userRepository.findById(pk, sk);

        // 4. Logic kiểm tra (Giống hệt logic if(!user) throw Error trong JS của bạn)
        if (user == null) {
            logger.warn("User not found in DB: {}", userId);
            throw new RuntimeException("User not found");
        }

        // 5. Logic xác định isOnboarded (Có industry = đã onboard)
        boolean isOnboarded = user.getIndustry() != null && !user.getIndustry().trim().isEmpty();

        logger.info("User {} onboarding status: {}", userId, isOnboarded);

        return Map.of("isOnboarded", isOnboarded);
    }

    // --- Helper Methods ---

    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            logger.error("Validation failed: UserId is null or empty");
            throw new IllegalArgumentException("UserId cannot be null or empty");
        }
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }
}