package khanh.careercoach.backend.model;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import java.util.List;

@Data
@DynamoDbBean
public class IndustryInsightEntity {
    private String pk; // Format: INDUSTRY#<tên_ngành> (Ví dụ: INDUSTRY#tech-software)
    private String sk; // Format: METADATA

    // AI Bedrock Generated Data
    private Float growthRate;
    private String demandLevel; // "High", "Medium", "Low"
    private String marketOutlook;

    private List<String> topSkills;
    private List<String> keyTrends;
    private List<String> recommendedSkills;

    // Nested JSON cho lương
    private List<SalaryRangeItem> salaryRanges;

    private String lastUpdated;
    private String nextUpdate;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() { return pk; }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() { return sk; }

    // Inner Class cho dải lương
    @Data
    @DynamoDbBean
    public static class SalaryRangeItem {
        private String role;
        private Float min;
        private Float max;
        private Float median;
        private String location;
    }
}
