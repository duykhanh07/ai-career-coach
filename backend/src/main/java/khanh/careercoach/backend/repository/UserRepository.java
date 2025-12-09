package khanh.careercoach.backend.repository;

import khanh.careercoach.backend.model.UserEntity;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;

@Repository
public class UserRepository extends AbstractDynamoRepository<UserEntity> {

    public UserRepository(DynamoDbEnhancedClient client) {
        // Truyền Class type để Abstract Repository biết map vào object nào
        super(client, UserEntity.class);
    }

}