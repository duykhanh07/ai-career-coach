package khanh.careercoach.backend.functions;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import khanh.careercoach.backend.dto.QuizQuestion;
import khanh.careercoach.backend.dto.SaveAssessmentRequest;
import khanh.careercoach.backend.model.AssessmentEntity;
import khanh.careercoach.backend.service.AssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class AssessmentFunctions {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentFunctions.class);

    private final AssessmentService assessmentService;
    private final ObjectMapper objectMapper;

    public AssessmentFunctions(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;

        // --- CẤU HÌNH JACKSON THỦ CÔNG ---
        // Đảm bảo parse được mọi loại object, kể cả private fields
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * HÀM TỔNG (ROUTER) CHO ASSESSMENT
     */
    @Bean
    public Function<Map<String, Object>, Map<String, Object>> assessmentHandler() {
        return event -> {
            try {
                // 1. Trích xuất thông tin Request
                String path = extractPath(event);
                String method = extractHttpMethod(event);
                logger.info("Assessment Router -> Path: [{}], Method: [{}]", path, method);

                // 2. Xác thực User (Auth)
                Map<String, String> headers = normalizeHeaders(event);
                String userId = extractUserIdOrThrow(headers);

                // 3. Phân luồng xử lý (Routing)

                // Case 1: POST /interview/generate (Generate Quiz)
                if (path.endsWith("/interview/generate") && "POST".equalsIgnoreCase(method)) {
                    return handleGenerateQuiz(userId);
                }

                // Case 2: POST /interview/save (Save Result)
                if (path.endsWith("/interview/save") && "POST".equalsIgnoreCase(method)) {
                    return handleSaveQuizResult(userId, event);
                }

                // Case 3: GET /interview/history (Get List)
                if (path.endsWith("/interview/history") && "GET".equalsIgnoreCase(method)) {
                    return handleGetAssessmentHistory(userId);
                }

                return buildResponse(404, Map.of("error", "Route not found: " + path));

            } catch (SecurityException e) {
                logger.warn("Auth Error: {}", e.getMessage());
                return buildResponse(401, Map.of("error", e.getMessage()));
            } catch (IllegalArgumentException e) {
                logger.warn("Validation Error: {}", e.getMessage());
                return buildResponse(400, Map.of("error", e.getMessage()));
            } catch (Exception e) {
                logger.error("System Error", e);
                return buildResponse(500, Map.of("error", e.getMessage()));
            }
        };
    }

    // =========================================================================
    // LOGIC CON (SUB-HANDLERS)
    // =========================================================================

    private Map<String, Object> handleGenerateQuiz(String userId) {
        logger.info("Generating interview quiz for user: {}", userId);

        // Gọi Service (Có thể mất thời gian do gọi AI)
        List<QuizQuestion> quiz = assessmentService.generateQuiz(userId);

        logger.info("Generated {} questions successfully.", quiz.size());
        return buildResponse(200, Map.of("questions", quiz));
    }

    private Map<String, Object> handleSaveQuizResult(String userId, Map<String, Object> event) throws Exception {
        String bodyString = extractBodyContent(event);

        if (bodyString == null || bodyString.trim().isEmpty()) {
            throw new IllegalArgumentException("Request body is required for saving result");
        }

        logger.debug("Saving quiz result payload: {}", bodyString);

        // Parse JSON sang DTO
        SaveAssessmentRequest req = objectMapper.readValue(bodyString, SaveAssessmentRequest.class);

        // Validate cơ bản DTO
        if (req.getQuestions() == null || req.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("Questions list cannot be empty");
        }

        // Gọi Service
        AssessmentEntity result = assessmentService.saveQuizResult(userId, req);

        logger.info("Saved assessment result ID: {}", result.getSk());
        return buildResponse(200, result);
    }

    private Map<String, Object> handleGetAssessmentHistory(String userId) {
        logger.info("Fetching assessment history for user: {}", userId);

        List<AssessmentEntity> list = assessmentService.getAssessments(userId);

        logger.info("Found {} past assessments.", list.size());
        return buildResponse(200, list);
    }

    // =========================================================================
    // HELPERS (Tiện ích - Copy y hệt từ các file trước)
    // =========================================================================

    private String extractPath(Map<String, Object> event) {
        return event.get("rawPath") != null ? event.get("rawPath").toString() : "";
    }

    private String extractHttpMethod(Map<String, Object> event) {
        try {
            Map req = (Map) event.get("requestContext");
            if (req != null) {
                Map http = (Map) req.get("http");
                if (http != null) return http.get("method").toString();
            }
        } catch (Exception e) { /* ignore */ }
        return "GET";
    }

    private Map<String, String> normalizeHeaders(Map<String, Object> event) {
        Map<String, String> headers = new HashMap<>();
        Object headersObj = event.get("headers");
        if (headersObj instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) headersObj;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    headers.put(entry.getKey().toString().toLowerCase(), entry.getValue().toString());
                }
            }
        }
        return headers;
    }

    private String extractUserIdOrThrow(Map<String, String> headers) {
        String authHeader = headers.get("authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Missing or invalid Authorization header");
        }
        try {
            String token = authHeader.substring(7);
            String[] parts = token.split("\\.");
            if (parts.length < 2) throw new SecurityException("Invalid JWT format");

            // Dùng getUrlDecoder cho JWT chuẩn
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readTree(payloadJson).get("sub").asText();
        } catch (Exception e) {
            logger.error("Token decode error", e);
            throw new SecurityException("Token validation failed");
        }
    }

    private String extractBodyContent(Map<String, Object> event) {
        Object bodyObj = event.get("body");
        if (bodyObj == null) return null;

        String bodyString;
        if (bodyObj instanceof String) {
            bodyString = (String) bodyObj;
        } else {
            try {
                bodyString = objectMapper.writeValueAsString(bodyObj);
            } catch (Exception e) { return "{}"; }
        }

        Object isBase64Obj = event.get("isBase64Encoded");
        if (isBase64Obj != null && Boolean.parseBoolean(isBase64Obj.toString())) {
            try {
                bodyString = new String(Base64.getDecoder().decode(bodyString));
            } catch (Exception e) { throw new RuntimeException("Invalid Base64 body"); }
        }
        return bodyString;
    }

    private Map<String, Object> buildResponse(int statusCode, Object body) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);
        response.put("headers", Map.of("Content-Type", "application/json"));
        try {
            // Dùng objectMapper thủ công để serialize
            response.put("body", objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            response.put("statusCode", 500);
            response.put("body", "{\"error\": \"JSON Serialization Error\"}");
        }
        return response;
    }
}