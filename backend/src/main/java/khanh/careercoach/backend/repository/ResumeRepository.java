package khanh.careercoach.backend.repository;

import khanh.careercoach.backend.model.ResumeEntity;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

@Repository
public class ResumeRepository extends AbstractDynamoRepository<ResumeEntity> {

    public ResumeRepository(DynamoDbEnhancedClient client) {
        super(client, ResumeEntity.class);
    }

    // Tìm Resume theo UserID (Quan hệ 1-1)
    // PK: USER#<userId>, SK: RESUME
    public ResumeEntity findByUserId(String userId) {
        String pk = "USER#" + userId;
        String sk = "RESUME";
        return findById(pk, sk);
    }
}