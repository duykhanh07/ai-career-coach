package khanh.careercoach.backend.model;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@Data
@DynamoDbBean
public class CoverLetterEntity {
    private String pk; // Format: USER#<cognito_sub>
    private String sk; // Format: LETTER#<uuid>

    private String content; // Markdown do AI Bedrock viết
    private String jobDescription;
    private String companyName;
    private String jobTitle;
    private String status; // "draft", "completed"

    private String createdAt;
    private String updatedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() { return pk; }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() { return sk; }
}
