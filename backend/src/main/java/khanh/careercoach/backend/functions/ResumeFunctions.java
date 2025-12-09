package khanh.careercoach.backend.functions;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import khanh.careercoach.backend.model.ResumeEntity;
import khanh.careercoach.backend.service.ResumeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class ResumeFunctions {

    private static final Logger logger = LoggerFactory.getLogger(ResumeFunctions.class);

    private final ResumeService resumeService;
    private final ObjectMapper objectMapper;

    public ResumeFunctions(ResumeService resumeService) {
        this.resumeService = resumeService;

        // --- CẤU HÌNH JACKSON THỦ CÔNG (THEO YÊU CẦU) ---
        this.objectMapper = new ObjectMapper();
        // Cho phép đọc/ghi trực tiếp vào field (kể cả private)
        this.objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        // Bỏ qua lỗi nếu JSON có trường thừa
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * HÀM TỔNG (ROUTER) CHO RESUME
     */
    @Bean
    public Function<Map<String, Object>, Map<String, Object>> resumeHandler() {
        return event -> {
            try {
                String path = extractPath(event);
                String method = extractHttpMethod(event);
                logger.info("Resume Handler -> Path: {}, Method: {}", path, method);

                Map<String, String> headers = normalizeHeaders(event);
                String userId = extractUserIdOrThrow(headers);

                // --- ROUTING ---

                // 1. GET /resume
                if (path.endsWith("/resume") && "GET".equalsIgnoreCase(method)) {
                    return handleGetResume(userId);
                }

                // 2. POST /resume (Save)
                if (path.endsWith("/resume") && "POST".equalsIgnoreCase(method)) {
                    return handleSaveResume(userId, event);
                }

                // 3. POST /resume/improve (AI Improve)
                if (path.endsWith("/resume/improve") && "POST".equalsIgnoreCase(method)) {
                    return handleImproveResume(userId, event);
                }

                return buildResponse(404, Map.of("error", "Route not found"));

            } catch (SecurityException e) {
                return buildResponse(401, Map.of("error", e.getMessage()));
            } catch (IllegalArgumentException e) {
                return buildResponse(400, Map.of("error", e.getMessage()));
            } catch (Exception e) {
                logger.error("System Error", e);
                return buildResponse(500, Map.of("error", e.getMessage()));
            }
        };
    }

    // --- LOGIC CON ---

    private Map<String, Object> handleGetResume(String userId) {
        ResumeEntity resume = resumeService.getResume(userId);
        // Nếu chưa có resume, trả về null (frontend sẽ hiển thị form trống)
        return buildResponse(200, resume);
    }

    private Map<String, Object> handleSaveResume(String userId, Map<String, Object> event) throws Exception {
        String bodyString = extractBodyContent(event);

        // Parse thủ công để lấy field 'content'
        JsonNode node = objectMapper.readTree(bodyString);
        if (!node.has("content")) {
            throw new IllegalArgumentException("Field 'content' is required");
        }
        String content = node.get("content").asText();

        ResumeEntity saved = resumeService.saveResume(userId, content);
        return buildResponse(200, saved);
    }

    private Map<String, Object> handleImproveResume(String userId, Map<String, Object> event) throws Exception {
        String bodyString = extractBodyContent(event);
        JsonNode node = objectMapper.readTree(bodyString);

        if (!node.has("current") || !node.has("type")) {
            throw new IllegalArgumentException("Fields 'current' and 'type' are required");
        }
        String current = node.get("current").asText();
        String type = node.get("type").asText();

        String improvedContent = resumeService.improveWithAI(userId, current, type);

        // Trả về JSON đơn giản
        return buildResponse(200, Map.of("improvedContent", improvedContent));
    }

    // --- HELPERS (Đã được chuẩn hóa và format đẹp) ---

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

            // QUAN TRỌNG: Dùng Base64UrlDecoder
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            return objectMapper.readTree(payloadJson).get("sub").asText();
        } catch (Exception e) {
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
            response.put("body", objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            response.put("statusCode", 500);
            response.put("body", "{\"error\": \"JSON Serialization Error\"}");
        }
        return response;
    }
}