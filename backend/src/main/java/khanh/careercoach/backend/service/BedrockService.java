package khanh.careercoach.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import khanh.careercoach.backend.model.IndustryInsightEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class BedrockService {

    private static final Logger logger = LoggerFactory.getLogger(BedrockService.class);

    private final BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper;

    // Model ID lấy từ biến môi trường (Config trong template.yaml)
    private final String modelId = System.getenv("BEDROCK_MODEL_ID");

    public BedrockService(ObjectMapper objectMapper) {
        this.bedrockClient = BedrockRuntimeClient.builder().build(); // Tự lấy region từ môi trường
        this.objectMapper = objectMapper;
    }

    public IndustryInsightEntity generateIndustryInsights(String industry) {
        logger.info("Calling AWS Bedrock to analyze industry: {}", industry);

        String prompt = """
             Analyze the current state of the %s industry and provide insights in ONLY the following JSON format without any additional notes or explanations:
             {
               "salaryRanges": [
                 { "role": "string", "min": number, "max": number, "median": number, "location": "string" }
               ],
               "growthRate": number,
               "demandLevel": "High" | "Medium" | "Low",
               "topSkills": ["skill1", "skill2"],
               "marketOutlook": "Positive" | "Neutral" | "Negative",
               "keyTrends": ["trend1", "trend2"],
               "recommendedSkills": ["skill1", "skill2"]
             }
             
             IMPORTANT: Return ONLY the JSON. No additional text, notes, or markdown formatting.
             Include at least 5 common roles for salary ranges.
             Growth rate should be a percentage float (e.g., 5.5).
             Include at least 5 skills and trends.
             """.formatted(industry);

        // Cấu trúc Body cho Claude 3
        Map<String, Object> payload = Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens", 2000,
                "messages", java.util.List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromUtf8String(payloadJson))
                    .contentType("application/json")
                    .accept("application/json")
                    .build();

            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asString(StandardCharsets.UTF_8);

            // Parse response của Claude để lấy phần text
            // Structure: { "content": [ { "text": "..." } ] }
            var jsonNode = objectMapper.readTree(responseBody);
            String aiText = jsonNode.get("content").get(0).get("text").asText();

            // Clean text (remove markdown ```json ... ```)
            String cleanedJson = aiText.replaceAll("```json", "").replaceAll("```", "").trim();
            logger.debug("Cleaned AI JSON: {}", cleanedJson);

            // Parse thành Entity
            return objectMapper.readValue(cleanedJson, IndustryInsightEntity.class);

        } catch (Exception e) {
            logger.error("Failed to generate AI insights", e);
            throw new RuntimeException("AI Generation Failed: " + e.getMessage());
        }
    }

    /**
     * Hàm gọi AI trả về Text thuần (dùng cho Resume improvement)
     */
    public String generateTextCorrection(String prompt) {
        logger.info("Calling Bedrock for Text Generation...");

        // Cấu trúc Body cho Claude 3
        Map<String, Object> payload = Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens", 1000,
                "messages", java.util.List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromUtf8String(payloadJson))
                    .contentType("application/json")
                    .accept("application/json")
                    .build();

            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asString(StandardCharsets.UTF_8);

            // Parse response: { "content": [ { "text": "..." } ] }
            var jsonNode = objectMapper.readTree(responseBody);
            String aiText = jsonNode.get("content").get(0).get("text").asText();

            return aiText.trim();

        } catch (Exception e) {
            logger.error("Bedrock Text Generation Failed", e);
            throw new RuntimeException("AI Service Unavailable");
        }
    }


    // Hàm mới: Tạo Quiz Questions
    public String generateQuizJson(String industry, String skills) {
        logger.info("Generating Quiz for {} with skills {}", industry, skills);

        String prompt = String.format("""
            Generate 10 technical interview questions for a %s professional with expertise in %s.
            Each question must be multiple choice with 4 options.
            
            Return the response in this JSON format only, no additional text, no markdown:
            {
              "questions": [
                {
                  "question": "string",
                  "options": ["string", "string", "string", "string"],
                  "correctAnswer": "string",
                  "explanation": "string"
                }
              ]
            }
            """, industry, skills);

        return callBedrock(prompt);
    }

    // Hàm tái sử dụng logic gọi Bedrock
    private String callBedrock(String prompt) {
        // Logic giống hệt hàm generateTextCorrection cũ, chỉ thay prompt
        // (Bạn có thể refactor code cũ để dùng chung hàm này cho gọn)
        Map<String, Object> payload = Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens", 4000, // Tăng token vì JSON quiz khá dài
                "messages", java.util.List.of(Map.of("role", "user", "content", prompt))
        );
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromUtf8String(payloadJson))
                    .contentType("application/json").accept("application/json").build();
            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asString(StandardCharsets.UTF_8);
            var jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.get("content").get(0).get("text").asText().trim();
        } catch (Exception e) {
            logger.error("Bedrock Error", e);
            throw new RuntimeException("AI Error");
        }
    }

}