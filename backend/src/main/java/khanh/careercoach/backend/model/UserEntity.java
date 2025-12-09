package khanh.careercoach.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.util.List;

@Data
@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    private String pk; // Format: USER#<cognito_sub>
    private String sk; // Format: METADATA

    // Core fields
    private String email;
    private String name;
    private String imageUrl;
    private String industry; // Link tới IndustryInsight (Lưu dạng chuỗi text)

    // Profile fields
    private String bio;
    private Integer experience;
    private List<String> skills; // DynamoDB hỗ trợ List<String> tự động

    // Timestamps (Lưu dạng ISO String cho dễ đọc: "2025-11-30T10:00:00Z")
    private String createdAt;
    private String updatedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() { return pk; }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() { return sk; }
}
