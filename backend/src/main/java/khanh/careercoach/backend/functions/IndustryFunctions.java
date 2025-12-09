package khanh.careercoach.backend.functions;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import khanh.careercoach.backend.model.IndustryInsightEntity;
import khanh.careercoach.backend.service.IndustryInsightService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class IndustryFunctions {

    private static final Logger logger = LoggerFactory.getLogger(IndustryFunctions.class);

    private final IndustryInsightService insightService;
    private final ObjectMapper objectMapper;

    public IndustryFunctions(IndustryInsightService insightService) {
        this.insightService = insightService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * HÀM TỔNG (DISPATCHER) CHO INDUSTRY
     * Quản lý tất cả route liên quan đến /industry-insights
     */
    @Bean
    public Function<Map<String, Object>, Map<String, Object>> industryInsightHandler() {
        return event -> {
            try {
                String path = extractPath(event);
                String method = extractHttpMethod(event);
                logger.info("Industry Handler -> Path: {}, Method: {}", path, method);

                // Chuẩn hóa Header & Auth
                Map<String, String> headers = normalizeHeaders(event);
                String userId = extractUserIdOrThrow(headers);

                // --- ROUTING LOGIC ---
                // Route 1: GET /industry-insights
                if (path.endsWith("/industry-insights") && "GET".equalsIgnoreCase(method)) {
                    return handleGetIndustryInsights(userId);
                }

                // Route 2 (Ví dụ tương lai): POST /industry-insights/refresh (Force update AI)
                // if (path.endsWith("/refresh") ...) { return handleRefresh(userId); }

                return buildResponse(404, Map.of("error", "Route not found in Industry Function"));

            } catch (SecurityException e) {
                return buildResponse(401, Map.of("error", e.getMessage()));
            } catch (Exception e) {
                logger.error("System Error", e);
                return buildResponse(500, Map.of("error", e.getMessage()));
            }
        };
    }

    // --- LOGIC CON ---
    private Map<String, Object> handleGetIndustryInsights(String userId) {
        IndustryInsightEntity result = insightService.getIndustryInsights(userId);
        return buildResponse(200, result);
    }

    // --- CÁC HÀM HELPER (Copy từ UserFunctions sang hoặc gom vào 1 file Utils dùng chung) ---
    // (Để code ngắn gọn ở đây mình ví dụ các hàm quan trọng, bạn copy phần decode token từ UserFunctions sang nhé)

    private String extractPath(Map<String, Object> event) {
        return event.get("rawPath") != null ? event.get("rawPath").toString() : "";
    }

    private String extractHttpMethod(Map<String, Object> event) {
        Map<String, Object> req = (Map) event.get("requestContext");
        if (req != null) {
            Map<String, Object> http = (Map) req.get("http");
            if (http != null) return http.get("method").toString();
        }
        return "GET";
    }

    private Map<String, String> normalizeHeaders(Map<String, Object> event) {
        Map<String, String> headers = new HashMap<>();
        Object headersObj = event.get("headers");
        if (headersObj instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) headersObj;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                headers.put(entry.getKey().toString().toLowerCase(), entry.getValue().toString());
            }
        }
        return headers;
    }

    private String extractUserIdOrThrow(Map<String, String> headers) {
        // ... Logic extract token giống hệt UserFunctions ...
        // (Lưu ý: Bạn nên tạo class JwtUtils để tái sử dụng đoạn này đỡ phải copy paste)
        String authHeader = headers.get("authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String[] parts = token.split("\\.");
                String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1])); // Nhớ dùng Base64UrlDecoder
                return new ObjectMapper().readTree(payloadJson).get("sub").asText();
            } catch (Exception e) { logger.error("Token error", e); }
        }
        throw new SecurityException("Invalid Token");
    }

    private Map<String, Object> buildResponse(int statusCode, Object body) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.put("headers", headers);
        try {
            response.put("body", objectMapper.writeValueAsString(body));
        } catch (Exception e) {
            response.put("statusCode", 500);
            response.put("body", "{}");
        }
        return response;
    }
}