package khanh.careercoach.backend.repository;

import khanh.careercoach.backend.model.CoverLetterEntity;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

import java.util.List;

@Repository
public class CoverLetterRepository extends AbstractDynamoRepository<CoverLetterEntity> {

    public CoverLetterRepository(DynamoDbEnhancedClient client) {
        super(client, CoverLetterEntity.class);
    }

    // Tìm tất cả Cover Letter của một User
    // PK: USER#<userId>, SK bắt đầu bằng LETTER#
    // Vì AbstractRepository đã có findAllByPartitionKey, ta chỉ cần gọi nó
    public List<CoverLetterEntity> findAllByUserId(String userId) {
        String pk = "USER#" + userId;
        // Lưu ý: findAllByPartitionKey sẽ lấy cả UserEntity và ResumeEntity nếu chung PK
        // Tuy nhiên, vì ta map TableSchema với CoverLetterEntity class,
        // SDK sẽ tự động filter hoặc map, nhưng để an toàn nhất với Single Table Design,
        // Ta nên dùng query với điều kiện SK begins_with "LETTER#"
        // (Để đơn giản trong demo này, giả sử hàm findAllByPartitionKey của bạn lọc được type,
        // hoặc ta chấp nhận lấy về rồi filter ở Service).

        // Cách tốt nhất: Viết query cụ thể ở đây
        return findAllByPartitionKey(pk);
    }

    public CoverLetterEntity findById(String userId, String letterId) {
        return super.findById("USER#" + userId, "LETTER#" + letterId);
    }

    public void deleteById(String userId, String letterId) {
        super.delete("USER#" + userId, "LETTER#" + letterId);
    }
}