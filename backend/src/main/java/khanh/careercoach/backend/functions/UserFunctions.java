package khanh.careercoach.backend.functions;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import khanh.careercoach.backend.dto.UpdateUserRequest;
import khanh.careercoach.backend.model.UserEntity;
import khanh.careercoach.backend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class UserFunctions {

    private static final Logger logger = LoggerFactory.getLogger(UserFunctions.class);

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public UserFunctions(UserService userService) {
        this.userService = userService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    //Hàm tổng (ROUTER FUNCTION) để điều phối
    @Bean
    public Function<Map<String, Object>, Map<String, Object>> profileHandler() {
        return event -> {
            try {
                // 1. Lấy thông tin cơ bản
                String httpMethod = extractHttpMethod(event);
                String path = extractPath(event); // <--- Hàm mới để lấy path

                logger.info("Routing Request -> Path: {}, Method: {}", path, httpMethod);

                Map<String, String> headers = normalizeHeaders(event);
                String userId = extractUserIdOrThrow(headers);
                String email = extractEmail(headers);

                // 2. ROUTING LOGIC (Bộ điều phối)
                // TH1: Check Onboarding Status
                if (path.endsWith("/onboarding") && "GET".equalsIgnoreCase(httpMethod)) {
                    return handleGetOnboardingStatus(userId);
                }

                // TH2: Get Profile
                if (path.endsWith("/profile") && "GET".equalsIgnoreCase(httpMethod)) {
                    return handleGetProfile(userId);
                }

                // TH3: Update Profile
                if (path.endsWith("/profile") && "POST".equalsIgnoreCase(httpMethod)) {
                    return handleUpdateProfile(userId, email, event);
                }

                return buildResponse(404, Map.of("error", "Route not found: " + path));

            } catch (SecurityException e) {
                return buildResponse(401, Map.of("error", e.getMessage()));
            } catch (IllegalArgumentException e) { // Bắt lỗi user not found từ service
                return buildResponse(400, Map.of("error", e.getMessage()));
            } catch (Exception e) {
                logger.error("System Error", e);
                return buildResponse(500, Map.of("error", e.getMessage()));
            }
        };
    }
    // Các hàm nghiệp vụ

    private Map<String, Object> handleGetProfile(String userId) {
        logger.info("Executing GET logic for User: {}", userId);
        UserEntity user = userService.getUserProfile(userId);

        if (user == null) {
            return buildResponse(404, Map.of("error", "Profile not found"));
        }
        return buildResponse(200, user);
    }

    private Map<String, Object> handleUpdateProfile(String userId, String email, Map<String, Object> event) throws Exception {
        logger.info("Executing POST logic for User: {}", userId);

        String bodyString = extractBodyContent(event);
        if (bodyString == null || bodyString.trim().isEmpty()) {
            return buildResponse(400, Map.of("error", "Request body is empty"));
        }

        logger.debug("Request Body: {}", bodyString);

        UpdateUserRequest request = objectMapper.readValue(bodyString, UpdateUserRequest.class);
        UserEntity updatedUser = userService.updateUserProfile(userId, email, request);

        return buildResponse(200, updatedUser);
    }

    private Map<String, Object> handleGetOnboardingStatus(String userId) {
        try {
            Map<String, Boolean> result = userService.checkOnboardingStatus(userId);
            return buildResponse(200, result);
        } catch (RuntimeException e) {
            // Nếu lỗi là "User not found", trả về 404 cho đúng chuẩn REST
            if ("User not found".equals(e.getMessage())) {
                return buildResponse(404, Map.of("error", "User not found"));
            }
            throw e; // Ném tiếp để handler chung xử lý 500
        }
    }

    // --- CÁC HÀM TIỆN ÍCH (HELPERS) ---

    private String extractHttpMethod(Map<String, Object> event) {
        // Trong API Gateway V2, method nằm ở: requestContext -> http -> method
        try {
            Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
            if (requestContext != null) {
                Map<String, Object> http = (Map<String, Object>) requestContext.get("http");
                if (http != null && http.get("method") != null) {
                    return http.get("method").toString();
                }
            }
            // Fallback cho một số trường hợp test local hoặc format khác
            if (event.get("httpMethod") != null) {
                return event.get("httpMethod").toString();
            }
        } catch (Exception e) {
            logger.warn("Could not extract HTTP method from event", e);
        }
        return "UNKNOWN";
    }

    private String extractPath(Map<String, Object> event) {
        if (event.get("rawPath") != null) {
            return event.get("rawPath").toString();
        }
        // Fallback nếu event cấu trúc khác
        return "/unknown";
    }

    // =========================================================================
    // HELPER METHODS (PRIVATE)
    // =========================================================================

    /**
     * Chuẩn hóa Header về dạng Map<String, String> và lowercase key để dễ tìm
     */
    private Map<String, String> normalizeHeaders(Map<String, Object> event) {
        Map<String, String> headers = new HashMap<>();
        Object headersObj = event.get("headers");

        logger.info("DEBUG RAW HEADERS TYPE: {}", (headersObj != null ? headersObj.getClass().getName() : "null"));
        logger.info("DEBUG RAW HEADERS CONTENT: {}", headersObj);

        if (headersObj instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) headersObj;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    headers.put(entry.getKey().toString().toLowerCase(), entry.getValue().toString());
                }
            }
        }

        logger.info("DEBUG NORMALIZED HEADERS: {}", headers);

        return headers;
    }

    /**
     * Trích xuất và giải mã Body từ Event (xử lý cả Base64)
     */
    private String extractBodyContent(Map<String, Object> event) {
        Object bodyObj = event.get("body");
        if (bodyObj == null) return null;

        String bodyString;
        if (bodyObj instanceof String) {
            bodyString = (String) bodyObj;
        } else {
            // Trường hợp Spring đã lỡ parse thành Map
            try {
                bodyString = objectMapper.writeValueAsString(bodyObj);
            } catch (Exception e) {
                logger.warn("Failed to convert body object to string", e);
                return "{}";
            }
        }

        // Xử lý Base64 nếu AWS API Gateway bật tính năng này
        Object isBase64Obj = event.get("isBase64Encoded");
        if (isBase64Obj != null && Boolean.parseBoolean(isBase64Obj.toString())) {
            try {
                bodyString = new String(Base64.getDecoder().decode(bodyString));
            } catch (Exception e) {
                logger.error("Failed to decode Base64 body", e);
                throw new RuntimeException("Invalid Base64 body");
            }
        }
        return bodyString;
    }

    /**
     * Tạo Response chuẩn cho API Gateway
     */
    private Map<String, Object> buildResponse(int statusCode, Object body) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        // CORS Headers (Nếu cần thiết, dù API Gateway thường đã xử lý)
        headers.put("Access-Control-Allow-Origin", "*");
        response.put("headers", headers);

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            response.put("body", jsonBody);
        } catch (Exception e) {
            response.put("statusCode", 500);
            response.put("body", "{\"error\": \"JSON Serialization Error\"}");
        }
        return response;
    }

    // --- SECURITY HELPERS ---

    private String extractUserIdOrThrow(Map<String, String> headers) {
        String sub = extractClaim(headers, "sub");
        if (sub == null) {
            throw new SecurityException("Missing 'sub' claim in token or Invalid Token");
        }
        return sub;
    }

    private String extractEmail(Map<String, String> headers) {
        return extractClaim(headers, "email");
    }

    private String extractClaim(Map<String, String> headers, String claimKey) {
        // Tìm header Authorization (đã được normalize về chữ thường)
        String authHeader = headers.get("authorization");

        logger.info("DEBUG AUTH HEADER FOUND: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Authorization header missing or invalid format");
            // Thay vì trả về test ID, ta trả về null để hàm gọi ném lỗi 401
            return null;
        }

        try {
            String token = authHeader.substring(7);
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode node = objectMapper.readTree(payloadJson);

            if (node.has(claimKey)) {
                return node.get(claimKey).asText();
            }
        } catch (Exception e) {
            logger.error("Token decoding failed: {}", e.getMessage());
        }
        return null;
    }
}