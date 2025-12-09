package khanh.careercoach.backend.functions;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import khanh.careercoach.backend.dto.CoverLetterRequest;
import khanh.careercoach.backend.model.CoverLetterEntity;
import khanh.careercoach.backend.service.CoverLetterService;
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
public class CoverLetterFunctions {

    private static final Logger logger = LoggerFactory.getLogger(CoverLetterFunctions.class);
    private final CoverLetterService coverLetterService;
    private final ObjectMapper objectMapper;

    public CoverLetterFunctions(CoverLetterService coverLetterService) {
        this.coverLetterService = coverLetterService;

        // --- CẤU HÌNH JACKSON THỦ CÔNG (QUAN TRỌNG) ---
        // Giúp serialize được các object không có getter/setter chuẩn hoặc field private
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * HÀM TỔNG (ROUTER)
     * Phân phối request dựa trên Path và Method
     */
    @Bean
    public Function<Map<String, Object>, Map<String, Object>> coverLetterHandler() {
        return event -> {
            try {
                // 1. Trích xuất thông tin Request
                String path = extractPath(event);
                String method = extractHttpMethod(event);
                logger.info("CoverLetter Router -> Path: [{}], Method: [{}]", path, method);

                // 2. Xác thực User (Auth)
                Map<String, String> headers = normalizeHeaders(event);
                String userId = extractUserIdOrThrow(headers);

                // 3. Phân luồng xử lý (Routing)

                // Case 1: GET /cover-letters (List All)
                if (path.endsWith("/cover-letters") && "GET".equalsIgnoreCase(method)) {
                    return handleListCoverLetters(userId);
                }

                // Case 2: POST /cover-letters (Generate)
                if (path.endsWith("/cover-letters") && "POST".equalsIgnoreCase(method)) {
                    return handleGenerateCoverLetter(userId, event);
                }

                // Case 3: GET /cover-letters/{id} (Get One)
                if (path.contains("/cover-letters/") && "GET".equalsIgnoreCase(method)) {
                    String id = extractId(event, path);
                    return handleGetOneCoverLetter(userId, id);
                }

                // Case 4: DELETE /cover-letters/{id}
                if (path.contains("/cover-letters/") && "DELETE".equalsIgnoreCase(method)) {
                    String id = extractId(event, path);
                    return handleDeleteCoverLetter(userId, id);
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

    private Map<String, Object> handleListCoverLetters(String userId) {
        logger.info("Fetching all cover letters for user: {}", userId);
        List<CoverLetterEntity> list = coverLetterService.getAllCoverLetters(userId);
        return buildResponse(200, list);
    }

    private Map<String, Object> handleGenerateCoverLetter(String userId, Map<String, Object> event) throws Exception {
        String bodyString = extractBodyContent(event);
        if (bodyString == null || bodyString.trim().isEmpty()) {
            throw new IllegalArgumentException("Request body is required");
        }

        logger.debug("Generating cover letter with body: {}", bodyString);
        CoverLetterRequest req = objectMapper.readValue(bodyString, CoverLetterRequest.class);

        CoverLetterEntity created = coverLetterService.generateCoverLetter(userId, req);

        logger.info("Successfully generated cover letter. ID: {}", created.getSk());
        return buildResponse(200, created);
    }

    private Map<String, Object> handleGetOneCoverLetter(String userId, String letterId) {
        if (letterId == null || letterId.isEmpty()) throw new IllegalArgumentException("ID is missing");

        logger.info("Fetching cover letter ID: {}", letterId);
        CoverLetterEntity item = coverLetterService.getCoverLetter(userId, letterId);

        if (item == null) {
            return buildResponse(404, Map.of("error", "Cover letter not found"));
        }
        return buildResponse(200, item);
    }

    private Map<String, Object> handleDeleteCoverLetter(String userId, String letterId) {
        if (letterId == null || letterId.isEmpty()) throw new IllegalArgumentException("ID is missing");

        logger.info("Deleting cover letter ID: {}", letterId);
        coverLetterService.deleteCoverLetter(userId, letterId);

        return buildResponse(200, Map.of("status", "deleted", "id", letterId));
    }

    // =========================================================================
    // HELPERS (Tiện ích)
    // =========================================================================

    private String extractId(Map<String, Object> event, String path) {
        // Ưu tiên lấy từ Path Parameters của API Gateway
        if (event.get("pathParameters") instanceof Map) {
            Map<?, ?> params = (Map<?, ?>) event.get("pathParameters");
            if (params != null && params.get("id") != null) {
                return params.get("id").toString();
            }
        }
        // Fallback: Cắt chuỗi URL
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }

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

            // Dùng getUrlDecoder cho JWT
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
            // Dùng objectMapper thủ công để serialize (tránh lỗi ClassCastException)
            response.put("body", objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            response.put("statusCode", 500);
            response.put("body", "{\"error\": \"JSON Serialization Error\"}");
        }
        return response;
    }
}