package khanh.careercoach.backend.model;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.util.List;

@Data
@DynamoDbBean
public class AssessmentEntity {
    private String pk; // Format: USER#<cognito_sub>
    private String sk; // Format: ASSESS#<uuid>

    private Double quizScore;
    private String category; // "Technical", "Behavioral"
    private String improvementTip; // AI Bedrock generated tip

    // Nested Object List (JSON)
    private List<QuestionItem> questions;

    private String createdAt;
    private String updatedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() { return pk; }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() { return sk; }

    // Inner Class cho cấu trúc câu hỏi
    @Data
    @DynamoDbBean
    public static class QuestionItem {
        private String question;
        private String answer; // Đáp án mẫu
        private String userAnswer; // Câu trả lời của user
        private Boolean isCorrect;
        private String explanation; // AI giải thích tại sao đúng/sai
    }
}
