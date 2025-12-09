package khanh.careercoach.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class DynamoDBConfig {

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                // Chọn Region Singapore (khớp với template.yaml của bạn)
                .region(Region.AP_SOUTHEAST_1)

                // Dùng HTTP Client nhẹ để giảm Cold Start cho Lambda
                .httpClient(UrlConnectionHttpClient.builder().build())

                // Tự động lấy Credentials từ môi trường Lambda (IAM Role)
                // Hoặc từ file ~/.aws/credentials nếu chạy local
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        // Đây là client cấp cao giúp map UserEntity <-> Table
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}
