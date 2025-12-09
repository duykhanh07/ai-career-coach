package khanh.careercoach.backend.model;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@Data
@DynamoDbBean
public class ResumeEntity {
    private String pk; // Format: USER#<cognito_sub>
    private String sk; // Format: RESUME

    private String content; // Markdown text
    private Double atsScore; // Điểm số ATS
    private String feedback; // Feedback từ Bedrock AI

    private String createdAt;
    private String updatedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() { return pk; }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() { return sk; }
}
