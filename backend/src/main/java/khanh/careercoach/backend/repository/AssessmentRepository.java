package khanh.careercoach.backend.repository;

import khanh.careercoach.backend.model.AssessmentEntity;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class AssessmentRepository extends AbstractDynamoRepository<AssessmentEntity> {

    public AssessmentRepository(DynamoDbEnhancedClient client) {
        super(client, AssessmentEntity.class);
    }

    // Lấy lịch sử làm bài của User
    public List<AssessmentEntity> findAllByUserId(String userId) {
        // Query theo PK = USER#<id>, sau đó lọc SK bắt đầu bằng ASSESS#
        // (Cách tối ưu hơn là dùng Query Conditional BeginsWith trong SDK,
        // nhưng dùng findAllByPartitionKey lọc mềm cũng ổn với số lượng ít)
        List<AssessmentEntity> allItems = findAllByPartitionKey("USER#" + userId);

        return allItems.stream()
                .filter(item -> item.getSk() != null && item.getSk().startsWith("ASSESS#"))
                // Sắp xếp theo ngày tạo (Mới nhất lên đầu) - Logic Java
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }
}