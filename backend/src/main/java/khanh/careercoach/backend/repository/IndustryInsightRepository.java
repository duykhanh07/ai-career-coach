package khanh.careercoach.backend.repository;

import khanh.careercoach.backend.model.IndustryInsightEntity;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

@Repository
public class IndustryInsightRepository extends AbstractDynamoRepository<IndustryInsightEntity> {

    public IndustryInsightRepository(DynamoDbEnhancedClient client) {
        super(client, IndustryInsightEntity.class);
    }

    /**
     * Tìm Insight dựa trên tên ngành.
     * PK: INDUSTRY#<industryName>
     * SK: METADATA
     */
    public IndustryInsightEntity findByIndustry(String industryName) {
        if (industryName == null || industryName.trim().isEmpty()) {
            return null;
        }
        // Chuẩn hóa key (ví dụ: lowercase hoặc slugify nếu cần thiết)
        // Ở đây giả định lưu nguyên văn chuỗi
        String pk = "INDUSTRY#" + industryName;
        String sk = "METADATA";

        return findById(pk, sk);
    }
}